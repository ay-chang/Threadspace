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
    TokenDiagram,
    ProjectNameDiagram,
    TeamIdDiagram,
    ConnectDiagram,
} from "./diagrams";

export const metadata = {
    title: "How to connect Vercel",
};

export default function VercelSetupGuide() {
    return (
        <GuideShell>
            <GuideHeader
                logo="/vercel-logo.svg"
                logoAlt="Vercel logo"
                title="How to connect Vercel"
                prerequisites={{
                    label: "Before you start",
                    items: [
                        <>
                            A Vercel project that has been deployed at least once — Threadspace
                            reads its deployments, domains, and environment variables.
                        </>,
                        <>
                            Access to the account or team that owns the project, so you can
                            create a token scoped to it.
                        </>,
                    ],
                }}
            >
                <p>
                    Threadspace reads your Vercel project through an{" "}
                    <Strong>API token</Strong> — a key tied to your Vercel account that grants
                    read access to the projects it is scoped to. You&apos;ll create one, note
                    your project&apos;s name, and paste both into Threadspace. Takes about two
                    minutes.
                </p>
            </GuideHeader>

            <div className="space-y-12">
                <Step n={1} title="Create an API token" diagram={<TokenDiagram />}>
                    <p>
                        Go to{" "}
                        <ExternalLink href="https://vercel.com/account/tokens">
                            Vercel → Settings → Tokens
                        </ExternalLink>{" "}
                        and click <Strong>Create Token</Strong>.
                    </p>
                    <p>
                        Name it something you&apos;ll recognise later, like{" "}
                        <Code>threadspace</Code>. Set <Strong>Scope</Strong> to the account or
                        team that owns the project you want to connect — a token scoped to your
                        personal account cannot read a team&apos;s projects, which is the most
                        common reason connecting fails.
                    </p>
                    <p>
                        Pick an expiration you&apos;re comfortable with. If you choose one,
                        Threadspace will stop fetching data when the token expires and
                        you&apos;ll need to reconnect with a fresh one.
                    </p>
                    <Note>
                        The token is displayed <Strong>once</Strong>, right after you create it.
                        Copy it before leaving the page — if you lose it, delete the token and
                        create another. Treat it like a password: anyone holding it can read your
                        Vercel projects.
                    </Note>
                </Step>

                <Step n={2} title="Find your project name" diagram={<ProjectNameDiagram />}>
                    <p>
                        Open your project in Vercel, go to{" "}
                        <Strong>Settings → General</Strong>, and copy the{" "}
                        <Strong>Project Name</Strong>.
                    </p>
                    <Warning>
                        This is the project&apos;s slug — the lowercase, hyphenated name like{" "}
                        <Code>my-app</Code>. It is <Strong>not</Strong> the deployment URL, and
                        not a prettier display name you may have set elsewhere. Threadspace looks
                        the project up by this exact value, so a mismatch fails with a not-found
                        error even when the token is perfectly valid.
                    </Warning>
                </Step>

                <Step n={3} title="Team ID, if the project belongs to a team" diagram={<TeamIdDiagram />}>
                    <p>
                        If the project sits under a Vercel team rather than your personal
                        account, Threadspace needs the team&apos;s ID to find it. Go to{" "}
                        <Strong>Team Settings → General</Strong> and copy the{" "}
                        <Strong>Team ID</Strong> — it starts with <Code>team_</Code>.
                    </p>
                    <p>
                        On a personal account, skip this and leave the field blank. Filling it in
                        with the wrong team&apos;s ID will cause a not-found error.
                    </p>
                </Step>

                <Step n={4} title="Hand it to Threadspace" diagram={<ConnectDiagram />}>
                    <p>
                        Back on the Connect Vercel screen, paste the token into{" "}
                        <Strong>API Token</Strong>, the slug into <Strong>Project Name</Strong>,
                        and the team ID into <Strong>Team ID</Strong> if you have one.
                    </p>
                    <p>
                        Threadspace verifies the credentials immediately by looking the project
                        up, so if the form accepts them, the connection genuinely works.
                    </p>
                </Step>
            </div>

            <Troubleshooting
                entries={[
                    {
                        q: "A not-found error on a token you know is valid",
                        a: (
                            <>
                                Almost always the project name or the team ID. Check the slug in{" "}
                                <Strong>Settings → General</Strong> matches exactly, and that
                                you&apos;ve supplied the Team ID for a team-owned project (or left
                                it blank for a personal one).
                            </>
                        ),
                    },
                    {
                        q: "An unauthorized or forbidden error",
                        a: (
                            <>
                                The token&apos;s scope doesn&apos;t cover the project. A token
                                scoped to your personal account can&apos;t read team projects —
                                create a new one scoped to the team that owns it.
                            </>
                        ),
                    },
                    {
                        q: "It worked before and now it doesn't",
                        a: "The token likely expired, or someone revoked it in Vercel's token settings. Create a fresh one and reconnect.",
                    },
                    {
                        q: "It connects, but there are no deployments",
                        a: "That's not an error. The credentials worked and the project simply hasn't been deployed yet — ship once and the data will appear.",
                    },
                ]}
            />
        </GuideShell>
    );
}
