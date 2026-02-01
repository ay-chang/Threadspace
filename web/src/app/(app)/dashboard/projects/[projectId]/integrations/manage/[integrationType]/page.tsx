import Link from "next/link";
import ManagePage from "./ManagePage";
import { INTEGRATION_SCHEMAS } from "../../schemas";

type PageProps = {
    params: Promise<{ projectId: string; integrationType: string }>;
};

const BACKEND_BASE = process.env.BACKEND_BASE!;
const INTERNAL_SYNC_TOKEN = process.env.INTERNAL_SYNC_TOKEN!;

export default async function ManageIntegration({ params }: PageProps) {
    const { projectId, integrationType } = await params;
    const schema = INTEGRATION_SCHEMAS[integrationType];

    if (!schema) {
        return (
            <div className="w-full px-4 sm:px-6 lg:px-10 py-6">
                <Link
                    href={`/dashboard/projects/${projectId}/integrations`}
                    className="text-sm text-muted-foreground hover:text-foreground"
                >
                    ← Back to Integrations
                </Link>
                <p className="text-sm text-muted-foreground mt-4">
                    Unknown integration type.
                </p>
            </div>
        );
    }

    let credentials: Record<string, string> = {};
    try {
        const res = await fetch(
            `${BACKEND_BASE}/projects/${projectId}/integrations/credentials/${schema.backendType}`,
            { headers: { "x-internal-token": INTERNAL_SYNC_TOKEN } }
        );
        if (res.ok) credentials = await res.json();
    } catch {}

    let summary = null;
    if (integrationType === "vercel") {
        try {
            const res = await fetch(`${BACKEND_BASE}/projects/${projectId}/vercel/summary`, {
                headers: { "x-internal-token": INTERNAL_SYNC_TOKEN },
            });
            if (res.ok) summary = await res.json();
        } catch {}
    }

    return (
        <ManagePage
            projectId={projectId}
            integrationType={integrationType}
            credentials={credentials}
            summary={summary}
        />
    );
}
