"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { cn } from "cn";

const SORT_OPTIONS = [
  { value: "SCORE", label: "추천순" },
  { value: "PRICE", label: "가격순" },
  { value: "DISTANCE", label: "거리순" },
] as const;

const RADIUS_OPTIONS = [500, 1000, 2000, 5000] as const;

export function SortToggle() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const sort = searchParams.get("sort") ?? "SCORE";
  const radius = searchParams.get("radius") ?? "2000";

  function updateParam(key: string, value: string) {
    const params = new URLSearchParams(searchParams);
    params.set(key, value);
    router.push(`/search?${params.toString()}`);
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div className="flex gap-1 rounded-lg bg-muted p-1">
        {SORT_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            type="button"
            className={cn(
              "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
              sort === opt.value
                ? "bg-background text-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground",
            )}
            onClick={() => updateParam("sort", opt.value)}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <div className="flex gap-1">
        {RADIUS_OPTIONS.map((r) => (
          <button
            key={r}
            type="button"
            className={cn(
              "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
              Number(radius) === r
                ? "border-foreground bg-foreground text-background"
                : "border-border text-muted-foreground hover:border-foreground hover:text-foreground",
            )}
            onClick={() => updateParam("radius", String(r))}
          >
            {r < 1000 ? `${r}m` : `${r / 1000}km`}
          </button>
        ))}
      </div>
    </div>
  );
}
