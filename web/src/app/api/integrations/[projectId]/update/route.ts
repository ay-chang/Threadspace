import { NextRequest, NextResponse } from "next/server"
import { getServerSession } from "next-auth"
import { authOptions } from "@/lib/authOptions"

const BACKEND_BASE = process.env.BACKEND_BASE!
const INTERNAL_SYNC_TOKEN = process.env.INTERNAL_SYNC_TOKEN!

export async function PUT(
    req: NextRequest,
    { params }: { params: Promise<{ projectId: string }> }
) {
    const session = await getServerSession(authOptions)

    if (!session?.user || !(session.user as { id?: string }).id) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 })
    }

    const { projectId } = await params

    let body: unknown
    try {
        body = await req.json()
    } catch {
        return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 })
    }

    try {
        const res = await fetch(
            `${BACKEND_BASE}/projects/${projectId}/integrations/update`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "x-internal-token": INTERNAL_SYNC_TOKEN,
                },
                body: JSON.stringify(body),
            }
        )

        if (!res.ok) {
            const text = await res.text()
            return NextResponse.json(
                { error: text || "Failed to update integration" },
                { status: res.status }
            )
        }

        const result = await res.json()
        return NextResponse.json(result, { status: 200 })
    } catch (err) {
        console.error("Error calling backend integrations/update:", err)
        return NextResponse.json(
            { error: "Internal server error" },
            { status: 500 }
        )
    }
}
