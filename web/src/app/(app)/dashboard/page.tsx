import { getServerSession } from "next-auth";
import SignOutButton from "@/components/SignOutButton";

export default async function DashboardPage() {
    const session = await getServerSession();
    const user = session?.user;

    return (
        <div className="flex flex-1 flex-col gap-4 p-4">
            <div className="grid auto-rows-min gap-4 md:grid-cols-3">
                <div className="bg-muted aspect-video rounded-xl" />
                <div className="bg-muted aspect-video rounded-xl" />
                <div className="bg-muted aspect-video rounded-xl" />
            </div>
            <div className="bg-muted min-h-[100vh] flex-1 rounded-xl md:min-h-min" />
        </div>
    );
}
