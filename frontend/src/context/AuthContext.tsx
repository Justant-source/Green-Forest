"use client";

import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { getToken, getNickname, getName, getRole, logout as authLogout } from "@/lib/auth";

interface AuthContextType {
  nickname: string | null;
  name: string | null;
  role: string | null;
  isLoggedIn: boolean;
  isAdmin: boolean;
  authLoaded: boolean;
  refresh: () => void;
  handleLogout: () => void;
}

const AuthContext = createContext<AuthContextType>({
  nickname: null,
  name: null,
  role: null,
  isLoggedIn: false,
  isAdmin: false,
  authLoaded: false,
  refresh: () => {},
  handleLogout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [nickname, setNickname] = useState<string | null>(null);
  const [name, setName] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [authLoaded, setAuthLoaded] = useState(false);

  const refresh = () => {
    const token = getToken();
    const nick = getNickname();
    const userName = getName();
    const userRole = getRole();
    setIsLoggedIn(!!token);
    setNickname(nick);
    setName(userName);
    setRole(userRole);
  };

  const handleLogout = () => {
    authLogout();
    setNickname(null);
    setName(null);
    setRole(null);
    setIsLoggedIn(false);
  };

  useEffect(() => {
    refresh();
    setAuthLoaded(true);
  }, []);

  const isAdmin = role === "ADMIN";

  return (
    <AuthContext.Provider value={{ nickname, name, role, isLoggedIn, isAdmin, authLoaded, refresh, handleLogout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
