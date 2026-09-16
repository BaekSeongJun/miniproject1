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

// authSlice가 스토어 초기화 시 주입한다. api.ts가 store.ts를 직접 import하면 순환 참조가 생기기 때문에
// 토큰 조회/재발급 로직을 함수로 위임받는다.
let getAccessToken: () => string | null = () => null;
let refreshAccessToken: () => Promise<string | null> = async () => null;

export function configureApiAuth(config: {
  getAccessToken: () => string | null;
  refreshAccessToken: () => Promise<string | null>;
}) {
  getAccessToken = config.getAccessToken;
  refreshAccessToken = config.refreshAccessToken;
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit & { auth?: boolean },
): Promise<T> {
  const { auth, headers, ...rest } = init ?? {};

  async function send(token: string | null) {
    return fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}${path}`, {
      ...rest,
      headers: {
        "Content-Type": "application/json",
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
    });
  }

  let res = await send(getAccessToken());

  // access 토큰 만료(401) 시 refresh 1회만 시도하고, 그래도 실패하면 그대로 에러를 던진다.
  if (auth && res.status === 401) {
    const newToken = await refreshAccessToken();
    if (newToken) res = await send(newToken);
  }

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
