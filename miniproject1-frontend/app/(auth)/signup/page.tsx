"use client";

import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { signupSchema, type SignupFormValues } from "@/lib/schemas/auth";
import { signup } from "@/lib/slices/auth-slice";
import { useAppDispatch } from "@/lib/hooks";
import { ApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";

export default function SignupPage() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const next = useSearchParams().get("next") ?? "/";

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormValues>({ resolver: zodResolver(signupSchema) });

  async function onSubmit(values: SignupFormValues) {
    try {
      await dispatch(signup(values)).unwrap();
      router.push(next);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === "EMAIL_ALREADY_EXISTS") {
          setError("email", { message: "이미 가입된 이메일입니다." });
          return;
        }
        for (const fe of err.fieldErrors ?? []) {
          setError(fe.field as keyof SignupFormValues, { message: fe.reason });
        }
        if (!err.fieldErrors?.length) {
          setError("root", { message: err.message });
        }
        return;
      }
      setError("root", { message: "회원가입에 실패했습니다. 잠시 후 다시 시도해주세요." });
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-6 px-4 py-16">
      <h1 className="text-xl font-semibold">회원가입</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="email" className="text-sm font-medium">
            이메일
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            className="h-9 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            {...register("email")}
          />
          {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="password" className="text-sm font-medium">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            className="h-9 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            {...register("password")}
          />
          {errors.password && <p className="text-xs text-destructive">{errors.password.message}</p>}
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="nickname" className="text-sm font-medium">
            닉네임
          </label>
          <input
            id="nickname"
            autoComplete="nickname"
            className="h-9 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            {...register("nickname")}
          />
          {errors.nickname && <p className="text-xs text-destructive">{errors.nickname.message}</p>}
        </div>

        {errors.root && <p className="text-sm text-destructive">{errors.root.message}</p>}

        <Button type="submit" disabled={isSubmitting} className="mt-2 w-full">
          {isSubmitting ? "가입 중..." : "가입하기"}
        </Button>
      </form>

      <p className="text-center text-sm text-muted-foreground">
        이미 계정이 있으신가요?{" "}
        <Link href={`/login?next=${encodeURIComponent(next)}`} className="text-primary underline-offset-4 hover:underline">
          로그인
        </Link>
      </p>
    </div>
  );
}
