"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { useAppSelector } from "@/lib/hooks";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { formatPrice } from "@/lib/format";
import { toUserMessage } from "@/lib/error-message";
import type { PriceReportListItem } from "@/types/report";

interface PageResponse<T> {
  content: T[];
  totalElements: number;
}

type StatusFilter = "ALL" | "ACTIVE" | "FLAGGED" | "HIDDEN" | "REJECTED";

const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "ACTIVE", label: "반영됨" },
  { value: "FLAGGED", label: "검토 중" },
  { value: "HIDDEN", label: "숨김 처리됨" },
  { value: "REJECTED", label: "반려됨" },
];

function statusOf(report: PriceReportListItem): Exclude<StatusFilter, "ALL"> {
  if (report.flagged) return "FLAGGED";
  return report.status;
}

function statusLabel(report: PriceReportListItem): { text: string; className: string } {
  if (report.flagged) return { text: "검토 중", className: "bg-secondary text-secondary-foreground" };
  if (report.status === "HIDDEN") return { text: "숨김 처리됨", className: "bg-muted text-muted-foreground" };
  if (report.status === "REJECTED") return { text: "반려됨", className: "bg-destructive/10 text-destructive" };
  return { text: "반영됨", className: "bg-accent text-accent-foreground" };
}

export default function MyReportsPage() {
  const auth = useAppSelector((s) => s.auth);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["price-reports", "mine"],
    queryFn: () => apiFetch<PageResponse<PriceReportListItem>>("/api/v1/price-reports?mine=true&size=50", { auth: true }),
    enabled: auth.status === "authenticated",
  });

  if (auth.status === "idle" || auth.status === "loading" || auth.status === "unauthenticated") {
    return null;
  }

  const filtered =
    data && statusFilter !== "ALL" ? data.content.filter((r) => statusOf(r) === statusFilter) : data?.content;

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-7 px-4 py-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">내 제보</h1>
        {data && <p className="mt-1 text-sm text-muted-foreground">총 {data.totalElements}건</p>}
      </div>

      {isLoading && <LoadingSkeleton rows={4} />}

      {isError && <ErrorState message={toUserMessage(error, "내 제보 목록을 불러오지 못했습니다.")} onRetry={() => refetch()} />}

      {data && data.content.length === 0 && (
        <div className="flex flex-col items-center gap-4">
          <EmptyState message="아직 제보한 가격이 없습니다." />
          <Button nativeButton={false} render={<Link href="/reports/new" />}>
            첫 가격을 제보해보세요
          </Button>
        </div>
      )}

      {data && data.content.length > 0 && (
        <>
          <Select value={statusFilter} onValueChange={(v) => v && setStatusFilter(v as StatusFilter)}>
            <SelectTrigger aria-label="상태 필터" className="w-40">
              <SelectValue>{(value: string) => STATUS_OPTIONS.find((o) => o.value === value)?.label ?? value}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              {STATUS_OPTIONS.map((o) => (
                <SelectItem key={o.value} value={o.value}>
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {filtered && filtered.length === 0 ? (
            <p className="rounded-2xl border border-dashed border-border py-12 text-center text-sm text-muted-foreground">
              해당 상태의 제보가 없습니다.
            </p>
          ) : (
            <ul className="divide-y divide-border overflow-hidden rounded-2xl border border-border bg-card card-elevated">
              {filtered?.map((report) => {
                const badge = statusLabel(report);
                return (
                  <li
                    key={report.id}
                    className="flex items-center justify-between gap-3 px-4 py-3.5 transition-colors hover:bg-muted/40"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">
                        {report.drug.displayName}
                        {report.drug.packageUnit && (
                          <span className="text-muted-foreground"> ({report.drug.packageUnit})</span>
                        )}
                      </p>
                      <p className="truncate text-xs text-muted-foreground">
                        {report.pharmacy.name} · {report.purchasedAt}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-1">
                      <span className="text-sm font-semibold tabular-nums">{formatPrice(report.price)}</span>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${badge.className}`}>{badge.text}</span>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </>
      )}
    </div>
  );
}
