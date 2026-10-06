"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Activity, Eye, MousePointerClick, Users } from "lucide-react";
import {
    Area,
    AreaChart,
    Bar,
    BarChart,
    CartesianGrid,
    Legend,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import { Notice, PageShell, PeriodSelector, StatCard, formatDay } from "../../_shared/metrics";

type GoogleAnalyticsMetrics = {
    propertyId: string;
    periodDays: number;
    timeZone: string;
    totals: {
        activeUsers: number;
        newUsers: number;
        sessions: number;
        pageViews: number;
        engagementRate: number;
        averageSessionDuration: number;
    };
    activeUsersNow: number | null;
    daily: { date: string; activeUsers: number; sessions: number; pageViews: number }[];
    topPages: { path: string; title: string; views: number; users: number }[];
    channels: { channel: string; sessions: number }[];
    countries: { country: string; users: number }[];
};

const GA_ORANGE = "#E37400";
const GA_YELLOW = "#F9AB00";
const PERIODS = [7, 28, 90];

function formatDuration(seconds: number): string {
    const total = Math.round(seconds);
    const m = Math.floor(total / 60);
    const s = total % 60;
    return m > 0 ? `${m}m ${s.toString().padStart(2, "0")}s` : `${s}s`;
}

export default function GoogleAnalyticsTrafficPage() {
    const { projectId } = useParams<{ projectId: string }>();
    const [days, setDays] = useState(28);
    const [metrics, setMetrics] = useState<GoogleAnalyticsMetrics | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchData = useCallback(async () => {
        if (!projectId) return;
        setIsLoading(true);
        setError(null);
        try {
            const res = await fetch(`/api/projects/${projectId}/google-analytics/metrics?days=${days}`);
            if (!res.ok) {
                const body = await res.json().catch(() => null);
                throw new Error(body?.message || "Failed to load Google Analytics data");
            }
            setMetrics(await res.json());
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to load Google Analytics data");
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
            <PageShell title="Google Analytics" actions={periodSelector}>
                <Notice tone="warning">{error}</Notice>
            </PageShell>
        );
    }

    if (isLoading && !metrics) {
        return <PageShell title="Google Analytics" subtitle="Loading Google Analytics data…" actions={periodSelector} />;
    }

    if (!metrics) return null;

    const { totals } = metrics;
    const noTraffic = totals.activeUsers === 0 && totals.sessions === 0;
    const channelTotal = metrics.channels.reduce((sum, c) => sum + c.sessions, 0);

    return (
        <PageShell
            title="Google Analytics"
            subtitle={`Property ${metrics.propertyId} · last ${metrics.periodDays} days${metrics.timeZone ? ` (${metrics.timeZone})` : ""}${isLoading ? " · refreshing…" : ""}`}
            actions={periodSelector}
        >
            <div className="space-y-6">
                {noTraffic && (
                    <Notice>
                        No traffic recorded in this period. If the site is live, check that the GA4 tag is
                        installed on it — new properties can also take up to a day before data appears.
                    </Notice>
                )}

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                    <StatCard
                        title="Active users"
                        icon={Users}
                        value={totals.activeUsers.toLocaleString()}
                        detail={
                            metrics.activeUsersNow !== null
                                ? `${metrics.activeUsersNow.toLocaleString()} right now · ${totals.newUsers.toLocaleString()} new`
                                : `${totals.newUsers.toLocaleString()} new`
                        }
                    />
                    <StatCard
                        title="Sessions"
                        icon={Activity}
                        value={totals.sessions.toLocaleString()}
                        detail={`Avg. ${formatDuration(totals.averageSessionDuration)} per session`}
                    />
                    <StatCard
                        title="Page views"
                        icon={Eye}
                        value={totals.pageViews.toLocaleString()}
                        detail={
                            totals.sessions > 0
                                ? `${(totals.pageViews / totals.sessions).toFixed(1)} per session`
                                : undefined
                        }
                    />
                    <StatCard
                        title="Engagement rate"
                        icon={MousePointerClick}
                        value={`${(totals.engagementRate * 100).toFixed(1)}%`}
                        detail="Sessions that were engaged"
                    />
                </div>

                <Card>
                    <CardHeader>
                        <CardTitle>Daily traffic</CardTitle>
                        <CardDescription>Active users and sessions per day, in the property&apos;s time zone</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <ResponsiveContainer width="100%" height={280}>
                            <AreaChart data={metrics.daily}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="date" tickFormatter={formatDay} minTickGap={16} />
                                <YAxis allowDecimals={false} width={48} />
                                <Tooltip labelFormatter={(label) => formatDay(String(label))} />
                                <Legend />
                                <Area
                                    type="monotone"
                                    dataKey="sessions"
                                    name="Sessions"
                                    stroke={GA_YELLOW}
                                    fill={GA_YELLOW}
                                    fillOpacity={0.25}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="activeUsers"
                                    name="Active users"
                                    stroke={GA_ORANGE}
                                    fill={GA_ORANGE}
                                    fillOpacity={0.3}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </CardContent>
                </Card>

                <div className="grid gap-6 xl:grid-cols-3">
                    <Card className="xl:col-span-2">
                        <CardHeader>
                            <CardTitle>Top pages</CardTitle>
                            <CardDescription>Most viewed pages in this period</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="overflow-x-auto">
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>Page</TableHead>
                                            <TableHead className="text-right">Views</TableHead>
                                            <TableHead className="text-right">Users</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {metrics.topPages.length === 0 ? (
                                            <TableRow>
                                                <TableCell colSpan={3} className="text-muted-foreground text-center">
                                                    No page views in this period
                                                </TableCell>
                                            </TableRow>
                                        ) : (
                                            metrics.topPages.map((page) => (
                                                <TableRow key={`${page.path}|${page.title}`}>
                                                    <TableCell className="max-w-[22rem]">
                                                        <div className="truncate font-medium">{page.title || page.path}</div>
                                                        <div className="text-muted-foreground truncate font-mono text-xs">
                                                            {page.path}
                                                        </div>
                                                    </TableCell>
                                                    <TableCell className="text-right tabular-nums">
                                                        {page.views.toLocaleString()}
                                                    </TableCell>
                                                    <TableCell className="text-right tabular-nums">
                                                        {page.users.toLocaleString()}
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
                            <CardTitle>Where visitors come from</CardTitle>
                            <CardDescription>Sessions by channel</CardDescription>
                        </CardHeader>
                        <CardContent>
                            {metrics.channels.length === 0 ? (
                                <p className="text-muted-foreground text-sm">No sessions in this period</p>
                            ) : (
                                <ResponsiveContainer width="100%" height={Math.max(160, metrics.channels.length * 34)}>
                                    <BarChart data={metrics.channels} layout="vertical" margin={{ left: 8 }}>
                                        <XAxis type="number" hide />
                                        <YAxis type="category" dataKey="channel" width={110} tick={{ fontSize: 12 }} />
                                        <Tooltip
                                            formatter={(value) => [
                                                `${Number(value).toLocaleString()} (${channelTotal > 0 ? ((Number(value) / channelTotal) * 100).toFixed(0) : 0}%)`,
                                                "Sessions",
                                            ]}
                                        />
                                        <Bar dataKey="sessions" fill={GA_ORANGE} radius={[0, 3, 3, 0]} />
                                    </BarChart>
                                </ResponsiveContainer>
                            )}
                        </CardContent>
                    </Card>
                </div>

                <Card>
                    <CardHeader>
                        <CardTitle>Top countries</CardTitle>
                        <CardDescription>Active users by country</CardDescription>
                    </CardHeader>
                    <CardContent>
                        {metrics.countries.length === 0 ? (
                            <p className="text-muted-foreground text-sm">No users in this period</p>
                        ) : (
                            <div className="grid gap-x-8 gap-y-2 sm:grid-cols-2">
                                {metrics.countries.map((c) => (
                                    <div key={c.country} className="flex items-center justify-between gap-3 text-sm">
                                        <span className="truncate">{c.country || "(not set)"}</span>
                                        <span className="font-medium tabular-nums">{c.users.toLocaleString()}</span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>

                <p className="text-muted-foreground text-xs">
                    Figures come from the GA4 Data API and match the Reports section of Google Analytics.
                    Today&apos;s numbers are partial and can take a few hours to settle.
                </p>
            </div>
        </PageShell>
    );
}
