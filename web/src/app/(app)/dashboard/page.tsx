import EmptyProjects from "@/components/dashboard/EmptyProjects";
import { fetchProjects } from "@/lib/projects";
import ProjectsList from "@/components/dashboard/ProjectsList";

export default async function DashboardPage() {
    const projects = await fetchProjects();
    const hasProjects = projects.length > 0;

    if (!hasProjects) {
        return <EmptyProjects />;
    }

    return <ProjectsList projects={projects} />;
}
