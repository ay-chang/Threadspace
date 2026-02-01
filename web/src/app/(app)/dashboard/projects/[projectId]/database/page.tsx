"use client";

import { useState, useEffect, useCallback } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MetricCard } from "@/components/dashboard/MetricCard";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Database, Activity, HardDrive } from "lucide-react";

type S3BucketInfo = {
    name: string;
    creationDate: string;
    objectCount: number;
    sizeBytes: number;
    region: string;
};

type S3Metrics = {
    totalBuckets: number;
    totalObjects: number;
    totalStorageGB: number;
    buckets: S3BucketInfo[];
};

export default function DatabasePage({
    params,
}: {
    params: Promise<{ projectId: string }>;
}) {
    const [projectId, setProjectId] = useState<string>("");
    const [metrics, setMetrics] = useState<S3Metrics | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        params.then((p) => setProjectId(p.projectId));
    }, [params]);

    const fetchData = useCallback(async () => {
        if (!projectId) return;

        setIsLoading(true);
        setError(null);

        try {
            const metricsRes = await fetch(`/api/projects/${projectId}/aws/s3/metrics`);

            if (!metricsRes.ok) {
                const errorMsg = await metricsRes.text();
                throw new Error(errorMsg || "Failed to fetch S3 data");
            }

            const metricsData = await metricsRes.json();
            setMetrics(metricsData);
        } catch (err) {
            console.error("Failed to fetch S3 data:", err);
            setError(err instanceof Error ? err.message : "Failed to fetch AWS data");
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

    if (isLoading) {
        return (
            <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
                <div className="mb-6">
                    <h1 className="text-2xl font-semibold">S3 Storage Dashboard</h1>
                    <p className="text-sm text-gray-500">Loading AWS S3 data...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
                <div className="mb-6">
                    <h1 className="text-2xl font-semibold">S3 Storage Dashboard</h1>
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
                <h1 className="text-2xl font-semibold">S3 Storage Dashboard</h1>
                <p className="text-sm text-gray-500">
                    Monitor your AWS S3 buckets and storage metrics
                </p>
            </div>

            {/* Content */}
            <div className="">
                <Tabs defaultValue="overview" className="w-full">
                    <TabsList className="mb-6">
                        <TabsTrigger value="overview">Overview</TabsTrigger>
                        <TabsTrigger value="buckets">Buckets</TabsTrigger>
                    </TabsList>

                    <TabsContent value="overview">
                        <div className="space-y-6">
                            {/* Metric Cards Row */}
                            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                                <MetricCard
                                    title="Total Buckets"
                                    value={metrics?.totalBuckets || 0}
                                    icon={<Database className="h-4 w-4" />}
                                />
                                <MetricCard
                                    title="Total Objects"
                                    value={metrics?.totalObjects || 0}
                                    icon={<Activity className="h-4 w-4" />}
                                />
                                <MetricCard
                                    title="Total Storage"
                                    value={`${Math.round((metrics?.totalStorageGB || 0) * 100) / 100} GB`}
                                    icon={<HardDrive className="h-4 w-4" />}
                                />
                            </div>
                        </div>
                    </TabsContent>

                    <TabsContent value="buckets">
                        <div className="rounded-2xl border bg-white shadow-sm">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Bucket Name</TableHead>
                                        <TableHead>Region</TableHead>
                                        <TableHead>Created</TableHead>
                                        <TableHead>Object Count</TableHead>
                                        <TableHead className="text-right">Size</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {metrics?.buckets.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={5} className="text-center text-gray-500">
                                                No S3 buckets found
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        metrics?.buckets.map((bucket) => (
                                            <TableRow key={bucket.name}>
                                                <TableCell className="font-mono text-sm">
                                                    {bucket.name}
                                                </TableCell>
                                                <TableCell>
                                                    <Badge variant="outline">{bucket.region}</Badge>
                                                </TableCell>
                                                <TableCell>
                                                    {bucket.creationDate ? new Date(bucket.creationDate).toLocaleDateString() : "N/A"}
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
                        </div>
                    </TabsContent>
                </Tabs>
            </div>
        </div>
    );
}

