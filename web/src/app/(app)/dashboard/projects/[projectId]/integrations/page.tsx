"use client";

import { useState, useEffect, useCallback } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import IntegrationCard from "@/components/dashboard/IntegrationCard";
import { AwsConnectionModal } from "@/components/dashboard/AwsConnectionModal";
import { AVAILABLE_INTEGRATIONS } from "./integrations";

type Integration = {
    id: string;
    projectId: string;
    integrationType: string;
    status: "PENDING" | "CONNECTED" | "ERROR" | "REVOKED";
    displayName: string;
    createdAt: string;
    updatedAt: string;
};

export default function Integrations({
    params,
}: {
    params: Promise<{ projectId: string }>;
}) {
    const [projectId, setProjectId] = useState<string>("");
    const [awsModalOpen, setAwsModalOpen] = useState(false);
    const [connectedIntegrations, setConnectedIntegrations] = useState<Integration[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        params.then((p) => setProjectId(p.projectId));
    }, [params]);

    const fetchIntegrations = useCallback(async () => {
        if (!projectId) return;

        setIsLoading(true);
        try {
            const response = await fetch(`/api/projects/${projectId}/integrations`);
            if (response.ok) {
                const data = await response.json();
                setConnectedIntegrations(data);
            }
        } catch (error) {
            console.error("Failed to fetch integrations:", error);
        } finally {
            setIsLoading(false);
        }
    }, [projectId]);

    useEffect(() => {
        fetchIntegrations();
    }, [fetchIntegrations]);

    const handleAwsConnect = () => {
        setAwsModalOpen(true);
    };

    const handleConnectionSuccess = () => {
        fetchIntegrations();
    };

    // Filter out already connected integrations from available list
    const availableIntegrations = AVAILABLE_INTEGRATIONS.filter(
        (integration) =>
            !connectedIntegrations.some(
                (conn) => conn.integrationType === integration.id.toUpperCase()
            )
    );

    return (
        <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
            {/** Header */}
            <div className="mb-6">
                <h1 className="text-2xl font-semibold">Integrations</h1>
                <p className="text-sm text-gray-500">
                    Integrate your applications using our comprehensive directory
                </p>
            </div>

            <div className="">
                <Tabs defaultValue="Connected" className="w-full">
                    <TabsList className="mb-6">
                        <TabsTrigger value="Connected">Connected</TabsTrigger>
                        <TabsTrigger value="Available">Available</TabsTrigger>
                    </TabsList>
                    <TabsContent value="Connected">
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
                            {isLoading ? (
                                <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-6 py-8 text-sm text-gray-500">
                                    Loading integrations...
                                </div>
                            ) : connectedIntegrations.length === 0 ? (
                                <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-6 py-8 text-sm text-gray-500">
                                    No integrations connected yet. Connect an integration from the
                                    <span className="font-medium"> Available</span> tab to see it
                                    here.
                                </div>
                            ) : (
                                connectedIntegrations.map((integration) => {
                                    const integrationConfig = AVAILABLE_INTEGRATIONS.find(
                                        (i) => i.id.toUpperCase() === integration.integrationType
                                    );

                                    return (
                                        <div key={integration.id} className="relative">
                                            <IntegrationCard
                                                name={integration.displayName}
                                                description={
                                                    integrationConfig?.description || "Connected integration"
                                                }
                                                logoSrc={integrationConfig?.logoSrc || ""}
                                                logoAlt={integrationConfig?.logoAlt || integration.displayName}
                                                connected={true}
                                            />
                                            {/* Status indicator */}
                                            <div className="absolute top-3 right-3">
                                                <div
                                                    className={`h-3 w-3 rounded-full ${
                                                        integration.status === "CONNECTED"
                                                            ? "bg-green-500"
                                                            : "bg-red-500"
                                                    }`}
                                                    title={
                                                        integration.status === "CONNECTED"
                                                            ? "Connected"
                                                            : "Connection failed"
                                                    }
                                                />
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </TabsContent>
                    <TabsContent value="Available">
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-3 2xl:grid-cols-5 gap-6">
                            {availableIntegrations.map((integration) => (
                                <IntegrationCard
                                    key={integration.id}
                                    name={integration.name}
                                    description={integration.description}
                                    logoSrc={integration.logoSrc}
                                    logoAlt={integration.logoAlt}
                                    onConnect={
                                        integration.id === "aws" ? handleAwsConnect : undefined
                                    }
                                />
                            ))}
                        </div>
                    </TabsContent>
                </Tabs>
            </div>

            {projectId && (
                <AwsConnectionModal
                    open={awsModalOpen}
                    onOpenChange={setAwsModalOpen}
                    projectId={projectId}
                    onConnectionSuccess={handleConnectionSuccess}
                />
            )}
        </div>
    );
}
