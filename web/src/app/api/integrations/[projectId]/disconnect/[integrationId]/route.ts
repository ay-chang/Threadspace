import { NextRequest, NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/authOptions";

const BACKEND_BASE = process.env.BACKEND_BASE!;
const INTERNAL_SYNC_TOKEN = process.env.INTERNAL_SYNC_TOKEN!;

export async function DELETE(
  req: NextRequest,
  { params }: { params: Promise<{ projectId: string; integrationId: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user) {
      return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
    }

    const { projectId, integrationId } = await params;

    const response = await fetch(
      `${BACKEND_BASE}/projects/${projectId}/integrations/${integrationId}`,
      {
        method: "DELETE",
        headers: { "x-internal-token": INTERNAL_SYNC_TOKEN },
      }
    );

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Failed to disconnect integration:", response.status, errorText);
      return NextResponse.json(
        { message: errorText || "Failed to disconnect integration" },
        { status: response.status }
      );
    }

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    console.error("Disconnect integration error:", error);
    return NextResponse.json(
      { message: "Internal server error" },
      { status: 500 }
    );
  }
}
