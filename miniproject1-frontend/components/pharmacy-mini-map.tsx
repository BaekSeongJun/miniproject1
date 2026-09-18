"use client";

import { PharmacyMap } from "@/components/pharmacy-map";

export function PharmacyMiniMap({ lat, lng }: { lat: number; lng: number }) {
  return (
    <PharmacyMap
      center={{ lat, lng }}
      markers={[]}
      selectedId={null}
      onSelect={() => {}}
      className="h-32 pointer-events-none"
      level={3}
    />
  );
}
