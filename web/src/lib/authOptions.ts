import { NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";

const NEST_API_BASE = process.env.NEST_API_BASE ?? "http://localhost:8080";
const INTERNAL_SYNC_TOKEN = process.env.INTERNAL_SYNC_TOKEN!; // server-side only

export const authOptions: NextAuthOptions = {
    session: { strategy: "jwt" },
    secret: process.env.NEXTAUTH_SECRET,
    providers: [
        GoogleProvider({
            clientId: process.env.GOOGLE_CLIENT_ID!,
            clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
        }),
    ],
    pages: {
        signIn: "/login",
    },

    // Runs on the server after a successful sign-in
    events: {
        async signIn({ user, account }) {
            try {
                const providerId = account?.providerAccountId; // Google's stable ID
                if (!providerId) return;

                await fetch(`${NEST_API_BASE}/auth/google/upsert`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "x-internal-token": INTERNAL_SYNC_TOKEN,
                    },
                    body: JSON.stringify({
                        providerId,
                        email: user.email,
                        name: user.name,
                    }),
                });
            } catch (err) {
                console.error("Failed to sync user to Nest:", err);
                // Don't throw; allow login to proceed
            }
        },
    },

    // (Optional) add fields to session later if you fetch your DB id, etc.
    callbacks: {
        async session({ session }) {
            return session;
        },
    },
};
