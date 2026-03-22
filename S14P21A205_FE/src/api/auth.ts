import axios from "axios";

// 401 인터셉터 무한루프 방지를 위해 별도 인스턴스 사용
const authClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string | null;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
}

/** refreshToken(HttpOnly 쿠키)으로 새 accessToken 발급 */
export async function refreshAccessToken(): Promise<string> {
  const res = await authClient.post<AuthTokenResponse>("/api/auth/refresh");
  return res.data.accessToken;
}

/** 로그아웃: refreshToken 폐기 + 쿠키 삭제 */
export async function logout(): Promise<void> {
  await authClient.post("/api/auth/logout");
}
