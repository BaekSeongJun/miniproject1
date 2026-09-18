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

export interface AdminTopPharmaciesRow {
  pharmacy: { id: number; name: string; sido: string | null; sigungu: string | null };
  reportCount: number;
}

export interface AdminTopPharmacies {
  period: string;
  rows: AdminTopPharmaciesRow[];
}

export interface AdminTopReportersRow {
  userId: number;
  nickname: string;
  reportCount: number;
}

export interface AdminTopReporters {
  period: string;
  rows: AdminTopReportersRow[];
}

export interface AdminTopDrugsByReportsRow {
  drug: { id: number; displayName: string };
  reportCount: number;
}

export interface AdminTopDrugsByReports {
  period: string;
  rows: AdminTopDrugsByReportsRow[];
}

export interface AdminTopDrugsBySearchesRow {
  drug: { id: number; displayName: string };
  searchCount: number;
}

export interface AdminTopDrugsBySearches {
  period: string;
  rows: AdminTopDrugsBySearchesRow[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
