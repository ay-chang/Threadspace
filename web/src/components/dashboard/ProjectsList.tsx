import { Project } from "@/lib/projects";
import { Badge } from "@/components/ui/badge";
import Link from "next/link";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";

type Props = {
    projects: Project[];
};

export default function ProjectsList({ projects }: Props) {
    if (!projects.length) {
        return <div className="text-sm text-muted-foreground">No projects found.</div>;
    }

    const formatDate = (value?: string) => {
        if (!value) return "—";
        const d = new Date(value);
        if (Number.isNaN(d.getTime())) return "—";
        return d.toLocaleDateString(undefined, {
            month: "short",
            day: "numeric",
            year: "numeric",
        });
    };

    return (
        <div className="overflow-hidden rounded-xl border border-border/70 bg-card">
            <Table>
                <TableHeader className="bg-muted/40">
                    <TableRow>
                        <TableHead className="text-xs uppercase tracking-wide text-muted-foreground">
                            Project
                        </TableHead>
                        <TableHead className="text-xs uppercase tracking-wide text-muted-foreground">
                            Status
                        </TableHead>
                        <TableHead className="text-xs uppercase tracking-wide text-muted-foreground">
                            Type
                        </TableHead>
                        <TableHead className="text-xs uppercase tracking-wide text-muted-foreground">
                            Created
                        </TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {projects.map((project) => (
                        <TableRow key={project.id} className="cursor-pointer">
                            <TableCell className="space-y-1">
                                <Link
                                    href={`/dashboard/projects/${project.id}`}
                                    className="block text-sm font-semibold hover:underline"
                                >
                                    {project.name}
                                </Link>
                                <div className="text-xs text-muted-foreground">
                                    {project.id}
                                </div>
                            </TableCell>
                            <TableCell>
                                <Badge className="bg-emerald-600 text-white hover:bg-emerald-600/90">
                                    Active
                                </Badge>
                            </TableCell>
                            <TableCell>
                                <Badge variant="secondary" className="uppercase">
                                    {project.type || "Unknown"}
                                </Badge>
                            </TableCell>
                            <TableCell className="text-sm text-muted-foreground">
                                {formatDate(project.createdAt)}
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
}
