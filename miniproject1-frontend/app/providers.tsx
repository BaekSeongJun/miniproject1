"use client";

import { useEffect, type ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Provider as ReduxProvider } from "react-redux";
import { store } from "@/lib/store";
import { restoreSession } from "@/lib/slices/auth-slice";

let browserQueryClient: QueryClient | undefined;

function getQueryClient() {
  if (typeof window === "undefined") return newQueryClient();
  browserQueryClient ??= newQueryClient();
  return browserQueryClient;
}

function newQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,
        retry: 1,
      },
    },
  });
}

function SessionBootstrap() {
  useEffect(() => {
    // StrictMode가 개발 모드에서 effect를 두 번 실행한다. restoreSession은 refresh 토큰을
    // rotation(1회성)하므로 중복 호출하면 두 번째 요청이 이미 무효화된 토큰으로 실패한다.
    if (store.getState().auth.status === "idle") {
      store.dispatch(restoreSession());
    }
  }, []);
  return null;
}

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={getQueryClient()}>
      <ReduxProvider store={store}>
        <SessionBootstrap />
        {children}
      </ReduxProvider>
    </QueryClientProvider>
  );
}
