import { NextRequest, NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/authOptions";

type SessionUser = {
    id: string;
    name?: string;
    email?: string;
};

const BACKEND_BASE = process.env.BACKEND_BASE!;
const INTERNAL_SYNC_TOKEN = process.env.INTERNAL_SYNC_TOKEN!;

export async function POST(req: NextRequest) {
    const session = await getServerSession(authOptions);

    if (!session || !session.user || !(session.user as SessionUser).id) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const userId = (session.user as SessionUser).id;

    let body: { name?: string; description?: string; type?: string };
    try {
        body = await req.json();
    } catch {
        return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
    }

    const { name, description, type } = body;

    if (!name || !name.trim()) {
        return NextResponse.json({ error: "Name is required" }, { status: 400 });
    }
    if (!type || !type.trim()) {
        return NextResponse.json({ error: "Type is required" }, { status: 400 });
    }

    try {
        const res = await fetch(`${BACKEND_BASE}/projects`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "x-internal-token": INTERNAL_SYNC_TOKEN,
            },
            body: JSON.stringify({
                name,
                description,
                type,
                userId,
            }),
        });

        if (!res.ok) {
            const text = await res.text();
            return NextResponse.json(
                { error: "Failed to create project", backend: text || undefined },
                { status: 500 }
            );
        }

        const project = await res.json();
        return NextResponse.json(project, { status: 200 });
    } catch (err) {
        console.error("Error calling backend /projects:", err);
        return NextResponse.json({ error: "Internal server error" }, { status: 500 });
    }
}

export async function GET() {
    const session = await getServerSession(authOptions);

    if (!session?.user) {
        return NextResponse.json({ error: "Unauthorized: no session" }, { status: 401 });
    }
    if (!(session.user as any).id) {
        return NextResponse.json(
            { error: "Session is missing internal user id. The backend upsert may have failed — check that the backend is running." },
            { status: 401 }
        );
    }

    const userId = (session.user as SessionUser).id;

    try {
        const res = await fetch(`${BACKEND_BASE}/projects?userId=${userId}`, {
            method: "GET",
            headers: {
                "x-internal-token": INTERNAL_SYNC_TOKEN,
            },
            cache: "no-store",
        });

        if (!res.ok) {
            const text = await res.text();
            return NextResponse.json(
                { error: "Failed to fetch projects", backend: text || undefined },
                { status: 500 }
            );
        }

        const projects = await res.json();
        return NextResponse.json(projects, { status: 200 });
    } catch (err) {
        console.error("Error calling backend /projects:", err);
        return NextResponse.json({ error: "Internal server error" }, { status: 500 });
    }
}
