import "./globals.css";
import type { Metadata } from "next";

/** Main layout.tsx file, project entry point */

export const metadata: Metadata = { title: "Threadspace" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="en">
            <body className="min-h-screen antialiased">{children}</body>
        </html>
    );
}
