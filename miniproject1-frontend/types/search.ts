export type Badge = "LOWEST_PRICE" | "LOW_CONFIDENCE" | "STALE_DATA" | "NEAREST";

export interface SearchResult {
  drug: { id: number; displayName: string; packageUnit: string | null; imageUrl: string | null };
  query: { lat: number; lng: number; radius: number; sort: string; locationSource: "GPS" | "REGION" };
  summary: {
    resultCount: number;
    candidateAvgPrice: number | null;
    candidateMinPrice: number | null;
    candidateMaxPrice: number | null;
    maxSaving: number | null;
  };
  dataSource: "SEED" | "MIXED" | "USER";
  results: PharmacyResult[];
  suggestion?: { type: "EXPAND_RADIUS"; recommendedRadius: number; estimatedCount: number };
}

export interface PharmacyResult {
  rank: number;
  recommended: boolean;
  pharmacy: {
    id: number;
    name: string;
    addressRoad: string;
    lat: number;
    lng: number;
    phone: string;
  };
  price: {
    repPrice: number;
    minPrice: number;
    avgPrice: number;
    savingVsCandidateAvg: number;
    reportCount: number;
    lastReportedAt: string;
    daysSinceLastReport: number;
  };
  distanceM: number;
  score: number;
  badges: Badge[];
}
