import { describe, expect, it } from "vitest";
import reducer, {
  requestStart,
  gpsGranted,
  regionSelected,
  denied,
  unavailable,
  restoredFromStorage,
} from "@/lib/slices/location-slice";

describe("locationSlice", () => {
  it("requestStart는 requesting 상태가 된다", () => {
    expect(reducer(undefined, requestStart())).toEqual({ status: "requesting" });
  });

  it("gpsGranted는 GPS 출처의 granted 상태가 된다", () => {
    const state = reducer(undefined, gpsGranted({ lat: 37.5, lng: 127.0 }));
    expect(state).toEqual({ status: "granted", lat: 37.5, lng: 127.0, source: "GPS" });
  });

  it("regionSelected는 REGION 출처의 fallback 상태가 된다", () => {
    const state = reducer(undefined, regionSelected({ lat: 37.4959, lng: 127.0664, regionCode: "11680" }));
    expect(state).toEqual({
      status: "fallback",
      lat: 37.4959,
      lng: 127.0664,
      regionCode: "11680",
      source: "REGION",
    });
  });

  it("denied는 denied 상태가 된다", () => {
    expect(reducer(undefined, denied())).toEqual({ status: "denied" });
  });

  it("unavailable은 unavailable 상태가 된다", () => {
    expect(reducer(undefined, unavailable())).toEqual({ status: "unavailable" });
  });

  it("restoredFromStorage는 payload를 그대로 상태로 치환한다", () => {
    const payload = { status: "granted" as const, lat: 1, lng: 2, source: "GPS" as const };
    expect(reducer(undefined, restoredFromStorage(payload))).toEqual(payload);
  });
});
