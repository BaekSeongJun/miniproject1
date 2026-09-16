"use client";

import { useState } from "react";
import { PharmacyMap } from "@/components/pharmacy-map";
import { PharmacyResultCard } from "@/components/pharmacy-result-card";
import type { SearchResult } from "@/types/search";

export function SearchResults({ data }: { data: SearchResult }) {
  const [selectedId, setSelectedId] = useState<number | null>(null);

  function selectAndScroll(id: number) {
    setSelectedId(id);
    document.getElementById(`pharmacy-card-${id}`)?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  return (
    <div className="flex flex-col gap-4">
      <PharmacyMap
        center={{ lat: data.query.lat, lng: data.query.lng }}
        results={data.results}
        selectedId={selectedId}
        onSelect={selectAndScroll}
      />
      <ul className="flex flex-col gap-3">
        {data.results.map((result) => (
          <PharmacyResultCard
            key={result.pharmacy.id}
            result={result}
            sort={data.query.sort}
            selected={selectedId === result.pharmacy.id}
            onHover={setSelectedId}
          />
        ))}
      </ul>
    </div>
  );
}
