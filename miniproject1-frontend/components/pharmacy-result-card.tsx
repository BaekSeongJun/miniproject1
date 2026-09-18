import Link from "next/link";
import { PriceTag } from "@/components/price-tag";
import { DistanceBadge } from "@/components/distance-badge";
import { formatPrice, formatRelativeDate } from "@/lib/format";
import { cn } from "cn";
import type { PharmacyResult } from "@/types/search";

const BADGE_LABEL: Record<string, string> = {
  LOWEST_PRICE: "최저가 추천",
  LOW_CONFIDENCE: "정보 부족",
  STALE_DATA: "오래된 정보",
  NEAREST: "최단거리",
};

const TOP_RANK_LABEL: Record<string, string> = {
  SCORE: "최저가 추천",
  PRICE: "최저가",
  DISTANCE: "최단거리",
};

const TOP_RANK_BADGE: Record<string, string> = {
  SCORE: "LOWEST_PRICE",
  PRICE: "LOWEST_PRICE",
  DISTANCE: "NEAREST",
};

export function PharmacyResultCard({
  result,
  sort,
  selected,
  onHover,
}: {
  result: PharmacyResult;
  sort: string;
  selected?: boolean;
  onHover?: (id: number | null) => void;
}) {
  const { pharmacy, price, distanceM, badges, recommended } = result;

  return (
    <li
      id={`pharmacy-card-${pharmacy.id}`}
      onMouseEnter={() => onHover?.(pharmacy.id)}
      onMouseLeave={() => onHover?.(null)}
      className={cn(
        "flex scroll-mt-6 flex-col gap-3 rounded-2xl border bg-card p-4 transition-all duration-200 card-elevated hover:-translate-y-0.5",
        recommended ? "border-primary/60" : "border-border",
        selected && "ring-2 ring-offset-2 ring-offset-background ring-primary",
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          {recommended && (
            <span className="mb-1.5 inline-block rounded-full bg-primary px-2 py-0.5 text-xs font-semibold tracking-tight text-primary-foreground">
              {TOP_RANK_LABEL[sort] ?? TOP_RANK_LABEL.SCORE}
            </span>
          )}
          <h3 className="font-semibold leading-tight">
            <Link href={`/pharmacies/${pharmacy.id}`} className="underline-offset-2 hover:underline">
              {pharmacy.name}
            </Link>
          </h3>
          <p className="mt-0.5 text-sm text-muted-foreground">{pharmacy.addressRoad}</p>
        </div>
        <DistanceBadge meters={distanceM} />
      </div>

      <div className="flex items-baseline gap-2">
        <PriceTag price={price.repPrice} />
        {price.minPrice !== price.repPrice && (
          <span className="text-xs text-muted-foreground">최저 {formatPrice(price.minPrice)}</span>
        )}
      </div>

      {price.savingVsCandidateAvg > 0 && (
        <p className="text-sm font-medium text-primary">
          평균보다 {formatPrice(price.savingVsCandidateAvg)} 저렴
        </p>
      )}

      <div className="flex flex-wrap items-center gap-x-2 gap-y-1.5 text-xs text-muted-foreground">
        <span>제보 {price.reportCount}건</span>
        <span aria-hidden="true">·</span>
        <span>{formatRelativeDate(price.lastReportedAt)} 갱신</span>
        {badges
          .filter((b) => !(recommended && b === TOP_RANK_BADGE[sort]))
          .map((badge) => (
            <span key={badge} className="rounded-full bg-muted px-2 py-0.5 font-medium">
              {BADGE_LABEL[badge]}
            </span>
          ))}
      </div>
    </li>
  );
}
