"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useQueryClient, useMutation, useQuery } from "@tanstack/react-query";
import { apiFetch, apiFetchBlob } from "@/lib/api";
import { useRequireAdmin } from "@/hooks/use-require-admin";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
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
import type { AdminPriceReportListItem, AdminPriceReportUpdateResponse, PageResponse } from "@/types/admin";

const PAGE_SIZE = 20;

const FLAG_OPTIONS = [
  { value: "ALL", label: "전체 제보" },
  { value: "FLAGGED", label: "이상치만" },
] as const;

export default function AdminReportsPage() {
  const { ready } = useRequireAdmin();
  const queryClient = useQueryClient();
  const [target, setTarget] = useState<AdminPriceReportListItem | null>(null);

  const [flagFilter, setFlagFilter] = useState<(typeof FLAG_OPTIONS)[number]["value"]>("FLAGGED");
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [page, setPage] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedKeyword(keyword), 300);
    return () => clearTimeout(timer);
  }, [keyword]);

  // 필터/검색어가 바뀌면 렌더 중에 페이지를 0으로 되돌린다(리스트 리셋 패턴, effect 불필요).
  const [prevFilterKey, setPrevFilterKey] = useState(`${flagFilter}-${debouncedKeyword}`);
  const filterKey = `${flagFilter}-${debouncedKeyword}`;
  if (filterKey !== prevFilterKey) {
    setPrevFilterKey(filterKey);
    setPage(0);
  }

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["admin", "price-reports", flagFilter, debouncedKeyword, page],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (flagFilter === "FLAGGED") params.set("flagged", "true");
      if (debouncedKeyword) params.set("keyword", debouncedKeyword);
      return apiFetch<PageResponse<AdminPriceReportListItem>>(
        `/api/v1/admin/price-reports?${params}`,
        { auth: true },
      );
    },
    enabled: ready,
  });

  const [action, setAction] = useState<"toggleHidden" | "approve" | "reject" | null>(null);
  const [receiptUrl, setReceiptUrl] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: ({ report, action }: { report: AdminPriceReportListItem; action: "toggleHidden" | "approve" | "reject" }) => {
      const body =
        action === "approve"
          ? { flagged: false }
          : action === "reject"
            ? { status: "REJECTED" }
            : { status: report.status === "HIDDEN" ? "ACTIVE" : "HIDDEN" };
      return apiFetch<AdminPriceReportUpdateResponse>(`/api/v1/admin/price-reports/${report.id}`, {
        method: "PATCH",
        auth: true,
        body: JSON.stringify(body),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "price-reports"] });
      closeDialog();
    },
  });

  function openAction(report: AdminPriceReportListItem, action: "toggleHidden" | "approve" | "reject") {
    setTarget(report);
    setAction(action);
  }

  function closeDialog() {
    setTarget(null);
    setAction(null);
  }

  async function openReceipt(fileId: number) {
    try {
      const blob = await apiFetchBlob(`/api/v1/uploads/${fileId}`);
      setReceiptUrl(URL.createObjectURL(blob));
    } catch (err) {
      toast.error(toUserMessage(err, "영수증을 불러오지 못했습니다."));
    }
  }

  function closeReceipt() {
    if (receiptUrl) URL.revokeObjectURL(receiptUrl);
    setReceiptUrl(null);
  }

  if (!ready) return <LoadingSkeleton rows={5} />;

  return (
    <div className="flex flex-col gap-6 p-6">
      <div>
        <h1 className="text-lg font-semibold">제보 관리</h1>
        {data && <p className="text-sm text-muted-foreground">{data.totalElements}건</p>}
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <Select value={flagFilter} onValueChange={(v) => v && setFlagFilter(v as typeof flagFilter)}>
          <SelectTrigger aria-label="이상치 필터" className="sm:w-40">
            <SelectValue>{(value: string) => FLAG_OPTIONS.find((o) => o.value === value)?.label ?? value}</SelectValue>
          </SelectTrigger>
          <SelectContent>
            {FLAG_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>
                {o.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="제보자 닉네임/이메일, 약국명, 약품명 검색"
          className="sm:max-w-sm"
        />
      </div>

      {isLoading && <LoadingSkeleton rows={5} />}

      {isError && (
        <ErrorState message={toUserMessage(error, "제보 목록을 불러오지 못했습니다.")} onRetry={() => refetch()} />
      )}

      {data && data.content.length === 0 && <EmptyState message="조건에 맞는 제보가 없습니다." />}

      {data && data.content.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-border">
          <table className="w-full min-w-[760px] text-sm">
            <thead className="sticky top-0 bg-muted/50 text-left text-xs text-muted-foreground">
              <tr>
                <th className="px-3 py-2 font-medium">약품</th>
                <th className="px-3 py-2 font-medium">약국</th>
                <th className="px-3 py-2 font-medium">구매일</th>
                <th className="px-3 py-2 font-medium">제보자</th>
                <th className="px-3 py-2 text-right font-medium">가격</th>
                <th className="px-3 py-2 font-medium">상태</th>
                <th className="px-3 py-2 text-right font-medium">액션</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {data.content.map((report) => (
                <tr key={report.id} className="hover:bg-muted/40 transition-colors">
                  <td className="px-3 py-2">
                    <p className="font-medium">{report.drug.displayName}</p>
                    {report.drug.packageUnit && (
                      <p className="text-xs text-muted-foreground">{report.drug.packageUnit}</p>
                    )}
                  </td>
                  <td className="px-3 py-2 text-muted-foreground">{report.pharmacy.name}</td>
                  <td className="px-3 py-2 font-mono text-muted-foreground">{report.purchasedAt}</td>
                  <td className="px-3 py-2 text-muted-foreground">
                    {report.reporter?.email ?? "익명"}
                    {report.receiptFileId && (
                      <button
                        type="button"
                        onClick={() => openReceipt(report.receiptFileId!)}
                        className="ml-2 text-xs underline"
                      >
                        영수증
                      </button>
                    )}
                  </td>
                  <td className="px-3 py-2 text-right font-mono tabular-nums">{formatPrice(report.price)}</td>
                  <td className="px-3 py-2">
                    {/* status와 flagged는 독립된 두 축이지만, flagged=true인 제보는 통계 계산에서
                        제외되므로(PriceStatRepository IQR 쿼리 WHERE flagged=false) "반영중"이라고
                        부를 수 없다. 사람이 실제로 신경 쓰는 하나의 상태로 우선순위를 매겨 합친다. */}
                    <span
                      className={
                        report.status === "REJECTED"
                          ? "rounded-full bg-destructive/15 px-2 py-0.5 text-xs text-destructive"
                          : report.status === "HIDDEN"
                            ? "rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground"
                            : report.flagged
                              ? "rounded-full bg-amber-500/15 px-2 py-0.5 text-xs text-amber-400"
                              : "rounded-full bg-accent px-2 py-0.5 text-xs text-accent-foreground"
                      }
                    >
                      {report.status === "REJECTED"
                        ? "반려됨"
                        : report.status === "HIDDEN"
                          ? "숨김됨"
                          : report.flagged
                            ? "이상치 (통계 제외·검토 대기)"
                            : "통계 반영중"}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-right">
                    <div className="flex justify-end gap-1.5">
                      {report.flagged && (
                        <Button variant="outline" size="sm" onClick={() => openAction(report, "approve")}>
                          정상 처리
                        </Button>
                      )}
                      {report.status !== "REJECTED" && (
                        <Button variant="outline" size="sm" onClick={() => openAction(report, "reject")}>
                          반려
                        </Button>
                      )}
                      <Button variant="outline" size="sm" onClick={() => openAction(report, "toggleHidden")}>
                        {report.status === "HIDDEN" ? "복구" : "숨김"}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.content.length > 0 && (
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">
            {data.totalElements}건 중{" "}
            {data.page * data.size + 1}-{Math.min((data.page + 1) * data.size, data.totalElements)}
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
      )}

      <Dialog open={target !== null} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {action === "approve" && "이상치 플래그 해제"}
              {action === "reject" && "제보 반려"}
              {action === "toggleHidden" && (target?.status === "HIDDEN" ? "제보 복구" : "제보 숨김")}
            </DialogTitle>
            <DialogDescription>
              {target?.drug.displayName} · {target?.pharmacy.name} 제보를{" "}
              {action === "approve" && "정상 제보로 판단하여 이상치 플래그를 해제"}
              {action === "reject" && "반려(REJECTED) 처리"}
              {action === "toggleHidden" && (target?.status === "HIDDEN" ? "다시 노출" : "숨김 처리")}
              하시겠습니까?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={closeDialog}>
              취소
            </Button>
            <Button
              onClick={() => target && action && mutation.mutate({ report: target, action })}
              disabled={mutation.isPending}
            >
              확인
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={receiptUrl !== null} onOpenChange={(open) => !open && closeReceipt()}>
        <DialogContent className="max-w-[calc(100%-2rem)] sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>영수증</DialogTitle>
          </DialogHeader>
          {receiptUrl && (
            // eslint-disable-next-line @next/next/no-img-element -- blob: URL은 next/image 로더가 처리할 수 없다
            <img src={receiptUrl} alt="제보 영수증" className="max-h-[70vh] w-full rounded-lg object-contain" />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
