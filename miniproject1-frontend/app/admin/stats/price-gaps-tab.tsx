"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { formatPrice } from "@/lib/format";
import type { AdminPriceGaps } from "@/types/admin";

export function PriceGapsTab() {
  const { data, isLoading } = useQuery({
    queryKey: ["admin", "stats", "price-gaps"],
    queryFn: () => apiFetch<AdminPriceGaps>("/api/v1/admin/stats/price-gaps?limit=10", { auth: true }),
  });

  if (isLoading) return <p className="py-12 text-center text-sm text-muted-foreground">불러오는 중...</p>;
  if (!data || data.rows.length === 0) {
    return <p className="py-12 text-center text-sm text-muted-foreground">표시할 데이터가 없습니다.</p>;
  }

  const maxGapPct = Math.max(...data.rows.map((r) => r.gapPct));

  return (
    <ul className="flex flex-col gap-3">
      {data.rows.map((row) => (
        <li key={row.drug.id} className="rounded-lg border p-3">
          <div className="flex items-baseline justify-between gap-2">
            <p className="text-sm font-medium">{row.drug.displayName}</p>
            <p className="text-sm font-semibold tabular-nums">
              {row.gapPct.toFixed(1)}% ({formatPrice(row.gap)})
            </p>
          </div>
          <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-foreground"
              style={{ width: `${(row.gapPct / maxGapPct) * 100}%` }}
            />
          </div>
          <p className="mt-1.5 text-xs text-muted-foreground">
            최저가 {row.cheapestRegion.sido} {row.cheapestRegion.sigungu} ({formatPrice(row.cheapestRegion.avgPrice)}) ·
            최고가 {row.priciestRegion.sido} {row.priciestRegion.sigungu} ({formatPrice(row.priciestRegion.avgPrice)})
          </p>
        </li>
      ))}
    </ul>
  );
}
