"use client";

import { useState } from "react";
import { Navigation, MapPin } from "lucide-react";
import { useUserLocation } from "@/hooks/use-user-location";
import { useRegions } from "@/hooks/use-regions";
import { RegionPicker } from "@/components/region-picker";

export function LocationIndicator() {
  const location = useUserLocation();
  const { data: regions } = useRegions();
  const [pickerOpen, setPickerOpen] = useState(false);
  const [autoOpenedFor, setAutoOpenedFor] = useState<typeof location.status | null>(null);

  const needsFallback = location.status === "denied" || location.status === "unavailable";
  if (needsFallback && autoOpenedFor !== location.status) {
    setAutoOpenedFor(location.status);
    setPickerOpen(true);
  }

  const label = (() => {
    switch (location.status) {
      case "idle":
      case "requesting":
        return "위치 확인 중...";
      case "granted":
        return "현재 위치";
      case "fallback": {
        const sigungu = regions
          ?.flatMap((g) => g.sigungus.map((s) => ({ ...s, sido: g.sido })))
          .find((s) => s.code === location.regionCode);
        return sigungu ? `${sigungu.sido} ${sigungu.sigungu}` : "지역 선택됨";
      }
      case "denied":
      case "unavailable":
        return "위치를 설정해주세요";
    }
  })();

  const Icon = location.status === "granted" ? Navigation : MapPin;

  return (
    <>
      <button
        type="button"
        onClick={() => setPickerOpen(true)}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <Icon className="size-4" />
        {label}
      </button>
      <RegionPicker open={pickerOpen} onOpenChange={setPickerOpen} />
    </>
  );
}
