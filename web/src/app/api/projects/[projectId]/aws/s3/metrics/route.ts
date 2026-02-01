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
    // Verify user session
    const session = await getServerSession(authOptions);
    if (!session?.user) {
      return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
    }

    const { projectId } = await params;

    // Fetch S3 metrics from backend
    const response = await fetch(
      `${BACKEND_BASE}/projects/${projectId}/aws/s3/metrics`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "x-internal-token": INTERNAL_SYNC_TOKEN,
        },
      }
    );

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Failed to fetch S3 metrics:", response.status, errorText);
      return NextResponse.json(
        { message: errorText || "Failed to fetch S3 metrics" },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("Fetch S3 metrics error:", error);
    return NextResponse.json(
      { message: "Internal server error" },
      { status: 500 }
    );
  }
}
