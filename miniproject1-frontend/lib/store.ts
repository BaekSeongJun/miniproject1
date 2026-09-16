import { configureStore } from "@reduxjs/toolkit";
import locationReducer from "@/lib/slices/location-slice";
import authReducer, { restoreSession } from "@/lib/slices/auth-slice";
import reportDraftReducer from "@/lib/slices/report-draft-slice";
import { configureApiAuth } from "@/lib/api";

export const store = configureStore({
  reducer: {
    location: locationReducer,
    auth: authReducer,
    reportDraft: reportDraftReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

// apiFetch가 store를 직접 import하면 순환 참조가 생기므로 함수로 위임한다.
configureApiAuth({
  getAccessToken: () => store.getState().auth.accessToken,
  refreshAccessToken: async () => {
    const result = await store.dispatch(restoreSession());
    return restoreSession.fulfilled.match(result) ? (result.payload?.accessToken ?? null) : null;
  },
});
