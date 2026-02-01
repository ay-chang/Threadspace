"use client";

import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";

export interface DataPoint {
    timestamp: string;
    [key: string]: number | string;
}

interface TimeSeriesChartProps {
    title: string;
    description?: string;
    data: DataPoint[];
    dataKeys: {
        key: string;
        label: string;
        color: string;
    }[];
    yAxisLabel?: string;
    height?: number;
}

export function TimeSeriesChart({
    title,
    description,
    data,
    dataKeys,
    yAxisLabel,
    height = 300,
}: TimeSeriesChartProps) {
    return (
        <Card className="rounded-2xl border bg-white shadow-sm">
            <CardHeader>
                <CardTitle className="text-lg font-semibold">{title}</CardTitle>
                {description && (
                    <CardDescription className="text-sm text-gray-500">{description}</CardDescription>
                )}
            </CardHeader>
            <CardContent>
                <ResponsiveContainer width="100%" height={height}>
                    <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                        <XAxis
                            dataKey="timestamp"
                            stroke="#6b7280"
                            fontSize={12}
                            tickLine={false}
                            axisLine={false}
                        />
                        <YAxis
                            stroke="#6b7280"
                            fontSize={12}
                            tickLine={false}
                            axisLine={false}
                            label={yAxisLabel ? { value: yAxisLabel, angle: -90, position: "insideLeft" } : undefined}
                        />
                        <Tooltip
                            contentStyle={{
                                backgroundColor: "#fff",
                                border: "1px solid #e5e7eb",
                                borderRadius: "8px",
                                padding: "8px 12px",
                            }}
                            labelStyle={{ color: "#374151", fontWeight: 600 }}
                        />
                        <Legend
                            wrapperStyle={{ paddingTop: "20px" }}
                            iconType="line"
                            formatter={(value) => <span className="text-sm text-gray-600">{value}</span>}
                        />
                        {dataKeys.map((dataKey) => (
                            <Line
                                key={dataKey.key}
                                type="monotone"
                                dataKey={dataKey.key}
                                name={dataKey.label}
                                stroke={dataKey.color}
                                strokeWidth={2}
                                dot={false}
                                activeDot={{ r: 4 }}
                            />
                        ))}
                    </LineChart>
                </ResponsiveContainer>
            </CardContent>
        </Card>
    );
}
