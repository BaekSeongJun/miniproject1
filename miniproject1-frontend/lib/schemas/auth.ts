import { z } from "zod";

// 백엔드 API.md §2 검증 규칙과 동일하게 맞춘다.
const email = z.string().min(1, "이메일을 입력해주세요.").email("이메일 형식이 올바르지 않습니다.");
const password = z
  .string()
  .min(8, "비밀번호는 8~64자여야 합니다.")
  .max(64, "비밀번호는 8~64자여야 합니다.")
  .regex(/(?=.*[A-Za-z])(?=.*\d)/, "비밀번호는 영문과 숫자를 포함해야 합니다.");
const nickname = z.string().min(2, "닉네임은 2~30자여야 합니다.").max(30, "닉네임은 2~30자여야 합니다.");

export const signupSchema = z.object({ email, password, nickname });
export type SignupFormValues = z.infer<typeof signupSchema>;

export const loginSchema = z.object({
  email: z.string().min(1, "이메일을 입력해주세요."),
  password: z.string().min(1, "비밀번호를 입력해주세요."),
});
export type LoginFormValues = z.infer<typeof loginSchema>;
