import { headers, cookies } from "next/headers";

export type Project = {
    id: string;
    name: string;
    description?: string | null;
    type: string;
    createdAt?: string;
};

const getBaseUrl = async () => {
    const hdrs = await headers();
    const proto = hdrs.get("x-forwarded-proto") ?? "http";
    const host = hdrs.get("x-forwarded-host") ?? hdrs.get("host");
    if (host) return `${proto}://${host}`;
    return "http://localhost:3000";
};

export async function fetchProjects(): Promise<Project[]> {
    const baseUrl = await getBaseUrl();

    try {
        const cookieHeader = (await cookies()).toString();
        const res = await fetch(`${baseUrl}/api/projects`, {
            cache: "no-store",
            headers: { cookie: cookieHeader },
        });

        if (!res.ok) {
            console.error("Failed to fetch projects:", await res.text());
            return [];
        }
        return res.json();
    } catch (err) {
        console.error("Error fetching projects", err);
        return [];
    }
}
