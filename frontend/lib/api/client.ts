/**
 * 백엔드 API 공통 클라이언트 (docs/api.md "공통 규칙").
 *
 * - 모든 요청은 `{NEXT_PUBLIC_API_URL}/api/v1` 아래로 보낸다.
 * - accessToken은 메모리에만 둔다. 새로고침하면 refreshToken 쿠키(HttpOnly)로 다시 받는다.
 * - 401이 오면 재발급(AUTH-03)을 1번만 시도하고 원래 요청을 다시 보낸다.
 *   재발급은 동시에 여러 번 부르면 서버가 탈취로 보고 모든 기기를 로그아웃시키므로,
 *   진행 중인 재발급 하나를 같이 기다린다 (탭이 여러 개면 Web Locks로 순서를 맞춘다).
 * - 실패 응답 `{ code, message, fieldErrors? }`는 ApiError로 던진다.
 */

const API_ORIGIN = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");

/** NEXT_PUBLIC_API_URL이 비어 있으면 백엔드 없이 mock 데이터로 동작한다 (프로토타입 모드). */
export const API_ENABLED = API_ORIGIN !== "";
export const API_BASE = `${API_ORIGIN}/api/v1`;

/** 구글 로그인 시작 주소 (AUTH-01). API 호출이 아니라 브라우저를 이 주소로 이동시킨다. */
export const GOOGLE_LOGIN_URL = `${API_ORIGIN}/oauth2/authorization/google`;

export interface FieldError {
  field: string;
  message: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors?: FieldError[];

  constructor(status: number, code: string, message: string, fieldErrors?: FieldError[]) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export const NETWORK_ERROR = "NETWORK_ERROR";

/** 화면에 띄울 문구. 서버 message가 있으면 그대로 쓴다. */
export function errorMessage(error: unknown, fallback = "문제가 생겼어요. 잠시 후 다시 시도해주세요."): string {
  if (error instanceof ApiError && error.message) return error.message;
  return fallback;
}

export function isApiError(error: unknown, code?: string): error is ApiError {
  return error instanceof ApiError && (code === undefined || error.code === code);
}

// ---------------------------------------------------------------------------
// accessToken
// ---------------------------------------------------------------------------

let accessToken: string | null = null;

export const tokenStore = {
  get: () => accessToken,
  set: (token: string | null) => {
    accessToken = token;
  },
  clear: () => {
    accessToken = null;
  },
};

/** 재발급까지 실패해 로그인이 풀렸을 때 부른다 (SessionProvider가 등록). */
let onUnauthenticated: (() => void) | null = null;
export function setUnauthenticatedHandler(handler: (() => void) | null) {
  onUnauthenticated = handler;
}

export interface TokenResponse {
  accessToken: string;
  expiresIn: number;
}

let refreshing: Promise<string | null> | null = null;

/**
 * AUTH-03 토큰 재발급. 성공하면 새 accessToken, refreshToken이 없거나 만료됐으면 null.
 * 서버에 닿지 못하면 ApiError(NETWORK_ERROR)를 던진다.
 */
export function refreshAccessToken(): Promise<string | null> {
  if (!refreshing) {
    refreshing = withRefreshLock(requestRefresh).finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

async function requestRefresh(): Promise<string | null> {
  const res = await send(`${API_BASE}/auth/refresh`, { method: "POST", credentials: "include" });
  if (res.status === 401) {
    accessToken = null;
    return null;
  }
  const data = await parse<TokenResponse>(res);
  accessToken = data.accessToken;
  return accessToken;
}

/** 다른 탭과 재발급이 겹치지 않게 한다. Web Locks를 못 쓰는 브라우저는 그냥 실행한다. */
function withRefreshLock<T>(task: () => Promise<T>): Promise<T> {
  if (typeof navigator !== "undefined" && navigator.locks?.request) {
    return navigator.locks.request("lionlink-token-refresh", task) as Promise<T>;
  }
  return task();
}

// ---------------------------------------------------------------------------
// 요청
// ---------------------------------------------------------------------------

type Query = Record<string, string | number | boolean | null | undefined | (string | number)[]>;

export interface ApiOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  /** JSON으로 보낼 본문. FormData면 그대로 보낸다 (파일 업로드). */
  body?: unknown;
  /** 쿼리스트링. undefined·null·빈 문자열은 뺀다. 배열은 같은 키로 여러 번 붙인다. */
  query?: Query;
  /** false면 Authorization 헤더를 붙이지 않고 401에도 재발급하지 않는다 (로그인 전 API). */
  auth?: boolean;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

/**
 * API 호출. 경로는 `/api/v1` 뒤만 쓴다.
 *
 * @example
 *   const me = await api<Me>("/me");
 *   const page = await api<Page<Post>>("/posts", { query: { page: 0, size: 20, category: "FREE" } });
 *   await api("/posts/1/like", { method: "PUT" });
 */
export async function api<T = void>(path: string, options: ApiOptions = {}): Promise<T> {
  const { auth = true } = options;
  const url = buildUrl(path, options.query);

  let res = await send(url, buildInit(options, auth ? accessToken : null));

  if (res.status === 401 && auth) {
    const token = await refreshAccessToken();
    if (token) {
      res = await send(url, buildInit(options, token));
    }
    if (res.status === 401) {
      accessToken = null;
      onUnauthenticated?.();
    }
  }

  return parse<T>(res);
}

function buildUrl(path: string, query?: Query): string {
  const url = `${API_BASE}${path.startsWith("/") ? path : `/${path}`}`;
  if (!query) return url;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === "") continue;
    if (Array.isArray(value)) value.forEach((v) => params.append(key, String(v)));
    else params.append(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

function buildInit(options: ApiOptions, token: string | null): RequestInit {
  const headers: Record<string, string> = { Accept: "application/json", ...options.headers };
  let body: BodyInit | undefined;
  if (options.body instanceof FormData) {
    body = options.body; // Content-Type은 브라우저가 boundary와 함께 붙인다
  } else if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }
  if (token) headers.Authorization = `Bearer ${token}`;
  return {
    method: options.method ?? "GET",
    headers,
    body,
    credentials: "include", // refreshToken·signupToken 쿠키
    signal: options.signal,
  };
}

async function send(url: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(url, init);
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new ApiError(0, NETWORK_ERROR, "서버에 연결할 수 없어요. 잠시 후 다시 시도해주세요.");
  }
}

async function parse<T>(res: Response): Promise<T> {
  const text = res.status === 204 ? "" : await res.text();
  let data: unknown = undefined;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = undefined;
    }
  }

  if (res.ok) return data as T;

  const err = (data ?? {}) as { code?: string; message?: string; fieldErrors?: FieldError[] };
  throw new ApiError(res.status, err.code ?? `HTTP_${res.status}`, err.message ?? defaultMessage(res.status), err.fieldErrors);
}

function defaultMessage(status: number): string {
  if (status === 401) return "로그인이 필요해요.";
  if (status === 403) return "권한이 없어요.";
  if (status === 404) return "찾을 수 없어요.";
  if (status >= 500) return "서버에 문제가 생겼어요. 잠시 후 다시 시도해주세요.";
  return "요청을 처리하지 못했어요.";
}

// ---------------------------------------------------------------------------
// 공통 응답 형식
// ---------------------------------------------------------------------------

/** 전체 개수가 있는 목록 */
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  hasNext: boolean;
}

/** 더보기 목록 (댓글, 관심 동문) */
export interface Slice<T> {
  items: T[];
  page: number;
  size: number;
  hasNext: boolean;
}
