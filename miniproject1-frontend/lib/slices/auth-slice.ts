import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { apiFetch } from "@/lib/api";
import type { AuthResponse, AuthUser } from "@/types/auth";

export type AuthStatus = "idle" | "loading" | "authenticated" | "unauthenticated";

export interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  status: AuthStatus;
}

const initialState: AuthState = {
  user: null,
  accessToken: null,
  status: "idle",
};

interface SessionResponse {
  accessToken: string;
  expiresIn: number;
  user: AuthUser;
}

export interface SignupInput {
  email: string;
  password: string;
  nickname: string;
}

export interface LoginInput {
  email: string;
  password: string;
}

// 백엔드가 발급한 토큰 쌍을 Route Handler(/api/auth/session)에 넘겨 refresh 토큰을 httpOnly 쿠키로 옮긴다.
async function persistSession(auth: AuthResponse): Promise<SessionResponse> {
  const res = await fetch("/api/auth/session", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(auth),
  });
  return res.json();
}

export const signup = createAsyncThunk("auth/signup", async (input: SignupInput) => {
  await apiFetch("/api/v1/auth/signup", { method: "POST", body: JSON.stringify(input) });
  const auth = await apiFetch<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email: input.email, password: input.password }),
  });
  return persistSession(auth);
});

export const login = createAsyncThunk("auth/login", async (input: LoginInput) => {
  const auth = await apiFetch<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(input),
  });
  return persistSession(auth);
});

// 새로고침 직후: httpOnly 쿠키에 남은 refresh 토큰으로 세션을 복원한다. 쿠키가 없거나 만료면 비로그인 상태로 남는다.
export const restoreSession = createAsyncThunk("auth/restore", async () => {
  const res = await fetch("/api/auth/session");
  if (!res.ok) return null;
  return (await res.json()) as SessionResponse;
});

export const logout = createAsyncThunk<void, void, { state: { auth: AuthState } }>(
  "auth/logout",
  async (_, { getState }) => {
    const { accessToken } = getState().auth;
    await fetch("/api/auth/session", {
      method: "DELETE",
      headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined,
    });
  },
);

function onPending(state: AuthState) {
  state.status = "loading";
}

function onFulfilled(state: AuthState, action: { payload: SessionResponse | null }) {
  if (!action.payload) {
    onRejected(state);
    return;
  }
  state.status = "authenticated";
  state.user = action.payload.user;
  state.accessToken = action.payload.accessToken;
}

function onRejected(state: AuthState) {
  state.status = "unauthenticated";
  state.user = null;
  state.accessToken = null;
}

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, onPending)
      .addCase(login.fulfilled, onFulfilled)
      .addCase(login.rejected, onRejected)
      .addCase(signup.pending, onPending)
      .addCase(signup.fulfilled, onFulfilled)
      .addCase(signup.rejected, onRejected)
      .addCase(restoreSession.pending, onPending)
      .addCase(restoreSession.fulfilled, onFulfilled)
      .addCase(restoreSession.rejected, onRejected)
      .addCase(logout.fulfilled, onRejected);
  },
});

export default authSlice.reducer;
