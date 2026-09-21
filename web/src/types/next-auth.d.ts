import type { DefaultSession } from "next-auth";

/**
 * NextAuth's built-in types only describe the fields it manages itself, so the
 * internal user id this app stores during the Google upsert is invisible to
 * TypeScript by default. Declaring it here is what lets `token.userId` and
 * `session.user.id` be read directly instead of being cast through `any`.
 *
 * Types only - this changes nothing at runtime.
 */

declare module "next-auth" {
    interface Session {
        user: {
            /** Threadspace's own user id, set from the backend upsert. */
            id: string;
        } & DefaultSession["user"];
    }
}

declare module "next-auth/jwt" {
    interface JWT {
        /** Threadspace's own user id, resolved at sign-in and cached on the token. */
        userId?: string;
    }
}
