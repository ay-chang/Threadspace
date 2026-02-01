import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import IntegrationCard from "@/components/dashboard/IntegrationCard";
import { AVAILABLE_INTEGRATIONS } from "./integrations";
import { INTEGRATION_SCHEMAS } from "./schemas";

type PageProps = {
    params: Promise<{ projectId: string }>;
};

type IntegrationResponse = {
    id: string;
    integrationType: string;
    integrationStatus: string;
    displayName: string;
};

const BACKEND_BASE = process.env.BACKEND_BASE!;
const INTERNAL_SYNC_TOKEN = process.env.INTERNAL_SYNC_TOKEN!;

export default async function Integrations({ params }: PageProps) {
    const { projectId } = await params;

    let connected: IntegrationResponse[] = [];
    try {
        const res = await fetch(`${BACKEND_BASE}/projects/${projectId}/integrations`, {
            headers: { "x-internal-token": INTERNAL_SYNC_TOKEN },
        });
        if (res.ok) {
            connected = await res.json();
        }
    } catch {}

    const connectedTypes = new Set(connected.map((i) => i.integrationType));

    // Reverse lookup: backendType → card info, for rendering connected cards
    const infoByBackendType = Object.fromEntries(
        AVAILABLE_INTEGRATIONS.map((i) => {
            const schema = INTEGRATION_SCHEMAS[i.id];
            return [schema?.backendType ?? i.id.toUpperCase(), i];
        })
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
                            {connected.length === 0 ? (
                                <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-6 py-8 text-sm text-gray-500">
                                    No integrations connected yet. Connect an integration from the
                                    <span className="font-medium"> Available</span> tab to see it
                                    here.
                                </div>
                            ) : (
                                connected.map((integration) => {
                                    const info = infoByBackendType[integration.integrationType];
                                    return info ? (
                                        <IntegrationCard
                                            key={integration.id}
                                            id={integration.id}
                                            name={info.name}
                                            description={info.description}
                                            logoSrc={info.logoSrc}
                                            logoAlt={info.logoAlt}
                                            connected={true}
                                        />
                                    ) : null;
                                })
                            )}
                        </div>
                    </TabsContent>
                    <TabsContent value="Available">
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-3 2xl:grid-cols-5 gap-6">
                            {AVAILABLE_INTEGRATIONS
                                .filter((integration) => !connectedTypes.has(INTEGRATION_SCHEMAS[integration.id]?.backendType))
                                .map((integration) => (
                                    <IntegrationCard
                                        key={integration.id}
                                        id={integration.id}
                                        name={integration.name}
                                        description={integration.description}
                                        logoSrc={integration.logoSrc}
                                        logoAlt={integration.logoAlt}
                                        href={
                                            INTEGRATION_SCHEMAS[integration.id]
                                                ? `/dashboard/projects/${projectId}/integrations/${integration.id}`
                                                : undefined
                                        }
                                    />
                                ))}
                        </div>
                    </TabsContent>
                </Tabs>
            </div>
        </div>
    );
}
