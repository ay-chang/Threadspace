"use client";

import React, { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Image from "next/image";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    Field,
    FieldDescription,
    FieldError,
    FieldGroup,
    FieldLabel,
    FieldLegend,
    FieldSeparator,
    FieldSet,
} from "@/components/ui/field";
import { INTEGRATION_SCHEMAS } from "../schemas";

export default function ConnectIntegrationPage() {
    const { projectId, integrationType } = useParams<{
        projectId: string;
        integrationType: string;
    }>();
    const router = useRouter();

    const schema = INTEGRATION_SCHEMAS[integrationType];

    const [values, setValues] = useState<Record<string, string>>({});
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    if (!schema) {
        return (
            <div className="flex w-full h-full items-center justify-center p-8">
                <div className="w-full max-w-md text-center">
                    <p className="text-sm text-muted-foreground">
                        This integration is not yet supported.
                    </p>
                    <Button
                        variant="outline"
                        className="mt-4"
                        onClick={() =>
                            router.push(`/dashboard/projects/${projectId}/integrations`)
                        }
                    >
                        Back to Integrations
                    </Button>
                </div>
            </div>
        );
    }

    const handleChange = (key: string, value: string) => {
        setValues((prev) => ({ ...prev, [key]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        const credentials = Object.fromEntries(
            Object.entries(values).filter(([, v]) => v.trim() !== ""),
        );

        try {
            const res = await fetch(`/api/integrations/${projectId}/connect`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    integrationType: schema.backendType,
                    displayName: values[schema.displayNameField] || schema.name,
                    credentials,
                }),
            });

            if (!res.ok) {
                const data = await res.json().catch(() => null);
                setError(data?.error || "Failed to connect. Please check your credentials.");
                return;
            }

            router.push(`/dashboard/projects/${projectId}/integrations`);
        } catch {
            setError("Network error. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex w-full h-full items-center justify-center p-8">
            <div className="w-full max-w-md">
                <form onSubmit={handleSubmit}>
                    <FieldGroup>
                        {schema.sections.map((section, sectionIndex) => (
                            <React.Fragment key={sectionIndex}>
                                {sectionIndex > 0 && <FieldSeparator />}
                                <FieldSet>
                                    <FieldLegend
                                        className={
                                            sectionIndex === 0
                                                ? "flex items-center gap-2 text-2xl! font-semibold! mb-5"
                                                : undefined
                                        }
                                    >
                                        {sectionIndex === 0 && (
                                            <Image
                                                src={schema.logo}
                                                alt={schema.logoAlt}
                                                width={32}
                                                height={32}
                                                className="h-8 w-8 object-contain"
                                            />
                                        )}
                                        {section.legend}
                                    </FieldLegend>
                                    <FieldDescription>{section.description}</FieldDescription>
                                    {section.fields.map((field) => (
                                        <Field key={field.key}>
                                            <FieldLabel>{field.label}</FieldLabel>
                                            {field.type === "select" ? (
                                                <Select
                                                    value={values[field.key] || ""}
                                                    onValueChange={(value) =>
                                                        handleChange(field.key, value)
                                                    }
                                                    required={field.required}
                                                    disabled={loading}
                                                >
                                                    <SelectTrigger>
                                                        <SelectValue placeholder={field.placeholder || "Select..."} />
                                                    </SelectTrigger>
                                                    <SelectContent>
                                                        {field.options?.map((option) => (
                                                            <SelectItem key={option.value} value={option.value}>
                                                                {option.label}
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>
                                            ) : (
                                                <Input
                                                    type={field.type || "text"}
                                                    placeholder={field.placeholder}
                                                    value={values[field.key] || ""}
                                                    onChange={(e) =>
                                                        handleChange(field.key, e.target.value)
                                                    }
                                                    required={field.required}
                                                    disabled={loading}
                                                />
                                            )}
                                        </Field>
                                    ))}
                                </FieldSet>
                            </React.Fragment>
                        ))}

                        {error && <FieldError>{error}</FieldError>}

                        <Field orientation="horizontal">
                            <Button type="submit" disabled={loading}>
                                {loading ? "Connecting..." : "Connect"}
                            </Button>
                            <Button
                                variant="outline"
                                type="button"
                                onClick={() =>
                                    router.push(
                                        `/dashboard/projects/${projectId}/integrations`,
                                    )
                                }
                                disabled={loading}
                            >
                                Cancel
                            </Button>
                        </Field>
                    </FieldGroup>
                </form>
            </div>
        </div>
    );
}
