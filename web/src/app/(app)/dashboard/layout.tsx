import { SidebarProvider, SidebarTrigger, SidebarInset } from "@/components/ui/sidebar";
import AppSidebar from "@/components/sidebar/AppSidebar";
import AppHeader from "@/components/AppHeader";

export default function AppLayout({ children }: { children: React.ReactNode }) {
    return (
        <div className="[--header-height:calc(--spacing(14))]">
            <SidebarProvider className="flex flex-col">
                <main className="flex flex-1">
                    <AppSidebar />
                    <SidebarInset>
                        <AppHeader />
                        {children}
                    </SidebarInset>
                </main>
            </SidebarProvider>
        </div>
    );
}
