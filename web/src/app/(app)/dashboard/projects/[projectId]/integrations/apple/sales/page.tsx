"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Coins, Download, RefreshCw, ShoppingBag } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import {
    Notice,
    PageShell,
    PeriodSelector,
    StatCard,
    formatDay,
    formatMoney,
} from "../../_shared/metrics";

type AppStoreMetrics = {
    periodDays: number;
    latestReportDate: string | null;
    apps: { id: string; name: string; bundleId: string }[];
    daily: {
        date: string;
        hasReport: boolean;
        downloads: number;
        redownloads: number;
        updates: number;
        inAppPurchases: number;
    }[];
    totalDownloads: number;
    totalRedownloads: number;
    totalUpdates: number;
    totalInAppPurchases: number;
    proceeds: { currency: string; amount: number }[];
    topTitles: { title: string; downloads: number; inAppPurchases: number; updates: number }[];
    daysWithoutReport: number;
};

const APP_STORE_BLUE = "#0071e3";
const IAP_GREEN = "#34c759";
const PERIODS = [7, 14, 30];

export default function AppStoreSalesPage() {
    const { projectId } = useParams<{ projectId: string }>();
    const [days, setDays] = useState(14);
    const [metrics, setMetrics] = useState<AppStoreMetrics | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchData = useCallback(async () => {
        if (!projectId) return;
        setIsLoading(true);
        setError(null);
        try {
            const res = await fetch(`/api/projects/${projectId}/appstore/metrics?days=${days}`);
            if (!res.ok) {
                const body = await res.json().catch(() => null);
                throw new Error(body?.message || "Failed to load App Store Connect data");
            }
            setMetrics(await res.json());
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to load App Store Connect data");
        } finally {
            setIsLoading(false);
        }
    }, [projectId, days]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const periodSelector = (
        <PeriodSelector options={PERIODS} value={days} onChange={setDays} disabled={isLoading} />
    );

    if (error) {
        return (
            <PageShell title="App Store Connect" actions={periodSelector}>
                <Notice tone="warning">{error}</Notice>
            </PageShell>
        );
    }

    if (isLoading && !metrics) {
        return (
            <PageShell
                title="App Store Connect"
                subtitle="Downloading daily sales reports from Apple — this can take a few seconds…"
                actions={periodSelector}
            />
        );
    }

    if (!metrics) return null;

    const noReportsAtAll = metrics.daysWithoutReport === metrics.periodDays;
    const chartData = metrics.daily.map((d) => ({ ...d, label: d.date }));

    return (
        <PageShell
            title="App Store Connect"
            subtitle={
                metrics.latestReportDate
                    ? `Sales over the last ${metrics.periodDays} days · reports through ${formatDay(metrics.latestReportDate)} (Pacific time)${isLoading ? " · refreshing…" : ""}`
                    : `Sales over the last ${metrics.periodDays} days`
            }
            actions={periodSelector}
        >
            <div className="space-y-6">
                {noReportsAtAll ? (
                    <Notice>
                        Apple hasn&apos;t published any sales reports for this period. That happens when
                        there were no downloads or purchases, or for a brand-new app. Apple publishes each
                        day&apos;s report roughly a day later, so recent activity can take up to 48 hours
                        to appear here.
                    </Notice>
                ) : (
                    metrics.daysWithoutReport > 0 && (
                        <Notice>
                            {metrics.daysWithoutReport} day{metrics.daysWithoutReport === 1 ? "" : "s"} in this
                            period {metrics.daysWithoutReport === 1 ? "has" : "have"} no report — either no
                            sales that day, or Apple hasn&apos;t published it yet. Reports usually arrive about
                            a day late.
                        </Notice>
                    )
                )}

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                    <StatCard
                        title="First-time downloads"
                        icon={Download}
                        value={metrics.totalDownloads.toLocaleString()}
                        detail={`${metrics.totalRedownloads.toLocaleString()} re-downloads`}
                    />
                    <StatCard
                        title="In-app purchases"
                        icon={ShoppingBag}
                        value={metrics.totalInAppPurchases.toLocaleString()}
                        detail="Net of refunds"
                    />
                    <StatCard
                        title="Updates"
                        icon={RefreshCw}
                        value={metrics.totalUpdates.toLocaleString()}
                        detail="Existing users updating"
                    />
                    <StatCard
                        title="Proceeds"
                        icon={Coins}
                        value={
                            metrics.proceeds.length > 0
                                ? formatMoney(metrics.proceeds[0].amount, metrics.proceeds[0].currency)
                                : "—"
                        }
                        detail={
                            metrics.proceeds.length > 1
                                ? metrics.proceeds
                                      .slice(1)
                                      .map((p) => formatMoney(p.amount, p.currency))
                                      .join(" · ")
                                : "After Apple's commission"
                        }
                    />
                </div>

                <Card>
                    <CardHeader>
                        <CardTitle>Daily units</CardTitle>
                        <CardDescription>First-time downloads and in-app purchases per report day</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <ResponsiveContainer width="100%" height={280}>
                            <BarChart data={chartData}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="label" tickFormatter={formatDay} minTickGap={16} />
                                <YAxis allowDecimals={false} width={48} />
                                <Tooltip
                                    labelFormatter={(label, payload) => {
                                        const day = payload?.[0]?.payload as { hasReport?: boolean } | undefined;
                                        const text = formatDay(String(label));
                                        return day && !day.hasReport ? `${text} · no report` : text;
                                    }}
                                />
                                <Legend />
                                <Bar dataKey="downloads" name="Downloads" stackId="units" fill={APP_STORE_BLUE} />
                                <Bar
                                    dataKey="inAppPurchases"
                                    name="In-app purchases"
                                    stackId="units"
                                    fill={IAP_GREEN}
                                    radius={[3, 3, 0, 0]}
                                />
                            </BarChart>
                        </ResponsiveContainer>
                    </CardContent>
                </Card>

                <div className="grid gap-6 xl:grid-cols-2">
                    <Card>
                        <CardHeader>
                            <CardTitle>Top titles</CardTitle>
                            <CardDescription>Apps and in-app purchases by units in this period</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="overflow-x-auto">
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>Title</TableHead>
                                            <TableHead className="text-right">Downloads</TableHead>
                                            <TableHead className="text-right">Purchases</TableHead>
                                            <TableHead className="text-right">Updates</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {metrics.topTitles.length === 0 ? (
                                            <TableRow>
                                                <TableCell colSpan={4} className="text-muted-foreground text-center">
                                                    No sales in this period
                                                </TableCell>
                                            </TableRow>
                                        ) : (
                                            metrics.topTitles.map((t) => (
                                                <TableRow key={t.title}>
                                                    <TableCell className="font-medium">{t.title}</TableCell>
                                                    <TableCell className="text-right tabular-nums">
                                                        {t.downloads.toLocaleString()}
                                                    </TableCell>
                                                    <TableCell className="text-right tabular-nums">
                                                        {t.inAppPurchases.toLocaleString()}
                                                    </TableCell>
                                                    <TableCell className="text-right tabular-nums">
                                                        {t.updates.toLocaleString()}
                                                    </TableCell>
                                                </TableRow>
                                            ))
                                        )}
                                    </TableBody>
                                </Table>
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle>Your apps</CardTitle>
                            <CardDescription>
                                {metrics.apps.length} app{metrics.apps.length === 1 ? "" : "s"} visible to this API key
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="overflow-x-auto">
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>Name</TableHead>
                                            <TableHead>Bundle ID</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {metrics.apps.length === 0 ? (
                                            <TableRow>
                                                <TableCell colSpan={2} className="text-muted-foreground text-center">
                                                    No apps found
                                                </TableCell>
                                            </TableRow>
                                        ) : (
                                            metrics.apps.map((app) => (
                                                <TableRow key={app.id}>
                                                    <TableCell className="font-medium">{app.name}</TableCell>
                                                    <TableCell className="text-muted-foreground font-mono text-xs">
                                                        {app.bundleId}
                                                    </TableCell>
                                                </TableRow>
                                            ))
                                        )}
                                    </TableBody>
                                </Table>
                            </div>
                        </CardContent>
                    </Card>
                </div>

                <p className="text-muted-foreground text-xs">
                    Figures come from Apple&apos;s daily summary sales reports, dated in Pacific time.
                    Proceeds are what you receive after Apple&apos;s commission, before tax, in each
                    report&apos;s payout currency — they&apos;re never converted or added across currencies.
                </p>
            </div>
        </PageShell>
    );
}
