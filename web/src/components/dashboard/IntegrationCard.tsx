import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import Image from "next/image";

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
        <Card className="flex flex-col rounded-2xl border bg-white shadow-sm">
            <CardHeader className="space-y-4 pb-3">
                <div className="flex items-center justify-between">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl">
                        <Image
                            src={logoSrc}
                            alt={logoAlt}
                            width={32}
                            height={32}
                            className="h-8 w-8 object-contain"
                        />
                    </div>
                </div>

                <div className="space-y-1">
                    <CardTitle className="text-lg font-semibold">{name}</CardTitle>
                    <CardDescription className="text-sm text-gray-500">
                        {description}
                    </CardDescription>
                </div>
            </CardHeader>

            <CardContent className="flex-1" />

            <CardFooter className="mt-auto">
                <Button variant={connected ? "outline" : "default"} className="w-full text-sm">
                    {connected ? "Manage" : "Connect"}
                </Button>
            </CardFooter>
        </Card>
    );
}
