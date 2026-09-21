"use client";

import React, { useState, useEffect } from "react";
import {
    WIDGET_REGISTRY,
    VERCEL_WIDGET_COMPONENTS,
    type VercelSummary,
} from "../integrations/widgets";

type Props = {
    projectId: string;
    integrationData: Record<string, unknown>;
};

function getStorageKey(projectId: string, integrationType: string) {
    return `threadspace:widgets:${projectId}:${integrationType}`;
}

function getDefaultEnabled(integrationType: string): string[] {
    return (WIDGET_REGISTRY[integrationType] ?? []).map((w) => w.id);
}

export default function DashboardWidgets({ projectId, integrationData }: Props) {
    const [enabledMap, setEnabledMap] = useState<Record<string, string[]>>(() => {
        const map: Record<string, string[]> = {};
        for (const type of Object.keys(integrationData)) {
            map[type] = getDefaultEnabled(type);
        }
        return map;
    });

    useEffect(() => {
        const map: Record<string, string[]> = {};
        for (const type of Object.keys(integrationData)) {
            const stored = localStorage.getItem(getStorageKey(projectId, type));
            map[type] = stored ? JSON.parse(stored) : getDefaultEnabled(type);
        }
        setEnabledMap(map);
    }, [projectId, integrationData]);

    const widgets: {
        key: string;
        label: string;
        Component: React.ComponentType<{ summary: VercelSummary }>;
        data: VercelSummary;
    }[] = [];

    if (integrationData.vercel && enabledMap.vercel) {
        const summary = integrationData.vercel as VercelSummary;
        for (const widgetDef of WIDGET_REGISTRY.vercel ?? []) {
            if (enabledMap.vercel.includes(widgetDef.id)) {
                const Component = VERCEL_WIDGET_COMPONENTS[widgetDef.id];
                if (Component) {
                    widgets.push({
                        key: `vercel-${widgetDef.id}`,
                        label: widgetDef.label,
                        Component,
                        data: summary,
                    });
                }
            }
        }
    }

    if (widgets.length === 0) {
        return (
            <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-6 py-8 text-sm text-gray-500">
                No widgets to display. Connect an integration and configure it from the{" "}
                <span className="font-medium">Integrations</span> page to see data here.
            </div>
        );
    }

    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {widgets.map(({ key, label, Component, data }) => (
                <div key={key} className="rounded-lg border bg-white p-4">
                    <h3 className="text-sm font-semibold mb-3">{label}</h3>
                    <Component summary={data} />
                </div>
            ))}
        </div>
    );
}
