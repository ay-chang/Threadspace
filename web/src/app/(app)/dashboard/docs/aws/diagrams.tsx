import {
    ConsoleFrame,
    card,
    border,
    muted,
    mutedFg,
    fg,
    destructive,
    AWS_ORANGE,
    AWS_SQUID,
} from "../_components/ConsoleFrame";

/** Mockups of the AWS IAM console screens referenced by the setup guide. */

export function CreateUserDiagram() {
    return (
        <ConsoleFrame title="IAM → Users" height={225}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Users
            </text>
            <rect x="512" y="50" width="104" height="30" rx="4" fill={AWS_ORANGE} />
            <text x="564" y="69" fontSize="12" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">Create user</text>

            <line x1="24" y1="94" x2="616" y2="94" stroke={border} />
            <text x="24" y="112" fontSize="11" fill={mutedFg} fontFamily="system-ui">User name</text>
            <text x="300" y="112" fontSize="11" fill={mutedFg} fontFamily="system-ui">Created</text>
            <line x1="24" y1="122" x2="616" y2="122" stroke={border} />

            <text x="24" y="146" fontSize="12" fill={fg} fontFamily="system-ui">
                threadspace-integration
            </text>
            <text x="300" y="146" fontSize="11" fill={mutedFg} fontFamily="system-ui">just now</text>
            <line x1="24" y1="160" x2="616" y2="160" stroke={border} strokeDasharray="3 3" />

            <rect x="24" y="176" width="592" height="34" rx="4" fill={muted} />
            <text x="38" y="197" fontSize="11" fill={fg} fontFamily="system-ui">
                Leave &ldquo;Provide user access to the AWS Management Console&rdquo; unchecked — this user only needs API keys.
            </text>
        </ConsoleFrame>
    );
}

export function PolicyDiagram() {
    return (
        <ConsoleFrame title="Set permissions" height={255}>
            <text x="24" y="64" fontSize="13" fill={fg} fontFamily="system-ui" fontWeight="600">
                Set permissions → Attach policies directly
            </text>

            <rect x="24" y="80" width="592" height="32" rx="4" fill={card} stroke={border} />
            <text x="38" y="100" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                🔍  s3 read
            </text>

            <rect x="24" y="120" width="592" height="34" rx="4" fill={card}
                  stroke={AWS_ORANGE} strokeWidth="2" />
            <rect x="38" y="130" width="14" height="14" rx="3" fill={AWS_ORANGE} />
            <text x="45" y="141" fontSize="11" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="700">✓</text>
            <text x="62" y="142" fontSize="12.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                AmazonS3ReadOnlyAccess
            </text>
            <text x="400" y="142" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">
                AWS managed
            </text>

            <rect x="24" y="164" width="592" height="50" rx="4" fill={muted} />
            <circle cx="44" cy="189" r="9" fill="none" stroke={destructive} strokeWidth="2" />
            <text x="44" y="194" fontSize="12" fill={destructive} fontFamily="system-ui"
                  textAnchor="middle" fontWeight="700">!</text>
            <text x="62" y="185" fontSize="11.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                Skipping this step is the usual cause of a failed connect.
            </text>
            <text x="62" y="201" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                A new user has no permissions at all until you attach a policy.
            </text>

            <rect x="516" y="222" width="100" height="26" rx="4" fill={AWS_ORANGE} />
            <text x="566" y="239" fontSize="12" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">Next</text>
        </ConsoleFrame>
    );
}

export function AccessKeyDiagram() {
    return (
        <ConsoleFrame title="User → Security credentials" height={250}>
            <text x="24" y="62" fontSize="12" fill={mutedFg} fontFamily="system-ui">Permissions</text>
            <text x="110" y="62" fontSize="12" fill={AWS_ORANGE} fontFamily="system-ui" fontWeight="600">
                Security credentials
            </text>
            <line x1="110" y1="70" x2="232" y2="70" stroke={AWS_ORANGE} strokeWidth="3" />
            <line x1="24" y1="70" x2="616" y2="70" stroke={border} />

            <text x="24" y="94" fontSize="12.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                Access keys
            </text>
            <rect x="490" y="80" width="126" height="28" rx="4" fill={AWS_ORANGE} />
            <text x="553" y="98" fontSize="11.5" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">Create access key</text>

            <text x="24" y="128" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Use case
            </text>
            <circle cx="32" cy="146" r="7" fill="none" stroke={AWS_ORANGE} strokeWidth="2" />
            <circle cx="32" cy="146" r="3.5" fill={AWS_ORANGE} />
            <text x="48" y="151" fontSize="12.5" fill={fg} fontFamily="system-ui" fontWeight="600">
                Third-party service
            </text>

            <rect x="24" y="168" width="290" height="34" rx="4" fill={muted} />
            <text x="38" y="182" fontSize="9.5" fill={mutedFg} fontFamily="system-ui">Access key</text>
            <text x="38" y="196" fontSize="11" fill={fg} fontFamily="ui-monospace, monospace">
                AKIAIOSFODNN7EXAMPLE
            </text>

            <rect x="326" y="168" width="290" height="34" rx="4" fill={muted} />
            <text x="340" y="182" fontSize="9.5" fill={mutedFg} fontFamily="system-ui">
                Secret access key
            </text>
            <text x="340" y="196" fontSize="11" fill={fg} fontFamily="ui-monospace, monospace">
                wJalrXUtnFEMI/K7MDENG/bPxRfi…
            </text>

            <rect x="24" y="212" width="592" height="26" rx="4" fill={AWS_SQUID} />
            <text x="38" y="229" fontSize="11" fill="#fff" fontFamily="system-ui">
                The secret is shown only on this screen. Copy it now or download the .csv.
            </text>
        </ConsoleFrame>
    );
}

export function ConnectDiagram() {
    return (
        <ConsoleFrame title="Threadspace → Connect AWS" height={235}>
            <text x="24" y="64" fontSize="14" fill={fg} fontFamily="system-ui" fontWeight="600">
                Connect AWS
            </text>

            <text x="24" y="94" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Access Key ID
            </text>
            <rect x="24" y="102" width="290" height="32" rx="6" fill={card} stroke={border} />
            <text x="38" y="123" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">
                AKIAIOSFODNN7EXAMPLE
            </text>

            <text x="330" y="94" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                Secret Access Key
            </text>
            <rect x="330" y="102" width="286" height="32" rx="6" fill={card} stroke={border} />
            <text x="344" y="123" fontSize="11" fill={mutedFg} fontFamily="ui-monospace, monospace">
                ••••••••••••••••••••••••
            </text>

            <text x="24" y="156" fontSize="11" fill={mutedFg} fontFamily="system-ui">Region</text>
            <rect x="24" y="164" width="290" height="32" rx="6" fill={card} stroke={border} />
            <text x="38" y="185" fontSize="11.5" fill={fg} fontFamily="system-ui">
                US East (N. Virginia)
            </text>
            <path d="M292 175 l6 6 l6 -6" stroke={mutedFg} strokeWidth="2" fill="none" />

            <rect x="330" y="164" width="286" height="32" rx="6" fill={muted} />
            <text x="344" y="184" fontSize="10.5" fill={mutedFg} fontFamily="system-ui">
                Buckets are global — region is for the API endpoint.
            </text>

            <rect x="24" y="208" width="90" height="14" rx="4" fill={AWS_ORANGE} />
            <text x="69" y="219" fontSize="10" fill="#fff" fontFamily="system-ui"
                  textAnchor="middle" fontWeight="600">Connect</text>
        </ConsoleFrame>
    );
}
