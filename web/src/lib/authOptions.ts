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

                    if (res.ok) {
                        const user: { id: string } = await res.json(); // { id, email, name, providerId }
                        (token as { userId?: string }).userId = user.id; // stash internal id
                    }
                } catch (e) {
                    console.error("Next upsert failed:", e);
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
