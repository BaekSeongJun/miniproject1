export interface AdminOverview {
  totals: {
    pharmacyCount: number;
    drugCount: number;
    reportCount: number;
    userCount: number;
    coveredPairCount: number;
  };
  recentTrend: { date: string; reportCount: number }[];
  flaggedReportCount: number;
  coverageRate: number;
}

export interface AdminPriceReportListItem {
  id: number;
  pharmacy: { id: number; name: string };
  drug: { id: number; displayName: string; packageUnit: string | null };
  price: number;
  purchasedAt: string;
  reporter: { id: number; nickname: string; email: string } | null;
  source: string;
  status: "ACTIVE" | "HIDDEN" | "REJECTED";
  flagged: boolean;
  flagReason: "OUTLIER_HIGH" | "OUTLIER_LOW" | "DUPLICATE" | "MANUAL" | null;
  receiptFileId: number | null;
  createdAt: string;
}

export interface AdminPriceReportUpdateResponse {
  id: number;
  status: "ACTIVE" | "HIDDEN" | "REJECTED";
  flagged: boolean;
  updatedAt: string;
  recalculatedStat: {
    pharmacyId: number;
    drugId: number;
    repPrice: number | null;
    reportCount: number;
  } | null;
}

export interface AdminDrugListItem {
  id: number;
  displayName: string;
  category: string;
  packageUnit: string | null;
  status: "PENDING" | "APPROVED" | "REJECTED";
}

export interface AdminRegionStatsRow {
  region: { code: string; sido: string; sigungu: string };
  drug: { id: number; displayName: string };
  avgPrice: number;
  minPrice: number;
  maxPrice: number;
  pharmacyCount: number;
  reportCount: number;
}

export interface AdminRegionStats {
  rows: AdminRegionStatsRow[];
}

export interface AdminDrugStats {
  drug: { id: number; displayName: string; packageUnit: string | null };
  distribution: { bucketFrom: number; bucketTo: number; count: number }[];
  byRegion: { sido: string; sigungu: string; avgPrice: number; pharmacyCount: number }[];
  national: { avg: number; median: number; min: number; max: number; stdDev: number };
}

export interface AdminPriceGapsRow {
  drug: { id: number; displayName: string };
  cheapestRegion: { sido: string; sigungu: string; avgPrice: number };
  priciestRegion: { sido: string; sigungu: string; avgPrice: number };
  gap: number;
  gapPct: number;
}

export interface AdminPriceGaps {
  rows: AdminPriceGapsRow[];
}
