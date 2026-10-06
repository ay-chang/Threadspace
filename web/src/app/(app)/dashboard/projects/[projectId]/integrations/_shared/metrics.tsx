"use client";

import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

/** Building blocks shared by the per-integration metrics pages. */

export function formatMoney(amount: number, currency: string): string {
    try {
        // Intl knows each currency's decimals, so JPY renders without cents.
        return new Intl.NumberFormat(undefined, {
            style: "currency",
            currency: currency.toUpperCase(),
        }).format(amount);
    } catch {
        return `${amount.toLocaleString()} ${currency.toUpperCase()}`;
    }
}

/** Formats a YYYY-MM-DD report date without shifting it through the local timezone. */
export function formatDay(isoDate: string): string {
    return new Date(`${isoDate}T00:00:00Z`).toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        timeZone: "UTC",
    });
}

export function PeriodSelector({
    options,
    value,
    onChange,
    disabled,
}: {
    options: number[];
    value: number;
    onChange: (days: number) => void;
    disabled?: boolean;
}) {
    return (
        <div className="flex gap-1" role="group" aria-label="Reporting period">
            {options.map((days) => (
                <Button
                    key={days}
                    size="sm"
                    variant={days === value ? "default" : "outline"}
                    onClick={() => onChange(days)}
                    disabled={disabled}
                    aria-pressed={days === value}
                >
                    {days}d
                </Button>
            ))}
        </div>
    );
}

export function StatCard({
    title,
    value,
    detail,
    icon: Icon,
}: {
    title: string;
    value: React.ReactNode;
    detail?: React.ReactNode;
    icon: React.ComponentType<{ className?: string }>;
}) {
    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">{title}</CardTitle>
                <Icon className="text-muted-foreground h-4 w-4" />
            </CardHeader>
            <CardContent>
                <div className="text-2xl font-bold tabular-nums">{value}</div>
                {detail && <div className="text-muted-foreground mt-1 text-xs">{detail}</div>}
            </CardContent>
        </Card>
    );
}

export function Notice({
    tone = "info",
    children,
}: {
    tone?: "info" | "warning";
    children: React.ReactNode;
}) {
    const styles =
        tone === "warning"
            ? "border-amber-300 bg-amber-50 text-amber-900 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-200"
            : "border-border bg-muted/50 text-foreground";
    return <div className={`rounded-md border px-4 py-3 text-sm ${styles}`}>{children}</div>;
}

export function PageShell({
    title,
    subtitle,
    actions,
    children,
}: {
    title: React.ReactNode;
    subtitle?: React.ReactNode;
    actions?: React.ReactNode;
    children?: React.ReactNode;
}) {
    return (
        <div className="w-full px-4 py-6 sm:px-6 lg:px-10">
            <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div>
                    <h1 className="flex items-center gap-2 text-2xl font-semibold">{title}</h1>
                    {subtitle && <p className="text-muted-foreground text-sm">{subtitle}</p>}
                </div>
                {actions}
            </div>
            {children}
        </div>
    );
}
