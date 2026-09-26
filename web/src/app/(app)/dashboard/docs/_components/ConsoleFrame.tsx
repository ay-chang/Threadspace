/**
 * Shared chrome and palette for the drawn console mockups used by the setup
 * guides. The screens are drawn rather than screenshotted so they stay legible
 * in both themes and never go stale against a vendor redesign — they show where
 * things sit, not a pixel-accurate capture.
 *
 * Colors come from the app's theme variables so the diagrams follow light/dark.
 * Each guide supplies its own vendor accent, which is part of what makes a
 * screen recognisable.
 */

export const card = "var(--card)";
export const border = "var(--border)";
export const muted = "var(--muted)";
export const mutedFg = "var(--muted-foreground)";
export const fg = "var(--foreground)";
export const destructive = "var(--destructive)";

/** Vendor accents, used only for the elements that carry brand recognition. */
export const GOOGLE_BLUE = "#1a73e8";
export const AWS_ORANGE = "#ec7211";
export const AWS_SQUID = "#232f3e";
export const VERCEL_FG = "var(--foreground)";

export function ConsoleFrame({
    children,
    title,
    height = 240,
}: {
    children: React.ReactNode;
    title: string;
    height?: number;
}) {
    return (
        <svg
            viewBox={`0 0 640 ${height}`}
            role="img"
            aria-label={title}
            className="h-auto w-full rounded-lg border"
            style={{ borderColor: border, background: card }}
        >
            {/* window chrome */}
            <rect x="0" y="0" width="640" height="34" fill={muted} />
            <circle cx="18" cy="17" r="5" fill={border} />
            <circle cx="34" cy="17" r="5" fill={border} />
            <circle cx="50" cy="17" r="5" fill={border} />
            <text x="70" y="21" fontSize="11" fill={mutedFg} fontFamily="system-ui">
                {title}
            </text>
            <line x1="0" y1="34" x2="640" y2="34" stroke={border} strokeWidth="1" />
            {children}
        </svg>
    );
}
