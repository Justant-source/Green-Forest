"use client";

import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { getToken, getNickname, getRole, logout as authLogout } from "@/lib/auth";

interface AuthContextType {
  nickname: string | null;
  role: string | null;
  isLoggedIn: boolean;
  isAdmin: boolean;
  authLoaded: boolean;
  refresh: () => void;
  handleLogout: () => void;
}

const AuthContext = createContext<AuthContextType>({
  nickname: null,
  role: null,
  isLoggedIn: false,
  isAdmin: false,
  authLoaded: false,
  refresh: () => {},
  handleLogout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [nickname, setNickname] = useState<string | null>(getNickname());
  const [role, setRole] = useState<string | null>(getRole());
  const [isLoggedIn, setIsLoggedIn] = useState(!!getToken());
  const [authLoaded, setAuthLoaded] = useState(false);

  const refresh = () => {
    const token = getToken();
    const name = getNickname();
    const userRole = getRole();
    setIsLoggedIn(!!token);
    setNickname(name);
    setRole(userRole);
  };

  const handleLogout = () => {
    authLogout();
    setNickname(null);
    setRole(null);
    setIsLoggedIn(false);
  };

  useEffect(() => {
    refresh();
    setAuthLoaded(true);
  }, []);

  const isAdmin = role === "ADMIN";

  return (
    <AuthContext.Provider value={{ nickname, role, isLoggedIn, isAdmin, authLoaded, refresh, handleLogout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
