"use client";

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

import { Cable, LayoutDashboard, Settings, ChevronUp } from "lucide-react";
import ProjectSwitcher from "@/components/sidebar/ProjectSwitcher";
import SidebarOptions from "@/components/sidebar/SidebarOptions";

export default function AppSidebar() {
    return (
        <Sidebar>
            {/** Sidebar header */}
            <SidebarHeader>
                <ProjectSwitcher
                    projects={["Appple Product", "Vercel Project"]}
                    overview="Overview"
                />
            </SidebarHeader>

            <SidebarContent>
                {/** Platform group */}
                <SidebarGroup>
                    <SidebarGroupLabel>Platform</SidebarGroupLabel>
                    {/** DashboardsItem */}
                    <SidebarGroupContent>
                        <SidebarMenu>
                            <SidebarMenuItem>
                                <SidebarMenuButton asChild>
                                    <a href="/">
                                        <LayoutDashboard />
                                        <span>Dashboard</span>
                                    </a>
                                </SidebarMenuButton>
                            </SidebarMenuItem>
                        </SidebarMenu>
                    </SidebarGroupContent>
                    {/** Integrations Item */}
                    <SidebarGroupContent>
                        <SidebarMenu>
                            <SidebarMenuItem>
                                <SidebarMenuButton asChild>
                                    <a href="/">
                                        <Cable />
                                        <span>Integrations</span>
                                    </a>
                                </SidebarMenuButton>
                            </SidebarMenuItem>
                        </SidebarMenu>
                    </SidebarGroupContent>

                    {/** Settings Item */}
                    <SidebarGroupContent>
                        <SidebarMenu>
                            <SidebarMenuItem>
                                <SidebarMenuButton asChild>
                                    <a href="/">
                                        <Settings />
                                        <span>Settings</span>
                                    </a>
                                </SidebarMenuButton>
                            </SidebarMenuItem>
                        </SidebarMenu>
                    </SidebarGroupContent>
                </SidebarGroup>
            </SidebarContent>

            {/** Sidebar Footer */}
            <SidebarFooter>
                <SidebarOptions />
            </SidebarFooter>
        </Sidebar>
    );
}
