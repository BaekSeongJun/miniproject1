import { formatDistance } from "@/lib/format";

export function DistanceBadge({ meters }: { meters: number }) {
  return (
    <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
      {formatDistance(meters)}
    </span>
  );
}
