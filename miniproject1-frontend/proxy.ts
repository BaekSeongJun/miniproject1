import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// 세션 존재 여부만 확인한다(실제 access 토큰 발급/검증은 클라이언트와 백엔드가 담당).
// Next.js 16부터 middleware.ts는 proxy.ts로 이름이 바뀌었다.
const REFRESH_COOKIE = "refresh_token";

export function proxy(request: NextRequest) {
  const hasSession = request.cookies.has(REFRESH_COOKIE);
  if (hasSession) return NextResponse.next();

  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("next", request.nextUrl.pathname + request.nextUrl.search);
  return NextResponse.redirect(loginUrl);
}

export const config = {
  matcher: ["/reports/new", "/me", "/admin/:path*"],
};
