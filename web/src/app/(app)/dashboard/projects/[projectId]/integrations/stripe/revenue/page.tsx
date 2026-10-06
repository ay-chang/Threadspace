"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { DollarSign, Repeat, UserPlus, Wallet } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import {
    Notice,
    PageShell,
    PeriodSelector,
    StatCard,
    formatDay,
    formatMoney,
} from "../../_shared/metrics";

type CurrencyAmount = { currency: string; amount: number };

type StripeMetrics = {
    livemode: boolean;
    periodDays: number;
    primaryCurrency: string;
    availableBalance: CurrencyAmount[];
    pendingBalance: CurrencyAmount[];
    volume: {
        currency: string;
        gross: number;
        refunded: number;
        net: number;
        successfulPayments: number;
    }[];
    dailyRevenue: { date: string; amount: number; payments: number }[];
    activeSubscriptions: number;
    mrr: CurrencyAmount[];
    unpricedSubscriptionItems: number;
    newCustomers: number;
    failedPayments: number;
    recentCharges: {
        id: string;
        amount: number;
        currency: string;
        status: string;
        refunded: boolean;
        created: number;
        description: string | null;
    }[];
    truncated: boolean;
};

const STRIPE_PURPLE = "#635BFF";
const PERIODS = [7, 30, 90];

const STATUS_STYLES: Record<string, string> = {
    succeeded: "bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300",
    failed: "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300",
    pending: "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300",
};

function inCurrency(amounts: CurrencyAmount[], currency: string): CurrencyAmount | undefined {
    return amounts.find((a) => a.currency === currency) ?? amounts[0];
}

export default function StripeRevenuePage() {
    const { projectId } = useParams<{ projectId: string }>();
    const [days, setDays] = useState(30);
    const [metrics, setMetrics] = useState<StripeMetrics | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchData = useCallback(async () => {
        if (!projectId) return;
        setIsLoading(true);
        setError(null);
        try {
            const res = await fetch(`/api/projects/${projectId}/stripe/metrics?days=${days}`);
            if (!res.ok) {
                const body = await res.json().catch(() => null);
                throw new Error(body?.message || "Failed to load Stripe data");
            }
            setMetrics(await res.json());
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to load Stripe data");
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
            <PageShell title="Stripe" actions={periodSelector}>
                <Notice tone="warning">{error}</Notice>
            </PageShell>
        );
    }

    if (isLoading && !metrics) {
        return (
            <PageShell title="Stripe" subtitle="Loading Stripe data…" actions={periodSelector} />
        );
    }

    if (!metrics) return null;

    const currency = metrics.primaryCurrency;
    const volume = metrics.volume.find((v) => v.currency === currency);
    const mrr = inCurrency(metrics.mrr, currency);
    const available = inCurrency(metrics.availableBalance, currency);
    const pending = inCurrency(metrics.pendingBalance, currency);
    const otherCurrencies = metrics.volume.filter((v) => v.currency !== currency);

    return (
        <PageShell
            title={
                <>
                    Stripe
                    {!metrics.livemode && <Badge variant="outline">Test mode</Badge>}
                </>
            }
            subtitle={`Revenue and payments over the last ${metrics.periodDays} days${isLoading ? " · refreshing…" : ""}`}
            actions={periodSelector}
        >
            <div className="space-y-6">
                {metrics.truncated && (
                    <Notice tone="warning">
                        This account has more activity than Threadspace reads in one go, so these
                        totals cover the most recent 1,000 payments, subscriptions and customers.
                        Try a shorter period for complete figures.
                    </Notice>
                )}

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                    <StatCard
                        title="Net volume"
                        icon={DollarSign}
                        value={formatMoney(volume?.net ?? 0, currency)}
                        detail={
                            <>
                                {volume?.successfulPayments ?? 0} successful payments
                                {metrics.failedPayments > 0 && ` · ${metrics.failedPayments} failed`}
                            </>
                        }
                    />
                    <StatCard
                        title="Monthly recurring revenue"
                        icon={Repeat}
                        value={mrr ? formatMoney(mrr.amount, mrr.currency) : "—"}
                        detail={`${metrics.activeSubscriptions} active subscription${metrics.activeSubscriptions === 1 ? "" : "s"}`}
                    />
                    <StatCard
                        title="New customers"
                        icon={UserPlus}
                        value={metrics.newCustomers.toLocaleString()}
                        detail={`In the last ${metrics.periodDays} days`}
                    />
                    <StatCard
                        title="Available balance"
                        icon={Wallet}
                        value={available ? formatMoney(available.amount, available.currency) : "—"}
                        detail={pending ? `${formatMoney(pending.amount, pending.currency)} pending` : undefined}
                    />
                </div>

                <Card>
                    <CardHeader>
                        <CardTitle>Daily net revenue</CardTitle>
                        <CardDescription>
                            {currency.toUpperCase()}, after refunds, by UTC day
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <ResponsiveContainer width="100%" height={280}>
                            <BarChart data={metrics.dailyRevenue}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="date" tickFormatter={formatDay} minTickGap={16} />
                                <YAxis
                                    tickFormatter={(v: number) => formatMoney(v, currency)}
                                    width={90}
                                />
                                <Tooltip
                                    labelFormatter={(label) => formatDay(String(label))}
                                    formatter={(value) => [formatMoney(Number(value), currency), "Net revenue"]}
                                />
                                <Bar dataKey="amount" fill={STRIPE_PURPLE} radius={[3, 3, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </CardContent>
                </Card>

                <div className="grid gap-6 xl:grid-cols-3">
                    <Card className="xl:col-span-2">
                        <CardHeader>
                            <CardTitle>Recent payments</CardTitle>
                            <CardDescription>The latest charges, including failures</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="overflow-x-auto">
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>Amount</TableHead>
                                            <TableHead>Status</TableHead>
                                            <TableHead>Description</TableHead>
                                            <TableHead className="text-right">Date</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {metrics.recentCharges.length === 0 ? (
                                            <TableRow>
                                                <TableCell colSpan={4} className="text-muted-foreground text-center">
                                                    No payments in this period
                                                </TableCell>
                                            </TableRow>
                                        ) : (
                                            metrics.recentCharges.map((charge) => (
                                                <TableRow key={charge.id}>
                                                    <TableCell className="font-medium tabular-nums">
                                                        {formatMoney(charge.amount, charge.currency)}
                                                    </TableCell>
                                                    <TableCell>
                                                        <span
                                                            className={`rounded px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[charge.status] ?? "bg-muted"}`}
                                                        >
                                                            {charge.refunded ? "refunded" : charge.status}
                                                        </span>
                                                    </TableCell>
                                                    <TableCell className="text-muted-foreground max-w-[16rem] truncate">
                                                        {charge.description ?? "—"}
                                                    </TableCell>
                                                    <TableCell className="text-muted-foreground text-right">
                                                        {new Date(charge.created * 1000).toLocaleDateString()}
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
                            <CardTitle>Breakdown</CardTitle>
                            <CardDescription>Gross, refunds and net for {currency.toUpperCase()}</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-3 text-sm">
                            <Row label="Gross volume" value={formatMoney(volume?.gross ?? 0, currency)} />
                            <Row label="Refunded" value={formatMoney(-(volume?.refunded ?? 0), currency)} />
                            <Row label="Net volume" value={formatMoney(volume?.net ?? 0, currency)} strong />

                            {otherCurrencies.length > 0 && (
                                <div className="border-t pt-3">
                                    <p className="text-muted-foreground mb-2 text-xs">Other currencies (net)</p>
                                    {otherCurrencies.map((v) => (
                                        <Row
                                            key={v.currency}
                                            label={`${v.currency.toUpperCase()} · ${v.successfulPayments} payments`}
                                            value={formatMoney(v.net, v.currency)}
                                        />
                                    ))}
                                </div>
                            )}
                            {metrics.mrr.length > 1 && (
                                <div className="border-t pt-3">
                                    <p className="text-muted-foreground mb-2 text-xs">MRR by currency</p>
                                    {metrics.mrr.map((m) => (
                                        <Row
                                            key={m.currency}
                                            label={m.currency.toUpperCase()}
                                            value={formatMoney(m.amount, m.currency)}
                                        />
                                    ))}
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </div>

                <p className="text-muted-foreground text-xs">
                    Volume is shown before Stripe fees. MRR is calculated from active subscriptions
                    before discounts and tax, with every billing interval converted to a monthly amount.
                    {metrics.unpricedSubscriptionItems > 0 &&
                        ` ${metrics.unpricedSubscriptionItems} usage-based subscription item${metrics.unpricedSubscriptionItems === 1 ? " isn't" : "s aren't"} included, since their monthly amount depends on usage.`}
                </p>
            </div>
        </PageShell>
    );
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
    return (
        <div className="flex items-center justify-between gap-3">
            <span className="text-muted-foreground">{label}</span>
            <span className={`tabular-nums ${strong ? "font-semibold" : "font-medium"}`}>{value}</span>
        </div>
    );
}
