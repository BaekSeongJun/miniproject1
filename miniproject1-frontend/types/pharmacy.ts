export interface BusinessHours {
  mon: [string, string] | null;
  tue: [string, string] | null;
  wed: [string, string] | null;
  thu: [string, string] | null;
  fri: [string, string] | null;
  sat: [string, string] | null;
  sun: [string, string] | null;
  holiday: [string, string] | null;
}

export interface DrugPrice {
  drugId: number;
  displayName: string;
  packageUnit: string | null;
  repPrice: number;
  minPrice: number;
  maxPrice: number;
  avgPrice: number;
  reportCount: number;
  lastReportedAt: string;
  nationalAvgPrice: number;
  diffFromNationalAvg: number;
}

export interface PharmacyDetail {
  id: number;
  name: string;
  addressRoad: string;
  addressJibun: string | null;
  lat: number;
  lng: number;
  phone: string | null;
  businessHours: BusinessHours | null;
  distanceM: number | null;
  region: { code: string; sido: string; sigungu: string } | null;
  drugPrices: DrugPrice[];
}

export interface PriceHistoryPoint {
  purchasedAt: string;
  price: number;
  flagged: boolean;
}

export interface DrugPriceHistory {
  pharmacyId: number;
  drugId: number;
  points: PriceHistoryPoint[];
}
