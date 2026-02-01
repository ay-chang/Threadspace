import { fetchProjects } from "@/lib/projects";
import Link from "next/link";

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

export default async function ProjectDashboard({ params }: PageProps) {
    const { projectId } = await params;
    const projects = await fetchProjects();
    const project = projects.find((p) => p.id === projectId);

    if (!project) {
        return (
            <div className="p-6">
                <h1 className="text-xl font-semibold">Project not found</h1>
                <p className="text-sm text-muted-foreground">
                    We couldn&apos;t locate that project. Please go back and try again.
                </p>
            </div>
        );
    }

    // Fetch connected integrations
    let connected: IntegrationResponse[] = [];
    try {
        const res = await fetch(`${BACKEND_BASE}/projects/${projectId}/integrations`, {
            headers: { "x-internal-token": INTERNAL_SYNC_TOKEN },
        });
        if (res.ok) {
            connected = await res.json();
        }
    } catch {}

    // Map sections to their routes based on connected integrations
    const hasAWS = connected.some((i) => i.integrationType === "AWS");

    const sectionRoutes: Record<string, string | null> = {
        Database: hasAWS ? `/dashboard/projects/${projectId}/integrations/aws/services` : null,
        Auth: null,
        Storage: hasAWS ? `/dashboard/projects/${projectId}/integrations/aws/services` : null,
        Realtime: null,
    };

    return (
        <div className="flex flex-col gap-6 p-6">
            <div className="flex flex-col gap-1">
                <h1 className="text-2xl font-semibold">{project.name}</h1>
                <p className="text-sm text-muted-foreground">Dashboard</p>
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                {["Database", "Auth", "Storage", "Realtime"].map((section) => {
                    const route = sectionRoutes[section];
                    const cardContent = (
                        <>
                            <div className="text-sm font-semibold text-foreground">{section}</div>
                            <div className="mt-2 text-xs">
                                {route ? "Click to view metrics" : "No data to show"}
                            </div>
                            <div className="mt-auto h-24 rounded-md border border-dashed border-border/60 bg-muted/40" />
                        </>
                    );

                    const className = `border-border/60 bg-muted/20 text-muted-foreground flex min-h-[220px] flex-col rounded-lg border p-4 ${
                        route ? "cursor-pointer transition-all hover:shadow-lg hover:border-border" : ""
                    }`;

                    return route ? (
                        <Link key={section} href={route} className={className}>
                            {cardContent}
                        </Link>
                    ) : (
                        <div key={section} className={className}>
                            {cardContent}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
