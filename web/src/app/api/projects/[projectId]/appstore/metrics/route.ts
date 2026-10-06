import { NextRequest, NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/authOptions";

const BACKEND_BASE = process.env.BACKEND_BASE!;
const INTERNAL_SYNC_TOKEN = process.env.INTERNAL_SYNC_TOKEN!;

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ projectId: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user) {
      return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
    }

    const { projectId } = await params;

    // Forward only a numeric window; the backend clamps it to its own limits.
    const days = req.nextUrl.searchParams.get("days");
    const query = days && /^\d+$/.test(days) ? `?days=${days}` : "";

    const response = await fetch(
      `${BACKEND_BASE}/projects/${projectId}/appstore/metrics${query}`,
      {
        method: "GET",
        headers: { "x-internal-token": INTERNAL_SYNC_TOKEN },
        cache: "no-store",
      }
    );

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Failed to fetch App Store Connect metrics:", response.status, errorText);
      return NextResponse.json(
        { message: errorText || "Failed to fetch App Store Connect metrics" },
        { status: response.status }
      );
    }

    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("Fetch App Store Connect metrics error:", error);
    return NextResponse.json({ message: "Internal server error" }, { status: 500 });
  }
}
