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
    <Card className="h-full flex flex-col">
      <CardHeader className="flex flex-row items-center gap-3">
        <div className="h-10 w-10 rounded-md bg-muted flex items-center justify-center overflow-hidden">
          <img
            src={logoSrc}
            alt={logoAlt}
            className="h-8 w-8 object-contain"
          />
        </div>
        <div>
          <CardTitle className="text-base">{name}</CardTitle>
          <CardDescription className="text-xs">
            {description}
          </CardDescription>
        </div>
      </CardHeader>

      <CardContent className="flex-1">
      </CardContent>

      <CardFooter className="flex items-center justify-between">
        <p className="text-xs text-muted-foreground">
          {connected ? "Connected" : "Not connected"}
        </p>
        <Button size="sm" variant={connected ? "outline" : "default"}>
          {connected ? "Manage" : "Connect"}
        </Button>
      </CardFooter>
    </Card>
  );
}
