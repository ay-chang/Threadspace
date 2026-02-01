"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { WIDGET_REGISTRY, type VercelSummary } from "../../widgets";
import { INTEGRATION_SCHEMAS } from "../../schemas";

type Props = {
    projectId: string;
    integrationType: string;
    credentials: Record<string, string>;
    summary: VercelSummary | null;
};

function getStorageKey(projectId: string, integrationType: string) {
    return `threadspace:widgets:${projectId}:${integrationType}`;
}

function getDefaultEnabled(integrationType: string): string[] {
    return (WIDGET_REGISTRY[integrationType] ?? []).map((w) => w.id);
}

export default function ManagePage({ projectId, integrationType, credentials, summary }: Props) {
    const router = useRouter();
    const schema = INTEGRATION_SCHEMAS[integrationType];
    const widgets = WIDGET_REGISTRY[integrationType] ?? [];

    // Widget toggle state
    const [enabled, setEnabled] = useState<string[]>(getDefaultEnabled(integrationType));
    const [initialized, setInitialized] = useState(false);

    useEffect(() => {
        const stored = localStorage.getItem(getStorageKey(projectId, integrationType));
        if (stored) {
            setEnabled(JSON.parse(stored));
        }
        setInitialized(true);
    }, [projectId, integrationType]);

    useEffect(() => {
        if (!initialized) return;
        localStorage.setItem(getStorageKey(projectId, integrationType), JSON.stringify(enabled));
    }, [enabled, initialized, projectId, integrationType]);

    const toggle = (widgetId: string) => {
        setEnabled((prev) =>
            prev.includes(widgetId)
                ? prev.filter((id) => id !== widgetId)
                : [...prev, widgetId]
        );
    };

    // Credential edit state
    const [formValues, setFormValues] = useState<Record<string, string>>(() => {
        const initial: Record<string, string> = {};
        if (schema) {
            for (const section of schema.sections) {
                for (const field of section.fields) {
                    const current = credentials[field.key] ?? "";
                    // Masked values (****xxxx) start empty so user can optionally replace
                    initial[field.key] = current.startsWith("****") ? "" : current;
                }
            }
        }
        return initial;
    });

    const [saveStatus, setSaveStatus] = useState<"idle" | "saving" | "saved" | "error">("idle");
    const [saveError, setSaveError] = useState("");

    const handleSave = async () => {
        if (!schema) return;
        setSaveStatus("saving");
        setSaveError("");

        try {
            const res = await fetch(`/api/integrations/${projectId}/update`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    integrationType: schema.backendType,
                    credentials: formValues,
                }),
            });

            if (!res.ok) {
                const data = await res.json().catch(() => ({ error: "Update failed" }));
                setSaveError(data.error || "Update failed");
                setSaveStatus("error");
                return;
            }

            setSaveStatus("saved");
            setTimeout(() => setSaveStatus("idle"), 2000);
            router.refresh();
        } catch {
            setSaveError("Network error");
            setSaveStatus("error");
        }
    };

    if (!schema) return null;

    return (
        <div className="w-full px-4 sm:px-6 lg:px-10 py-6 max-w-2xl">
            <div className="mb-8">
                <button
                    onClick={() => router.push(`/dashboard/projects/${projectId}/integrations`)}
                    className="text-sm text-muted-foreground hover:text-foreground mb-3 inline-block"
                >
                    ← Back to Integrations
                </button>
                <h1 className="text-xl font-semibold capitalize">{integrationType}</h1>
                {summary ? (
                    <p className="text-sm text-muted-foreground">{summary.name}</p>
                ) : (
                    <p className="text-sm text-amber-600 mt-1">
                        Unable to connect — check your credentials below.
                    </p>
                )}
            </div>

            {/* Credential Edit Form */}
            <div className="mb-8">
                <h2 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">
                    Credentials
                </h2>
                {schema.sections.map((section) => (
                    <fieldset key={section.legend} className="mb-4">
                        <legend className="text-sm font-medium mb-1">{section.legend}</legend>
                        <p className="text-xs text-muted-foreground mb-3">{section.description}</p>
                        <div className="flex flex-col gap-3">
                            {section.fields.map((field) => {
                                const currentMasked = credentials[field.key];
                                const isMasked = currentMasked?.startsWith("****");
                                return (
                                    <div key={field.key}>
                                        <label className="text-xs font-medium text-muted-foreground">
                                            {field.label}
                                            {field.required && " *"}
                                        </label>
                                        {isMasked && (
                                            <p className="text-xs text-muted-foreground mb-1">
                                                Current: {currentMasked} — leave blank to keep
                                            </p>
                                        )}
                                        <input
                                            type={isMasked ? "password" : "text"}
                                            value={formValues[field.key] ?? ""}
                                            onChange={(e) =>
                                                setFormValues((prev) => ({
                                                    ...prev,
                                                    [field.key]: e.target.value,
                                                }))
                                            }
                                            placeholder={field.placeholder}
                                            className="w-full text-sm border border-border rounded-md px-3 py-1.5 bg-transparent focus:outline-none focus:ring-1 focus:ring-foreground"
                                        />
                                    </div>
                                );
                            })}
                        </div>
                    </fieldset>
                ))}
                <div className="flex items-center gap-3 mt-4">
                    <button
                        onClick={handleSave}
                        disabled={saveStatus === "saving"}
                        className="text-sm px-4 py-1.5 rounded-md bg-foreground text-background disabled:opacity-50"
                    >
                        {saveStatus === "saving" ? "Saving…" : saveStatus === "saved" ? "Saved" : "Save"}
                    </button>
                    {saveStatus === "error" && (
                        <p className="text-xs text-red-500">{saveError}</p>
                    )}
                </div>
            </div>

            {/* Widget toggles — only shown when connected */}
            {summary && (
                <div className="mb-8">
                    <h2 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">
                        What to display on dashboard
                    </h2>
                    <div className="grid grid-cols-2 gap-2">
                        {widgets.map((widget) => {
                            const isEnabled = enabled.includes(widget.id);
                            return (
                                <button
                                    key={widget.id}
                                    onClick={() => toggle(widget.id)}
                                    className={[
                                        "text-left p-3 rounded-lg border transition-colors",
                                        isEnabled
                                            ? "border-foreground bg-muted"
                                            : "border-border bg-transparent hover:bg-muted/50",
                                    ].join(" ")}
                                >
                                    <div className="flex items-center gap-2">
                                        <span
                                            className={[
                                                "w-3.5 h-3.5 rounded border-2 flex items-center justify-center text-xs",
                                                isEnabled
                                                    ? "border-foreground bg-foreground text-background"
                                                    : "border-border",
                                            ].join(" ")}
                                        >
                                            {isEnabled && "✓"}
                                        </span>
                                        <span className="text-sm font-medium">{widget.label}</span>
                                    </div>
                                    <p className="text-xs text-muted-foreground mt-1 ml-5">
                                        {widget.description}
                                    </p>
                                </button>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
}
