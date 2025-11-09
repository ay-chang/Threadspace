import { ArrowUpRightIcon, FolderPen } from "lucide-react";
import { Button } from "../ui/button";
import {
    Empty,
    EmptyHeader,
    EmptyMedia,
    EmptyTitle,
    EmptyDescription,
    EmptyContent,
} from "../ui/empty";
import Link from "next/link";

export default function EmptyProjects() {
    return (
        <Empty className="from-muted/50 to-background h-full bg-linear-to-b from-30">
            <EmptyHeader>
                <EmptyMedia variant="icon">
                    <FolderPen />
                </EmptyMedia>
                <EmptyTitle>No Projects Yet</EmptyTitle>
                <EmptyDescription>
                    You haven&apos;t created any projects yet. Get started by creating your
                    first project.
                </EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
                <div className="flex gap-2">
                    <Button>
                        <Link href="/dashboard/create-project">Create Project</Link>
                    </Button>
                </div>
            </EmptyContent>
            <Button variant="link" asChild className="text-muted-foreground" size="sm">
                <a href="#">
                    Learn More <ArrowUpRightIcon />
                </a>
            </Button>
        </Empty>
    );
}
