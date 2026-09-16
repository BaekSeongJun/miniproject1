import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type LocationState =
  | { status: "idle" | "requesting" }
  | { status: "granted"; lat: number; lng: number; source: "GPS" }
  | {
      status: "fallback";
      lat: number;
      lng: number;
      source: "REGION";
      regionCode: string;
    }
  | { status: "denied" | "unavailable" };

const initialState: LocationState = { status: "idle" };

const locationSlice = createSlice({
  name: "location",
  initialState: initialState as LocationState,
  reducers: {
    requestStart: (): LocationState => ({ status: "requesting" }),
    gpsGranted: (_state, action: PayloadAction<{ lat: number; lng: number }>): LocationState => ({
      status: "granted",
      lat: action.payload.lat,
      lng: action.payload.lng,
      source: "GPS",
    }),
    regionSelected: (
      _state,
      action: PayloadAction<{ lat: number; lng: number; regionCode: string }>,
    ): LocationState => ({
      status: "fallback",
      lat: action.payload.lat,
      lng: action.payload.lng,
      regionCode: action.payload.regionCode,
      source: "REGION",
    }),
    denied: (): LocationState => ({ status: "denied" }),
    unavailable: (): LocationState => ({ status: "unavailable" }),
    restoredFromStorage: (_state, action: PayloadAction<LocationState>): LocationState => action.payload,
  },
});

export const { requestStart, gpsGranted, regionSelected, denied, unavailable, restoredFromStorage } =
  locationSlice.actions;
export default locationSlice.reducer;
