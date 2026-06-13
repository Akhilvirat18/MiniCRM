import React, { createContext, useContext, useState, useCallback } from 'react';
import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

const AuthContext = createContext(null);

// ── Storage helpers ───────────────────────────────────────────────────────────
const TOKEN_KEY = 'minicrm_token';
const USER_KEY  = 'minicrm_user';

const loadFromStorage = () => ({
  token: localStorage.getItem(TOKEN_KEY) ?? null,
  user:  JSON.parse(localStorage.getItem(USER_KEY) ?? 'null'),
});

const saveToStorage = (token, user) => {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
};

const clearStorage = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
};

// ── Provider ──────────────────────────────────────────────────────────────────
export function AuthProvider({ children }) {
  const stored = loadFromStorage();
  const [token, setToken] = useState(stored.token);
  const [user,  setUser]  = useState(stored.user);
  const [error, setError] = useState(null);

  const applyAuth = (data) => {
    setToken(data.token);
    setUser(data.user);
    saveToStorage(data.token, data.user);
    setError(null);
  };

  const login = useCallback(async (email, password) => {
    setError(null);
    const res = await axios.post(`${API_BASE}/auth/login`, { email, password });
    applyAuth(res.data);
  }, []);

  const register = useCallback(async (name, email, password) => {
    setError(null);
    const res = await axios.post(`${API_BASE}/auth/register`, { name, email, password });
    applyAuth(res.data);
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    clearStorage();
  }, []);

  return (
    <AuthContext.Provider value={{ token, user, login, register, logout, error, setError }}>
      {children}
    </AuthContext.Provider>
  );
}

// ── Hook ──────────────────────────────────────────────────────────────────────
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
};
