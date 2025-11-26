import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";

type IntegrationCardProps = {
    name: string;
    description: string;
    logoSrc: string;
    logoAlt: string;
    connected?: boolean;
};

export default function IntegrationCard({
    name,
    description,
    logoSrc,
    logoAlt,
    connected = false,
}: IntegrationCardProps) {
    return (
        <Card className="flex flex-col rounded-2xl border bg-white shadow-sm h-full">
            <CardHeader className="space-y-4 pb-3">
                <div className="flex items-center justify-between">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl">
                        <img
                            src={logoSrc}
                            alt={logoAlt}
                            className="max-h-8 max-w-8 object-contain"
                        />
                    </div>
                </div>

                <div className="space-y-1">
                    <CardTitle className="text-sm font-semibold leading-snug">
                        {name}
                    </CardTitle>
                    <CardDescription className="text-sm leading-snug text-gray-500">
                        {description}
                    </CardDescription>
                </div>
            </CardHeader>

            <CardContent className="flex-1" />

            <CardFooter className="mt-auto pt-0">
                <Button
                    size="sm"
                    variant={connected ? "outline" : "default"}
                    className="w-full px-3 text-sm"
                >
                    {connected ? "Manage" : "Connect"}
                </Button>
            </CardFooter>
        </Card>
    );
}
