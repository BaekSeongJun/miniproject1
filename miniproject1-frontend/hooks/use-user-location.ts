"use client";

import { useEffect } from "react";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";
import {
  requestStart,
  gpsGranted,
  denied,
  unavailable,
  restoredFromStorage,
  type LocationState,
} from "@/lib/slices/location-slice";

const STORAGE_KEY = "location";

function isRestorable(value: unknown): value is LocationState & { status: "granted" | "fallback" } {
  return (
    typeof value === "object" &&
    value !== null &&
    "status" in value &&
    (value.status === "granted" || value.status === "fallback")
  );
}

export function useUserLocation(): LocationState {
  const dispatch = useAppDispatch();
  const state = useAppSelector((s) => s.location);

  useEffect(() => {
    if (state.status !== "idle") return;

    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (isRestorable(parsed)) {
          dispatch(restoredFromStorage(parsed));
          return;
        }
      }
    } catch {
      // 시크릿 모드 등으로 접근이 막혀도 조용히 GPS 요청으로 진행한다
    }

    if (!navigator.geolocation) {
      dispatch(unavailable());
      return;
    }

    dispatch(requestStart());
    navigator.geolocation.getCurrentPosition(
      (position) => {
        dispatch(gpsGranted({ lat: position.coords.latitude, lng: position.coords.longitude }));
      },
      (error) => {
        if (error.code === error.PERMISSION_DENIED) {
          dispatch(denied());
        } else {
          dispatch(unavailable());
        }
      },
      { enableHighAccuracy: false, timeout: 8000 },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.status]);

  useEffect(() => {
    if (state.status !== "granted" && state.status !== "fallback") return;
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // 시크릿 모드 등으로 저장이 막혀도 앱 동작에는 영향 없다
    }
  }, [state]);

  return state;
}
