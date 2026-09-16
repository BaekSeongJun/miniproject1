export type UserRole = "USER" | "ADMIN";

export interface AuthUser {
  id: number;
  nickname: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: AuthUser;
}

export interface MeResponse {
  id: number;
  email: string;
  nickname: string;
  role: UserRole;
  reportCount: number;
  createdAt: string;
}
