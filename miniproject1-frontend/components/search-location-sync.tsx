"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAppSelector } from "@/lib/hooks";

// 검색 결과 화면에서 헤더의 위치를 바꾸면(GPS 재요청, 지역 선택) 사용자가 메인으로
// 돌아가지 않아도 같은 약품으로 즉시 재검색되도록, URL의 lat/lng·regionCode를
// Redux location 상태와 동기화한다. 뒤로가기 스택을 늘리지 않도록 push 대신 replace.
export function SearchLocationSync() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const location = useAppSelector((s) => s.location);

  useEffect(() => {
    if (location.status !== "granted" && location.status !== "fallback") return;

    const currentLat = searchParams.get("lat");
    const currentLng = searchParams.get("lng");
    const nextLat = String(location.lat);
    const nextLng = String(location.lng);
    if (currentLat === nextLat && currentLng === nextLng) return;

    const params = new URLSearchParams(searchParams);
    params.set("lat", nextLat);
    params.set("lng", nextLng);
    params.delete("regionCode");
    router.replace(`/search?${params.toString()}`);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location]);

  return null;
}
