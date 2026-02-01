"use client";

import { useParams, useRouter } from "next/navigation";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Database, HardDrive, Server, CloudCog } from "lucide-react";

const AWS_SERVICES = [
    {
        id: "s3",
        name: "S3 Storage",
        description: "Object storage buckets and metrics",
        icon: HardDrive,
        available: true,
    },
    {
        id: "rds",
        name: "RDS Databases",
        description: "Relational database instances",
        icon: Database,
        available: false,
    },
    {
        id: "ec2",
        name: "EC2 Instances",
        description: "Virtual servers and compute",
        icon: Server,
        available: false,
    },
    {
        id: "lambda",
        name: "Lambda Functions",
        description: "Serverless compute functions",
        icon: CloudCog,
        available: false,
    },
];

export default function AwsIntegrationPage() {
    const { projectId } = useParams<{ projectId: string }>();
    const router = useRouter();

    const handleServiceClick = (serviceId: string) => {
        router.push(`/dashboard/projects/${projectId}/integrations/aws/${serviceId}`);
    };

    return (
        <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
            {/* Header */}
            <div className="mb-6">
                <h1 className="text-2xl font-semibold">AWS Integration</h1>
                <p className="text-sm text-gray-500">
                    Select an AWS service to view its metrics and data
                </p>
            </div>

            {/* Service Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {AWS_SERVICES.map((service) => {
                    const Icon = service.icon;
                    return (
                        <Card
                            key={service.id}
                            className={`cursor-pointer transition-all hover:shadow-lg ${
                                !service.available ? "opacity-50 cursor-not-allowed" : ""
                            }`}
                            onClick={() => service.available && handleServiceClick(service.id)}
                        >
                            <CardHeader>
                                <div className="flex items-center gap-3 mb-2">
                                    <div className="p-2 bg-blue-100 rounded-lg">
                                        <Icon className="h-6 w-6 text-blue-600" />
                                    </div>
                                    {!service.available && (
                                        <span className="ml-auto text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
                                            Coming Soon
                                        </span>
                                    )}
                                </div>
                                <CardTitle>{service.name}</CardTitle>
                                <CardDescription>{service.description}</CardDescription>
                            </CardHeader>
                            <CardContent>
                                <p className="text-sm text-muted-foreground">
                                    {service.available
                                        ? "Click to view details"
                                        : "This service is not yet available"}
                                </p>
                            </CardContent>
                        </Card>
                    );
                })}
            </div>
        </div>
    );
}
