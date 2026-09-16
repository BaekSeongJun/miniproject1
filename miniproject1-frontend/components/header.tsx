import Link from "next/link";
import { LocationIndicator } from "@/components/location-indicator";
import { NoticeBanner } from "@/components/notice-banner";

export function Header() {
  return (
    <header>
      <div className="flex items-center justify-between px-4 py-3">
        <Link href="/" className="text-lg font-semibold">
          약가비교
        </Link>
        <div className="flex items-center gap-4">
          <LocationIndicator />
          <Link href="/login" className="text-sm text-muted-foreground">
            로그인
          </Link>
        </div>
      </div>
      <NoticeBanner />
    </header>
  );
}
