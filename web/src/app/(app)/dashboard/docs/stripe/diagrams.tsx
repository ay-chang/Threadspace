import {
    ConsoleFrame,
    card,
    border,
    muted,
    mutedFg,
    fg,
    destructive,
    STRIPE_PURPLE,
} from "../_components/ConsoleFrame";

/** Mockups of the Stripe dashboard screens referenced by the setup guide. */

export function ApiKeysDiagram() {
    return (
        <ConsoleFrame title="dashboard.stripe.com/apikeys" height={230}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                API keys
            </text>
            <rect x="470" y="50" width="146" height="30" rx="6" fill={STRIPE_PURPLE} />
            <text x="543" y="70" fontSize="11.5" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">+ Create restricted key</text>

            <text x="24" y="104" fontSize="11" fill={mutedFg} fontFamily="system-ui">Standard keys</text>
            <rect x="24" y="112" width="592" height="30" rx="4" fill={muted} />
            <text x="38" y="131" fontSize="11" fill={fg} fontFamily="system-ui">Publishable key</text>
            <text x="200" y="131" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">pk_live_…</text>
            <text x="420" y="131" fontSize="10.5" fill={destructive} fontFamily="system-ui">✗ can&apos;t read data</text>

            <rect x="24" y="148" width="592" height="30" rx="4" fill={muted} />
            <text x="38" y="167" fontSize="11" fill={fg} fontFamily="system-ui">Secret key</text>
            <text x="200" y="167" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">sk_live_…</text>
            <text x="420" y="167" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">works, but grants full access</text>

            <text x="24" y="206" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Use a restricted key instead: it can only read what Threadspace needs.
            </text>
        </ConsoleFrame>
    );
}

export function PermissionsDiagram() {
    const rows: [string, string][] = [
        ["Balance", "Core"],
        ["Charges", "Core"],
        ["Customers", "Core"],
        ["Subscriptions", "Billing"],
    ];
    return (
        <ConsoleFrame title="Create restricted key → Permissions" height={266}>
            <text x="24" y="62" fontSize="11" fill={mutedFg} fontFamily="system-ui">Key name</text>
            <rect x="24" y="70" width="260" height="28" rx="4" fill={card} stroke={border} />
            <text x="36" y="89" fontSize="11.5" fill={fg} fontFamily="system-ui">Threadspace</text>

            <text x="24" y="122" fontSize="11" fill={mutedFg} fontFamily="system-ui">Resource</text>
            <text x="400" y="122" fontSize="11" fill={mutedFg} fontFamily="system-ui">None</text>
            <text x="460" y="122" fontSize="11" fill={mutedFg} fontFamily="system-ui">Read</text>
            <text x="520" y="122" fontSize="11" fill={mutedFg} fontFamily="system-ui">Write</text>
            <line x1="24" y1="130" x2="616" y2="130" stroke={border} />

            {rows.map(([name, group], i) => {
                const y = 150 + i * 26;
                return (
                    <g key={name}>
                        <text x="24" y={y} fontSize="12" fill={fg} fontFamily="system-ui" fontWeight="500">{name}</text>
                        <text x="160" y={y} fontSize="10" fill={mutedFg} fontFamily="system-ui">{group}</text>
                        <circle cx="410" cy={y - 4} r="6" fill="none" stroke={border} strokeWidth="1.5" />
                        <circle cx="470" cy={y - 4} r="6" fill="none" stroke={STRIPE_PURPLE} strokeWidth="2" />
                        <circle cx="470" cy={y - 4} r="3" fill={STRIPE_PURPLE} />
                        <circle cx="530" cy={y - 4} r="6" fill="none" stroke={border} strokeWidth="1.5" />
                    </g>
                );
            })}

            <rect x="24" y="238" width="592" height="22" rx="4" fill={muted} />
            <text x="36" y="253" fontSize="10.5" fill={fg} fontFamily="system-ui">
                Leave every other resource at None. Threadspace never writes to Stripe.
            </text>
        </ConsoleFrame>
    );
}

export function RevealKeyDiagram() {
    return (
        <ConsoleFrame title="Restricted keys" height={180}>
            <text x="24" y="64" fontSize="12" fill={fg} fontFamily="system-ui" fontWeight="600">
                Threadspace
            </text>
            <rect x="24" y="76" width="420" height="34" rx="6" fill={muted} />
            <text x="38" y="98" fontSize="12" fill={fg} fontFamily="ui-monospace, monospace">
                rk_live_51Nq…xY7t
            </text>
            <rect x="456" y="76" width="70" height="34" rx="6" fill={card} stroke={border} />
            <text x="491" y="98" fontSize="11" fill={fg} fontFamily="system-ui" textAnchor="middle">Copy</text>

            <text x="24" y="138" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Starts with rk_live_ for real data, or rk_test_ for test mode.
            </text>
            <text x="24" y="158" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Copy it straight away — Stripe may not show a live key a second time.
            </text>
        </ConsoleFrame>
    );
}

export function ConnectDiagram() {
    return (
        <ConsoleFrame title="Threadspace → Connect Stripe" height={170}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Connect Stripe
            </text>
            <text x="24" y="94" fontSize="11" fill={mutedFg} fontFamily="system-ui">Restricted API Key</text>
            <rect x="24" y="102" width="592" height="32" rx="6" fill={card} stroke={border} />
            <text x="38" y="123" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">
                ••••••••••••••••••••••••••
            </text>
            <rect x="24" y="146" width="90" height="16" rx="4" fill={STRIPE_PURPLE} />
            <text x="69" y="158" fontSize="10" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">Connect</text>
        </ConsoleFrame>
    );
}
