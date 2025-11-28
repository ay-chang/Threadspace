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
            // On first sign-in with Google, we’ll have `account`+`profile`
            if (account?.provider === "google" && account.providerAccountId) {
                try {
                    const res = await fetch(`${BACKEND_BASE}/auth/google/upsert`, {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                            "x-internal-token": INTERNAL_SYNC_TOKEN,
                        },
                        body: JSON.stringify({
                            providerId: account.providerAccountId,
                            email:
                                profile && "email" in profile
                                    ? (profile as any).email
                                    : undefined,
                            name:
                                profile && "name" in profile
                                    ? (profile as any).name
                                    : undefined,
                        }),
                    });
                    if (res.ok) {
                        const user = await res.json(); // { id, email, name, providerId }
                        token.userId = user.id; // <- stash internal id on the JWT
                    }
                } catch (e) {
                    console.error("Next upsert failed:", e);
                }
            }
            return token;
        },

        // Expose userId on the session object for client usage
        async session({ session, token }) {
            if (session.user) {
                (session.user as any).id = token.userId; // <- now available client-side
            }
            return session;
        },
    },
};
