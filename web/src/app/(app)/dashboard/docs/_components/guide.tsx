import Link from "next/link";
import Image from "next/image";

/** Shared building blocks for the integration setup guides. */

export function ExternalLink({
    href,
    children,
}: {
    href: string;
    children: React.ReactNode;
}) {
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

/** Inline literal - a field value, policy name, or file name. */
export function Code({ children }: { children: React.ReactNode }) {
    return (
        <code className="bg-muted rounded px-1.5 py-0.5 text-xs">{children}</code>
    );
}

/** Emphasis inside guide prose, which is muted by default. */
export function Strong({ children }: { children: React.ReactNode }) {
    return <strong className="text-foreground font-medium">{children}</strong>;
}

/** A callout for the mistake people actually make. */
export function Warning({ children }: { children: React.ReactNode }) {
    return (
        <p className="border-destructive/40 bg-destructive/5 text-foreground rounded-md border-l-2 py-2 pl-3">
            {children}
        </p>
    );
}

/** A neutral aside - security notes, "this is the only copy", and so on. */
export function Note({ children }: { children: React.ReactNode }) {
    return (
        <p className="border-border bg-muted/50 text-foreground rounded-md border-l-2 py-2 pl-3">
            {children}
        </p>
    );
}

export function Step({
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

export function GuideHeader({
    logo,
    logoAlt,
    title,
    children,
    prerequisites,
}: {
    logo: string;
    logoAlt: string;
    title: string;
    children: React.ReactNode;
    prerequisites?: { label: string; items: React.ReactNode[] };
}) {
    return (
        <header className="mb-10">
            <div className="mb-3 flex items-center gap-3">
                <Image
                    src={logo}
                    alt={logoAlt}
                    width={36}
                    height={36}
                    className="h-9 w-9 object-contain"
                />
                <h1 className="text-3xl font-semibold">{title}</h1>
            </div>
            <div className="text-muted-foreground space-y-3 text-sm leading-relaxed">
                {children}
            </div>
            {prerequisites && (
                <div className="bg-muted mt-4 rounded-lg p-4 text-sm">
                    <p className="text-foreground font-medium">{prerequisites.label}</p>
                    <ul className="text-muted-foreground mt-2 list-disc space-y-1 pl-5">
                        {prerequisites.items.map((item, i) => (
                            <li key={i}>{item}</li>
                        ))}
                    </ul>
                </div>
            )}
        </header>
    );
}

export function Troubleshooting({
    entries,
}: {
    entries: { q: string; a: React.ReactNode }[];
}) {
    return (
        <section className="mt-14">
            <h2 className="mb-4 text-xl font-semibold">If it doesn&apos;t connect</h2>
            <dl className="divide-border divide-y rounded-lg border">
                {entries.map(({ q, a }) => (
                    <div key={q} className="p-4">
                        <dt className="text-sm font-medium">{q}</dt>
                        <dd className="text-muted-foreground mt-1 text-sm leading-relaxed">
                            {a}
                        </dd>
                    </div>
                ))}
            </dl>
        </section>
    );
}

export function GuideShell({ children }: { children: React.ReactNode }) {
    return (
        <div className="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6">
            {children}
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
