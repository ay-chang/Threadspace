"use client";

import React, { useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

type Props = {
    value: string;
    onChange: (value: string) => void;
    accept: string;
    hint: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
};

/**
 * A credential field that accepts either an uploaded file or pasted text.
 *
 * The textarea overrides the base component's `field-sizing-content`: a service
 * account key holds a ~1700 character private_key with no whitespace in it, and
 * content-sizing grows the element to fit that on one line, which drags the
 * whole page into horizontal scroll. Fixed sizing plus break-all keeps it inside
 * the form.
 */
export default function CredentialFileField({
    value,
    onChange,
    accept,
    hint,
    placeholder,
    required,
    disabled,
}: Props) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [fileName, setFileName] = useState<string | null>(null);
    const [dragging, setDragging] = useState(false);
    const [readError, setReadError] = useState<string | null>(null);

    const readFile = (file: File) => {
        setReadError(null);
        const reader = new FileReader();
        reader.onload = () => {
            onChange(String(reader.result ?? "").trim());
            setFileName(file.name);
        };
        reader.onerror = () => setReadError("Could not read that file.");
        reader.readAsText(file);
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setDragging(false);
        if (disabled) return;
        const file = e.dataTransfer.files?.[0];
        if (file) readFile(file);
    };

    return (
        <div className="flex w-full min-w-0 flex-col gap-2">
            <div
                onDragOver={(e) => {
                    e.preventDefault();
                    if (!disabled) setDragging(true);
                }}
                onDragLeave={() => setDragging(false)}
                onDrop={handleDrop}
                className={`flex flex-col items-center gap-2 rounded-md border border-dashed px-4 py-5 text-center transition-colors ${
                    dragging ? "border-ring bg-accent/50" : "border-input"
                } ${disabled ? "opacity-50" : ""}`}
            >
                <p className="text-muted-foreground text-xs">{hint}</p>
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={disabled}
                    onClick={() => inputRef.current?.click()}
                >
                    Choose file
                </Button>
                {fileName && (
                    <p className="text-muted-foreground text-xs">
                        Loaded <span className="font-medium">{fileName}</span>
                    </p>
                )}
                {readError && <p className="text-destructive text-xs">{readError}</p>}
                <input
                    ref={inputRef}
                    type="file"
                    accept={accept}
                    className="hidden"
                    onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) readFile(file);
                        // Allow re-picking the same file.
                        e.target.value = "";
                    }}
                />
            </div>

            <Textarea
                placeholder={placeholder}
                value={value}
                onChange={(e) => {
                    onChange(e.target.value);
                    if (fileName) setFileName(null);
                }}
                required={required}
                disabled={disabled}
                rows={10}
                spellCheck={false}
                wrap="soft"
                className="w-full max-w-full resize-y overflow-auto font-mono text-xs break-all [field-sizing:fixed]"
            />
        </div>
    );
}
