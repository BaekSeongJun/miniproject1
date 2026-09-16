import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export interface AuthState {
  userId: number | null;
  email: string | null;
}

const initialState: AuthState = {
  userId: null,
  email: null,
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setUser(state, action: PayloadAction<{ userId: number; email: string }>) {
      state.userId = action.payload.userId;
      state.email = action.payload.email;
    },
    clearUser(state) {
      state.userId = null;
      state.email = null;
    },
  },
});

export const { setUser, clearUser } = authSlice.actions;
export default authSlice.reducer;
