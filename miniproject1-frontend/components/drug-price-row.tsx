"use client";

import { useState } from "react";
import { PriceHistoryChart } from "@/components/price-history-chart";
import { PriceTag } from "@/components/price-tag";
import { formatRelativeDate } from "@/lib/format";
import { cn } from "cn";
import type { DrugPrice } from "@/types/pharmacy";

export function DrugPriceRow({ pharmacyId, drugPrice }: { pharmacyId: number; drugPrice: DrugPrice }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const { drugId, displayName, packageUnit, repPrice, reportCount, lastReportedAt, diffFromNationalAvg } = drugPrice;

  return (
    <li className="border-b last:border-b-0">
      <button
        type="button"
        onClick={() => setIsExpanded((prev) => !prev)}
        aria-expanded={isExpanded}
        className="flex w-full items-center justify-between gap-3 px-1 py-3 text-left hover:bg-muted/50"
      >
        <div className="min-w-0">
          <p className="truncate font-medium">{displayName}</p>
          {packageUnit && <p className="text-xs text-muted-foreground">{packageUnit}</p>}
        </div>
        <div className="flex shrink-0 flex-col items-end gap-0.5">
          <PriceTag price={repPrice} />
          <p className="text-xs text-muted-foreground">
            제보 {reportCount}건 · {formatRelativeDate(lastReportedAt)}
          </p>
          <p
            className={cn(
              "text-xs",
              diffFromNationalAvg < 0 ? "text-emerald-600 dark:text-emerald-400" : "text-muted-foreground",
            )}
          >
            전국 평균 대비 {diffFromNationalAvg > 0 ? "+" : ""}
            {diffFromNationalAvg.toLocaleString("ko-KR")}원
          </p>
        </div>
      </button>
      {isExpanded && <PriceHistoryChart pharmacyId={pharmacyId} drugId={drugId} />}
    </li>
  );
}
