"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import { DrugPriceRow } from "@/components/drug-price-row";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { DrugPrice } from "@/types/pharmacy";

const PAGE_SIZE = 20;

const SORT_OPTIONS = [
  { value: "NAME", label: "이름순" },
  { value: "PRICE_ASC", label: "낮은 가격순" },
  { value: "PRICE_DESC", label: "높은 가격순" },
] as const;

type SortOption = (typeof SORT_OPTIONS)[number]["value"];

function sortDrugPrices(items: DrugPrice[], sort: SortOption): DrugPrice[] {
  const sorted = [...items];
  if (sort === "PRICE_ASC") sorted.sort((a, b) => a.repPrice - b.repPrice);
  else if (sort === "PRICE_DESC") sorted.sort((a, b) => b.repPrice - a.repPrice);
  else sorted.sort((a, b) => a.displayName.localeCompare(b.displayName, "ko"));
  return sorted;
}

export function DrugPriceList({ pharmacyId, drugPrices }: { pharmacyId: number; drugPrices: DrugPrice[] }) {
  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState<SortOption>("NAME");
  const [page, setPage] = useState(0);

  const filtered = keyword.trim()
    ? drugPrices.filter((d) => d.displayName.toLowerCase().includes(keyword.trim().toLowerCase()))
    : drugPrices;
  const sorted = sortDrugPrices(filtered, sort);

  // 검색어/정렬이 바뀌면 렌더 중에 페이지를 0으로 되돌린다(리스트 리셋 패턴, effect 불필요).
  const filterKey = `${keyword}-${sort}`;
  const [prevFilterKey, setPrevFilterKey] = useState(filterKey);
  if (filterKey !== prevFilterKey) {
    setPrevFilterKey(filterKey);
    setPage(0);
  }

  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages - 1);
  const visible = sorted.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-2 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" strokeWidth={1.75} />
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="취급 약품 이름으로 검색"
            className="pl-9"
          />
        </div>
        <Select value={sort} onValueChange={(v) => v && setSort(v as SortOption)}>
          <SelectTrigger aria-label="정렬" className="sm:w-36">
            <SelectValue>{(value: string) => SORT_OPTIONS.find((o) => o.value === value)?.label ?? value}</SelectValue>
          </SelectTrigger>
          <SelectContent>
            {SORT_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>
                {o.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {sorted.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-border py-12 text-center text-sm text-muted-foreground">
          검색 결과가 없습니다.
        </p>
      ) : (
        <>
          <ul className="overflow-hidden rounded-2xl border border-border bg-card card-elevated">
            {visible.map((drugPrice) => (
              <DrugPriceRow key={drugPrice.drugId} pharmacyId={pharmacyId} drugPrice={drugPrice} />
            ))}
          </ul>
          {totalPages > 1 && (
            <div className="flex flex-wrap items-center justify-center gap-1">
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage === 0}
                onClick={() => setPage(currentPage - 1)}
              >
                이전
              </Button>
              {Array.from({ length: totalPages }, (_, i) => i).map((i) => (
                <Button
                  key={i}
                  variant={i === currentPage ? "default" : "outline"}
                  size="sm"
                  onClick={() => setPage(i)}
                >
                  {i + 1}
                </Button>
              ))}
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage === totalPages - 1}
                onClick={() => setPage(currentPage + 1)}
              >
                다음
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
