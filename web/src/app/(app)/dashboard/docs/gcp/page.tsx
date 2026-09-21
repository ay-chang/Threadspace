import Link from "next/link";
import Image from "next/image";
import {
    Step1Diagram,
    Step2Diagram,
    Step3Diagram,
    Step4Diagram,
    Step5Diagram,
} from "./diagrams";

export const metadata = {
    title: "How to connect Google Cloud",
};

function Step({
    n,
    title,
    children,
    diagram,
}: {
    n: number;
    title: string;
    children: React.ReactNode;
    diagram: React.ReactNode;
}) {
    return (
        <section className="scroll-mt-8" id={`step-${n}`}>
            <div className="mb-3 flex items-baseline gap-3">
                <span className="bg-primary text-primary-foreground flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
                    {n}
                </span>
                <h2 className="text-xl font-semibold">{title}</h2>
            </div>
            <div className="text-muted-foreground space-y-3 pl-10 text-sm leading-relaxed">
                {children}
            </div>
            <div className="mt-4 pl-10">{diagram}</div>
        </section>
    );
}

function ExternalLink({ href, children }: { href: string; children: React.ReactNode }) {
    return (
        <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary font-medium underline underline-offset-4"
        >
            {children}
        </a>
    );
}

export default function GcpSetupGuide() {
    return (
        <div className="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6">
            <header className="mb-10">
                <div className="mb-3 flex items-center gap-3">
                    <Image
                        src="/integrations/gcp.svg"
                        alt="Google Cloud logo"
                        width={36}
                        height={36}
                        className="h-9 w-9 object-contain"
                    />
                    <h1 className="text-3xl font-semibold">How to connect Google Cloud</h1>
                </div>
                <p className="text-muted-foreground text-sm leading-relaxed">
                    Threadspace reads your Cloud Storage buckets through a{" "}
                    <strong className="text-foreground font-medium">service account</strong> — a
                    robot account that belongs to your project rather than to you personally.
                    You&apos;ll create one, give it permission to read storage, download its key
                    file, and hand that file to Threadspace. Takes about five minutes.
                </p>
                <div className="bg-muted mt-4 rounded-lg p-4 text-sm">
                    <p className="text-foreground font-medium">Before you start</p>
                    <ul className="text-muted-foreground mt-2 list-disc space-y-1 pl-5">
                        <li>
                            A Google Cloud project with{" "}
                            <ExternalLink href="https://console.cloud.google.com/billing">
                                billing enabled
                            </ExternalLink>{" "}
                            — Cloud Storage refuses API calls without it, even on the free tier.
                        </li>
                        <li>
                            The{" "}
                            <ExternalLink href="https://console.cloud.google.com/apis/library/storage.googleapis.com">
                                Cloud Storage API
                            </ExternalLink>{" "}
                            enabled on that project.
                        </li>
                    </ul>
                </div>
            </header>

            <div className="space-y-12">
                <Step n={1} title="Find your project" diagram={<Step1Diagram />}>
                    <p>
                        Open the{" "}
                        <ExternalLink href="https://console.cloud.google.com/">
                            Google Cloud console
                        </ExternalLink>{" "}
                        and pick the project you want to connect from the dropdown in the top bar.
                    </p>
                    <p>
                        Note its <strong className="text-foreground font-medium">Project ID</strong>,
                        not its display name. The ID is lowercase and usually has a number on the
                        end, like <code className="bg-muted rounded px-1.5 py-0.5 text-xs">my-project-401516</code>.
                        You won&apos;t have to type it anywhere — Threadspace reads it out of the key
                        file — but it&apos;s how you&apos;ll confirm you connected the right project.
                    </p>
                </Step>

                <Step n={2} title="Create a service account" diagram={<Step2Diagram />}>
                    <p>
                        Go to{" "}
                        <ExternalLink href="https://console.cloud.google.com/iam-admin/serviceaccounts">
                            IAM &amp; Admin → Service Accounts
                        </ExternalLink>{" "}
                        and click{" "}
                        <strong className="text-foreground font-medium">Create Service Account</strong>.
                    </p>
                    <p>
                        Name it something you&apos;ll recognise later —{" "}
                        <code className="bg-muted rounded px-1.5 py-0.5 text-xs">threadspace-integration</code>{" "}
                        works well. The email address is generated for you and ends in{" "}
                        <code className="bg-muted rounded px-1.5 py-0.5 text-xs">.iam.gserviceaccount.com</code>.
                        That is <em>not</em> your own Google account address.
                    </p>
                </Step>

                <Step n={3} title="Grant the Storage Admin role" diagram={<Step3Diagram />}>
                    <p>
                        On the &ldquo;Grant this service account access&rdquo; step, set the role to{" "}
                        <strong className="text-foreground font-medium">Storage Admin</strong>.
                    </p>
                    <p className="border-destructive/40 bg-destructive/5 text-foreground rounded-md border-l-2 py-2 pl-3">
                        This is the step people get wrong.{" "}
                        <strong className="font-medium">Storage Object Viewer</strong> sounds like
                        the read-only choice, but it only grants access to objects — not the
                        permission to <em>list buckets</em>, which is the first thing Threadspace
                        does. Connecting will fail with a permissions error. If you want to stay
                        read-only, the basic <strong className="font-medium">Viewer</strong> role
                        works instead.
                    </p>
                    <p>
                        A service account starts with no permissions at all, so skipping this step
                        entirely will also fail.
                    </p>
                </Step>

                <Step n={4} title="Download the JSON key" diagram={<Step4Diagram />}>
                    <p>
                        Click into your new service account, open the{" "}
                        <strong className="text-foreground font-medium">Keys</strong> tab, then{" "}
                        <strong className="text-foreground font-medium">Add Key → Create new key</strong>.
                        Choose <strong className="text-foreground font-medium">JSON</strong> and click
                        Create.
                    </p>
                    <p>
                        A <code className="bg-muted rounded px-1.5 py-0.5 text-xs">.json</code> file
                        downloads immediately. This is the only copy — Google won&apos;t show you the
                        private key again, though you can always create another key.
                    </p>
                    <p className="border-border bg-muted/50 text-foreground rounded-md border-l-2 py-2 pl-3">
                        Treat this file like a password. Anyone holding it can read your storage.
                        Don&apos;t commit it to git or paste it into a chat.
                    </p>
                </Step>

                <Step n={5} title="Hand it to Threadspace" diagram={<Step5Diagram />}>
                    <p>
                        Back on the Connect Google Cloud screen, either drag the{" "}
                        <code className="bg-muted rounded px-1.5 py-0.5 text-xs">.json</code> file in,
                        click <strong className="text-foreground font-medium">Choose file</strong>, or
                        open it in a text editor and paste the whole thing.
                    </p>
                    <p>
                        Paste the entire file, braces and all — don&apos;t pull out individual fields
                        or reformat anything. Threadspace reads the project and account details
                        straight out of it.
                    </p>
                </Step>
            </div>

            <section className="mt-14">
                <h2 className="mb-4 text-xl font-semibold">If it doesn&apos;t connect</h2>
                <dl className="divide-border divide-y rounded-lg border">
                    {[
                        {
                            q: "“missing required field …”",
                            a: "Something was lost in the copy. Paste the whole file rather than selected lines, or use the upload button instead.",
                        },
                        {
                            q: "A permissions or 403 error",
                            a: "The service account almost certainly has Storage Object Viewer rather than Storage Admin. Go back to IAM, find the account, and edit its role.",
                        },
                        {
                            q: "A billing error",
                            a: "Cloud Storage needs an active billing account on the project. An old project may have had its card expire — check the billing page.",
                        },
                        {
                            q: "It connects, but everything shows zero",
                            a: "That's not an error. The credentials worked and the project simply has no buckets, or the buckets are empty. Create a bucket and upload a few files.",
                        },
                    ].map(({ q, a }) => (
                        <div key={q} className="p-4">
                            <dt className="text-sm font-medium">{q}</dt>
                            <dd className="text-muted-foreground mt-1 text-sm leading-relaxed">{a}</dd>
                        </div>
                    ))}
                </dl>
            </section>

            <footer className="border-border mt-12 border-t pt-6">
                <Link
                    href="/dashboard"
                    className="text-muted-foreground hover:text-foreground text-sm"
                >
                    ← Back to dashboard
                </Link>
            </footer>
        </div>
    );
}
