import { fetchProjects } from "@/lib/projects";
import ProjectsList from "@/components/dashboard/ProjectsList";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Search } from "lucide-react";
import Link from "next/link";

export default async function ProjectsPage() {
    const projects = await fetchProjects();
    return (
        <div className="flex flex-col gap-6 p-6">
            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div className="flex flex-col gap-1">
                    <h1 className="text-2xl font-semibold">Projects</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage and explore your projects.
                    </p>
                </div>
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
                        <Link href="/dashboard/create-project">New project</Link>
                    </Button>
                </div>
            </div>
            <ProjectsList projects={projects} />
        </div>
    );
}
