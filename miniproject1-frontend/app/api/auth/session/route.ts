import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import type { AuthResponse } from "@/types/auth";

const COOKIE_NAME = "refresh_token";
const COOKIE_MAX_AGE = 60 * 60 * 24 * 14; // 14일, 백엔드 refresh 토큰 만료와 동일

async function setRefreshCookie(refreshToken: string) {
  const cookieStore = await cookies();
  cookieStore.set(COOKIE_NAME, refreshToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: COOKIE_MAX_AGE,
  });
}

// 로그인/가입 직후: 프론트가 받은 refreshToken을 httpOnly 쿠키에 저장하고, accessToken 등은 그대로 돌려준다.
export async function POST(request: Request) {
  const body = (await request.json()) as AuthResponse;
  await setRefreshCookie(body.refreshToken);
  return NextResponse.json({
    accessToken: body.accessToken,
    expiresIn: body.expiresIn,
    user: body.user,
  });
}

// 새로고침 직후 세션 복원: 쿠키의 refresh 토큰으로 백엔드에서 새 토큰을 받아 rotation한다.
export async function GET() {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(COOKIE_NAME)?.value;
  if (!refreshToken) {
    return NextResponse.json({ error: "NO_SESSION" }, { status: 401 });
  }

  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    cookieStore.delete(COOKIE_NAME);
    return NextResponse.json({ error: "SESSION_EXPIRED" }, { status: 401 });
  }

  const data = (await res.json()) as AuthResponse;
  await setRefreshCookie(data.refreshToken);
  return NextResponse.json({
    accessToken: data.accessToken,
    expiresIn: data.expiresIn,
    user: data.user,
  });
}

// 로그아웃: 쿠키의 refresh 토큰으로 백엔드를 무효화한 뒤(실패해도 무시) 쿠키를 지운다.
export async function DELETE(request: Request) {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(COOKIE_NAME)?.value;

  if (refreshToken) {
    try {
      await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/auth/logout`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(request.headers.get("authorization")
            ? { Authorization: request.headers.get("authorization")! }
            : {}),
        },
        body: JSON.stringify({ refreshToken }),
      });
    } catch {
      // 로그아웃은 멱등하게 끝나야 하므로 백엔드 호출 실패는 무시한다.
    }
  }

  cookieStore.delete(COOKIE_NAME);
  return new NextResponse(null, { status: 204 });
}
