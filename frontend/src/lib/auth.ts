const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("token");
}

export function getNickname(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("nickname");
}

export function getRole(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("role");
}

export function isLoggedIn(): boolean {
  return !!getToken();
}

export function saveAuth(token: string, nickname: string, role: string) {
  localStorage.setItem("token", token);
  localStorage.setItem("nickname", nickname);
  localStorage.setItem("role", role);
}

export function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("nickname");
  localStorage.removeItem("role");
}

export async function login(email: string, password: string) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    let msg = "아이디/비밀번호를 확인해주세요.";
    try {
      const body = await res.json();
      if (body.message) msg = body.message;
    } catch {}
    throw new Error(msg);
  }
  const data = await res.json();
  saveAuth(data.token, data.nickname, data.role || "USER");
  return data;
}

export async function register(email: string, password: string, nickname: string) {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, nickname }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "회원가입에 실패했습니다.");
  }
  const data = await res.json();
  saveAuth(data.token, data.nickname, data.role || "USER");
  return data;
}
