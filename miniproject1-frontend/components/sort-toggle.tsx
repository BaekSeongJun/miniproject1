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
      <div className="flex gap-1 rounded-full border-2 border-border bg-card p-1">
        {SORT_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            type="button"
            className={cn(
              "rounded-full px-3 py-1.5 text-sm font-medium transition-colors",
              sort === opt.value
                ? "bg-primary text-primary-foreground"
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
              "rounded-full border-2 px-3 py-1 text-xs font-medium transition-colors",
              Number(radius) === r
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-card text-muted-foreground hover:border-primary hover:text-primary",
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
