"use client";

import { signOut } from "next-auth/react";
import { Button } from "@/components/ui/button"; // or any button you use

export default function SignOutButton() {
    return (
        <Button
            variant="outline"
            onClick={() => signOut({ callbackUrl: "/" })} // redirect to home after logout
        >
            Sign out
        </Button>
    );
}
