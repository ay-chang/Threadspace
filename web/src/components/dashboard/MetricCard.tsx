import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowUp, ArrowDown } from "lucide-react";

interface MetricCardProps {
    title: string;
    value: string | number;
    change?: {
        value: number;
        label: string;
    };
    icon?: React.ReactNode;
    status?: "success" | "warning" | "error" | "neutral";
}

export function MetricCard({ title, value, change, icon, status = "neutral" }: MetricCardProps) {
    const statusColors = {
        success: "text-green-600",
        warning: "text-yellow-600",
        error: "text-red-600",
        neutral: "text-gray-600",
    };

    const isPositive = change && change.value > 0;
    const isNegative = change && change.value < 0;

    return (
        <Card className="rounded-2xl border bg-white shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium text-gray-500">{title}</CardTitle>
                {icon && <div className="text-gray-400">{icon}</div>}
            </CardHeader>
            <CardContent>
                <div className="text-2xl font-bold">{value}</div>
                {change && (
                    <div className="mt-1 flex items-center text-xs">
                        {isPositive && <ArrowUp className="mr-1 h-3 w-3 text-green-600" />}
                        {isNegative && <ArrowDown className="mr-1 h-3 w-3 text-red-600" />}
                        <span
                            className={
                                isPositive ? "text-green-600" : isNegative ? "text-red-600" : "text-gray-600"
                            }
                        >
                            {Math.abs(change.value)}% {change.label}
                        </span>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
