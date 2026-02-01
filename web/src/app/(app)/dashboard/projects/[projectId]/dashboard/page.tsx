import { fetchProjects } from "@/lib/projects";

type PageProps = {
    params: Promise<{ projectId: string }>;
};

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

    return (
        <div className="flex flex-col gap-6 p-6">
            <div className="flex flex-col gap-1">
                <h1 className="text-2xl font-semibold">{project.name}</h1>
                <p className="text-sm text-muted-foreground">Dashboard</p>
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                {["Database", "Auth", "Storage", "Realtime"].map((section) => (
                    <div
                        key={section}
                        className="border-border/60 bg-muted/20 text-muted-foreground flex min-h-[220px] flex-col rounded-lg border p-4"
                    >
                        <div className="text-sm font-semibold text-foreground">{section}</div>
                        <div className="mt-2 text-xs">No data to show</div>
                        <div className="mt-auto h-24 rounded-md border border-dashed border-border/60 bg-muted/40" />
                    </div>
                ))}
            </div>
        </div>
    );
}
