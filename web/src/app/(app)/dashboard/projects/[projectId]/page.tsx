import { fetchProjects } from "@/lib/projects";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import Link from "next/link";

type PageProps = {
    params: { projectId: string };
};

export default async function ProjectDetailPage({ params }: PageProps) {
    const { projectId } = params;
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
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="flex items-center gap-3">
                    <h1 className="text-3xl font-semibold">{project.name}</h1>
                    <Badge variant="secondary" className="uppercase">
                        {project.type || "Unknown"}
                    </Badge>
                </div>
                <Button asChild>
                    <Link href={`/dashboard/projects/${project.id}/integrations`}>
                        + Integration
                    </Link>
                </Button>
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
