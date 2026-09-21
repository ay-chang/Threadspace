import React from "react";

export type WidgetDef = {
    id: string;
    label: string;
    description: string;
};

export type VercelSummary = {
    id: string;
    name: string;
    framework: string | null;
    repo: string | null;
    domains: { name: string; verified: boolean; primary: boolean }[];
    envs: { key: string; target: string[] }[];
    deployments: {
        id: string;
        url: string | null;
        state: string;
        target: string | null;
        createdAt: number;
    }[];
};

export const WIDGET_REGISTRY: Record<string, WidgetDef[]> = {
    vercel: [
        { id: "project-info", label: "Project Info", description: "Name, framework, and repository" },
        { id: "deployments", label: "Deployments", description: "Recent deployment history" },
        { id: "domains", label: "Domains", description: "Custom domains and verification" },
        { id: "env-vars", label: "Env Variables", description: "Environment variable keys" },
    ],
};

function timeAgo(timestamp: number): string {
    const seconds = Math.floor((Date.now() - timestamp) / 1000);
    if (seconds < 60) return "just now";
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
}

const DEPLOY_STATE_STYLES: Record<string, string> = {
    READY: "bg-green-500",
    BUILDING: "bg-amber-500",
    ERROR: "bg-red-500",
    CANCELED: "bg-gray-400",
    INITIALIZING: "bg-blue-400",
};

export function ProjectInfoWidget({ summary }: { summary: VercelSummary }) {
    return (
        <div className="space-y-2">
            <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Project</span>
                <span className="font-medium">{summary.name}</span>
            </div>
            {summary.framework && (
                <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Framework</span>
                    <span className="font-medium capitalize">{summary.framework}</span>
                </div>
            )}
            {summary.repo && (
                <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Repository</span>
                    <a
                        href={summary.repo.startsWith("http") ? summary.repo : `https://github.com/${summary.repo}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="font-medium text-blue-600 hover:underline truncate"
                    >
                        {summary.repo}
                    </a>
                </div>
            )}
        </div>
    );
}

export function DeploymentsWidget({ summary }: { summary: VercelSummary }) {
    if (summary.deployments.length === 0) {
        return <p className="text-sm text-muted-foreground">No deployments yet</p>;
    }

    return (
        <div className="space-y-2">
            {summary.deployments.slice(0, 5).map((deploy) => (
                <div key={deploy.id} className="flex items-center gap-2 text-sm">
                    <span
                        className={`inline-block w-2 h-2 rounded-full ${DEPLOY_STATE_STYLES[deploy.state] ?? "bg-gray-400"}`}
                    />
                    <span className="font-medium capitalize">
                        {(deploy.target ?? "preview").toLowerCase()}
                    </span>
                    <span className="text-muted-foreground ml-auto">{timeAgo(deploy.createdAt)}</span>
                </div>
            ))}
        </div>
    );
}

export function DomainsWidget({ summary }: { summary: VercelSummary }) {
    if (summary.domains.length === 0) {
        return <p className="text-sm text-muted-foreground">No custom domains</p>;
    }

    return (
        <div className="space-y-2">
            {summary.domains.map((domain) => (
                <div key={domain.name} className="flex items-center gap-2 text-sm">
                    <span className="font-medium">{domain.name}</span>
                    {domain.primary && (
                        <span className="text-xs bg-muted px-1.5 py-0.5 rounded">primary</span>
                    )}
                    <span
                        className={`ml-auto text-xs ${domain.verified ? "text-green-600" : "text-amber-600"}`}
                    >
                        {domain.verified ? "verified" : "unverified"}
                    </span>
                </div>
            ))}
        </div>
    );
}

export function EnvVarsWidget({ summary }: { summary: VercelSummary }) {
    if (summary.envs.length === 0) {
        return <p className="text-sm text-muted-foreground">No environment variables</p>;
    }

    return (
        <div className="space-y-2">
            {summary.envs.map((env) => (
                <div key={env.key} className="flex items-center justify-between text-sm">
                    <code className="font-mono text-xs">{env.key}</code>
                    <span className="text-xs text-muted-foreground">
                        {env.target?.join(", ") ?? "all"}
                    </span>
                </div>
            ))}
        </div>
    );
}

export const VERCEL_WIDGET_COMPONENTS: Record<string, React.ComponentType<{ summary: VercelSummary }>> = {
    "project-info": ProjectInfoWidget,
    "deployments": DeploymentsWidget,
    "domains": DomainsWidget,
    "env-vars": EnvVarsWidget,
};
