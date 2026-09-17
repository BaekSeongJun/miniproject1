export interface UploadedFileResponse {
  id: number;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  url: string;
  createdAt: string;
}

export interface PriceReportCreateRequest {
  pharmacyId: number;
  drugId: number;
  price: number;
  purchasedAt?: string;
  receiptFileId?: number;
  memo?: string;
}

export interface PriceReportListItem {
  id: number;
  pharmacy: { id: number; name: string };
  drug: { id: number; displayName: string; packageUnit: string | null };
  price: number;
  purchasedAt: string;
  reporter: { nickname: string } | null;
  source: string;
  status: "ACTIVE" | "HIDDEN" | "REJECTED";
  flagged: boolean;
  hasReceipt: boolean;
  createdAt: string;
}

export interface PriceReportResponse {
  id: number;
  pharmacyId: number;
  drugId: number;
  price: number;
  purchasedAt: string;
  status: "ACTIVE" | "HIDDEN" | "REJECTED";
  flagged: boolean;
  flagReason: "OUTLIER_HIGH" | "OUTLIER_LOW" | "DUPLICATE" | "MANUAL" | null;
  createdAt: string;
  warning: string | null;
  updatedStat: {
    repPrice: number;
    minPrice: number;
    avgPrice: number;
    reportCount: number;
    lastReportedAt: string;
  } | null;
}
