"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const AWS_REGIONS = [
  { value: "us-east-1", label: "US East (N. Virginia)" },
  { value: "us-east-2", label: "US East (Ohio)" },
  { value: "us-west-1", label: "US West (N. California)" },
  { value: "us-west-2", label: "US West (Oregon)" },
  { value: "eu-west-1", label: "EU (Ireland)" },
  { value: "eu-west-2", label: "EU (London)" },
  { value: "eu-central-1", label: "EU (Frankfurt)" },
  { value: "ap-south-1", label: "Asia Pacific (Mumbai)" },
  { value: "ap-southeast-1", label: "Asia Pacific (Singapore)" },
  { value: "ap-southeast-2", label: "Asia Pacific (Sydney)" },
  { value: "ap-northeast-1", label: "Asia Pacific (Tokyo)" },
  { value: "ca-central-1", label: "Canada (Central)" },
];

interface AwsConnectionModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: string;
  onConnectionSuccess: () => void;
}

export function AwsConnectionModal({
  open,
  onOpenChange,
  projectId,
  onConnectionSuccess,
}: AwsConnectionModalProps) {
  const [accessKeyId, setAccessKeyId] = useState("");
  const [secretAccessKey, setSecretAccessKey] = useState("");
  const [region, setRegion] = useState("us-east-1");
  const [roleArn, setRoleArn] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleConnect = async () => {
    // Validate required fields
    if (!accessKeyId || !secretAccessKey || !region) {
      setError("Please fill in all required fields");
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/projects/${projectId}/integrations/connect`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            integrationType: "AWS",
            displayName: "AWS",
            credentials: {
              accessKeyId,
              secretAccessKey,
              region,
              ...(roleArn && { roleArn }),
            },
          }),
        }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          errorData.message || `Connection failed: ${response.status}`
        );
      }

      // Success - close modal and notify parent
      setAccessKeyId("");
      setSecretAccessKey("");
      setRegion("us-east-1");
      setRoleArn("");
      onOpenChange(false);
      onConnectionSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to connect to AWS");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setAccessKeyId("");
    setSecretAccessKey("");
    setRegion("us-east-1");
    setRoleArn("");
    setError(null);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Connect AWS Account</DialogTitle>
          <DialogDescription>
            Enter your AWS credentials to connect your account. Your credentials
            will be securely stored and verified.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-4">
          <div className="grid gap-2">
            <Label htmlFor="accessKeyId">
              Access Key ID <span className="text-red-500">*</span>
            </Label>
            <Input
              id="accessKeyId"
              type="text"
              placeholder="AKIAIOSFODNN7EXAMPLE"
              value={accessKeyId}
              onChange={(e) => setAccessKeyId(e.target.value)}
              disabled={isLoading}
            />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="secretAccessKey">
              Secret Access Key <span className="text-red-500">*</span>
            </Label>
            <Input
              id="secretAccessKey"
              type="password"
              placeholder="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
              value={secretAccessKey}
              onChange={(e) => setSecretAccessKey(e.target.value)}
              disabled={isLoading}
            />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="region">
              Region <span className="text-red-500">*</span>
            </Label>
            <Select value={region} onValueChange={setRegion} disabled={isLoading}>
              <SelectTrigger id="region">
                <SelectValue placeholder="Select a region" />
              </SelectTrigger>
              <SelectContent>
                {AWS_REGIONS.map((r) => (
                  <SelectItem key={r.value} value={r.value}>
                    {r.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="roleArn">Role ARN (Optional)</Label>
            <Input
              id="roleArn"
              type="text"
              placeholder="arn:aws:iam::123456789012:role/YourRole"
              value={roleArn}
              onChange={(e) => setRoleArn(e.target.value)}
              disabled={isLoading}
            />
            <p className="text-xs text-muted-foreground">
              Advanced: Specify an IAM role ARN to assume
            </p>
          </div>

          {error && (
            <div className="rounded-md bg-red-50 p-3 text-sm text-red-800 border border-red-200">
              {error}
            </div>
          )}
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={handleCancel}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button type="button" onClick={handleConnect} disabled={isLoading}>
            {isLoading ? "Connecting..." : "Connect"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
