"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { DrugAutocomplete, type DrugSummary } from "@/components/drug-autocomplete";

export function SearchDrugSwitcher({ currentDrugName }: { currentDrugName: string }) {
  const router = useRouter();
  const searchParams = useSearchParams();

  function handleSelect(drug: DrugSummary) {
    const params = new URLSearchParams(searchParams);
    params.set("drugId", String(drug.id));
    router.push(`/search?${params.toString()}`);
  }

  return (
    <DrugAutocomplete
      size="sm"
      showPopular={false}
      placeholder="다른 약품 검색"
      initialQuery={currentDrugName}
      onSelect={handleSelect}
    />
  );
}
