export interface Sigungu {
  code: string;
  sigungu: string;
  centerLat: number;
  centerLng: number;
  pharmacyCount: number;
}

export interface RegionGroup {
  sido: string;
  sigungus: Sigungu[];
}
