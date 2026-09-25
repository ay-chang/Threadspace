import {
    ConsoleFrame,
    card,
    border,
    muted,
    mutedFg,
    fg,
    destructive,
} from "../_components/ConsoleFrame";

/** Mockups of the Vercel dashboard screens referenced by the setup guide. */

export function TokenDiagram() {
    return (
        <ConsoleFrame title="vercel.com/account/tokens" height={250}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Create Token
            </text>

            <text x="24" y="92" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Token Name
            </text>
            <rect x="24" y="100" width="280" height="34" rx="6" fill={card} stroke={border} />
            <text x="38" y="122" fontSize="12" fill={fg} fontFamily="system-ui">
                threadspace
            </text>

            <text x="330" y="92" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Scope
            </text>
            <rect x="330" y="100" width="286" height="34" rx="6" fill={card} stroke={border} />
            <text x="344" y="122" fontSize="12" fill={fg} fontFamily="system-ui">
                Your account, or the team that owns the project
            </text>

            <text x="24" y="160" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Expiration
            </text>
            <rect x="24" y="168" width="180" height="34" rx="6" fill={card} stroke={border} />
            <text x="38" y="190" fontSize="12" fill={fg} fontFamily="system-ui">
                No Expiration
            </text>

            <rect x="24" y="214" width="592" height="26" rx="6" fill={muted} />
            <text x="38" y="231" fontSize="11" fill={fg} fontFamily="system-ui">
                The token is shown once, immediately after you click Create.
            </text>

            <rect x="516" y="164" width="100" height="30" rx="6" fill={fg} />
            <text x="566" y="184" fontSize="12" fill={card} fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">CREATE</text>
        </ConsoleFrame>
    );
}

export function ProjectNameDiagram() {
    return (
        <ConsoleFrame title="vercel.com/<scope>/<project>/settings" height={230}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Project Settings → General
            </text>

            <text x="24" y="96" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Project Name
            </text>
            <rect x="24" y="106" width="340" height="36" rx="6" fill={card}
                  stroke={fg} strokeWidth="2" />
            <text x="40" y="129" fontSize="13" fill={fg} fontFamily="ui-monospace, monospace">
                my-app
            </text>
            <text x="380" y="129" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                ← use this exact value
            </text>

            <rect x="24" y="158" width="592" height="52" rx="6" fill={muted} />
            <circle cx="44" cy="184" r="9" fill="none" stroke={destructive} strokeWidth="2" />
            <text x="44" y="189" fontSize="12" fill={destructive} fontFamily="system-ui"
                  textAnchor="middle" fontWeight="700">!</text>
            <text x="62" y="180" fontSize="11.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                Not the display name, and not the deployment URL.
            </text>
            <text x="62" y="196" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Threadspace looks the project up by this slug.
            </text>
        </ConsoleFrame>
    );
}

export function TeamIdDiagram() {
    return (
        <ConsoleFrame title="vercel.com/<team>/~/settings" height={210}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Team Settings → General
            </text>

            <text x="24" y="96" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Team ID
            </text>
            <rect x="24" y="106" width="380" height="36" rx="6" fill={muted} />
            <text x="40" y="129" fontSize="12" fill={fg} fontFamily="ui-monospace, monospace">
                team_AbCdEf1234567890
            </text>

            <text x="24" y="172" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Personal account? Leave the Team ID field blank in Threadspace.
            </text>
        </ConsoleFrame>
    );
}

export function ConnectDiagram() {
    return (
        <ConsoleFrame title="Threadspace → Connect Vercel" height={230}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Connect Vercel
            </text>

            <text x="24" y="94" fontSize="11" fill={mutedFg} fontFamily="system-ui">API Token</text>
            <rect x="24" y="102" width="592" height="32" rx="6" fill={card} stroke={border} />
            <text x="38" y="123" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">
                vercel_xxxxxxxxxxxxxxxxxxxxxxxx
            </text>

            <text x="24" y="156" fontSize="11" fill={mutedFg} fontFamily="system-ui">Project Name</text>
            <rect x="24" y="164" width="290" height="32" rx="6" fill={card} stroke={border} />
            <text x="38" y="185" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">
                my-app
            </text>

            <text x="330" y="156" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Team ID (optional)
            </text>
            <rect x="330" y="164" width="286" height="32" rx="6" fill={card} stroke={border} />
            <text x="344" y="185" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">
                team_…
            </text>

            <rect x="24" y="208" width="90" height="14" rx="4" fill={fg} opacity="0.85" />
            <text x="69" y="219" fontSize="10" fill={card} fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">Connect</text>
        </ConsoleFrame>
    );
}
