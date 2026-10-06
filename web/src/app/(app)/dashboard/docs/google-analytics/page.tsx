import Link from "next/link";
import {
    GuideShell,
    GuideHeader,
    Step,
    ExternalLink,
    Code,
    Strong,
    Warning,
    Note,
    Troubleshooting,
} from "../_components/guide";
import { Step4Diagram as ServiceAccountKeyDiagram } from "../gcp/diagrams";
import {
    PropertyIdDiagram,
    EnableApiDiagram,
    PropertyAccessDiagram,
    ConnectDiagram,
} from "./diagrams";

export const metadata = {
    title: "How to connect Google Analytics",
};

export default function GoogleAnalyticsSetupGuide() {
    return (
        <GuideShell>
            <GuideHeader
                logo="/integrations/google_analytics.svg"
                logoAlt="Google Analytics logo"
                title="How to connect Google Analytics"
                prerequisites={{
                    label: "Before you start",
                    items: [
                        <>
                            A <Strong>GA4</Strong> property. Universal Analytics was shut down in 2024, so
                            older <Code>UA-</Code> properties can&apos;t be connected.
                        </>,
                        <>
                            The <Strong>Administrator</Strong> role on that property, so you can grant
                            access to it.
                        </>,
                        <>
                            A Google Cloud project to hold the service account. It can be any project —
                            it doesn&apos;t need billing for this.
                        </>,
                    ],
                }}
            >
                <p>
                    Threadspace reads your analytics with a <Strong>service account</Strong> — a robot
                    Google account that you invite into your GA4 property as a read-only Viewer, the
                    same way you&apos;d invite a teammate. You&apos;ll create the account, give it
                    access, and paste its key into Threadspace. Takes about five minutes.
                </p>
            </GuideHeader>

            <div className="space-y-12">
                <Step n={1} title="Find your Property ID" diagram={<PropertyIdDiagram />}>
                    <p>
                        In{" "}
                        <ExternalLink href="https://analytics.google.com/">Google Analytics</ExternalLink>
                        , open <Strong>Admin</Strong> (the gear icon), then{" "}
                        <Strong>Property settings → Property details</Strong>. Copy the{" "}
                        <Strong>Property ID</Strong> shown at the top right — digits only, like{" "}
                        <Code>123456789</Code>.
                    </p>
                    <Warning>
                        Don&apos;t use the <Strong>Measurement ID</Strong> (<Code>G-ABC123XYZ</Code>) from{" "}
                        <Strong>Data streams</Strong>. That&apos;s for your site&apos;s tracking tag; the
                        API only accepts the numeric Property ID. Threadspace will tell you if you paste
                        the wrong one.
                    </Warning>
                </Step>

                <Step n={2} title="Create a service account and download its key" diagram={<ServiceAccountKeyDiagram />}>
                    <p>
                        In{" "}
                        <ExternalLink href="https://console.cloud.google.com/iam-admin/serviceaccounts">
                            Google Cloud → IAM &amp; Admin → Service Accounts
                        </ExternalLink>
                        , create a service account named something like <Code>threadspace</Code>. Then
                        open it, go to <Strong>Keys → Add Key → Create new key → JSON</Strong>, and keep
                        the file that downloads.
                    </p>
                    <p>
                        These are steps 2 and 4 of the{" "}
                        <Link href="/dashboard/docs/gcp" className="text-primary font-medium underline underline-offset-4">
                            Google Cloud guide
                        </Link>{" "}
                        — with one difference: <Strong>skip the role step</Strong>. Access to analytics is
                        granted inside Google Analytics in step 4, not in Cloud IAM.
                    </p>
                    <Note>
                        Already connected Google Cloud to Threadspace? You can reuse that same key file
                        here — just complete steps 3 and 4 for it.
                    </Note>
                </Step>

                <Step n={3} title="Enable the Google Analytics Data API" diagram={<EnableApiDiagram />}>
                    <p>
                        With the service account&apos;s project selected, open the{" "}
                        <ExternalLink href="https://console.cloud.google.com/apis/library/analyticsdata.googleapis.com">
                            Google Analytics Data API
                        </ExternalLink>{" "}
                        page and click <Strong>Enable</Strong>.
                    </p>
                    <p>
                        Google refuses API calls from projects that haven&apos;t enabled the API, even
                        when everything else is right. It can take a minute to take effect.
                    </p>
                </Step>

                <Step n={4} title="Give the service account access to your property" diagram={<PropertyAccessDiagram />}>
                    <p>
                        Open the key file and copy the <Code>client_email</Code> value — it ends in{" "}
                        <Code>.iam.gserviceaccount.com</Code>.
                    </p>
                    <p>
                        Back in Google Analytics, go to{" "}
                        <Strong>Admin → Property access management</Strong>, click <Strong>+</Strong>{" "}
                        then <Strong>Add users</Strong>. Paste the email, choose the{" "}
                        <Strong>Viewer</Strong> role, and click <Strong>Add</Strong>. Leave{" "}
                        <Strong>Notify new users by email</Strong> unchecked — the address has no inbox.
                    </p>
                    <Warning>
                        This is the step that gets missed. Without it the key is perfectly valid but has
                        no permission to read your property, and connecting fails with an access error.
                    </Warning>
                </Step>

                <Step n={5} title="Hand it to Threadspace" diagram={<ConnectDiagram />}>
                    <p>
                        On the Connect Google Analytics screen, enter the <Strong>Property ID</Strong>{" "}
                        and upload the <Code>.json</Code> key file (or paste its contents). Threadspace
                        checks the key with Google and runs a small report against your property before
                        saving, so if it&apos;s accepted, it works.
                    </p>
                    <Note>
                        Treat the key file like a password — anyone with it can read your analytics.
                        Don&apos;t commit it to git.
                    </Note>
                </Step>
            </div>

            <Troubleshooting
                entries={[
                    {
                        q: "“That's a Measurement ID”",
                        a: "You copied the G- ID from Data streams. Use the numeric Property ID from Admin → Property details instead.",
                    },
                    {
                        q: "“The service account … can't read GA4 property …”",
                        a: "Step 4 is missing or was done on a different property. Add the exact client_email from the key file under Property access management with the Viewer role.",
                    },
                    {
                        q: "“The Google Analytics Data API isn't enabled …”",
                        a: "Step 3 is missing, or the API was enabled in a different Cloud project than the one the service account belongs to. The error names the right project and links straight to it.",
                    },
                    {
                        q: "“Google rejected this service account key”",
                        a: "The key was deleted, or the service account was disabled. Create a new JSON key for the account and connect again.",
                    },
                    {
                        q: "It connects, but everything shows zero",
                        a: "The credentials worked, but GA has no data for the period — usually because the tracking tag isn't installed on the site, or the property is brand new. Data typically starts appearing within a day.",
                    },
                ]}
            />
        </GuideShell>
    );
}
