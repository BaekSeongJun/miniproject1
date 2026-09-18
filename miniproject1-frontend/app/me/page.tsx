"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { useAppSelector } from "@/lib/hooks";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";
import { toUserMessage } from "@/lib/error-message";
import type { PriceReportListItem } from "@/types/report";

interface PageResponse<T> {
  content: T[];
  totalElements: number;
}

function statusLabel(report: PriceReportListItem): { text: string; className: string } {
  if (report.flagged) return { text: "검토 중", className: "bg-amber-100 text-amber-800" };
  if (report.status === "HIDDEN") return { text: "숨김 처리됨", className: "bg-muted text-muted-foreground" };
  if (report.status === "REJECTED") return { text: "반려됨", className: "bg-destructive/10 text-destructive" };
  return { text: "반영됨", className: "bg-accent text-accent-foreground" };
}

export default function MyReportsPage() {
  const auth = useAppSelector((s) => s.auth);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["price-reports", "mine"],
    queryFn: () => apiFetch<PageResponse<PriceReportListItem>>("/api/v1/price-reports?mine=true&size=50", { auth: true }),
    enabled: auth.status === "authenticated",
  });

  if (auth.status === "idle" || auth.status === "loading" || auth.status === "unauthenticated") {
    return null;
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 px-4 py-6">
      <div>
        <h1 className="text-lg font-semibold">내 제보</h1>
        {data && <p className="text-sm text-muted-foreground">총 {data.totalElements}건</p>}
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
        <ul className="rounded-lg border divide-y">
          {data.content.map((report) => {
            const badge = statusLabel(report);
            return (
              <li key={report.id} className="flex items-center justify-between gap-3 px-4 py-3">
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
                  <span className="text-sm font-semibold">{formatPrice(report.price)}</span>
                  <span className={`rounded-full px-2 py-0.5 text-xs ${badge.className}`}>{badge.text}</span>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
