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
import {
    IntegrationsDiagram,
    GenerateKeyDiagram,
    DownloadKeyDiagram,
    VendorNumberDiagram,
    ConnectDiagram,
} from "./diagrams";

export const metadata = {
    title: "How to connect App Store Connect",
};

export default function AppStoreConnectSetupGuide() {
    return (
        <GuideShell>
            <GuideHeader
                logo="/integrations/apple.svg"
                logoAlt="Apple logo"
                title="How to connect App Store Connect"
                prerequisites={{
                    label: "Before you start",
                    items: [
                        <>
                            The <Strong>Admin</Strong> role in App Store Connect, which is needed to
                            create API keys.
                        </>,
                        <>
                            API access enabled for your team. If the Integrations page shows a{" "}
                            <Strong>Request Access</Strong> button, your team&apos;s Account Holder
                            has to click it once first.
                        </>,
                    ],
                }}
            >
                <p>
                    Threadspace reads your downloads, in-app purchases and proceeds from
                    Apple&apos;s daily sales reports, using an{" "}
                    <Strong>App Store Connect API key</Strong>. You&apos;ll create a key limited to
                    sales data, download its private key file, and collect three identifiers.
                    Takes about five minutes.
                </p>
            </GuideHeader>

            <div className="space-y-12">
                <Step n={1} title="Open the App Store Connect API page" diagram={<IntegrationsDiagram />}>
                    <p>
                        Go to{" "}
                        <ExternalLink href="https://appstoreconnect.apple.com/access/integrations/api">
                            Users and Access → Integrations → App Store Connect API
                        </ExternalLink>{" "}
                        and select the <Strong>Team Keys</Strong> tab.
                    </p>
                    <p>
                        Copy the <Strong>Issuer ID</Strong> shown above the keys table — a long value
                        with dashes, like <Code>57246542-96fe-1a63-e053-0824d011072a</Code>. Every key
                        on your team shares it.
                    </p>
                </Step>

                <Step n={2} title="Generate a key with the Sales role" diagram={<GenerateKeyDiagram />}>
                    <p>
                        Click <Strong>+</Strong> (or <Strong>Generate API Key</Strong>), name it
                        something like <Code>Threadspace</Code>, and set <Strong>Access</Strong> to{" "}
                        <Strong>Sales</Strong>.
                    </p>
                    <Warning>
                        The role matters. Only <Strong>Sales</Strong>, <Strong>Finance</Strong> and{" "}
                        <Strong>Admin</Strong> keys can download sales reports. A Developer or App
                        Manager key will connect to Apple but fail on the sales check — Threadspace
                        will tell you so. Sales is the narrowest role that works.
                    </Warning>
                    <p>
                        Once it&apos;s created, the key appears in the table with its{" "}
                        <Strong>Key ID</Strong> — a 10-character value like <Code>2X9R4HXF34</Code>.
                        Copy that too.
                    </p>
                </Step>

                <Step n={3} title="Download the private key" diagram={<DownloadKeyDiagram />}>
                    <p>
                        Click <Strong>Download</Strong> next to the new key. You&apos;ll get a file
                        named <Code>AuthKey_2X9R4HXF34.p8</Code>, with your Key ID in the name.
                    </p>
                    <Note>
                        Apple only lets you download this file <Strong>once</Strong>. Keep it
                        somewhere safe, and never commit it to git — anyone with it can read your
                        sales data. If you lose it, revoke the key and generate a new one.
                    </Note>
                </Step>

                <Step n={4} title="Find your vendor number" diagram={<VendorNumberDiagram />}>
                    <p>
                        Open{" "}
                        <ExternalLink href="https://appstoreconnect.apple.com/itc/payments_and_financial_reports">
                            Payments and Financial Reports
                        </ExternalLink>
                        . Your <Strong>vendor number</Strong> is shown at the top left, next to your
                        legal entity name — digits only, like <Code>85012345</Code>.
                    </p>
                    <p>
                        It tells Apple whose sales reports to return, which is separate from which
                        key is asking.
                    </p>
                </Step>

                <Step n={5} title="Hand it to Threadspace" diagram={<ConnectDiagram />}>
                    <p>
                        Back on the Connect App Store Connect screen, fill in the{" "}
                        <Strong>Issuer ID</Strong> and <Strong>Key ID</Strong>, upload the{" "}
                        <Code>.p8</Code> file (or open it in a text editor and paste the whole thing),
                        and enter the <Strong>vendor number</Strong>.
                    </p>
                    <p>
                        Threadspace checks the key with Apple and confirms it can read sales reports
                        before saving, so if it&apos;s accepted, it works.
                    </p>
                    <Note>
                        Apple publishes each day&apos;s sales report about a day later, dated in
                        Pacific time. Yesterday&apos;s numbers may not appear until tomorrow — that&apos;s
                        normal, not a broken connection.
                    </Note>
                </Step>
            </div>

            <Troubleshooting
                entries={[
                    {
                        q: "“That looks like a Key ID in the Issuer ID field”",
                        a: "The two values sit next to each other and are easy to swap. The Issuer ID is the long one with dashes above the table; the Key ID is the 10-character one in the table.",
                    },
                    {
                        q: "“Apple rejected these credentials”",
                        a: "The Key ID doesn't match the .p8 file, the Issuer ID is from a different team, or the key was revoked. Check that the Key ID matches the one in the .p8 file's name.",
                    },
                    {
                        q: "“This API key can't download sales reports”",
                        a: (
                            <>
                                The key&apos;s role is Developer or App Manager. You can&apos;t change a
                                key&apos;s role after creating it — generate a new key with the{" "}
                                <Strong>Sales</Strong> role and connect with that.
                            </>
                        ),
                    },
                    {
                        q: "“Apple didn't accept vendor number …”",
                        a: "Double-check the number in Payments and Financial Reports. If your account belongs to more than one legal entity, make sure it's the vendor number for the apps you want to see.",
                    },
                    {
                        q: "It connects, but there are no reports",
                        a: "Apple only publishes a daily report when there was activity, and about a day late. A brand-new app, or one with no sales in the period, shows no reports — that isn't an error.",
                    },
                ]}
            />
        </GuideShell>
    );
}
