import {
    ConsoleFrame,
    card,
    border,
    muted,
    mutedFg,
    fg,
    destructive,
    GOOGLE_BLUE,
} from "../_components/ConsoleFrame";

/** Mockups of the Google Cloud console screens referenced by the setup guide. */

export function Step1Diagram() {
    return (
        <ConsoleFrame title="console.cloud.google.com" height={200}>
            <text x="24" y="70" fontSize="13" fill={mutedFg} fontFamily="system-ui">
                Select a project
            </text>
            <rect
                x="24" y="84" width="320" height="38" rx="6"
                fill={card} stroke={GOOGLE_BLUE} strokeWidth="2"
            />
            <text x="40" y="108" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="500">
                my-project
            </text>
            <path d="M320 99 l8 8 l8 -8" stroke={mutedFg} strokeWidth="2" fill="none" />

            <rect x="24" y="136" width="440" height="40" rx="6" fill={muted} />
            <text x="40" y="153" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Project ID
            </text>
            <text x="40" y="168" fontSize="13" fill={fg} fontFamily="ui-monospace, monospace">
                my-project-401516
            </text>
            <text x="300" y="162" fontSize="11" fill={GOOGLE_BLUE} fontFamily="system-ui">
                ← this is what you need
            </text>
        </ConsoleFrame>
    );
}

export function Step2Diagram() {
    return (
        <ConsoleFrame title="IAM & Admin → Service Accounts" height={230}>
            <text x="24" y="66" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Service Accounts
            </text>
            <rect x="452" y="50" width="164" height="32" rx="6" fill={GOOGLE_BLUE} />
            <text x="534" y="70" fontSize="12" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="500">
                + CREATE SERVICE ACCOUNT
            </text>

            <line x1="24" y1="96" x2="616" y2="96" stroke={border} />
            <text x="24" y="114" fontSize="11" fill={mutedFg} fontFamily="system-ui">Name</text>
            <text x="300" y="114" fontSize="11" fill={mutedFg} fontFamily="system-ui">Email</text>
            <line x1="24" y1="124" x2="616" y2="124" stroke={border} />

            <text x="24" y="148" fontSize="12" fill={fg} fontFamily="system-ui">
                threadspace-integration
            </text>
            <text x="300" y="148" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">
                threadspace-integration@…
            </text>
            <line x1="24" y1="162" x2="616" y2="162" stroke={border} strokeDasharray="3 3" />

            <text x="24" y="192" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Give it any name — the email is generated for you.
            </text>
        </ConsoleFrame>
    );
}

export function Step3Diagram() {
    return (
        <ConsoleFrame title="Grant this service account access to project" height={250}>
            <text x="24" y="68" fontSize="13" fill={fg} fontFamily="system-ui" fontWeight="600">
                Grant this service account access to the project
            </text>

            <text x="24" y="100" fontSize="11" fill={mutedFg} fontFamily="system-ui">Role</text>
            <rect
                x="24" y="110" width="340" height="38" rx="6"
                fill={card} stroke={GOOGLE_BLUE} strokeWidth="2"
            />
            <text x="40" y="134" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="500">
                Storage Admin
            </text>
            <path d="M340 125 l8 8 l8 -8" stroke={mutedFg} strokeWidth="2" fill="none" />

            {/* the mistake worth calling out */}
            <rect x="24" y="162" width="592" height="46" rx="6" fill={muted} />
            <circle cx="44" cy="185" r="9" fill="none" stroke={destructive} strokeWidth="2" />
            <text x="44" y="190" fontSize="12" fill={destructive} fontFamily="system-ui"
                  textAnchor="middle" fontWeight="700">!</text>
            <text x="62" y="181" fontSize="11.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                Do not pick “Storage Object Viewer”.
            </text>
            <text x="62" y="197" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                It cannot list buckets, so Threadspace will fail to connect.
            </text>

            <rect x="516" y="218" width="100" height="26" rx="6" fill={GOOGLE_BLUE} />
            <text x="566" y="235" fontSize="12" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="500">CONTINUE</text>
        </ConsoleFrame>
    );
}

export function Step4Diagram() {
    return (
        <ConsoleFrame title="Service account → Keys" height={250}>
            {/* tab row */}
            <text x="24" y="62" fontSize="12" fill={mutedFg} fontFamily="system-ui">DETAILS</text>
            <text x="100" y="62" fontSize="12" fill={mutedFg} fontFamily="system-ui">PERMISSIONS</text>
            <text x="200" y="62" fontSize="12" fill={GOOGLE_BLUE} fontFamily="system-ui" fontWeight="600">KEYS</text>
            <line x1="200" y1="70" x2="238" y2="70" stroke={GOOGLE_BLUE} strokeWidth="3" />
            <line x1="24" y1="70" x2="616" y2="70" stroke={border} />

            <rect x="24" y="88" width="110" height="30" rx="6"
                  fill={card} stroke={GOOGLE_BLUE} strokeWidth="1.5" />
            <text x="79" y="108" fontSize="12" fill={GOOGLE_BLUE} fontFamily="system-ui"
                  textAnchor="middle" fontWeight="500">ADD KEY ▾</text>

            {/* dropdown */}
            <rect x="24" y="122" width="180" height="56" rx="6" fill={card}
                  stroke={border} strokeWidth="1" />
            <rect x="24" y="122" width="180" height="28" fill={muted} />
            <text x="38" y="140" fontSize="12" fill={fg} fontFamily="system-ui" fontWeight="500">
                Create new key
            </text>
            <text x="38" y="167" fontSize="12" fill={mutedFg} fontFamily="system-ui">
                Upload existing key
            </text>

            {/* key type */}
            <text x="240" y="140" fontSize="11" fill={mutedFg} fontFamily="system-ui">Key type</text>
            <circle cx="248" cy="157" r="7" fill="none" stroke={GOOGLE_BLUE} strokeWidth="2" />
            <circle cx="248" cy="157" r="3.5" fill={GOOGLE_BLUE} />
            <text x="264" y="162" fontSize="13" fill={fg} fontFamily="system-ui" fontWeight="600">JSON</text>
            <circle cx="248" cy="181" r="7" fill="none" stroke={border} strokeWidth="2" />
            <text x="264" y="186" fontSize="13" fill={mutedFg} fontFamily="system-ui">P12</text>

            <rect x="24" y="200" width="300" height="34" rx="6" fill={muted} />
            <text x="40" y="221" fontSize="11.5" fill={fg} fontFamily="system-ui">
                ↓ my-project-401516-a1b2c3d4.json
            </text>
        </ConsoleFrame>
    );
}

export function Step5Diagram() {
    return (
        <ConsoleFrame title="Threadspace → Connect Google Cloud" height={240}>
            <text x="24" y="66" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Connect Google Cloud
            </text>
            <text x="24" y="88" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Service Account JSON
            </text>

            {/* drop zone */}
            <rect x="24" y="100" width="592" height="64" rx="8" fill="none"
                  stroke={border} strokeWidth="2" strokeDasharray="6 5" />
            <text x="320" y="124" fontSize="11.5" fill={mutedFg} fontFamily="system-ui"
                  textAnchor="middle">
                Upload the .json key file, or paste its contents below.
            </text>
            <rect x="272" y="132" width="96" height="24" rx="6"
                  fill={card} stroke={border} strokeWidth="1" />
            <text x="320" y="148" fontSize="11" fill={fg} fontFamily="system-ui"
                  textAnchor="middle">Choose file</text>

            {/* textarea */}
            <rect x="24" y="174" width="592" height="46" rx="6" fill={card}
                  stroke={border} strokeWidth="1" />
            <text x="36" y="192" fontSize="10" fill={mutedFg} fontFamily="ui-monospace, monospace">
                {'{ "type": "service_account", "project_id": "my-project-401516",'}
            </text>
            <text x="36" y="208" fontSize="10" fill={mutedFg} fontFamily="ui-monospace, monospace">
                {'  "private_key_id": "a1b2c3…", "client_email": "…" }'}
            </text>
        </ConsoleFrame>
    );
}
