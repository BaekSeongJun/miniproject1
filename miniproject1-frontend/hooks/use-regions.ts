"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import type { RegionGroup } from "@/types/region";

export function useRegions() {
  return useQuery({
    queryKey: ["regions"],
    queryFn: () => apiFetch<RegionGroup[]>("/api/v1/regions"),
    staleTime: Infinity,
  });
}
