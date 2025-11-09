"use client";

import { signIn } from "next-auth/react";
import { useState } from "react";
import { Button } from "@/components/ui/button"; // your shadcn Button if you have it

export default function LoginCard() {
    const [email, setEmail] = useState("");

    return (
        <div className="w-full max-w-sm rounded-2xl border bg-card p-6 shadow-sm">
            <div className="mb-6">
                <h1 className="text-xl font-semibold">Sign in to Threadspace</h1>
                <p className="text-sm text-muted-foreground">
                    Don't have an account? <span className="underline">Get started</span>
                </p>
            </div>

            {/* Email box — purely visual for now */}
            <label className="block text-sm mb-1">Enter your email</label>
            <div className="flex items-center gap-2 rounded-md border px-2">
                <input
                    className="grow bg-transparent py-2 outline-none placeholder-gray-500 text-sm"
                    placeholder="name@email.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                />
            </div>

            <Button disabled className="w-full mt-3" variant="secondary">
                Continue
            </Button>

            <div className="my-4 flex items-center gap-3 text-xs text-muted-foreground">
                <div className="h-px w-full bg-border" />
                <span>OR</span>
                <div className="h-px w-full bg-border" />
            </div>

            <Button
                className="w-full"
                onClick={() => signIn("google", { callbackUrl: "/dashboard" })}
            >
                Continue with Google
            </Button>

            <p className="mt-4 text-xs text-muted-foreground">
                By signing in, you agree to our Terms and Privacy Policy.
            </p>
        </div>
    );
}
