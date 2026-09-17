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
