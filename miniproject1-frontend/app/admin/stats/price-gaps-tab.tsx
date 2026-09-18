"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { formatPrice } from "@/lib/format";
import { toUserMessage } from "@/lib/error-message";
import type { AdminPriceGaps } from "@/types/admin";

export function PriceGapsTab() {
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["admin", "stats", "price-gaps"],
    queryFn: () => apiFetch<AdminPriceGaps>("/api/v1/admin/stats/price-gaps?limit=10", { auth: true }),
  });

  if (isLoading) return <LoadingSkeleton rows={5} />;
  if (isError) {
    return <ErrorState message={toUserMessage(error, "가격 격차 통계를 불러오지 못했습니다.")} onRetry={() => refetch()} />;
  }
  if (!data || data.rows.length === 0) {
    return <p className="py-12 text-center text-sm text-muted-foreground">표시할 데이터가 없습니다.</p>;
  }

  const maxGapPct = Math.max(...data.rows.map((r) => r.gapPct));

  return (
    <ul className="flex flex-col gap-3">
      {data.rows.map((row, i) => (
        <li
          key={row.drug.id}
          className="card-elevated rounded-xl border border-border bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-primary/40"
        >
          <div className="flex items-baseline justify-between gap-2">
            <p className="flex items-center gap-2 text-sm font-medium">
              <span className="font-mono text-xs text-muted-foreground tabular-nums">{String(i + 1).padStart(2, "0")}</span>
              {row.drug.displayName}
            </p>
            <p className="font-mono text-sm font-semibold tabular-nums">
              {row.gapPct.toFixed(1)}% ({formatPrice(row.gap)})
            </p>
          </div>
          <div className="mt-2.5 h-1.5 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-primary"
              style={{ width: `${(row.gapPct / maxGapPct) * 100}%` }}
            />
          </div>
          <p className="mt-2 text-xs text-muted-foreground">
            최저가 {row.cheapestRegion.sido} {row.cheapestRegion.sigungu}{" "}
            (<span className="font-mono tabular-nums">{formatPrice(row.cheapestRegion.avgPrice)}</span>) · 최고가{" "}
            {row.priciestRegion.sido} {row.priciestRegion.sigungu}{" "}
            (<span className="font-mono tabular-nums">{formatPrice(row.priciestRegion.avgPrice)}</span>)
          </p>
        </li>
      ))}
    </ul>
  );
}
