import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import LoginCard from "@/components/LoginCard";

export default async function LoginPage() {
    const session = await getServerSession();
    if (session?.user) {
        redirect("/dashboard");
    }

    return (
        <main className="min-h-screen grid place-items-center bg-[--background]">
            <LoginCard />
        </main>
    );
}
