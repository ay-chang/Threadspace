import { fetchProjects } from "@/lib/projects";
import ProjectsList from "@/components/dashboard/ProjectsList";

export default async function ProjectsPage() {
    const projects = await fetchProjects();
    return <ProjectsList projects={projects} />;
}
