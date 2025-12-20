// /app/api/auth/[...nextauth]/authOptions.ts (or wherever yours lives)
import { NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";

const BACKEND_BASE = process.env.BACKEND_BASE!;
const INTERNAL_SYNC_TOKEN = process.env.INTERNAL_SYNC_TOKEN!;

export const authOptions: NextAuthOptions = {
    session: { strategy: "jwt" },
    secret: process.env.NEXTAUTH_SECRET,
    providers: [
        GoogleProvider({
            clientId: process.env.GOOGLE_CLIENT_ID!,
            clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
        }),
    ],
    pages: { signIn: "/login" },

    callbacks: {
        // Called at sign-in time and on subsequent JWT refreshes
        async jwt({ token, account, profile }) {
            // If we already have the internal userId, do nothing
            if (typeof (token as any).userId === "string") {
                return token;
            }

            // Only at sign-in time do we have account/providerAccountId
            if (account?.provider === "google" && account.providerAccountId) {
                try {
                    const typedProfile =
                        profile && typeof profile === "object"
                            ? (profile as { email?: string; name?: string })
                            : undefined;

                    const res = await fetch(`${BACKEND_BASE}/auth/google/upsert`, {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                            "x-internal-token": INTERNAL_SYNC_TOKEN,
                        },
                        body: JSON.stringify({
                            providerId: account.providerAccountId,
                            email: typedProfile?.email,
                            name: typedProfile?.name,
                        }),
                    });

                    if (!res.ok) {
                        console.error("Upsert failed:", res.status, await res.text());
                        return token;
                    }

                    const user: { id: string } = await res.json();
                    (token as any).userId = user.id;
                } catch (e) {
                    console.error("Next upsert failed:", e);
                }
            } else if (!(token as any).userId && token.sub) {
                // Fallback: if the token lacks userId (e.g., after a reset) but we have a provider id,
                // upsert again using token fields to recover the internal user id.
                try {
                    const res = await fetch(`${BACKEND_BASE}/auth/google/upsert`, {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                            "x-internal-token": INTERNAL_SYNC_TOKEN,
                        },
                        body: JSON.stringify({
                            providerId: token.sub,
                            email: token.email,
                            name: token.name,
                        }),
                    });

                    if (!res.ok) {
                        console.error("Upsert fallback failed:", res.status, await res.text());
                        return token;
                    }

                    const user: { id: string } = await res.json();
                    (token as any).userId = user.id;
                } catch (e) {
                    console.error("Next upsert fallback failed:", e);
                }
            }

            return token;
        },
        // Expose userId on the session object for client usage
        async session({ session, token }) {
            if (session.user && "userId" in token && typeof token.userId === "string") {
                (session.user as { id?: string }).id = token.userId;
            }
            return session;
        },
    },
};
