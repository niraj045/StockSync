import { createContext, PropsWithChildren, useContext, useEffect, useState } from 'react';
import { apiClient, configureApi, initializeCsrf, onSessionExpired } from '../api/client';
import type { User } from '../types/api';

type AuthValue = {
  user: User | null;
  loading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  reconnect: () => Promise<void>;
};

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const reconnect = async () => {
    setLoading(true);
    try {
      await configureApi();
      await initializeCsrf();
      const response = await apiClient.get<User>('/auth/me');
      setUser(response.data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    onSessionExpired(() => setUser(null));
    void reconnect();
    return () => onSessionExpired(null);
  }, []);

  const login = async (usernameOrEmail: string, password: string) => {
    await initializeCsrf();
    const response = await apiClient.post<User>('/auth/login', { usernameOrEmail, password });
    setUser(response.data);
  };

  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, reconnect }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
