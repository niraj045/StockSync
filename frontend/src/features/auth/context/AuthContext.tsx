import React, { createContext, useContext, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';

export interface User {
  id: number;
  fullName: string;
  username: string;
  email: string;
  active: boolean;
  roles: string[];
  version: number;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const queryClient = useQueryClient();

  const initCsrf = async () => {
    await apiClient.get('/auth/csrf');
  };

  const refreshUser = async () => {
    try {
      const response = await apiClient.get<User>('/auth/me');
      setUser(response.data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const initSession = async () => {
      try {
        await initCsrf();
        await refreshUser();
      } catch {
        setLoading(false);
      }
    };

    initSession();

    const handleSessionExpired = () => {
      setUser(null);
      queryClient.clear();
    };

    window.addEventListener('auth:session-expired', handleSessionExpired);
    return () => {
      window.removeEventListener('auth:session-expired', handleSessionExpired);
    };
  }, [queryClient]);

  const login = async (usernameOrEmail: string, password: string): Promise<User> => {
    // 1. Fetch CSRF token before login submission as required by the flow
    await initCsrf();

    // 2. Submit credentials
    const response = await apiClient.post<User>('/auth/login', {
      usernameOrEmail,
      password,
    });

    setUser(response.data);
    return response.data;
  };

  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      setUser(null);
      queryClient.clear();
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
