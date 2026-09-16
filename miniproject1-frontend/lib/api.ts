export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public fieldErrors?: { field: string; reason: string }[],
    public traceId?: string,
  ) {
    super(message);
  }
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit & { auth?: boolean },
): Promise<T> {
  const { auth, headers, ...rest } = init ?? {};

  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(auth ? { Authorization: `Bearer ${getAccessToken()}` } : {}),
      ...headers,
    },
  });

  if (!res.ok) {
    try {
      const body = await res.json();
      throw new ApiError(
        res.status,
        body.code ?? "UNKNOWN_ERROR",
        body.message ?? res.statusText,
        body.fieldErrors,
        body.traceId,
      );
    } catch (err) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(res.status, "UNKNOWN_ERROR", res.statusText);
    }
  }

  if (res.status === 204) return undefined as T;

  return res.json() as Promise<T>;
}

function getAccessToken(): string {
  // T-25에서 실제 토큰 저장소와 연결한다.
  return "";
}
