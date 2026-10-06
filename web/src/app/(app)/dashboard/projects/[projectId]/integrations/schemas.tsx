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
    /**
     * Field whose value becomes the integration's display name. Omit it when
     * every field is a secret; the backend then names the integration itself.
     */
    displayNameField?: string;
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
        help: {
            href: "/dashboard/docs/vercel",
            label: "How to connect Vercel",
        },
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
        help: {
            href: "/dashboard/docs/aws",
            label: "How to connect AWS",
        },
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
                        one? Follow the step-by-step guide linked below.
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
    stripe: {
        backendType: "STRIPE",
        name: "Stripe",
        logo: "/integrations/stripe.svg",
        logoAlt: "Stripe logo",
        help: {
            href: "/dashboard/docs/stripe",
            label: "How to connect Stripe",
        },
        sections: [
            {
                legend: "Connect Stripe",
                description: (
                    <>
                        Paste a restricted API key with read access. A restricted key is safer than
                        your secret key: it can only read the data Threadspace shows. The guide
                        linked below walks through creating one.
                    </>
                ),
                fields: [
                    {
                        key: "apiKey",
                        label: "Restricted API Key",
                        placeholder: "rk_live_...",
                        required: true,
                        type: "password",
                    },
                ],
            },
        ],
    },
    apple: {
        backendType: "APPLE_CONNECT",
        name: "App Store Connect",
        logo: "/integrations/apple.svg",
        logoAlt: "Apple logo",
        displayNameField: "keyId",
        help: {
            href: "/dashboard/docs/app-store-connect",
            label: "How to connect App Store Connect",
        },
        sections: [
            {
                legend: "Connect App Store Connect",
                description: (
                    <>
                        Create an App Store Connect API key with the Sales role, then enter its
                        details here. The guide linked below shows where each value lives.
                    </>
                ),
                fields: [
                    {
                        key: "issuerId",
                        label: "Issuer ID",
                        placeholder: "57246542-96fe-1a63-e053-0824d011072a",
                        required: true,
                    },
                    {
                        key: "keyId",
                        label: "Key ID",
                        placeholder: "2X9R4HXF34",
                        required: true,
                    },
                    {
                        key: "privateKey",
                        label: "Private Key (.p8)",
                        placeholder: "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
                        required: true,
                        type: "textarea",
                        upload: {
                            accept: ".p8",
                            hint: "Upload the AuthKey_XXXXXXXXXX.p8 file, or paste its contents below.",
                        },
                    },
                ],
            },
            {
                legend: "Sales Reports",
                description:
                    "Your vendor number identifies which sales reports to read. Find it under Payments and Financial Reports.",
                fields: [
                    {
                        key: "vendorNumber",
                        label: "Vendor Number",
                        placeholder: "85012345",
                        required: true,
                    },
                ],
            },
        ],
    },
    google_analytics: {
        backendType: "GOOGLE_ANALYTICS",
        name: "Google Analytics",
        logo: "/integrations/google_analytics.svg",
        logoAlt: "Google Analytics logo",
        displayNameField: "propertyId",
        help: {
            href: "/dashboard/docs/google-analytics",
            label: "How to connect Google Analytics",
        },
        sections: [
            {
                legend: "Connect Google Analytics",
                description: (
                    <>
                        Enter your GA4 Property ID and a service account key that has been added to
                        the property as a Viewer. The guide linked below covers both.
                    </>
                ),
                fields: [
                    {
                        key: "propertyId",
                        label: "Property ID",
                        placeholder: "123456789",
                        required: true,
                    },
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
