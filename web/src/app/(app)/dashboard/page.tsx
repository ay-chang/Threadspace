import EmptyProjects from "@/components/dashboard/EmptyProjects";

export default function DashboardPage() {
    const hasProjects = false;

    if (!hasProjects) {
        return EmptyProjects();
    }

    return (
        <div className="flex flex-1 flex-col gap-4 p-4">
            <div className="grid auto-rows-min gap-4 md:grid-cols-3">
                <div className="bg-muted aspect-video rounded-xl" />
                <div className="bg-muted aspect-video rounded-xl" />
                <div className="bg-muted aspect-video rounded-xl" />
            </div>
            <div className="bg-muted min-h-screen flex-1 rounded-xl md:min-h-min" />
        </div>
    );
}
