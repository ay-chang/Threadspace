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
    <Card className="w-60 flex-shrink-0 flex flex-col rounded-2xl border bg-white shadow-sm">
      <CardHeader className="space-y-4 pb-3">
        <div className="flex items-center justify-between">
          <div className="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl bg-gray-100">
            <img
              src={logoSrc}
              alt={logoAlt}
              className="max-h-8 max-w-8 object-contain"
            />
          </div>

          {/* <span className="text-[11px] px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-100">
            Connected
          </span> */}
           {<span className="text-[11px] px-2 py-0.5 rounded-full bg-red-50 text-red-700 border border-red-100">
            Not Connected
          </span>}
        </div>

        <div className="space-y-1">
          <CardTitle className="text-sm font-semibold leading-snug">
            {name}
          </CardTitle>
          <CardDescription className="text-xs leading-snug text-gray-500">
            {description}
          </CardDescription>
        </div>
      </CardHeader>

      <CardContent className="flex-1" />

      <CardFooter className="mt-auto flex items-center justify-end pt-0">
        <Button
          size="sm"
          variant={connected ? "outline" : "default"}
          className="px-3 text-xs"
        >
          {connected ? "Manage" : "Connect"}
        </Button>
      </CardFooter>
    </Card>
  );
}
