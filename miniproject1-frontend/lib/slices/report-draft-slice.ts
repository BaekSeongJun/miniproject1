import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export interface ReportDraftState {
  pharmacyId: number | null;
  drugId: number | null;
  price: number | null;
}

const initialState: ReportDraftState = {
  pharmacyId: null,
  drugId: null,
  price: null,
};

const reportDraftSlice = createSlice({
  name: "reportDraft",
  initialState,
  reducers: {
    updateDraft(state, action: PayloadAction<Partial<ReportDraftState>>) {
      Object.assign(state, action.payload);
    },
    resetDraft() {
      return initialState;
    },
  },
});

export const { updateDraft, resetDraft } = reportDraftSlice.actions;
export default reportDraftSlice.reducer;
