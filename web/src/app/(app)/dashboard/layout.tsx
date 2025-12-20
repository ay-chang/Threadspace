import { SidebarInset } from "@/components/ui/sidebar";
import AppSidebar from "@/components/sidebar/AppSidebar";
import AppHeader from "@/components/AppHeader";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/authOptions";
import ClientProviders from "./clientProviders";
import { redirect } from "next/navigation";

/**
 * Entry point for the use dashboard. The dashboard is always displayed
 * alongside the app header and the app sidebar. The children are the main
 * content which could be showing dashbaords, integrations, etc.
 */

export default async function AppLayout({ children }: { children: React.ReactNode }) {
    const session = await getServerSession(authOptions);

    if (!session?.user) {
        redirect("/login");
    }

    return (
        <div className="[--header-height:calc(--spacing(14))]">
            <ClientProviders session={session}>
                <main className="flex flex-1">
                    <AppSidebar />
                    <SidebarInset>
                        <AppHeader />
                        {children}
                    </SidebarInset>
                </main>
            </ClientProviders>
        </div>
    );
}
