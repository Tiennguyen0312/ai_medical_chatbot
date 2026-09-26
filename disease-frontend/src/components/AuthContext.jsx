import React, { createContext, useContext, useMemo, useState } from "react";

const AuthContext = createContext(null);
const STORAGE_KEY = "auth_state_v1";

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : { token: null, user: null };
    } catch {
      return { token: null, user: null };
    }
  });
  const value = useMemo(() => {
  const isAuthed = !!auth?.token;
    function login(payload) {
      const next = {
        token: payload?.token ?? null,
        user: payload?.user ?? null,
      };
      setAuth(next);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    }
    function logout() {
      const next = { token: null, user: null };
      setAuth(next);
      localStorage.removeItem(STORAGE_KEY);
    }
  return { auth, isAuthed, login, logout };}, [auth]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}