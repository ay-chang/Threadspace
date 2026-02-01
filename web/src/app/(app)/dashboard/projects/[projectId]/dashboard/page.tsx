import { fetchProjects } from "@/lib/projects";
import DashboardWidgets from "./DashboardWidgets";

type PageProps = {
    params: Promise<{ projectId: string }>;
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

    let integrations: { integrationType: string }[] = [];
    try {
        const res = await fetch(`${BACKEND_BASE}/projects/${projectId}/integrations`, {
            headers: { "x-internal-token": INTERNAL_SYNC_TOKEN },
        });
        if (res.ok) integrations = await res.json();
    } catch {}

    const integrationData: Record<string, unknown> = {};
    if (integrations.some((i) => i.integrationType === "VERCEL")) {
        try {
            const res = await fetch(`${BACKEND_BASE}/projects/${projectId}/vercel/summary`, {
                headers: { "x-internal-token": INTERNAL_SYNC_TOKEN },
            });
            if (res.ok) integrationData.vercel = await res.json();
        } catch {}
    }

    return (
        <div className="flex flex-col gap-6 p-6">
            <div className="flex flex-col gap-1">
                <h1 className="text-2xl font-semibold">{project.name}</h1>
                <p className="text-sm text-muted-foreground">Dashboard</p>
            </div>

            <DashboardWidgets projectId={projectId} integrationData={integrationData} />
        </div>
    );
}
