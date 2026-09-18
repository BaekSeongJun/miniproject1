"use client";

import { useState } from "react";
import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { ErrorState } from "@/components/error-state";
import { toUserMessage } from "@/lib/error-message";
import type {
  AdminTopPharmacies,
  AdminTopReporters,
  AdminTopDrugsByReports,
  AdminTopDrugsBySearches,
} from "@/types/admin";

const PERIOD_OPTIONS = [
  { value: "DAY", label: "오늘" },
  { value: "WEEK", label: "최근 7일" },
  { value: "MONTH", label: "최근 30일" },
  { value: "YEAR", label: "최근 1년" },
  { value: "ALL", label: "전체" },
] as const;

function useRanking<T>(path: string, period: string) {
  return useQuery({
    queryKey: ["admin", "stats", path, period],
    queryFn: () => apiFetch<T>(`/api/v1/admin/stats/${path}?period=${period}&limit=10`, { auth: true }),
  });
}

export function RankingTab() {
  const [period, setPeriod] = useState("ALL");
  const pharmacies = useRanking<AdminTopPharmacies>("top-pharmacies", period);
  const reporters = useRanking<AdminTopReporters>("top-reporters", period);
  const drugsByReports = useRanking<AdminTopDrugsByReports>("top-drugs-by-reports", period);
  const drugsBySearches = useRanking<AdminTopDrugsBySearches>("top-drugs-by-searches", period);

  return (
    <div className="flex flex-col gap-6">
      <Select value={period} onValueChange={(v) => v && setPeriod(v)}>
        <SelectTrigger aria-label="기간 필터">
          <SelectValue>
            {(value: string) => PERIOD_OPTIONS.find((o) => o.value === value)?.label ?? value}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          {PERIOD_OPTIONS.map((o) => (
            <SelectItem key={o.value} value={o.value}>
              {o.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <div className="grid gap-4 sm:grid-cols-2">
        <RankingSection
          title="약국 제보 Top 10"
          query={pharmacies}
          render={(row) => `${row.pharmacy.name} (${row.pharmacy.sido ?? "-"} ${row.pharmacy.sigungu ?? ""})`}
          count={(row) => row.reportCount}
        />
        <RankingSection
          title="제보자 순위"
          query={reporters}
          render={(row) => row.nickname}
          count={(row) => row.reportCount}
        />
        <RankingSection
          title="약품별 제보 순위"
          query={drugsByReports}
          render={(row) => row.drug.displayName}
          count={(row) => row.reportCount}
        />
        <RankingSection
          title="약품별 검색 순위"
          query={drugsBySearches}
          render={(row) => row.drug.displayName}
          count={(row) => row.searchCount}
        />
      </div>
    </div>
  );
}

function RankingSection<R>({
  title,
  query,
  render,
  count,
}: {
  title: string;
  query: UseQueryResult<{ rows: R[] }>;
  render: (row: R) => string;
  count: (row: R) => number;
}) {
  return (
    <div className="rounded-lg border border-border p-4">
      <p className="mb-3 text-sm font-medium">{title}</p>

      {query.isLoading && <LoadingSkeleton rows={3} />}

      {query.isError && (
        <ErrorState
          message={toUserMessage(query.error, "데이터를 불러오지 못했습니다.")}
          onRetry={() => query.refetch()}
        />
      )}

      {query.data && query.data.rows.length === 0 && (
        <p className="py-6 text-center text-sm text-muted-foreground">표시할 데이터가 없습니다.</p>
      )}

      {query.data && query.data.rows.length > 0 && (
        <ol className="divide-y divide-border">
          {query.data.rows.map((row, i) => (
            <li key={i} className="flex items-center justify-between gap-2 py-2 text-sm">
              <span className="flex items-center gap-2 truncate">
                <span className="w-5 shrink-0 font-mono text-xs text-muted-foreground">{i + 1}</span>
                <span className="truncate">{render(row)}</span>
              </span>
              <span className="shrink-0 font-mono tabular-nums">{count(row).toLocaleString()}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
