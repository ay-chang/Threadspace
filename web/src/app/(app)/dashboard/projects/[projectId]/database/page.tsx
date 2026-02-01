"use client";

import { useState, useEffect } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MetricCard } from "@/components/dashboard/MetricCard";
import { TimeSeriesChart } from "@/components/dashboard/TimeSeriesChart";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Database, Activity, HardDrive, Network } from "lucide-react";

type RdsInstance = {
    instanceId: string;
    instanceClass: string;
    engine: string;
    status: string;
    storageGB: number;
    multiAz: boolean;
    availabilityZone: string;
};

type MetricDataPoint = {
    timestamp: string;
    value: number;
};

type RdsMetrics = {
    totalDatabases: number;
    activeConnections: number;
    storageUsedGB: number;
    avgCpuUtilization: number;
    cpuData: MetricDataPoint[];
    connectionsData: MetricDataPoint[];
    readIopsData: MetricDataPoint[];
    writeIopsData: MetricDataPoint[];
};

export default function DatabasePage({
    params,
}: {
    params: Promise<{ projectId: string }>;
}) {
    const [projectId, setProjectId] = useState<string>("");
    const [instances, setInstances] = useState<RdsInstance[]>([]);
    const [metrics, setMetrics] = useState<RdsMetrics | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        params.then((p) => setProjectId(p.projectId));
    }, [params]);

    useEffect(() => {
        if (projectId) {
            fetchData();
        }
    }, [projectId]);

    const fetchData = async () => {
        setIsLoading(true);
        setError(null);

        try {
            const [instancesRes, metricsRes] = await Promise.all([
                fetch(`/api/projects/${projectId}/aws/rds/instances`),
                fetch(`/api/projects/${projectId}/aws/rds/metrics`),
            ]);

            if (!instancesRes.ok || !metricsRes.ok) {
                const errorMsg = !instancesRes.ok
                    ? await instancesRes.text()
                    : await metricsRes.text();
                throw new Error(errorMsg || "Failed to fetch RDS data");
            }

            const instancesData = await instancesRes.json();
            const metricsData = await metricsRes.json();

            setInstances(instancesData);
            setMetrics(metricsData);
        } catch (err) {
            console.error("Failed to fetch RDS data:", err);
            setError(err instanceof Error ? err.message : "Failed to fetch AWS data");
        } finally {
            setIsLoading(false);
        }
    };

    const getStatusBadge = (status: string) => {
        const variants: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
            available: "default",
            "backing-up": "secondary",
            maintenance: "outline",
        };
        return (
            <Badge variant={variants[status] || "default"} className="capitalize">
                {status}
            </Badge>
        );
    };

    const formatTimestamp = (timestamp: string) => {
        const date = new Date(timestamp);
        return date.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" });
    };

    // Transform metrics data for charts
    const cpuChartData = metrics?.cpuData.map((d) => ({
        timestamp: formatTimestamp(d.timestamp),
        cpu: Math.round(d.value * 100) / 100,
    })) || [];

    const connectionsChartData = metrics?.connectionsData.map((d) => ({
        timestamp: formatTimestamp(d.timestamp),
        connections: Math.round(d.value),
    })) || [];

    const iopsChartData = metrics
        ? metrics.readIopsData.map((d, i) => ({
              timestamp: formatTimestamp(d.timestamp),
              read: Math.round(d.value),
              write: Math.round(metrics.writeIopsData[i]?.value || 0),
          }))
        : [];

    if (isLoading) {
        return (
            <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
                <div className="mb-6">
                    <h1 className="text-2xl font-semibold">Database Dashboard</h1>
                    <p className="text-sm text-gray-500">Loading AWS RDS data...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
                <div className="mb-6">
                    <h1 className="text-2xl font-semibold">Database Dashboard</h1>
                    <p className="text-sm text-red-500">{error}</p>
                    <p className="text-xs text-gray-500 mt-2">
                        Make sure you have connected your AWS integration with valid credentials.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
            {/* Header */}
            <div className="mb-6">
                <h1 className="text-2xl font-semibold">Database Dashboard</h1>
                <p className="text-sm text-gray-500">
                    Monitor your AWS RDS database instances and performance metrics
                </p>
            </div>

            {/* Content */}
            <div className="">
                <Tabs defaultValue="overview" className="w-full">
                    <TabsList className="mb-6">
                        <TabsTrigger value="overview">Overview</TabsTrigger>
                        <TabsTrigger value="instances">Instances</TabsTrigger>
                        <TabsTrigger value="performance">Performance</TabsTrigger>
                    </TabsList>

                    <TabsContent value="overview">
                        <div className="space-y-6">
                            {/* Metric Cards Row */}
                            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                                <MetricCard
                                    title="Total Databases"
                                    value={metrics?.totalDatabases || 0}
                                    icon={<Database className="h-4 w-4" />}
                                />
                                <MetricCard
                                    title="Active Connections"
                                    value={metrics?.activeConnections || 0}
                                    icon={<Activity className="h-4 w-4" />}
                                />
                                <MetricCard
                                    title="Storage Used"
                                    value={`${metrics?.storageUsedGB || 0} GB`}
                                    icon={<HardDrive className="h-4 w-4" />}
                                />
                                <MetricCard
                                    title="Avg CPU Utilization"
                                    value={`${Math.round(metrics?.avgCpuUtilization || 0)}%`}
                                    icon={<Network className="h-4 w-4" />}
                                />
                            </div>

                            {/* Charts Section */}
                            <div className="grid gap-6 md:grid-cols-2">
                                <TimeSeriesChart
                                    title="CPU Utilization"
                                    description="Last 24 hours"
                                    data={cpuChartData}
                                    dataKeys={[{ key: "cpu", label: "CPU %", color: "#3b82f6" }]}
                                    yAxisLabel="CPU %"
                                />
                                <TimeSeriesChart
                                    title="Database Connections"
                                    description="Last 24 hours"
                                    data={connectionsChartData}
                                    dataKeys={[{ key: "connections", label: "Connections", color: "#10b981" }]}
                                    yAxisLabel="Count"
                                />
                            </div>

                            <div className="grid gap-6 md:grid-cols-2">
                                <TimeSeriesChart
                                    title="IOPS (Input/Output Operations)"
                                    description="Last 24 hours"
                                    data={iopsChartData}
                                    dataKeys={[
                                        { key: "read", label: "Read IOPS", color: "#8b5cf6" },
                                        { key: "write", label: "Write IOPS", color: "#f59e0b" },
                                    ]}
                                    yAxisLabel="IOPS"
                                />
                        </div>
                    </TabsContent>

                    <TabsContent value="instances">
                        <div className="rounded-2xl border bg-white shadow-sm">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Instance ID</TableHead>
                                        <TableHead>Name</TableHead>
                                        <TableHead>Engine</TableHead>
                                        <TableHead>Status</TableHead>
                                        <TableHead>Size</TableHead>
                                        <TableHead>Storage</TableHead>
                                        <TableHead>Multi-AZ</TableHead>
                                        <TableHead className="text-right">Connections</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {instances.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={8} className="text-center text-gray-500">
                                                No RDS instances found
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        instances.map((instance) => (
                                            <TableRow key={instance.instanceId}>
                                                <TableCell className="font-mono text-sm">
                                                    {instance.instanceId}
                                                </TableCell>
                                                <TableCell className="font-medium">
                                                    {instance.instanceId}
                                                </TableCell>
                                                <TableCell>{instance.engine}</TableCell>
                                                <TableCell>{getStatusBadge(instance.status)}</TableCell>
                                                <TableCell>{instance.instanceClass}</TableCell>
                                                <TableCell>{instance.storageGB} GB</TableCell>
                                                <TableCell>
                                                    <Badge variant={instance.multiAz ? "default" : "outline"}>
                                                        {instance.multiAz ? "Yes" : "No"}
                                                    </Badge>
                                                </TableCell>
                                                <TableCell className="text-right">
                                                    {instance.availabilityZone}
                                                </TableCell>
                                            </TableRow>
                                        ))
                                    )}
                                </TableBody>
                            </Table>
                        </div>
                    </TabsContent>

                    <TabsContent value="performance">
                        <div className="space-y-6">
                            <div className="grid gap-6 md:grid-cols-2">
                                <TimeSeriesChart
                                    title="CPU Utilization Over Time"
                                    description="Detailed CPU metrics"
                                    data={cpuChartData}
                                    dataKeys={[{ key: "cpu", label: "CPU %", color: "#3b82f6" }]}
                                    yAxisLabel="CPU %"
                                    height={350}
                                />
                                <TimeSeriesChart
                                    title="Database Connections Over Time"
                                    description="Connection pool usage"
                                    data={connectionsChartData}
                                    dataKeys={[{ key: "connections", label: "Active Connections", color: "#10b981" }]}
                                    yAxisLabel="Connections"
                                    height={350}
                                />
                            </div>
                            <TimeSeriesChart
                                title="Read vs Write IOPS"
                                description="Input/Output operations per second"
                                data={iopsChartData}
                                dataKeys={[
                                    { key: "read", label: "Read IOPS", color: "#8b5cf6" },
                                    { key: "write", label: "Write IOPS", color: "#f59e0b" },
                                ]}
                                yAxisLabel="IOPS"
                                height={350}
                            />
                        </div>
                    </TabsContent>
                </Tabs>
            </div>
        </div>
    );
}

