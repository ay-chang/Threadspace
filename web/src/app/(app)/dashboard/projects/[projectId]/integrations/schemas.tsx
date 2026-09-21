import React from "react";

export type FieldDef = {
    key: string;
    label: string;
    placeholder?: string;
    required?: boolean;
    type?: "text" | "password" | "select" | "textarea";
    options?: { value: string; label: string }[];
    /** Textarea fields only: also accept the value as an uploaded file. */
    upload?: { accept: string; hint: string };
};

export type SectionDef = {
    legend: string;
    description: React.ReactNode;
    fields: FieldDef[];
};

export type IntegrationSchema = {
    backendType: string;
    name: string;
    logo: string;
    logoAlt: string;
    displayNameField: string;
    sections: SectionDef[];
    /** Optional link to a setup walkthrough, opened in a new tab. */
    help?: { href: string; label: string };
};

const GCP_REGIONS = [
    { value: "us-central1", label: "US Central (Iowa)" },
    { value: "us-east1", label: "US East (South Carolina)" },
    { value: "us-east4", label: "US East (N. Virginia)" },
    { value: "us-west1", label: "US West (Oregon)" },
    { value: "us-west2", label: "US West (Los Angeles)" },
    { value: "us-west3", label: "US West (Salt Lake City)" },
    { value: "us-west4", label: "US West (Las Vegas)" },
    { value: "europe-west1", label: "Europe West (Belgium)" },
    { value: "europe-west2", label: "Europe West (London)" },
    { value: "europe-west3", label: "Europe West (Frankfurt)" },
    { value: "europe-west4", label: "Europe West (Netherlands)" },
    { value: "europe-north1", label: "Europe North (Finland)" },
    { value: "asia-east1", label: "Asia East (Taiwan)" },
    { value: "asia-east2", label: "Asia East (Hong Kong)" },
    { value: "asia-northeast1", label: "Asia Northeast (Tokyo)" },
    { value: "asia-northeast2", label: "Asia Northeast (Osaka)" },
    { value: "asia-south1", label: "Asia South (Mumbai)" },
    { value: "asia-southeast1", label: "Asia Southeast (Singapore)" },
    { value: "asia-southeast2", label: "Asia Southeast (Jakarta)" },
    { value: "australia-southeast1", label: "Australia Southeast (Sydney)" },
    { value: "southamerica-east1", label: "South America East (São Paulo)" },
];

export const INTEGRATION_SCHEMAS: Record<string, IntegrationSchema> = {
    vercel: {
        backendType: "VERCEL",
        name: "Vercel",
        logo: "/vercel-logo.svg",
        logoAlt: "Vercel logo",
        displayNameField: "projectName",
        sections: [
            {
                legend: "Connect Vercel",
                description: (
                    <>
                        Enter your Vercel credentials to connect this project. You can create
                        an API token at{" "}
                        <a
                            href="https://vercel.com/account/tokens"
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            Vercel Settings → Tokens
                        </a>
                        .
                    </>
                ),
                fields: [
                    {
                        key: "apiToken",
                        label: "API Token",
                        placeholder: "vercel_",
                        required: true,
                    },
                    {
                        key: "projectName",
                        label: "Project Name",
                        placeholder: "my-app",
                        required: true,
                    },
                ],
            },
            {
                legend: "Team Settings",
                description:
                    "If your project belongs to a Vercel team, enter the Team ID here. Leave blank for personal accounts.",
                fields: [
                    {
                        key: "teamId",
                        label: "Team ID",
                        placeholder: "team_",
                    },
                ],
            },
        ],
    },
    aws: {
        backendType: "AWS",
        name: "AWS",
        logo: "/integrations/aws.webp",
        logoAlt: "AWS logo",
        displayNameField: "region",
        sections: [
            {
                legend: "Connect AWS",
                description: (
                    <>
                        Enter your AWS credentials to connect this project. You can create
                        access keys in the{" "}
                        <a
                            href="https://console.aws.amazon.com/iam/home#/users"
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            AWS IAM Console
                        </a>
                        .
                    </>
                ),
                fields: [
                    {
                        key: "accessKeyId",
                        label: "Access Key ID",
                        placeholder: "AKIA...",
                        required: true,
                    },
                    {
                        key: "secretAccessKey",
                        label: "Secret Access Key",
                        placeholder: "Your secret key",
                        required: true,
                        type: "password",
                    },
                    {
                        key: "region",
                        label: "Region",
                        required: true,
                        type: "select",
                        options: [
                            { value: "us-east-1", label: "US East (N. Virginia)" },
                            { value: "us-east-2", label: "US East (Ohio)" },
                            { value: "us-west-1", label: "US West (N. California)" },
                            { value: "us-west-2", label: "US West (Oregon)" },
                            { value: "af-south-1", label: "Africa (Cape Town)" },
                            { value: "ap-east-1", label: "Asia Pacific (Hong Kong)" },
                            { value: "ap-south-1", label: "Asia Pacific (Mumbai)" },
                            { value: "ap-northeast-1", label: "Asia Pacific (Tokyo)" },
                            { value: "ap-northeast-2", label: "Asia Pacific (Seoul)" },
                            { value: "ap-northeast-3", label: "Asia Pacific (Osaka)" },
                            { value: "ap-southeast-1", label: "Asia Pacific (Singapore)" },
                            { value: "ap-southeast-2", label: "Asia Pacific (Sydney)" },
                            { value: "ca-central-1", label: "Canada (Central)" },
                            { value: "eu-central-1", label: "Europe (Frankfurt)" },
                            { value: "eu-west-1", label: "Europe (Ireland)" },
                            { value: "eu-west-2", label: "Europe (London)" },
                            { value: "eu-west-3", label: "Europe (Paris)" },
                            { value: "eu-north-1", label: "Europe (Stockholm)" },
                            { value: "eu-south-1", label: "Europe (Milan)" },
                            { value: "me-south-1", label: "Middle East (Bahrain)" },
                            { value: "sa-east-1", label: "South America (São Paulo)" },
                        ],
                    },
                ],
            },
            {
                legend: "Role Settings",
                description:
                    "Optionally specify an IAM role ARN to assume when accessing AWS resources.",
                fields: [
                    {
                        key: "roleArn",
                        label: "Role ARN",
                        placeholder: "arn:aws:iam::123456789:role/...",
                    },
                ],
            },
        ],
    },
    gcp: {
        backendType: "GOOGLE_CLOUD",
        name: "Google Cloud",
        logo: "/integrations/gcp.svg",
        logoAlt: "Google Cloud logo",
        displayNameField: "projectId",
        help: {
            href: "/dashboard/docs/gcp",
            label: "How to connect Google Cloud",
        },
        sections: [
            {
                legend: "Connect Google Cloud",
                description: (
                    <>
                        Upload or paste your service account key file. Not sure where to get
                        one? Follow the step-by-step guide linked above.
                    </>
                ),
                fields: [
                    {
                        key: "serviceAccountJson",
                        label: "Service Account JSON",
                        placeholder: '{\n  "type": "service_account",\n  "project_id": "my-gcp-project",\n  ...\n}',
                        required: true,
                        type: "textarea",
                        upload: {
                            accept: "application/json,.json",
                            hint: "Upload the .json key file, or paste its contents below.",
                        },
                    },
                ],
            },
        ],
    },
};
