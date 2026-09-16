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
        "flex flex-col gap-3 rounded-lg border p-4",
        recommended ? "border-2 border-foreground" : "border-border",
        selected && "ring-2 ring-offset-2 ring-foreground",
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          {recommended && (
            <span className="mb-1 inline-block rounded-full bg-foreground px-2 py-0.5 text-xs font-semibold text-background">
              {TOP_RANK_LABEL[sort] ?? TOP_RANK_LABEL.SCORE}
            </span>
          )}
          <h3 className="font-semibold">
            <Link href={`/pharmacies/${pharmacy.id}`} className="hover:underline">
              {pharmacy.name}
            </Link>
          </h3>
          <p className="text-sm text-muted-foreground">{pharmacy.addressRoad}</p>
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
        <p className="text-sm text-emerald-600 dark:text-emerald-400">
          평균보다 {formatPrice(price.savingVsCandidateAvg)} 저렴
        </p>
      )}

      <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
        <span>제보 {price.reportCount}건</span>
        <span>·</span>
        <span>{formatRelativeDate(price.lastReportedAt)} 갱신</span>
        {badges
          .filter((b) => !(recommended && b === TOP_RANK_BADGE[sort]))
          .map((badge) => (
            <span key={badge} className="rounded-full bg-muted px-2 py-0.5">
              {BADGE_LABEL[badge]}
            </span>
          ))}
      </div>
    </li>
  );
}
