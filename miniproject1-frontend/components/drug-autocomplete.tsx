"use client";

import { useEffect, useId, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { useAppSelector } from "@/lib/hooks";
import { cn } from "cn";

interface DrugSummary {
  id: number;
  displayName: string;
  packageUnit: string | null;
}

interface PageResponse<T> {
  content: T[];
}

const POPULAR_DRUGS = ["타이레놀", "게보린", "판콜에이", "베아제", "부루펜", "이지엔6", "판피린", "훼스탈"];

export function DrugAutocomplete() {
  const router = useRouter();
  const location = useAppSelector((s) => s.location);
  const listboxId = useId();

  const [query, setQuery] = useState("");
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
    goToSearch(drug.id);
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
    <div ref={containerRef} className="relative w-full max-w-xl md:max-w-2xl">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground md:left-4 md:size-5" />
        <input
          role="combobox"
          aria-expanded={open}
          aria-controls={listboxId}
          aria-activedescendant={activeIndex >= 0 ? `${listboxId}-${activeIndex}` : undefined}
          autoComplete="off"
          className="h-12 w-full rounded-lg border border-border bg-background pl-10 pr-4 text-base outline-none focus-visible:ring-3 focus-visible:ring-ring/50 md:h-14 md:pl-12 md:text-lg"
          placeholder="약품 이름을 입력하세요 (예: 타이레놀)"
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
          {!isFetching && results.length === 0 && (
            <li className="px-4 py-3 text-sm text-muted-foreground">검색 결과가 없습니다.</li>
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
            className="rounded-full border border-border px-3 py-1.5 text-sm text-muted-foreground hover:border-foreground hover:text-foreground"
            onClick={() => {
              setQuery(name);
              setOpen(true);
            }}
          >
            {name}
          </button>
        ))}
      </div>
    </div>
  );
}
