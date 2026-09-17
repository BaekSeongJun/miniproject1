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
import type { AdminDrugListItem } from "@/types/admin";

interface PageResponse<T> {
  content: T[];
  totalElements: number;
}

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
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 py-6">
      <div>
        <h1 className="text-lg font-semibold">약품 승인 대기</h1>
        {data && <p className="text-sm text-muted-foreground">승인 대기 {data.totalElements}건</p>}
      </div>

      {isLoading && <LoadingSkeleton rows={5} />}

      {isError && (
        <ErrorState message={toUserMessage(error, "약품 목록을 불러오지 못했습니다.")} onRetry={() => refetch()} />
      )}

      {data && data.content.length === 0 && <EmptyState message="승인 대기 중인 약품이 없습니다." />}

      {data && data.content.length > 0 && (
        <ul className="rounded-lg border divide-y">
          {data.content.map((drug) => (
            <li key={drug.id} className="flex items-center justify-between gap-3 px-4 py-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{drug.displayName}</p>
                <p className="truncate text-xs text-muted-foreground">
                  {drug.category} · {drug.packageUnit ?? "규격 미상"}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <Button variant="outline" size="sm" onClick={() => setTarget({ drug, status: "REJECTED" })}>
                  반려
                </Button>
                <Button size="sm" onClick={() => setTarget({ drug, status: "APPROVED" })}>
                  승인
                </Button>
              </div>
            </li>
          ))}
        </ul>
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
