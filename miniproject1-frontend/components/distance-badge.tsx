import { formatDistance } from "@/lib/format";

export function DistanceBadge({ meters }: { meters: number }) {
  return (
    <span className="shrink-0 rounded-full bg-muted px-2 py-0.5 text-xs tabular-nums text-muted-foreground">
      {formatDistance(meters)}
    </span>
  );
}
