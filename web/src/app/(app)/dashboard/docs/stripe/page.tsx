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
    ApiKeysDiagram,
    PermissionsDiagram,
    RevealKeyDiagram,
    ConnectDiagram,
} from "./diagrams";

export const metadata = {
    title: "How to connect Stripe",
};

export default function StripeSetupGuide() {
    return (
        <GuideShell>
            <GuideHeader
                logo="/integrations/stripe.svg"
                logoAlt="Stripe logo"
                title="How to connect Stripe"
                prerequisites={{
                    label: "Before you start",
                    items: [
                        <>
                            Access to your Stripe dashboard with permission to create API keys —
                            typically the Administrator or Developer role.
                        </>,
                        <>
                            Want to try it safely first? Switch the dashboard to{" "}
                            <Strong>Test mode</Strong> and create a test key. Threadspace labels
                            test data clearly, and you can reconnect with a live key later.
                        </>,
                    ],
                }}
            >
                <p>
                    Threadspace reads your revenue, subscriptions and payments through a{" "}
                    <Strong>restricted API key</Strong> — a key you create that can only read the
                    specific data you allow. It can&apos;t issue refunds, move money, or change
                    anything in your account. Takes about three minutes.
                </p>
            </GuideHeader>

            <div className="space-y-12">
                <Step n={1} title="Open your API keys" diagram={<ApiKeysDiagram />}>
                    <p>
                        In the Stripe dashboard, go to{" "}
                        <ExternalLink href="https://dashboard.stripe.com/apikeys">
                            Developers → API keys
                        </ExternalLink>{" "}
                        and click <Strong>Create restricted key</Strong>. If Stripe asks how
                        you&apos;ll use the key, either option works — what matters is the
                        permissions you set next.
                    </p>
                    <Warning>
                        Don&apos;t use the <Strong>publishable key</Strong> (<Code>pk_live_…</Code>).
                        It&apos;s meant for web pages and can&apos;t read any account data, so
                        connecting with it fails. Your <Strong>secret key</Strong> (
                        <Code>sk_live_…</Code>) would work, but it grants full control of your
                        account — a restricted key is the safer choice.
                    </Warning>
                </Step>

                <Step n={2} title="Grant read access to four resources" diagram={<PermissionsDiagram />}>
                    <p>
                        Name the key something like <Code>Threadspace</Code>, then set these
                        permissions to <Strong>Read</Strong>:
                    </p>
                    <ul className="list-disc space-y-1 pl-5">
                        <li>
                            <Strong>Balance</Strong> — your available and pending balance
                        </li>
                        <li>
                            <Strong>Charges</Strong> — payments, refunds and failures
                        </li>
                        <li>
                            <Strong>Customers</Strong> — new customer counts
                        </li>
                        <li>
                            <Strong>Subscriptions</Strong> (under Billing) — active subscriptions and
                            MRR
                        </li>
                    </ul>
                    <p>Leave everything else at <Strong>None</Strong>, then create the key.</p>
                    <Note>
                        Threadspace checks each of these when you connect. If one is missing,
                        you&apos;ll get a message naming exactly which permission to add — no
                        guessing.
                    </Note>
                </Step>

                <Step n={3} title="Copy the key" diagram={<RevealKeyDiagram />}>
                    <p>
                        Copy the new key. It starts with <Code>rk_live_</Code> for your real account,
                        or <Code>rk_test_</Code> if you created it in test mode.
                    </p>
                    <Note>
                        Copy it right away — Stripe may not show a live key again after you leave the
                        page. If you lose it, just create another. Treat it like a password: anyone
                        with it can read your payment data.
                    </Note>
                </Step>

                <Step n={4} title="Hand it to Threadspace" diagram={<ConnectDiagram />}>
                    <p>
                        Back on the Connect Stripe screen, paste the key and click{" "}
                        <Strong>Connect</Strong>. Threadspace verifies it against Stripe before
                        saving, so if it&apos;s accepted, it works.
                    </p>
                </Step>
            </div>

            <Troubleshooting
                entries={[
                    {
                        q: "“That's a publishable key”",
                        a: "You copied the pk_ key from the Standard keys section. Create a restricted key (rk_) as in step 1 instead.",
                    },
                    {
                        q: "“This key doesn't have permission to read …”",
                        a: (
                            <>
                                One of the four permissions from step 2 is still set to None. Edit
                                the restricted key in Stripe, set the named resource to{" "}
                                <Strong>Read</Strong>, and connect again with the same key.
                            </>
                        ),
                    },
                    {
                        q: "“Stripe rejected this key”",
                        a: "The key was rolled, deleted, or mistyped. Create a fresh restricted key and paste it again.",
                    },
                    {
                        q: "The numbers don't quite match Stripe's dashboard",
                        a: "Threadspace shows volume before Stripe's fees, groups days in UTC rather than your account's time zone, and calculates MRR before discounts and tax. Totals over a full period line up; individual days near midnight may land on a different date.",
                    },
                ]}
            />
        </GuideShell>
    );
}
