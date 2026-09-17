import { z } from "zod";

// DATABASE.md §3.4 drug.category 허용값.
export const DRUG_CATEGORY_OPTIONS = ["해열진통", "소화제", "감기약", "연고", "소독약", "비타민", "기타"] as const;

// 백엔드 API.md §6 검증 규칙과 동일하게 맞춘다 (가격 100~200,000, 구매일 오늘~180일 전).
// drugId 또는 newDrug 중 하나만 있어야 한다 (미등록 약품 제보).
export const reportFormSchema = z
  .object({
    pharmacyId: z.number("약국을 선택해주세요.").int().positive(),
    drugId: z.number().int().positive().optional(),
    newDrug: z
      .object({
        displayName: z.string().min(1, "약품명을 입력해주세요.").max(100),
        category: z.enum(DRUG_CATEGORY_OPTIONS, "카테고리를 선택해주세요."),
      })
      .optional(),
    price: z
      .number("가격을 입력해주세요.")
      .int("가격은 정수로 입력해주세요.")
      .min(100, "가격은 100원 이상이어야 합니다.")
      .max(200_000, "가격은 200,000원 이하여야 합니다."),
    purchasedAt: z.string().refine(isWithinAllowedRange, "구매일은 오늘부터 180일 이내여야 합니다."),
    memo: z.string().max(200, "메모는 200자 이내여야 합니다.").optional(),
  })
  .refine((v) => (v.drugId != null) !== (v.newDrug != null), {
    message: "약품을 선택하거나 새 약품 정보를 입력해주세요.",
    path: ["drugId"],
  });

export type ReportFormValues = z.infer<typeof reportFormSchema>;

function isWithinAllowedRange(value: string): boolean {
  const date = new Date(`${value}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const minDate = new Date(today);
  minDate.setDate(minDate.getDate() - 180);
  return date <= today && date >= minDate;
}
