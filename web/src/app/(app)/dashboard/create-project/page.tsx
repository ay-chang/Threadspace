"use client";

import { Button } from "@/components/ui/button";
import {
    Field,
    FieldDescription,
    FieldGroup,
    FieldLabel,
    FieldLegend,
    FieldSeparator,
    FieldSet,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useRouter } from "next/navigation";
import { useState } from "react";

type ProjectType = "web" | "ios" | "backend" | "fullstack" | "android" | "other";

export default function CreateProjectPage() {
    const router = useRouter();

    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [type, setType] = useState<ProjectType>();

    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            const res = await fetch("/api/projects", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ name, description, type }),
            });

            console.log("Response: ", res);

            if (!res.ok) throw new Error("Failed to create project");
            const project = await res.json();
            router.push(`/dashboard/projects/${project.id}/integrations`);
        } catch (err) {
            console.error(err);
            alert("Could not create project.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex w-full h-full items-center justify-center p-8">
            <div className="w-full max-w-md">
                <form onSubmit={handleSubmit}>
                    <FieldGroup>
                        <FieldSet>
                            <FieldLegend>Create a new Project</FieldLegend>
                            <FieldDescription>
                                Create a new project and hook it up with integrations to a
                                bunch of different services.
                            </FieldDescription>
                            <Field>
                                <FieldLabel>Project Name</FieldLabel>
                                <Input
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    placeholder="This is a sample title"
                                    required
                                />
                            </Field>
                            <Field>
                                <FieldLabel>Project Description</FieldLabel>
                                <Textarea
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                    placeholder="This is a sample description"
                                />
                            </Field>
                        </FieldSet>

                        <FieldSeparator />

                        <FieldSet>
                            <FieldLegend>Project Settings</FieldLegend>
                            <FieldDescription>
                                Select a project type so that we can give recomendations of
                                which integrations you can hook up to your project.
                            </FieldDescription>
                            <Field>
                                <FieldLabel>Project Type</FieldLabel>
                                <Select
                                    value={type}
                                    onValueChange={(v) => setType(v as ProjectType)}
                                >
                                    <SelectTrigger
                                        className="w-full"
                                        aria-label="Project Type"
                                    >
                                        <SelectValue placeholder="Select a type" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value="web">Web</SelectItem>
                                        <SelectItem value="ios">iOS (Mobile)</SelectItem>
                                        <SelectItem value="backend">Backend/API</SelectItem>
                                        <SelectItem value="android">Android</SelectItem>
                                        <SelectItem value="fullstack">Full Stack</SelectItem>
                                        <SelectItem value="other">Other</SelectItem>
                                    </SelectContent>
                                </Select>
                            </Field>
                        </FieldSet>

                        <Field orientation="horizontal">
                            <Button type="submit" disabled={loading}>
                                {loading ? "Creating..." : "Create Project"}
                            </Button>
                            <Button variant="outline" type="button">
                                Cancel
                            </Button>
                        </Field>
                    </FieldGroup>
                </form>
            </div>
        </div>
    );
}
