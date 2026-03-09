"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams } from "next/navigation";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Database, Activity, HardDrive } from "lucide-react";
import { BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";

type GcsBucketInfo = {
    name: string;
    creationDate: string;
    objectCount: number;
    sizeBytes: number;
    location: string;
};

type StorageMetrics = {
    totalBuckets: number;
    totalObjects: number;
    totalStorageGB: number;
    buckets: GcsBucketInfo[];
};

const COLORS = ["#4285f4", "#34a853", "#fbbc05", "#ea4335", "#9334e6", "#ff6d01"];

export default function GcsStorageDashboardPage() {
    const { projectId } = useParams<{ projectId: string }>();
    const [metrics, setMetrics] = useState<StorageMetrics | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchData = useCallback(async () => {
        if (!projectId) return;

        setIsLoading(true);
        setError(null);

        try {
            const metricsRes = await fetch(`/api/projects/${projectId}/gcp/storage/metrics`);

            if (!metricsRes.ok) {
                const errorMsg = await metricsRes.text();
                throw new Error(errorMsg || "Failed to fetch Cloud Storage data");
            }

            const metricsData = await metricsRes.json();
            setMetrics(metricsData);
        } catch (err) {
            console.error("Failed to fetch Cloud Storage data:", err);
            setError(err instanceof Error ? err.message : "Failed to fetch Google Cloud data");
        } finally {
            setIsLoading(false);
        }
    }, [projectId]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const formatBytes = (bytes: number) => {
        if (bytes === 0) return "0 Bytes";
        const k = 1024;
        const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
    };

    const bucketSizeData = metrics?.buckets
        .sort((a, b) => b.sizeBytes - a.sizeBytes)
        .slice(0, 5)
        .map((bucket) => ({
            name: bucket.name.length > 15 ? bucket.name.substring(0, 12) + "..." : bucket.name,
            size: Number((bucket.sizeBytes / (1024 * 1024 * 1024)).toFixed(2)),
        })) || [];

    const bucketObjectsData = metrics?.buckets
        .sort((a, b) => b.objectCount - a.objectCount)
        .slice(0, 5)
        .map((bucket) => ({
            name: bucket.name.length > 15 ? bucket.name.substring(0, 12) + "..." : bucket.name,
            objects: bucket.objectCount,
        })) || [];

    const locationDistribution = metrics?.buckets.reduce((acc, bucket) => {
        acc[bucket.location] = (acc[bucket.location] || 0) + 1;
        return acc;
    }, {} as Record<string, number>) || {};

    const locationData = Object.entries(locationDistribution).map(([location, count]) => ({
        name: location,
        value: count,
    }));

    if (isLoading) {
        return (
            <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
                <div className="mb-6">
                    <h1 className="text-2xl font-semibold">Cloud Storage</h1>
                    <p className="text-sm text-gray-500">Loading Google Cloud Storage data...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
                <div className="mb-6">
                    <h1 className="text-2xl font-semibold">Cloud Storage</h1>
                    <p className="text-sm text-red-500">{error}</p>
                    <p className="text-xs text-gray-500 mt-2">
                        Make sure you have connected your Google Cloud integration with valid credentials.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
            {/* Header */}
            <div className="mb-6">
                <h1 className="text-2xl font-semibold">Cloud Storage</h1>
                <p className="text-sm text-gray-500">
                    Monitor your Google Cloud Storage buckets and metrics
                </p>
            </div>

            {/* Content */}
            <Tabs defaultValue="overview" className="w-full">
                <TabsList className="mb-6">
                    <TabsTrigger value="overview">Overview</TabsTrigger>
                    <TabsTrigger value="buckets">Buckets</TabsTrigger>
                    <TabsTrigger value="analytics">Analytics</TabsTrigger>
                </TabsList>

                <TabsContent value="overview" className="space-y-6">
                    {/* Metric Cards Row */}
                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Total Buckets</CardTitle>
                                <Database className="h-4 w-4 text-muted-foreground" />
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">{metrics?.totalBuckets || 0}</div>
                                <p className="text-xs text-muted-foreground mt-1">
                                    Across all locations
                                </p>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Total Objects</CardTitle>
                                <Activity className="h-4 w-4 text-muted-foreground" />
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">
                                    {metrics?.totalObjects.toLocaleString() || 0}
                                </div>
                                <p className="text-xs text-muted-foreground mt-1">
                                    Files stored
                                </p>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Total Storage</CardTitle>
                                <HardDrive className="h-4 w-4 text-muted-foreground" />
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">
                                    {Math.round((metrics?.totalStorageGB || 0) * 100) / 100} GB
                                </div>
                                <p className="text-xs text-muted-foreground mt-1">
                                    Total size
                                </p>
                            </CardContent>
                        </Card>
                    </div>

                    {/* Top Buckets by Size */}
                    <Card>
                        <CardHeader>
                            <CardTitle>Top 5 Buckets by Size</CardTitle>
                            <CardDescription>Largest buckets in your project</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <ResponsiveContainer width="100%" height={300}>
                                <BarChart data={bucketSizeData}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="name" />
                                    <YAxis label={{ value: "Size (GB)", angle: -90, position: "insideLeft" }} />
                                    <Tooltip />
                                    <Legend />
                                    <Bar dataKey="size" fill="#4285f4" name="Size (GB)" />
                                </BarChart>
                            </ResponsiveContainer>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="buckets" className="space-y-6">
                    <Card>
                        <CardHeader>
                            <CardTitle>All Buckets</CardTitle>
                            <CardDescription>Complete list of your Cloud Storage buckets</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Bucket Name</TableHead>
                                        <TableHead>Location</TableHead>
                                        <TableHead>Created</TableHead>
                                        <TableHead>Objects</TableHead>
                                        <TableHead className="text-right">Size</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {metrics?.buckets.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={5} className="text-center text-gray-500">
                                                No Cloud Storage buckets found
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        metrics?.buckets.map((bucket) => (
                                            <TableRow key={bucket.name}>
                                                <TableCell className="font-mono text-sm">
                                                    {bucket.name}
                                                </TableCell>
                                                <TableCell>
                                                    <Badge variant="outline">{bucket.location}</Badge>
                                                </TableCell>
                                                <TableCell>
                                                    {bucket.creationDate
                                                        ? new Date(bucket.creationDate).toLocaleDateString()
                                                        : "N/A"}
                                                </TableCell>
                                                <TableCell>{bucket.objectCount.toLocaleString()}</TableCell>
                                                <TableCell className="text-right">
                                                    {formatBytes(bucket.sizeBytes)}
                                                </TableCell>
                                            </TableRow>
                                        ))
                                    )}
                                </TableBody>
                            </Table>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="analytics" className="space-y-6">
                    <div className="grid gap-6 md:grid-cols-2">
                        {/* Location Distribution */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Bucket Distribution by Location</CardTitle>
                                <CardDescription>Number of buckets per location</CardDescription>
                            </CardHeader>
                            <CardContent>
                                <ResponsiveContainer width="100%" height={300}>
                                    <PieChart>
                                        <Pie
                                            data={locationData}
                                            cx="50%"
                                            cy="50%"
                                            labelLine={false}
                                            label={({ name, percent }) =>
                                                `${name}: ${(percent * 100).toFixed(0)}%`
                                            }
                                            outerRadius={80}
                                            fill="#8884d8"
                                            dataKey="value"
                                        >
                                            {locationData.map((entry, index) => (
                                                <Cell
                                                    key={`cell-${index}`}
                                                    fill={COLORS[index % COLORS.length]}
                                                />
                                            ))}
                                        </Pie>
                                        <Tooltip />
                                    </PieChart>
                                </ResponsiveContainer>
                            </CardContent>
                        </Card>

                        {/* Top Buckets by Object Count */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Top 5 Buckets by Object Count</CardTitle>
                                <CardDescription>Buckets with most files</CardDescription>
                            </CardHeader>
                            <CardContent>
                                <ResponsiveContainer width="100%" height={300}>
                                    <BarChart data={bucketObjectsData}>
                                        <CartesianGrid strokeDasharray="3 3" />
                                        <XAxis dataKey="name" />
                                        <YAxis />
                                        <Tooltip />
                                        <Legend />
                                        <Bar dataKey="objects" fill="#34a853" name="Objects" />
                                    </BarChart>
                                </ResponsiveContainer>
                            </CardContent>
                        </Card>
                    </div>
                </TabsContent>
            </Tabs>
        </div>
    );
}
