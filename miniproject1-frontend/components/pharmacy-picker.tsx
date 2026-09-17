"use client";

import { useEffect, useId, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { MapPin, Search } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { useUserLocation } from "@/hooks/use-user-location";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { PharmacyMap } from "@/components/pharmacy-map";
import { cn } from "cn";

interface PharmacySummary {
  id: number;
  name: string;
  addressRoad: string | null;
  lat: number;
  lng: number;
}

interface PageResponse<T> {
  content: T[];
}

export function PharmacyPicker({
  value,
  onChange,
}: {
  value: { id: number; name: string } | null;
  onChange: (pharmacy: { id: number; name: string }) => void;
}) {
  const listboxId = useId();
  const location = useUserLocation();

  const [query, setQuery] = useState(value?.name ?? "");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [mapOpen, setMapOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // value가 외부(prefill 등)에서 바뀌면 query를 동기화한다. 렌더 중 비교해 setState하는
  // React 공식 패턴(https://react.dev/learn/you-might-not-need-an-effect)을 따른다.
  const [syncedName, setSyncedName] = useState(value?.name);
  if (value && value.name !== syncedName) {
    setSyncedName(value.name);
    setQuery(value.name);
  }

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  const { data, isFetching } = useQuery({
    queryKey: ["pharmacies", debouncedQuery],
    queryFn: () =>
      apiFetch<PageResponse<PharmacySummary>>(
        `/api/v1/pharmacies?q=${encodeURIComponent(debouncedQuery)}&size=8`,
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

  function select(pharmacy: PharmacySummary) {
    setQuery(pharmacy.name);
    setOpen(false);
    onChange({ id: pharmacy.id, name: pharmacy.name });
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
        select(results[activeIndex]);
      }
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  }

  return (
    <div ref={containerRef} className="relative">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          role="combobox"
          aria-expanded={open}
          aria-controls={listboxId}
          autoComplete="off"
          className="h-10 w-full rounded-lg border border-border bg-background pl-9 pr-10 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          placeholder="약국 이름을 입력하세요"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setActiveIndex(-1);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
        />
        <button
          type="button"
          aria-label="지도에서 고르기"
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
          onClick={() => setMapOpen(true)}
        >
          <MapPin className="size-4" />
        </button>
      </div>

      {open && debouncedQuery.length >= 2 && (
        <ul
          id={listboxId}
          role="listbox"
          className="absolute z-10 mt-1 w-full rounded-lg border border-border bg-popover shadow-md"
        >
          {isFetching && <li className="px-4 py-3 text-sm text-muted-foreground">검색 중...</li>}
          {!isFetching && results.length === 0 && (
            <li className="px-4 py-3 text-sm text-muted-foreground">검색 결과가 없습니다.</li>
          )}
          {!isFetching &&
            results.map((pharmacy, index) => (
              <li
                key={pharmacy.id}
                role="option"
                aria-selected={index === activeIndex}
                className={cn(
                  "cursor-pointer px-4 py-2.5 text-sm",
                  index === activeIndex ? "bg-muted" : "hover:bg-muted",
                )}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => select(pharmacy)}
              >
                <p className="font-medium">{pharmacy.name}</p>
                {pharmacy.addressRoad && (
                  <p className="text-xs text-muted-foreground">{pharmacy.addressRoad}</p>
                )}
              </li>
            ))}
        </ul>
      )}

      <MapPickerDialog
        open={mapOpen}
        onOpenChange={setMapOpen}
        center={
          location.status === "granted" || location.status === "fallback"
            ? { lat: location.lat, lng: location.lng }
            : null
        }
        onSelect={select}
      />
    </div>
  );
}

function MapPickerDialog({
  open,
  onOpenChange,
  center,
  onSelect,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  center: { lat: number; lng: number } | null;
  onSelect: (pharmacy: PharmacySummary) => void;
}) {
  const { data } = useQuery({
    queryKey: ["pharmacies-nearby", center],
    queryFn: () =>
      apiFetch<PageResponse<PharmacySummary>>(
        `/api/v1/pharmacies?lat=${center!.lat}&lng=${center!.lng}&radius=2000&size=20`,
      ),
    enabled: open && center != null,
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>지도에서 약국 고르기</DialogTitle>
        </DialogHeader>
        {!center && (
          <p className="py-6 text-center text-sm text-muted-foreground">위치 정보를 사용할 수 없습니다.</p>
        )}
        {center && (
          <PharmacyMap
            center={center}
            markers={(data?.content ?? []).map((p) => ({ id: p.id, lat: p.lat, lng: p.lng, label: p.name }))}
            selectedId={null}
            onSelect={(id) => {
              const found = data?.content.find((p) => p.id === id);
              if (found) {
                onSelect(found);
                onOpenChange(false);
              }
            }}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
