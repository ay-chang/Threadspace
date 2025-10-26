// components/ClientProviders.tsx
"use client";

import { SessionProvider } from "next-auth/react";
import { SidebarProvider } from "@/components/ui/sidebar";
import type { Session } from "next-auth";

export default function ClientProviders({
    children,
    session,
}: {
    children: React.ReactNode;
    session: Session | null;
}) {
    return (
        <SessionProvider session={session}>
            <SidebarProvider className="flex flex-col">{children}</SidebarProvider>
        </SessionProvider>
    );
}
