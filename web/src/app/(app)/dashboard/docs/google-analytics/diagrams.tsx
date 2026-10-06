import {
    ConsoleFrame,
    card,
    border,
    muted,
    mutedFg,
    fg,
    destructive,
    GA_ORANGE,
    GOOGLE_BLUE,
} from "../_components/ConsoleFrame";

/** Mockups of the Google Analytics and Cloud console screens referenced by the setup guide. */

export function PropertyIdDiagram() {
    return (
        <ConsoleFrame title="analytics.google.com → Admin → Property details" height={250}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Property details
            </text>

            <text x="24" y="96" fontSize="11" fill={mutedFg} fontFamily="system-ui">Property name</text>
            <text x="24" y="114" fontSize="12.5" fill={fg} fontFamily="system-ui">My Website</text>

            <text x="360" y="96" fontSize="11" fill={mutedFg} fontFamily="system-ui">PROPERTY ID</text>
            <rect x="360" y="102" width="200" height="30" rx="6" fill={card} stroke={GA_ORANGE} strokeWidth="2" />
            <text x="374" y="122" fontSize="13" fill={fg} fontFamily="ui-monospace, monospace" fontWeight="600">
                123456789
            </text>
            <text x="570" y="122" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">← this</text>

            <rect x="24" y="150" width="592" height="80" rx="6" fill={muted} />
            <circle cx="44" cy="174" r="9" fill="none" stroke={destructive} strokeWidth="2" />
            <text x="44" y="179" fontSize="12" fill={destructive} fontFamily="system-ui"
                  textAnchor="middle" fontWeight="700">!</text>
            <text x="62" y="170" fontSize="11.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                Not the Measurement ID.
            </text>
            <text x="62" y="187" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Data streams show a Measurement ID like
            </text>
            <text x="276" y="187" fontSize="11" fill={fg} fontFamily="ui-monospace, monospace">G-ABC123XYZ</text>
            <text x="62" y="204" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                — that one goes in your site&apos;s tracking tag. Threadspace needs the digits-only Property ID.
            </text>
        </ConsoleFrame>
    );
}

export function EnableApiDiagram() {
    return (
        <ConsoleFrame title="console.cloud.google.com → APIs & Services → Library" height={200}>
            <rect x="24" y="52" width="48" height="48" rx="10" fill={muted} />
            <rect x="52" y="60" width="10" height="32" rx="5" fill="#f9ab00" />
            <rect x="40" y="72" width="10" height="20" rx="5" fill={GA_ORANGE} />
            <circle cx="33" cy="87" r="5" fill={GA_ORANGE} />

            <text x="88" y="70" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Google Analytics Data API
            </text>
            <text x="88" y="90" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Google Enterprise API · analyticsdata.googleapis.com
            </text>

            <rect x="88" y="112" width="92" height="32" rx="4" fill={GOOGLE_BLUE} />
            <text x="134" y="132" fontSize="12" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">ENABLE</text>

            <text x="24" y="176" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Enable it in the same Cloud project as the service account — check the project picker first.
            </text>
        </ConsoleFrame>
    );
}

export function PropertyAccessDiagram() {
    return (
        <ConsoleFrame title="Admin → Property access management → Add users" height={270}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Add roles and data restrictions
            </text>

            <text x="24" y="94" fontSize="11" fill={mutedFg} fontFamily="system-ui">Email addresses</text>
            <rect x="24" y="102" width="592" height="30" rx="6" fill={card} stroke={GA_ORANGE} strokeWidth="2" />
            <text x="38" y="122" fontSize="11.5" fill={fg} fontFamily="ui-monospace, monospace">
                threadspace@my-project.iam.gserviceaccount.com
            </text>

            <rect x="24" y="142" width="14" height="14" rx="3" fill={card} stroke={border} strokeWidth="1.5" />
            <text x="46" y="153" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Notify new users by email — leave unchecked
            </text>

            <text x="24" y="182" fontSize="11" fill={mutedFg} fontFamily="system-ui">Direct roles</text>
            <circle cx="32" cy="200" r="7" fill="none" stroke={GA_ORANGE} strokeWidth="2" />
            <circle cx="32" cy="200" r="3.5" fill={GA_ORANGE} />
            <text x="48" y="205" fontSize="12.5" fill={fg} fontFamily="system-ui" fontWeight="600">Viewer</text>
            <text x="110" y="205" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">
                can see report data — all Threadspace needs
            </text>

            <rect x="24" y="222" width="592" height="36" rx="6" fill={muted} />
            <text x="38" y="244" fontSize="11" fill={fg} fontFamily="system-ui">
                This step is what lets the service account read your data. Skipping it is the usual cause of a failed connect.
            </text>
        </ConsoleFrame>
    );
}

export function ConnectDiagram() {
    return (
        <ConsoleFrame title="Threadspace → Connect Google Analytics" height={220}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Connect Google Analytics
            </text>
            <text x="24" y="92" fontSize="11" fill={mutedFg} fontFamily="system-ui">Property ID</text>
            <rect x="24" y="100" width="220" height="28" rx="6" fill={card} stroke={border} />
            <text x="36" y="119" fontSize="11.5" fill={fg} fontFamily="ui-monospace, monospace">123456789</text>

            <text x="24" y="148" fontSize="11" fill={mutedFg} fontFamily="system-ui">Service Account JSON</text>
            <rect x="24" y="156" width="592" height="46" rx="8" fill="none"
                  stroke={border} strokeWidth="2" strokeDasharray="6 5" />
            <text x="320" y="184" fontSize="11" fill={mutedFg} fontFamily="system-ui" textAnchor="middle">
                Upload the .json key file, or paste its contents
            </text>
        </ConsoleFrame>
    );
}
