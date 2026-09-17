"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAppSelector } from "@/lib/hooks";

// 관리자 메뉴 존재 자체를 일반 사용자에게 노출하지 않기 위해, 권한이 없으면 홈으로 리다이렉트한다.
export function useRequireAdmin() {
  const router = useRouter();
  const auth = useAppSelector((s) => s.auth);

  useEffect(() => {
    if (auth.status === "unauthenticated" || (auth.status === "authenticated" && auth.user?.role !== "ADMIN")) {
      router.replace("/");
    }
  }, [auth.status, auth.user, router]);

  const ready = auth.status === "authenticated" && auth.user?.role === "ADMIN";
  return { ready };
}
