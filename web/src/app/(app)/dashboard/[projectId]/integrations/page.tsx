import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import IntegrationCard from "@/components/dashboard/IntegrationCard";
import { AVAILABLE_INTEGRATIONS } from "./integrations";

export default function Integrations() {
    return (
        <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
            {/** Header */}
            <div className="mb-6">
                <h1 className="text-2xl font-semibold">Integrations</h1>
                <p className="text-sm text-gray-500">
                    Integrate your applications using our comprehensive directory
                </p>
            </div>

            <div className="mb-4">
                <Tabs defaultValue="Connected" className="w-full">
                    <TabsList>
                        <TabsTrigger value="Connected">Connected</TabsTrigger>
                        <TabsTrigger value="Available">Available</TabsTrigger>
                    </TabsList>
                    <TabsContent value="Connected">
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
                            <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-6 py-8 text-sm text-gray-500">
                                No integrations connected yet. Connect an integration from the
                                <span className="font-medium"> Available</span> tab to see it
                                here.
                            </div>
                        </div>
                    </TabsContent>
                    <TabsContent value="Available">
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-3 2xl:grid-cols-5 gap-6">
                            {AVAILABLE_INTEGRATIONS.map((integration) => (
                                <IntegrationCard
                                    key={integration.id}
                                    name={integration.name}
                                    description={integration.description}
                                    logoSrc={integration.logoSrc}
                                    logoAlt={integration.logoAlt}
                                />
                            ))}
                        </div>
                    </TabsContent>
                </Tabs>
            </div>
        </div>
    );
}
