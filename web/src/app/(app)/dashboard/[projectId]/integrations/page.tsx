import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import IntegrationCard from "@/components/dashboard/IntegrationCard";

export default function Integrations() {
    return (
        <div className="px-4 py-6">
            {/** Header */}
            <div className="mb-6">
                <h1 className="text-2xl font-semibold">Integrations</h1>
                <p className="text-sm text-gray-500">
                    Integrate your applications using our comprehensive directory
                </p>
            </div>

            <div className="">
                <Tabs defaultValue="Connected" className="w-[400px]">
                    <TabsList>
                        <TabsTrigger value="Connected">Connected</TabsTrigger>
                        <TabsTrigger value="Available">Available</TabsTrigger>
                    </TabsList>
                    <TabsContent value="Connected">See your connected APIs.</TabsContent>
                    <TabsContent value="Available">
                        <div className="grid grid-cols-1 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-5 gap-4">
                            <IntegrationCard />
                            <IntegrationCard />
                            <IntegrationCard />
                            <IntegrationCard />
                            <IntegrationCard />
                            <IntegrationCard />
                        </div>
                    </TabsContent>
                </Tabs>
            </div>
        </div>
    );
}
