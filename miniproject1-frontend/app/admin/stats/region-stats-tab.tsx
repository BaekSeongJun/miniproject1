"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronDown, ChevronUp } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { useRegions } from "@/hooks/use-regions";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { formatPrice } from "@/lib/format";
import { toUserMessage } from "@/lib/error-message";
import type { AdminRegionStatsRow, PageResponse } from "@/types/admin";

type SortKey = "avgPrice" | "minPrice" | "maxPrice" | "pharmacyCount" | "reportCount";

const COLUMNS: { key: SortKey; label: string }[] = [
  { key: "avgPrice", label: "평균가" },
  { key: "minPrice", label: "최저가" },
  { key: "maxPrice", label: "최고가" },
  { key: "pharmacyCount", label: "약국 수" },
  { key: "reportCount", label: "제보 수" },
];

const PAGE_SIZE = 20;

export function RegionStatsTab() {
  const [sido, setSido] = useState<string | null>(null);
  const [sort, setSort] = useState<{ key: SortKey; desc: boolean }>({ key: "reportCount", desc: true });
  const [page, setPage] = useState(0);
  const { data: regionGroups } = useRegions();

  // 필터/정렬이 바뀌면 렌더 중에 페이지를 0으로 되돌린다(리스트 리셋 패턴, effect 불필요).
  const [prevFilterKey, setPrevFilterKey] = useState(`${sido}-${sort.key}-${sort.desc}`);
  const filterKey = `${sido}-${sort.key}-${sort.desc}`;
  if (filterKey !== prevFilterKey) {
    setPrevFilterKey(filterKey);
    setPage(0);
  }

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["admin", "stats", "regions", sido, sort.key, sort.desc, page],
    queryFn: () => {
      const params = new URLSearchParams({
        sortCol: sort.key,
        sortDir: sort.desc ? "DESC" : "ASC",
        page: String(page),
        size: String(PAGE_SIZE),
      });
      if (sido) params.set("sido", sido);
      return apiFetch<PageResponse<AdminRegionStatsRow>>(`/api/v1/admin/stats/regions?${params}`, { auth: true });
    },
  });

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
        <>
          <div className="overflow-x-auto rounded-lg border border-border">
            <table className="w-full min-w-[640px] text-sm">
              <thead className="sticky top-0 bg-muted/50 text-left text-xs text-muted-foreground">
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
              <tbody className="divide-y divide-border">
                {data.content.map((row: AdminRegionStatsRow) => (
                  <tr key={`${row.region.code}-${row.drug.id}`} className="hover:bg-muted/40 transition-colors">
                    <td className="px-3 py-2 whitespace-nowrap">
                      {row.region.sido} {row.region.sigungu}
                    </td>
                    <td className="px-3 py-2 whitespace-nowrap">{row.drug.displayName}</td>
                    <td className="px-3 py-2 text-right font-mono tabular-nums">{formatPrice(row.avgPrice)}</td>
                    <td className="px-3 py-2 text-right font-mono tabular-nums">{formatPrice(row.minPrice)}</td>
                    <td className="px-3 py-2 text-right font-mono tabular-nums">{formatPrice(row.maxPrice)}</td>
                    <td className="px-3 py-2 text-right font-mono tabular-nums">{row.pharmacyCount}</td>
                    <td className="px-3 py-2 text-right font-mono tabular-nums">{row.reportCount}</td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-3 py-8 text-center text-muted-foreground">
                      표시할 통계가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">
              {data.totalElements}건 중{" "}
              {data.totalElements === 0 ? 0 : data.page * data.size + 1}-
              {Math.min((data.page + 1) * data.size, data.totalElements)}
            </span>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                이전
              </Button>
              <Button variant="outline" size="sm" disabled={!data.hasNext} onClick={() => setPage((p) => p + 1)}>
                다음
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
