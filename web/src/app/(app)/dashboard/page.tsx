import EmptyProjects from "@/components/dashboard/EmptyProjects";
import { fetchProjects } from "@/lib/projects";
import ProjectsList from "@/components/dashboard/ProjectsList";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import Link from "next/link";
import { Search } from "lucide-react";

export default async function DashboardPage() {
    const projects = await fetchProjects();
    const hasProjects = projects.length > 0;

    if (!hasProjects) {
        return <EmptyProjects />;
    }

    return (
        <div className="flex flex-col gap-6 p-6">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <h1 className="text-2xl font-semibold">Projects</h1>
                <div className="flex w-full flex-col gap-3 md:w-auto md:flex-row md:items-center">
                    <div className="relative w-full md:w-72">
                        <Search className="text-muted-foreground absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2" />
                        <Input
                            placeholder="Search for a project"
                            className="pl-9"
                            aria-label="Search projects"
                        />
                    </div>
                    <Button asChild>
                        <Link href="/dashboard/create-project">+ New Project</Link>
                    </Button>
                </div>
            </div>

            <ProjectsList projects={projects} />
        </div>
    );
}
