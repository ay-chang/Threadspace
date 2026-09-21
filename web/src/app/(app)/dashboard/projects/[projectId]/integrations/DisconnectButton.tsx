"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

type Props = {
    projectId: string;
    integrationId: string;
    name: string;
};

/**
 * Two-step confirm rather than a modal: disconnecting destroys the stored
 * credentials and cannot be undone, so the click that does it should never be
 * the same click that started the interaction.
 */
export default function DisconnectButton({ projectId, integrationId, name }: Props) {
    const router = useRouter();
    const [confirming, setConfirming] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleDisconnect = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(
                `/api/integrations/${projectId}/disconnect/${integrationId}`,
                { method: "DELETE" },
            );
            if (!res.ok) {
                setError("Failed to disconnect. Please try again.");
                setConfirming(false);
                return;
            }
            router.refresh();
        } catch {
            setError("Network error. Please try again.");
            setConfirming(false);
        } finally {
            setLoading(false);
        }
    };

    if (error) {
        return (
            <div className="flex items-center gap-2">
                <span className="text-destructive text-xs">{error}</span>
                <Button variant="outline" size="sm" onClick={() => setError(null)}>
                    Retry
                </Button>
            </div>
        );
    }

    if (confirming) {
        return (
            <div className="flex items-center gap-2">
                <span className="text-muted-foreground hidden text-xs sm:inline">
                    Remove {name} and its credentials?
                </span>
                <Button
                    variant="destructive"
                    size="sm"
                    onClick={handleDisconnect}
                    disabled={loading}
                >
                    {loading ? "Removing..." : "Confirm"}
                </Button>
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setConfirming(false)}
                    disabled={loading}
                >
                    Cancel
                </Button>
            </div>
        );
    }

    return (
        <Button variant="outline" size="sm" onClick={() => setConfirming(true)}>
            Disconnect
        </Button>
    );
}
