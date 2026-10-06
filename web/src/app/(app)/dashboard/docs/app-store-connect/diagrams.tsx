import {
    ConsoleFrame,
    card,
    border,
    muted,
    mutedFg,
    fg,
    destructive,
    APPLE_BLUE,
} from "../_components/ConsoleFrame";

/** Mockups of the App Store Connect screens referenced by the setup guide. */

export function IntegrationsDiagram() {
    return (
        <ConsoleFrame title="Users and Access → Integrations → App Store Connect API" height={260}>
            <text x="24" y="62" fontSize="12" fill={APPLE_BLUE} fontFamily="system-ui" fontWeight="600">
                Team Keys
            </text>
            <line x1="24" y1="70" x2="96" y2="70" stroke={APPLE_BLUE} strokeWidth="2.5" />
            <line x1="24" y1="70" x2="616" y2="70" stroke={border} />

            <text x="24" y="96" fontSize="11" fill={mutedFg} fontFamily="system-ui">Issuer ID</text>
            <rect x="24" y="104" width="360" height="30" rx="6" fill={card}
                  stroke={APPLE_BLUE} strokeWidth="2" />
            <text x="38" y="124" fontSize="11.5" fill={fg} fontFamily="ui-monospace, monospace">
                57246542-96fe-1a63-e053-0824d011072a
            </text>
            <text x="396" y="124" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">← Issuer ID (one per team)</text>

            <circle cx="598" cy="96" r="12" fill={APPLE_BLUE} />
            <text x="598" y="101" fontSize="15" fill="#fff" fontFamily="system-ui" textAnchor="middle">+</text>

            <line x1="24" y1="156" x2="616" y2="156" stroke={border} />
            <text x="24" y="172" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">Name</text>
            <text x="200" y="172" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">Key ID</text>
            <text x="340" y="172" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">Access</text>
            <line x1="24" y1="180" x2="616" y2="180" stroke={border} />

            <text x="24" y="202" fontSize="12" fill={fg} fontFamily="system-ui">Threadspace</text>
            <rect x="192" y="188" width="104" height="22" rx="4" fill={muted} />
            <text x="200" y="203" fontSize="11.5" fill={fg} fontFamily="ui-monospace, monospace">2X9R4HXF34</text>
            <text x="340" y="203" fontSize="11.5" fill={fg} fontFamily="system-ui">Sales</text>
            <text x="440" y="203" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">← Key ID (per key)</text>

            <rect x="24" y="224" width="592" height="26" rx="4" fill={muted} />
            <text x="38" y="241" fontSize="10.5" fill={fg} fontFamily="system-ui">
                Issuer ID: long, with dashes, above the table. Key ID: 10 characters, in the table.
            </text>
        </ConsoleFrame>
    );
}

export function GenerateKeyDiagram() {
    return (
        <ConsoleFrame title="Generate API Key" height={250}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Generate API Key
            </text>

            <text x="24" y="92" fontSize="11" fill={mutedFg} fontFamily="system-ui">Name</text>
            <rect x="24" y="100" width="300" height="30" rx="6" fill={card} stroke={border} />
            <text x="38" y="120" fontSize="12" fill={fg} fontFamily="system-ui">Threadspace</text>

            <text x="340" y="92" fontSize="11" fill={mutedFg} fontFamily="system-ui">Access</text>
            <rect x="340" y="100" width="276" height="30" rx="6" fill={card}
                  stroke={APPLE_BLUE} strokeWidth="2" />
            <text x="354" y="120" fontSize="12" fill={fg} fontFamily="system-ui" fontWeight="600">Sales</text>
            <path d="M596 111 l6 6 l6 -6" stroke={mutedFg} strokeWidth="2" fill="none" />

            <rect x="24" y="148" width="592" height="52" rx="6" fill={muted} />
            <circle cx="44" cy="174" r="9" fill="none" stroke={destructive} strokeWidth="2" />
            <text x="44" y="179" fontSize="12" fill={destructive} fontFamily="system-ui"
                  textAnchor="middle" fontWeight="700">!</text>
            <text x="62" y="170" fontSize="11.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                Developer and App Manager keys can&apos;t read sales reports.
            </text>
            <text x="62" y="186" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Choose Sales. Finance or Admin also work, but grant more than Threadspace needs.
            </text>

            <rect x="516" y="212" width="100" height="28" rx="6" fill={APPLE_BLUE} />
            <text x="566" y="231" fontSize="12" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">Generate</text>
        </ConsoleFrame>
    );
}

export function DownloadKeyDiagram() {
    return (
        <ConsoleFrame title="Threadspace · Key ID 2X9R4HXF34" height={190}>
            <text x="24" y="64" fontSize="12" fill={fg} fontFamily="system-ui" fontWeight="600">
                Download API Key
            </text>
            <rect x="24" y="76" width="360" height="34" rx="6" fill={muted} />
            <text x="38" y="98" fontSize="12" fill={fg} fontFamily="ui-monospace, monospace">
                ↓ AuthKey_2X9R4HXF34.p8
            </text>

            <rect x="24" y="124" width="592" height="46" rx="6" fill={card}
                  stroke={destructive} strokeWidth="1.5" />
            <text x="38" y="143" fontSize="11.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                Apple lets you download this file only once.
            </text>
            <text x="38" y="159" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                If you lose it, revoke the key and generate a new one.
            </text>
        </ConsoleFrame>
    );
}

export function VendorNumberDiagram() {
    return (
        <ConsoleFrame title="Payments and Financial Reports" height={180}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Payments and Financial Reports
            </text>
            <text x="24" y="96" fontSize="11" fill={mutedFg} fontFamily="system-ui">Your Company LLC</text>
            <rect x="24" y="104" width="220" height="30" rx="6" fill={card}
                  stroke={APPLE_BLUE} strokeWidth="2" />
            <text x="38" y="124" fontSize="12" fill={fg} fontFamily="ui-monospace, monospace">85012345</text>
            <text x="258" y="124" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">← vendor number</text>

            <text x="24" y="160" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Digits only — usually 8, often starting with 8.
            </text>
        </ConsoleFrame>
    );
}

export function ConnectDiagram() {
    return (
        <ConsoleFrame title="Threadspace → Connect App Store Connect" height={250}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Connect App Store Connect
            </text>

            <text x="24" y="92" fontSize="11" fill={mutedFg} fontFamily="system-ui">Issuer ID</text>
            <rect x="24" y="100" width="380" height="28" rx="6" fill={card} stroke={border} />
            <text x="420" y="92" fontSize="11" fill={mutedFg} fontFamily="system-ui">Key ID</text>
            <rect x="420" y="100" width="196" height="28" rx="6" fill={card} stroke={border} />

            <text x="24" y="148" fontSize="11" fill={mutedFg} fontFamily="system-ui">Private Key (.p8)</text>
            <rect x="24" y="156" width="592" height="38" rx="8" fill="none"
                  stroke={border} strokeWidth="2" strokeDasharray="6 5" />
            <text x="320" y="180" fontSize="11" fill={mutedFg} fontFamily="system-ui" textAnchor="middle">
                Drop AuthKey_2X9R4HXF34.p8 here, or choose file
            </text>

            <text x="24" y="214" fontSize="11" fill={mutedFg} fontFamily="system-ui">Vendor Number</text>
            <rect x="24" y="220" width="200" height="24" rx="6" fill={card} stroke={border} />
        </ConsoleFrame>
    );
}
