"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronDown, ChevronUp } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { useRegions } from "@/hooks/use-regions";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { formatPrice } from "@/lib/format";
import { toUserMessage } from "@/lib/error-message";
import type { AdminRegionStats, AdminRegionStatsRow } from "@/types/admin";

type SortKey = "avgPrice" | "minPrice" | "maxPrice" | "pharmacyCount" | "reportCount";

const COLUMNS: { key: SortKey; label: string }[] = [
  { key: "avgPrice", label: "평균가" },
  { key: "minPrice", label: "최저가" },
  { key: "maxPrice", label: "최고가" },
  { key: "pharmacyCount", label: "약국 수" },
  { key: "reportCount", label: "제보 수" },
];

export function RegionStatsTab() {
  const [sido, setSido] = useState<string | null>(null);
  const [sort, setSort] = useState<{ key: SortKey; desc: boolean }>({ key: "reportCount", desc: true });
  const { data: regionGroups } = useRegions();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["admin", "stats", "regions", sido],
    queryFn: () =>
      apiFetch<AdminRegionStats>(
        `/api/v1/admin/stats/regions${sido ? `?sido=${encodeURIComponent(sido)}` : ""}`,
        { auth: true },
      ),
  });

  const rows = useMemo(() => {
    if (!data) return [];
    const sorted = [...data.rows].sort((a, b) => a[sort.key] - b[sort.key]);
    return sort.desc ? sorted.reverse() : sorted;
  }, [data, sort]);

  function toggleSort(key: SortKey) {
    setSort((prev) => (prev.key === key ? { key, desc: !prev.desc } : { key, desc: true }));
  }

  return (
    <div className="flex flex-col gap-4">
      <Select value={sido} onValueChange={setSido}>
        <SelectTrigger aria-label="시도 필터">
          <SelectValue placeholder="전체 시도" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={null}>전체 시도</SelectItem>
          {regionGroups?.map((group) => (
            <SelectItem key={group.sido} value={group.sido}>
              {group.sido}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {isLoading && <LoadingSkeleton rows={5} />}

      {isError && (
        <ErrorState message={toUserMessage(error, "지역별 통계를 불러오지 못했습니다.")} onRetry={() => refetch()} />
      )}

      {data && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full min-w-[640px] text-sm">
            <thead className="border-b bg-muted/50 text-left text-xs text-muted-foreground">
              <tr>
                <th className="px-3 py-2 font-medium">지역</th>
                <th className="px-3 py-2 font-medium">약품</th>
                {COLUMNS.map((col) => (
                  <th key={col.key} className="px-3 py-2 text-right font-medium">
                    <button
                      type="button"
                      onClick={() => toggleSort(col.key)}
                      className="inline-flex items-center gap-0.5 hover:text-foreground"
                    >
                      {col.label}
                      {sort.key === col.key &&
                        (sort.desc ? <ChevronDown className="size-3" /> : <ChevronUp className="size-3" />)}
                    </button>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y">
              {rows.map((row: AdminRegionStatsRow) => (
                <tr key={`${row.region.code}-${row.drug.id}`}>
                  <td className="px-3 py-2 whitespace-nowrap">
                    {row.region.sido} {row.region.sigungu}
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap">{row.drug.displayName}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{formatPrice(row.avgPrice)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{formatPrice(row.minPrice)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{formatPrice(row.maxPrice)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{row.pharmacyCount}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{row.reportCount}</td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-3 py-8 text-center text-muted-foreground">
                    표시할 통계가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
