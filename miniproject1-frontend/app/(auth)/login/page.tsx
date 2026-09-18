"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { loginSchema, type LoginFormValues } from "@/lib/schemas/auth";
import { login } from "@/lib/slices/auth-slice";
import { useAppDispatch } from "@/lib/hooks";
import { ApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}

function LoginForm() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const next = useSearchParams().get("next") ?? "/";

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  async function onSubmit(values: LoginFormValues) {
    try {
      await dispatch(login(values)).unwrap();
      router.push(next);
    } catch (err) {
      if (err instanceof ApiError && err.code === "ACCOUNT_SUSPENDED") {
        setError("root", { message: "정지된 계정입니다. 고객센터에 문의해주세요." });
        return;
      }
      setError("root", { message: "이메일 또는 비밀번호가 올바르지 않습니다." });
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-7 px-4 py-16">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">로그인</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">우리 동네 약값을 계속 비교해보세요.</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="email" className="text-sm font-medium">
            이메일
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            aria-invalid={!!errors.email}
            aria-describedby={errors.email ? "email-error" : undefined}
            className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none transition-shadow focus-visible:border-primary focus-visible:ring-3 focus-visible:ring-ring/30"
            {...register("email")}
          />
          {errors.email && (
            <p id="email-error" className="text-xs text-destructive">
              {errors.email.message}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="password" className="text-sm font-medium">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            aria-invalid={!!errors.password}
            aria-describedby={errors.password ? "password-error" : undefined}
            className="h-10 rounded-lg border border-border bg-background px-3 text-sm outline-none transition-shadow focus-visible:border-primary focus-visible:ring-3 focus-visible:ring-ring/30"
            {...register("password")}
          />
          {errors.password && (
            <p id="password-error" className="text-xs text-destructive">
              {errors.password.message}
            </p>
          )}
        </div>

        {errors.root && (
          <p role="alert" className="text-sm text-destructive">
            {errors.root.message}
          </p>
        )}

        <Button type="submit" disabled={isSubmitting} className="mt-2 w-full">
          {isSubmitting ? "로그인 중..." : "로그인"}
        </Button>
      </form>

      <p className="text-center text-sm text-muted-foreground">
        아직 계정이 없으신가요?{" "}
        <Link href={`/signup?next=${encodeURIComponent(next)}`} className="font-medium text-primary underline-offset-4 hover:underline">
          회원가입
        </Link>
      </p>
    </div>
  );
}
