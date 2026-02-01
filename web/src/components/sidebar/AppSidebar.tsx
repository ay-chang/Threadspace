"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
} from "@/components/ui/sidebar";

import { Cable, LayoutDashboard, Settings } from "lucide-react";
import ProjectSwitcher from "@/components/sidebar/ProjectSwitcher";
import SidebarOptions from "@/components/sidebar/SidebarOptions";
import { Project } from "@/lib/projects";

const NAV_ITEMS = [
    { icon: LayoutDashboard, label: "Dashboard", path: "dashboard" },
    { icon: Cable, label: "Integrations", path: "integrations" },
    { icon: Settings, label: "Settings", path: "settings" },
];

export default function AppSidebar({ projects }: { projects: Project[] }) {
    const pathname = usePathname();

    // Extract projectId from /dashboard/projects/{projectId}/...
    const segments = pathname.split("/");
    const projectsIdx = segments.indexOf("projects");
    const projectId = projectsIdx !== -1 && segments[projectsIdx + 1]
        ? segments[projectsIdx + 1]
        : null;

    return (
        <Sidebar>
            <SidebarHeader>
                <ProjectSwitcher projects={projects} currentProjectId={projectId} />
            </SidebarHeader>

            <SidebarContent>
                <SidebarGroup>
                    <SidebarGroupLabel>Platform</SidebarGroupLabel>
                    <SidebarGroupContent>
                        <SidebarMenu>
                            {NAV_ITEMS.map(({ icon: Icon, label, path }) => {
                                const href = projectId
                                    ? `/dashboard/projects/${projectId}/${path}`
                                    : null;
                                return (
                                    <SidebarMenuItem key={label}>
                                        <SidebarMenuButton asChild={!!href} disabled={!href}>
                                            {href ? (
                                                <Link href={href}>
                                                    <Icon />
                                                    <span>{label}</span>
                                                </Link>
                                            ) : (
                                                <>
                                                    <Icon />
                                                    <span>{label}</span>
                                                </>
                                            )}
                                        </SidebarMenuButton>
                                    </SidebarMenuItem>
                                );
                            })}
                        </SidebarMenu>
                    </SidebarGroupContent>
                </SidebarGroup>
            </SidebarContent>

            <SidebarFooter>
                <SidebarOptions />
            </SidebarFooter>
        </Sidebar>
    );
}
