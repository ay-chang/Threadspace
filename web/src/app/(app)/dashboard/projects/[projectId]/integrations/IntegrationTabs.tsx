"use client";

import { useState } from "react";

export default function IntegrationTabs({
    defaultTab = "connected",
    connected,
    available,
}: {
    defaultTab?: "connected" | "available";
    connected: React.ReactNode;
    available: React.ReactNode;
}) {
    const [active, setActive] = useState(defaultTab);

    const tabs = [
        { id: "connected" as const, label: "Connected" },
        { id: "available" as const, label: "Available" },
    ];

    return (
        <div>
            <nav className="flex gap-6 border-b border-border">
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        onClick={() => setActive(tab.id)}
                        className={[
                            "pb-2 text-sm font-medium border-b-2 -mb-px transition-colors",
                            active === tab.id
                                ? "border-foreground text-foreground"
                                : "border-transparent text-muted-foreground hover:text-foreground",
                        ].join(" ")}
                    >
                        {tab.label}
                    </button>
                ))}
            </nav>
            <div className="mt-6">
                {active === "connected" ? connected : available}
            </div>
        </div>
    );
}
