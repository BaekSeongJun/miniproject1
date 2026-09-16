"use client";

import Script from "next/script";
import { useEffect, useRef, useState } from "react";
import { formatPrice } from "@/lib/format";
import type { PharmacyResult } from "@/types/search";

type KakaoLatLng = { __brand: "KakaoLatLng" };
interface KakaoOverlay {
  getPosition(): KakaoLatLng;
}
interface KakaoMap {
  setBounds(bounds: unknown): void;
  panTo(position: KakaoLatLng): void;
}
interface KakaoMapsNamespace {
  load(callback: () => void): void;
  Map: new (container: HTMLElement, options: { center: KakaoLatLng; level: number }) => KakaoMap;
  LatLng: new (lat: number, lng: number) => KakaoLatLng;
  LatLngBounds: new () => { extend(position: KakaoLatLng): void };
  Marker: new (options: { map: KakaoMap; position: KakaoLatLng; image: unknown }) => unknown;
  MarkerImage: new (src: string, size: unknown) => unknown;
  Size: new (width: number, height: number) => unknown;
  CustomOverlay: new (options: {
    map: KakaoMap;
    position: KakaoLatLng;
    yAnchor: number;
    content: HTMLElement;
  }) => KakaoOverlay;
}

declare global {
  interface Window {
    kakao: { maps: KakaoMapsNamespace };
  }
}

export function PharmacyMap({
  center,
  results,
  selectedId,
  onSelect,
}: {
  center: { lat: number; lng: number };
  results: PharmacyResult[];
  selectedId: number | null;
  onSelect: (id: number) => void;
}) {
  const [ready, setReady] = useState(false);
  const [failed, setFailed] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const overlaysRef = useRef<Map<number, KakaoOverlay>>(new Map());
  const overlayElsRef = useRef<Map<number, HTMLDivElement>>(new Map());
  const onSelectRef = useRef(onSelect);

  useEffect(() => {
    onSelectRef.current = onSelect;
  }, [onSelect]);

  useEffect(() => {
    const container = containerRef.current;
    if (!ready || !container) return;

    window.kakao.maps.load(() => {
      const map = new window.kakao.maps.Map(container, {
        center: new window.kakao.maps.LatLng(center.lat, center.lng),
        level: 5,
      });
      mapRef.current = map;

      new window.kakao.maps.Marker({
        map,
        position: new window.kakao.maps.LatLng(center.lat, center.lng),
        image: new window.kakao.maps.MarkerImage(
          "data:image/svg+xml;base64," +
            btoa(
              '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><circle cx="8" cy="8" r="7" fill="#2563eb" stroke="white" stroke-width="2"/></svg>',
            ),
          new window.kakao.maps.Size(16, 16),
        ),
      });

      const bounds = new window.kakao.maps.LatLngBounds();
      bounds.extend(new window.kakao.maps.LatLng(center.lat, center.lng));

      const overlays = new Map<number, KakaoOverlay>();
      const overlayEls = new Map<number, HTMLDivElement>();
      for (const result of results) {
        const position = new window.kakao.maps.LatLng(result.pharmacy.lat, result.pharmacy.lng);
        bounds.extend(position);

        const el = document.createElement("div");
        el.className = `kakao-price-overlay${result.recommended ? " kakao-price-overlay--top" : ""}`;
        el.textContent = formatPrice(result.price.repPrice);
        el.onclick = () => onSelectRef.current(result.pharmacy.id);

        const overlay = new window.kakao.maps.CustomOverlay({
          map,
          position,
          yAnchor: 1.3,
          content: el,
        });

        overlays.set(result.pharmacy.id, overlay);
        overlayEls.set(result.pharmacy.id, el);
      }
      overlaysRef.current = overlays;
      overlayElsRef.current = overlayEls;

      if (results.length > 0) {
        map.setBounds(bounds);
      }
    });
  }, [ready, center.lat, center.lng, results]);

  useEffect(() => {
    for (const el of overlayElsRef.current.values()) {
      el.classList.remove("kakao-price-overlay--selected");
    }
    if (selectedId == null) return;

    const el = overlayElsRef.current.get(selectedId);
    el?.classList.add("kakao-price-overlay--selected");

    const map = mapRef.current;
    const overlay = overlaysRef.current.get(selectedId);
    if (map && overlay) map.panTo(overlay.getPosition());
  }, [selectedId]);

  if (failed) return null;

  const appKey = process.env.NEXT_PUBLIC_KAKAO_MAP_KEY;
  if (!appKey) return null;

  return (
    <>
      <Script
        src={`https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&autoload=false&libraries=services`}
        strategy="afterInteractive"
        onLoad={() => setReady(true)}
        onError={() => setFailed(true)}
      />
      <div ref={containerRef} className="h-64 w-full rounded-lg border" />
    </>
  );
}
