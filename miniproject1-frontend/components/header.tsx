"use client";

import Link from "next/link";
import { LocationIndicator } from "@/components/location-indicator";
import { LogoMark } from "@/components/logo-mark";
import { NoticeBanner } from "@/components/notice-banner";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";
import { logout } from "@/lib/slices/auth-slice";

export function Header() {
  const dispatch = useAppDispatch();
  const { user, status } = useAppSelector((state) => state.auth);

  return (
    <header className="border-b-2 border-border bg-card">
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 px-4 py-3">
        <Link href="/" className="flex shrink-0 items-center gap-2">
          <LogoMark className="size-7 sm:size-8" />
          <span className="text-base font-bold tracking-tight sm:text-lg">우리동네약값</span>
        </Link>
        <div className="flex items-center gap-3 sm:gap-4">
          <LocationIndicator />
          {status === "authenticated" && user ? (
            <div className="flex items-center gap-2.5 text-sm sm:gap-3">
              <span className="hidden text-muted-foreground md:inline">{user.nickname}님</span>
              <Link href="/me" className="text-muted-foreground hover:text-foreground">
                내 제보
              </Link>
              {user.role === "ADMIN" && (
                <Link href="/admin" className="text-muted-foreground hover:text-foreground">
                  관리자
                </Link>
              )}
              <button
                type="button"
                onClick={() => dispatch(logout())}
                className="text-muted-foreground hover:text-foreground"
              >
                로그아웃
              </button>
            </div>
          ) : (
            <Link href="/login" className="text-sm font-medium text-primary hover:underline">
              로그인
            </Link>
          )}
        </div>
      </div>
      <NoticeBanner />
    </header>
  );
}
