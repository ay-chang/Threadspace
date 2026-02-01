import React from "react";

export type FieldDef = {
    key: string;
    label: string;
    placeholder: string;
    required?: boolean;
    type?: "text" | "password";
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
};

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
                        placeholder: "us-east-1",
                        required: true,
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
};
