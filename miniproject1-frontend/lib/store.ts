import { configureStore } from "@reduxjs/toolkit";
import locationReducer from "@/lib/slices/location-slice";
import authReducer from "@/lib/slices/auth-slice";
import reportDraftReducer from "@/lib/slices/report-draft-slice";

export const store = configureStore({
  reducer: {
    location: locationReducer,
    auth: authReducer,
    reportDraft: reportDraftReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
