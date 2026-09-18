"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Paperclip, X } from "lucide-react";
import { cn } from "cn";
import { reportFormSchema, DRUG_CATEGORY_OPTIONS, type ReportFormValues } from "@/lib/schemas/report";
import { apiFetch, uploadFile, ApiError } from "@/lib/api";
import { updateDraft, resetDraft } from "@/lib/slices/report-draft-slice";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";
import { PharmacyPicker } from "@/components/pharmacy-picker";
import { DrugAutocomplete, type DrugSummary } from "@/components/drug-autocomplete";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import type { PriceReportCreateRequest, PriceReportResponse, UploadedFileResponse } from "@/types/report";
import type { PharmacyDetail } from "@/types/pharmacy";

function todayString(): string {
  return new Date().toLocaleDateString("sv-SE");
}

export default function NewReportPage() {
  return (
    <Suspense>
      <NewReportForm />
    </Suspense>
  );
}

function NewReportForm() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const pharmacyIdParam = searchParams.get("pharmacyId");

  const auth = useAppSelector((s) => s.auth);
  const draft = useAppSelector((s) => s.reportDraft);

  const [detailsOpen, setDetailsOpen] = useState(false);
  const [warningDialog, setWarningDialog] = useState<{ warning: string; pharmacyId: number } | null>(null);
  const [pharmacy, setPharmacy] = useState<{ id: number; name: string } | null>(null);
  const [drug, setDrug] = useState<DrugSummary | null>(null);
  const [newDrugMode, setNewDrugMode] = useState(false);
  const [receiptFile, setReceiptFile] = useState<UploadedFileResponse | null>(null);
  const [receiptPreview, setReceiptPreview] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 미리보기용 blob URL은 브라우저 메모리에 남으므로 교체·언마운트 시 반드시 해제한다.
  useEffect(() => {
    return () => {
      if (receiptPreview) URL.revokeObjectURL(receiptPreview);
    };
  }, [receiptPreview]);

  const {
    register,
    handleSubmit,
    control,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ReportFormValues>({
    resolver: zodResolver(reportFormSchema),
    defaultValues: {
      pharmacyId: draft.pharmacyId ?? undefined,
      drugId: draft.drugId ?? undefined,
      price: draft.price ?? undefined,
      purchasedAt: todayString(),
    },
  });

  // ?pharmacyId= 쿼리로 진입 시 약국 정보를 미리 불러와 채운다.
  const { data: prefillPharmacy } = useQuery({
    queryKey: ["pharmacy", pharmacyIdParam],
    queryFn: () => apiFetch<PharmacyDetail>(`/api/v1/pharmacies/${pharmacyIdParam}`),
    enabled: !!pharmacyIdParam && !pharmacy,
  });

  // 렌더 중 비교해 setState하는 React 공식 패턴(effect 대신)으로 prefill 결과를 반영한다.
  if (prefillPharmacy && pharmacy?.id !== prefillPharmacy.id) {
    setPharmacy({ id: prefillPharmacy.id, name: prefillPharmacy.name });
    setValue("pharmacyId", prefillPharmacy.id);
  }

  // 로그인 리다이렉트 왕복에도 입력값을 지키기 위해 draft로 남긴 pharmacyId/drugId를 복원한다.
  useEffect(() => {
    if (draft.pharmacyId && !pharmacy) {
      // 이름은 draft에 없으므로 표시용으로만 id를 채우고, 사용자가 다시 확인하도록 둔다.
      setValue("pharmacyId", draft.pharmacyId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (auth.status === "unauthenticated") {
      const pharmacyId = pharmacy?.id ?? (pharmacyIdParam ? Number(pharmacyIdParam) : null);
      dispatch(updateDraft({ pharmacyId, drugId: drug?.id ?? null }));
      const next = pharmacyId ? `/reports/new?pharmacyId=${pharmacyId}` : "/reports/new";
      router.push(`/login?next=${encodeURIComponent(next)}`);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth.status]);

  const uploadMutation = useMutation({
    mutationFn: (file: File) => {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("purpose", "RECEIPT");
      setUploadProgress(0);
      return uploadFile<UploadedFileResponse>("/api/v1/uploads", formData, setUploadProgress);
    },
    onSuccess: setReceiptFile,
    onError: (err) => {
      setReceiptPreview(null);
      const message = err instanceof ApiError ? err.message : "영수증 업로드에 실패했습니다.";
      toast.error(message);
    },
  });

  function handleReceiptFile(file: File) {
    setReceiptPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return URL.createObjectURL(file);
    });
    uploadMutation.mutate(file);
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (file) handleReceiptFile(file);
  }

  function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleReceiptFile(file);
  }

  function removeReceipt() {
    setReceiptFile(null);
    setReceiptPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return null;
    });
  }

  const mutation = useMutation({
    mutationFn: (values: ReportFormValues) =>
      apiFetch<PriceReportResponse>("/api/v1/price-reports", {
        method: "POST",
        auth: true,
        body: JSON.stringify({
          pharmacyId: values.pharmacyId,
          drugId: values.drugId,
          newDrug: values.newDrug,
          price: values.price,
          purchasedAt: values.purchasedAt,
          receiptFileId: receiptFile?.id,
          memo: values.memo || undefined,
        } satisfies PriceReportCreateRequest),
      }),
    onSuccess: (res, values) => {
      queryClient.invalidateQueries({ queryKey: ["pharmacy", String(values.pharmacyId)] });
      queryClient.invalidateQueries({ queryKey: ["drugs"] });
      dispatch(resetDraft());
      removeReceipt();

      if (res.flagged) {
        setWarningDialog({ warning: res.warning ?? "통계에 반영되지 않았습니다.", pharmacyId: values.pharmacyId });
        return;
      }
      toast.success("가격 제보가 완료되었습니다.");
      router.push(`/pharmacies/${values.pharmacyId}`);
    },
    onError: (err) => {
      if (err instanceof ApiError) {
        if (err.code === "DUPLICATE_REPORT") {
          setError("root", { message: "오늘 이미 이 약국의 해당 약품 가격을 제보하셨습니다." });
          return;
        }
        if (err.code === "DRUG_NOT_OTC") {
          setError("drugId", { message: "전문의약품은 제보할 수 없습니다." });
          return;
        }
        if (err.code === "DRUG_PENDING_APPROVAL") {
          setError("drugId", { message: "관리자 승인 대기 중인 약품입니다." });
          return;
        }
        if (err.code === "INVALID_DATE_RANGE") {
          setError("purchasedAt", { message: "구매일은 오늘부터 180일 이내여야 합니다." });
          return;
        }
        setError("root", { message: err.message });
        return;
      }
      setError("root", { message: "제보에 실패했습니다. 잠시 후 다시 시도해주세요." });
    },
  });

  if (auth.status === "idle" || auth.status === "loading" || auth.status === "unauthenticated") {
    return null;
  }

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-8 px-4 py-10">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">가격 제보하기</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">약국과 약품, 가격만 입력하면 돼요. 30초면 충분해요.</p>
      </div>

      <form
        onSubmit={handleSubmit((values) => mutation.mutate(values))}
        className="flex flex-col gap-5"
        noValidate
      >
        <div className="flex flex-col gap-1.5">
          <label htmlFor="pharmacyId" className="text-sm font-medium">
            약국
          </label>
          <PharmacyPicker
            id="pharmacyId"
            describedBy={errors.pharmacyId ? "pharmacyId-error" : undefined}
            value={pharmacy}
            onChange={(p) => {
              setPharmacy(p);
              setValue("pharmacyId", p.id, { shouldValidate: true });
            }}
          />
          {errors.pharmacyId && (
            <p id="pharmacyId-error" className="text-xs text-destructive">
              {errors.pharmacyId.message}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="drugId" className="text-sm font-medium">
            약품
          </label>
          {!newDrugMode && (
            <>
              <DrugAutocomplete
                id="drugId"
                describedBy={errors.drugId ? "drugId-error" : undefined}
                allowCreate
                onSelect={(d) => {
                  setDrug(d);
                  setValue("drugId", d.id, { shouldValidate: true });
                }}
                onCreateNew={(displayName) => {
                  setDrug(null);
                  setValue("drugId", undefined);
                  setValue("newDrug", { displayName, category: "" as never }, { shouldValidate: false });
                  setNewDrugMode(true);
                }}
              />
              {drug && <p className="text-xs text-muted-foreground">선택됨: {drug.displayName}</p>}
            </>
          )}

          {newDrugMode && (
            <div className="flex flex-col gap-3 rounded-xl border border-dashed border-border bg-muted/30 p-3.5">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">새 약품 등록 (관리자 승인 후 반영됩니다)</span>
                <button
                  type="button"
                  className="text-xs text-muted-foreground underline-offset-2 hover:underline"
                  onClick={() => {
                    setNewDrugMode(false);
                    setValue("newDrug", undefined);
                  }}
                >
                  취소
                </button>
              </div>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="newDrugDisplayName" className="text-sm font-medium">
                  약품명
                </label>
                <input
                  id="newDrugDisplayName"
                  className="h-9 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                  {...register("newDrug.displayName")}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="newDrugCategory" className="text-sm font-medium">
                  카테고리
                </label>
                <select
                  id="newDrugCategory"
                  className="h-9 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                  defaultValue=""
                  {...register("newDrug.category")}
                >
                  <option value="" disabled>
                    선택해주세요
                  </option>
                  {DRUG_CATEGORY_OPTIONS.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {errors.drugId && (
            <p id="drugId-error" className="text-xs text-destructive">
              {errors.drugId.message}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="price" className="text-sm font-medium">
            가격
          </label>
          <Controller
            control={control}
            name="price"
            render={({ field }) => (
              <input
                id="price"
                inputMode="numeric"
                autoComplete="off"
                aria-invalid={!!errors.price}
                aria-describedby={errors.price ? "price-error" : undefined}
                className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                placeholder="예: 2,800"
                value={field.value != null ? field.value.toLocaleString("ko-KR") : ""}
                onChange={(e) => {
                  const digits = e.target.value.replace(/[^0-9]/g, "");
                  field.onChange(digits ? Number(digits) : undefined);
                }}
              />
            )}
          />
          {errors.price && (
            <p id="price-error" className="text-xs text-destructive">
              {errors.price.message}
            </p>
          )}
        </div>

        <button
          type="button"
          onClick={() => setDetailsOpen((v) => !v)}
          className="self-start text-sm text-muted-foreground underline-offset-4 hover:underline"
        >
          {detailsOpen ? "구매일·메모·영수증 접기" : "구매일·메모·영수증 추가 (선택)"}
        </button>

        {detailsOpen && (
          <div className="flex flex-col gap-4 rounded-xl border border-border bg-muted/20 p-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="purchasedAt" className="text-sm font-medium">
                구매일
              </label>
              <input
                id="purchasedAt"
                type="date"
                max={todayString()}
                aria-invalid={!!errors.purchasedAt}
                aria-describedby={errors.purchasedAt ? "purchasedAt-error" : undefined}
                className="h-9 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                {...register("purchasedAt")}
              />
              {errors.purchasedAt && (
                <p id="purchasedAt-error" className="text-xs text-destructive">
                  {errors.purchasedAt.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="memo" className="text-sm font-medium">
                메모
              </label>
              <input
                id="memo"
                maxLength={200}
                aria-invalid={!!errors.memo}
                aria-describedby={errors.memo ? "memo-error" : undefined}
                className="h-9 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                placeholder="예: 1+1 행사 아님, 정가"
                {...register("memo")}
              />
              {errors.memo && (
                <p id="memo-error" className="text-xs text-destructive">
                  {errors.memo.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label id="receipt-label" className="text-sm font-medium">
                영수증 사진
              </label>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={handleFileSelect}
              />

              {!receiptPreview && (
                <div
                  role="button"
                  tabIndex={0}
                  aria-labelledby="receipt-label"
                  onClick={() => fileInputRef.current?.click()}
                  onKeyDown={(e) => e.key === "Enter" && fileInputRef.current?.click()}
                  onDragOver={(e) => {
                    e.preventDefault();
                    setDragOver(true);
                  }}
                  onDragLeave={() => setDragOver(false)}
                  onDrop={handleDrop}
                  className={cn(
                    "flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-3 py-8 text-sm text-muted-foreground transition-all duration-150",
                    dragOver ? "border-primary bg-accent scale-[1.01]" : "border-border hover:bg-muted",
                  )}
                >
                  <Paperclip className="size-5" />
                  <span>클릭하거나 파일을 끌어다 놓으세요</span>
                </div>
              )}

              {receiptPreview && (
                <div className="flex flex-col gap-2 rounded-xl border border-border p-2">
                  <div className="relative">
                    <img
                      src={receiptPreview}
                      alt="영수증 미리보기"
                      className="max-h-48 w-full rounded-md object-contain"
                    />
                    <button
                      type="button"
                      aria-label="영수증 제거"
                      onClick={removeReceipt}
                      className="absolute right-1 top-1 rounded-full bg-background/90 p-1 text-muted-foreground shadow hover:text-foreground"
                    >
                      <X className="size-4" />
                    </button>
                  </div>

                  {uploadMutation.isPending && (
                    <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                      <div
                        className="h-full rounded-full bg-primary transition-all"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                  )}

                  {receiptFile && (
                    <p className="truncate text-xs text-muted-foreground">{receiptFile.originalName}</p>
                  )}
                </div>
              )}
            </div>
          </div>
        )}

        {errors.root && (
          <p role="alert" className="text-sm text-destructive">
            {errors.root.message}
          </p>
        )}

        <Button type="submit" disabled={isSubmitting || mutation.isPending} className="mt-2 w-full">
          {isSubmitting || mutation.isPending ? "제보 중..." : "제보하기"}
        </Button>
      </form>

      <Dialog
        open={!!warningDialog}
        onOpenChange={(open) => {
          if (!open && warningDialog) {
            const pharmacyId = warningDialog.pharmacyId;
            setWarningDialog(null);
            router.push(`/pharmacies/${pharmacyId}`);
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>제보가 접수되었습니다</DialogTitle>
            <DialogDescription>{warningDialog?.warning}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              onClick={() => {
                const pharmacyId = warningDialog?.pharmacyId;
                setWarningDialog(null);
                if (pharmacyId) router.push(`/pharmacies/${pharmacyId}`);
              }}
            >
              확인
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
