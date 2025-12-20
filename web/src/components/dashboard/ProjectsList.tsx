import { Project } from "@/lib/projects";

type Props = {
    projects: Project[];
};

export default function ProjectsList({ projects }: Props) {
    if (!projects.length) {
        return <div>No projects found.</div>;
    }

    return (
        <div className="flex flex-col gap-3 p-4">
            {projects.map((project) => (
                <div
                    key={project.id}
                    className="rounded-lg border border-border bg-card p-4 shadow-sm"
                >
                    <div className="text-lg font-semibold">{project.name}</div>
                    <div className="text-sm text-muted-foreground">
                        {project.description || "No description provided"}
                    </div>
                    <div className="mt-1 text-xs uppercase text-muted-foreground">
                        Type: {project.type}
                    </div>
                </div>
            ))}
        </div>
    );
}
