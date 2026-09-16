import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch } from "@/lib/api";

function mockFetch(response: Response) {
  vi.stubGlobal(
    "fetch",
    vi.fn(() => Promise.resolve(response)),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("apiFetch", () => {
  it("정상 응답은 JSON을 그대로 반환한다", async () => {
    mockFetch(new Response(JSON.stringify({ id: 1 }), { status: 200 }));

    await expect(apiFetch<{ id: number }>("/x")).resolves.toEqual({ id: 1 });
  });

  it("JSON 에러 바디를 ApiError로 파싱한다", async () => {
    mockFetch(
      new Response(
        JSON.stringify({
          code: "VALIDATION_FAILED",
          message: "가격은 100원 이상이어야 합니다.",
          fieldErrors: [{ field: "price", reason: "must be >= 100" }],
          traceId: "abc123",
        }),
        { status: 400 },
      ),
    );

    await expect(apiFetch("/x")).rejects.toMatchObject({
      status: 400,
      code: "VALIDATION_FAILED",
      message: "가격은 100원 이상이어야 합니다.",
      traceId: "abc123",
    });
  });

  it("비-JSON 에러 바디도 ApiError를 던진다", async () => {
    mockFetch(new Response("<html>Internal Server Error</html>", { status: 500 }));

    await expect(apiFetch("/x")).rejects.toBeInstanceOf(ApiError);
  });

  it("204 No Content는 undefined를 반환한다", async () => {
    mockFetch(new Response(null, { status: 204 }));

    await expect(apiFetch("/x")).resolves.toBeUndefined();
  });
});
