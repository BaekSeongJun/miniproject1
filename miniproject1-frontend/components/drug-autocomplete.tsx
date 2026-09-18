"use client";

import { useEffect, useId, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { useAppSelector } from "@/lib/hooks";
import { cn } from "cn";

export interface DrugSummary {
  id: number;
  displayName: string;
  packageUnit: string | null;
}

interface PageResponse<T> {
  content: T[];
}

const POPULAR_DRUGS = ["타이레놀", "게보린", "판콜에이", "베아제", "부루펜", "이지엔6", "판피린", "훼스탈"];

export function DrugAutocomplete({
  id,
  describedBy,
  onSelect,
  allowCreate,
  onCreateNew,
  size = "lg",
  placeholder = "약품 이름을 입력하세요 (예: 타이레놀)",
  showPopular = true,
  initialQuery = "",
}: {
  id?: string;
  describedBy?: string;
  onSelect?: (drug: DrugSummary) => void;
  allowCreate?: boolean;
  onCreateNew?: (displayName: string) => void;
  size?: "lg" | "sm";
  placeholder?: string;
  showPopular?: boolean;
  initialQuery?: string;
}) {
  const router = useRouter();
  const location = useAppSelector((s) => s.location);
  const listboxId = useId();

  const [query, setQuery] = useState(initialQuery);
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  const { data, isFetching } = useQuery({
    queryKey: ["drugs", debouncedQuery],
    queryFn: () =>
      apiFetch<PageResponse<DrugSummary>>(
        `/api/v1/drugs?q=${encodeURIComponent(debouncedQuery)}&size=8`,
      ),
    enabled: debouncedQuery.length >= 2,
  });

  const results = debouncedQuery.length >= 2 ? (data?.content ?? []) : [];

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  function goToSearch(drugId: number) {
    const params = new URLSearchParams({ drugId: String(drugId), radius: "2000" });
    if (location.status === "granted" || location.status === "fallback") {
      params.set("lat", String(location.lat));
      params.set("lng", String(location.lng));
    }
    router.push(`/search?${params.toString()}`);
  }

  function selectDrug(drug: DrugSummary) {
    setQuery(drug.displayName);
    setOpen(false);
    if (onSelect) {
      onSelect(drug);
    } else {
      goToSearch(drug.id);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!open || results.length === 0) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((i) => (i + 1) % results.length);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((i) => (i - 1 + results.length) % results.length);
    } else if (e.key === "Enter") {
      if (activeIndex >= 0) {
        e.preventDefault();
        selectDrug(results[activeIndex]);
      }
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  }

  return (
    <div
      ref={containerRef}
      className={cn("relative w-full", size === "lg" ? "max-w-xl md:max-w-2xl" : "max-w-sm")}
    >
      <div className="relative">
        <Search
          className={cn(
            "pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground",
            size === "lg" ? "size-4 md:left-4 md:size-5" : "size-4",
          )}
        />
        <input
          id={id}
          role="combobox"
          aria-expanded={open}
          aria-controls={listboxId}
          aria-activedescendant={activeIndex >= 0 ? `${listboxId}-${activeIndex}` : undefined}
          aria-describedby={describedBy}
          autoComplete="off"
          className={cn(
            "w-full rounded-2xl border-2 border-border bg-card pr-4 outline-none focus-visible:border-primary focus-visible:ring-3 focus-visible:ring-ring/30",
            size === "lg"
              ? "h-14 pl-11 text-base shadow-sm md:h-16 md:pl-13 md:text-lg"
              : "h-9 pl-9 text-sm",
          )}
          placeholder={placeholder}
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setActiveIndex(-1);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
        />
      </div>

      {open && debouncedQuery.length >= 2 && (
        <ul
          id={listboxId}
          role="listbox"
          className="absolute z-10 mt-1 w-full rounded-lg border border-border bg-popover shadow-md"
        >
          {isFetching && (
            <li className="px-4 py-3 text-sm text-muted-foreground">검색 중...</li>
          )}
          {!isFetching && results.length === 0 && !allowCreate && (
            <li className="px-4 py-3 text-sm text-muted-foreground">검색 결과가 없습니다.</li>
          )}
          {!isFetching && results.length === 0 && allowCreate && (
            <li className="flex flex-col gap-2 px-4 py-3 text-sm text-muted-foreground">
              <span>검색 결과가 없습니다.</span>
              <button
                type="button"
                className="self-start text-sm font-medium text-primary underline-offset-2 hover:underline"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  setOpen(false);
                  onCreateNew?.(query);
                }}
              >
                &ldquo;{query}&rdquo;(으)로 새 약품 등록하기
              </button>
            </li>
          )}
          {!isFetching &&
            results.map((drug, index) => (
              <li
                key={drug.id}
                id={`${listboxId}-${index}`}
                role="option"
                aria-selected={index === activeIndex}
                className={cn(
                  "flex cursor-pointer items-baseline justify-between gap-2 px-4 py-2.5 text-sm",
                  index === activeIndex ? "bg-muted" : "hover:bg-muted",
                )}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => selectDrug(drug)}
              >
                <span className="font-medium">{drug.displayName}</span>
                {drug.packageUnit && (
                  <span className="shrink-0 text-xs text-muted-foreground">{drug.packageUnit}</span>
                )}
              </li>
            ))}
        </ul>
      )}

      {showPopular && (
        <div
          className={cn(
            "mt-4 flex flex-wrap gap-2",
            open && debouncedQuery.length >= 2 && "invisible",
          )}
        >
          {POPULAR_DRUGS.map((name) => (
            <button
              key={name}
              type="button"
              className="rounded-full border-2 border-border bg-card px-3.5 py-1.5 text-sm font-medium text-foreground transition-colors hover:border-primary hover:text-primary"
              onClick={() => {
                setQuery(name);
                setOpen(true);
              }}
            >
              {name}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
