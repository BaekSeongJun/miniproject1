"use client";

import Link from "next/link";
import { LocationIndicator } from "@/components/location-indicator";
import { NoticeBanner } from "@/components/notice-banner";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";
import { logout } from "@/lib/slices/auth-slice";

export function Header() {
  const dispatch = useAppDispatch();
  const { user, status } = useAppSelector((state) => state.auth);

  return (
    <header>
      <div className="flex items-center justify-between px-4 py-3">
        <Link href="/" className="text-lg font-semibold">
          약가비교
        </Link>
        <div className="flex items-center gap-4">
          <LocationIndicator />
          {status === "authenticated" && user ? (
            <div className="flex items-center gap-3 text-sm">
              <span className="text-muted-foreground">{user.nickname}님</span>
              <button
                type="button"
                onClick={() => dispatch(logout())}
                className="text-muted-foreground hover:text-foreground"
              >
                로그아웃
              </button>
            </div>
          ) : (
            <Link href="/login" className="text-sm text-muted-foreground">
              로그인
            </Link>
          )}
        </div>
      </div>
      <NoticeBanner />
    </header>
  );
}
