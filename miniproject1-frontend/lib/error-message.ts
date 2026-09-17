import { ApiError } from "@/lib/api";

// 백엔드가 이미 한글 message를 내려주므로(GlobalExceptionHandler) 그대로 쓰고,
// 네트워크 단절·파싱 실패 등 message가 없거나 영문 폴백인 경우만 한글로 감싼다.
export function toUserMessage(err: unknown, fallback = "잠시 후 다시 시도해주세요."): string {
  if (err instanceof ApiError) {
    if (err.code === "NETWORK_ERROR") return "네트워크 연결을 확인해주세요.";
    if (err.code === "UNKNOWN_ERROR" || !err.message) return fallback;
    return err.message;
  }
  return fallback;
}
