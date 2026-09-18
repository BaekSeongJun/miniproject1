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
import type { AdminDrugListItem, PageResponse } from "@/types/admin";

export default function AdminDrugsPage() {
  const { ready } = useRequireAdmin();
  const queryClient = useQueryClient();
  const [target, setTarget] = useState<{ drug: AdminDrugListItem; status: "APPROVED" | "REJECTED" } | null>(null);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["admin", "drugs", "pending"],
    queryFn: () => apiFetch<PageResponse<AdminDrugListItem>>("/api/v1/admin/drugs?size=50", { auth: true }),
    enabled: ready,
  });

  const mutation = useMutation({
    mutationFn: ({ drug, status }: { drug: AdminDrugListItem; status: "APPROVED" | "REJECTED" }) =>
      apiFetch<AdminDrugListItem>(`/api/v1/admin/drugs/${drug.id}`, {
        method: "PATCH",
        auth: true,
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "drugs"] });
      queryClient.invalidateQueries({ queryKey: ["drugs"] });
      setTarget(null);
    },
  });

  if (!ready) return <LoadingSkeleton rows={5} />;

  return (
    <div className="flex flex-col gap-6 px-6 py-6">
      <div>
        <h1 className="admin-page-title">약품 승인 대기</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {data ? `승인 대기 중인 약품 ${data.totalElements.toLocaleString()}건입니다.` : "신규 등록 약품을 검토하고 승인합니다."}
        </p>
      </div>

      {isLoading && <LoadingSkeleton rows={5} />}

      {isError && (
        <ErrorState message={toUserMessage(error, "약품 목록을 불러오지 못했습니다.")} onRetry={() => refetch()} />
      )}

      {data && data.content.length === 0 && <EmptyState message="승인 대기 중인 약품이 없습니다." />}

      {data && data.content.length > 0 && (
        <div className="card-elevated overflow-hidden rounded-xl border border-border bg-card">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] text-sm">
              <thead className="admin-table-head">
                <tr>
                  <th className="px-4 py-2.5">약품명</th>
                  <th className="px-4 py-2.5">카테고리</th>
                  <th className="px-4 py-2.5">규격</th>
                  <th className="px-4 py-2.5 text-right">액션</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {data.content.map((drug) => (
                  <tr key={drug.id} className="transition-colors hover:bg-muted/40">
                    <td className="px-4 py-3 font-medium">{drug.displayName}</td>
                    <td className="px-4 py-3 text-muted-foreground">
                      <span className="inline-flex items-center rounded-full border border-border bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                        {drug.category}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{drug.packageUnit ?? "규격 미상"}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        <Button variant="outline" size="sm" onClick={() => setTarget({ drug, status: "REJECTED" })}>
                          반려
                        </Button>
                        <Button size="sm" onClick={() => setTarget({ drug, status: "APPROVED" })}>
                          승인
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Dialog open={target !== null} onOpenChange={(open) => !open && setTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{target?.status === "APPROVED" ? "약품 승인" : "약품 반려"}</DialogTitle>
            <DialogDescription>
              {target?.drug.displayName}을(를) {target?.status === "APPROVED" ? "승인" : "반려"}하시겠습니까?
              {target?.status === "APPROVED" && " 승인하면 검색·제보에 노출됩니다."}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTarget(null)}>
              취소
            </Button>
            <Button onClick={() => target && mutation.mutate(target)} disabled={mutation.isPending}>
              확인
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
