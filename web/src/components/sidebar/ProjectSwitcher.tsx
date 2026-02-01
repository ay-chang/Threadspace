"use client";

import { useRouter } from "next/navigation";
import { Check, ChevronsUpDown, GalleryVerticalEnd } from "lucide-react";

import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { SidebarMenu, SidebarMenuButton, SidebarMenuItem } from "@/components/ui/sidebar";
import { Project } from "@/lib/projects";

export default function ProjectSwitcher({
    projects,
    currentProjectId,
}: {
    projects: Project[];
    currentProjectId: string | null;
}) {
    const router = useRouter();
    const currentProject = projects.find((p) => p.id === currentProjectId);

    return (
        <SidebarMenu>
            <SidebarMenuItem>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <SidebarMenuButton
                            size="lg"
                            className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
                        >
                            <div className="bg-sidebar-primary text-sidebar-primary-foreground flex aspect-square size-8 items-center justify-center rounded-lg">
                                <GalleryVerticalEnd className="size-4" />
                            </div>
                            <div className="flex flex-col gap-0.5 leading-none">
                                <span className="font-medium">Projects</span>
                                <span className="text-xs text-sidebar-foreground/60">
                                    {currentProject?.name ?? "Select a project"}
                                </span>
                            </div>
                            <ChevronsUpDown className="ml-auto" />
                        </SidebarMenuButton>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                        className="w-(--radix-dropdown-menu-trigger-width)"
                        align="start"
                    >
                        {projects.length === 0 ? (
                            <DropdownMenuItem disabled>No projects yet</DropdownMenuItem>
                        ) : (
                            projects.map((project) => (
                                <DropdownMenuItem
                                    key={project.id}
                                    onSelect={() =>
                                        router.push(`/dashboard/projects/${project.id}/dashboard`)
                                    }
                                >
                                    {project.name}
                                    {project.id === currentProjectId && <Check className="ml-auto" />}
                                </DropdownMenuItem>
                            ))
                        )}
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}
