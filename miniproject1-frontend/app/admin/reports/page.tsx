"use client";

import { useState } from "react";
import { useQueryClient, useMutation, useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { useRequireAdmin } from "@/hooks/use-require-admin";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { Button } from "@/components/ui/button";
import { toUserMessage } from "@/lib/error-message";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { formatPrice } from "@/lib/format";
import type { AdminPriceReportListItem, AdminPriceReportUpdateResponse } from "@/types/admin";

interface PageResponse<T> {
  content: T[];
  totalElements: number;
}

export default function AdminReportsPage() {
  const { ready } = useRequireAdmin();
  const queryClient = useQueryClient();
  const [target, setTarget] = useState<AdminPriceReportListItem | null>(null);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["admin", "price-reports", "flagged"],
    queryFn: () =>
      apiFetch<PageResponse<AdminPriceReportListItem>>(
        "/api/v1/admin/price-reports?flagged=true&size=50",
        { auth: true },
      ),
    enabled: ready,
  });

  const mutation = useMutation({
    mutationFn: (report: AdminPriceReportListItem) => {
      const nextStatus = report.status === "HIDDEN" ? "ACTIVE" : "HIDDEN";
      return apiFetch<AdminPriceReportUpdateResponse>(`/api/v1/admin/price-reports/${report.id}`, {
        method: "PATCH",
        auth: true,
        body: JSON.stringify({ status: nextStatus }),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "price-reports"] });
      setTarget(null);
    },
  });

  if (!ready) return <LoadingSkeleton rows={5} />;

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 py-6">
      <div>
        <h1 className="text-lg font-semibold">제보 관리</h1>
        {data && <p className="text-sm text-muted-foreground">이상치 플래그 {data.totalElements}건</p>}
      </div>

      {isLoading && <LoadingSkeleton rows={5} />}

      {isError && (
        <ErrorState message={toUserMessage(error, "제보 목록을 불러오지 못했습니다.")} onRetry={() => refetch()} />
      )}

      {data && data.content.length === 0 && <EmptyState message="이상치로 플래그된 제보가 없습니다." />}

      {data && data.content.length > 0 && (
        <ul className="rounded-lg border divide-y">
          {data.content.map((report) => (
            <li key={report.id} className="flex items-center justify-between gap-3 px-4 py-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">
                  {report.drug.displayName}
                  {report.drug.packageUnit && (
                    <span className="text-muted-foreground"> ({report.drug.packageUnit})</span>
                  )}
                </p>
                <p className="truncate text-xs text-muted-foreground">
                  {report.pharmacy.name} · {report.purchasedAt} · {report.reporter?.email ?? "익명"}
                </p>
                {report.receiptFileId && (
                  <a
                    href={`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/uploads/${report.receiptFileId}`}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-muted-foreground underline"
                  >
                    영수증 보기
                  </a>
                )}
              </div>
              <div className="flex shrink-0 items-center gap-3">
                <span className="text-sm font-semibold">{formatPrice(report.price)}</span>
                <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs text-amber-800">
                  {report.status === "HIDDEN" ? "숨김됨" : "검토 대기"}
                </span>
                <Button variant="outline" size="sm" onClick={() => setTarget(report)}>
                  {report.status === "HIDDEN" ? "복구" : "숨김"}
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Dialog open={target !== null} onOpenChange={(open) => !open && setTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{target?.status === "HIDDEN" ? "제보 복구" : "제보 숨김"}</DialogTitle>
            <DialogDescription>
              {target?.drug.displayName} · {target?.pharmacy.name} 제보를{" "}
              {target?.status === "HIDDEN" ? "다시 노출" : "숨김 처리"}하시겠습니까?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTarget(null)}>
              취소
            </Button>
            <Button
              onClick={() => target && mutation.mutate(target)}
              disabled={mutation.isPending}
            >
              확인
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
