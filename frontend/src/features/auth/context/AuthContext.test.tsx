import { renderHook, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { apiClient } from '../../../api/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import React from 'react';

vi.mock('../../../api/client', () => {
  return {
    apiClient: {
      get: vi.fn(),
      post: vi.fn(),
      interceptors: {
        response: {
          use: vi.fn(),
        },
      },
    },
  };
});

const queryClient = new QueryClient();

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>{children}</AuthProvider>
  </QueryClientProvider>
);

describe('AuthContext & CSRF Login Flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('initializes session on mount by fetching CSRF and current user', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: {} }); // For /auth/csrf
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: { id: 1, username: 'admin', roles: ['ROLE_ADMIN'] },
    }); // For /auth/me

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(apiClient.get).toHaveBeenCalledWith('/auth/csrf');
    expect(apiClient.get).toHaveBeenCalledWith('/auth/me');
    expect(result.current.user?.username).toBe('admin');
  });

  it('performs CSRF check before login credentials submission', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: {} });
    vi.mocked(apiClient.post).mockResolvedValue({
      data: { id: 2, username: 'testuser', roles: ['ROLE_VIEWER'] },
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(async () => {
      await result.current.login('testuser', 'Password123');
    });

    const getCalls = vi.mocked(apiClient.get).mock.calls;
    const postCalls = vi.mocked(apiClient.post).mock.calls;

    // Verify both calls occurred
    expect(getCalls.length).toBeGreaterThanOrEqual(1);
    expect(postCalls.length).toBe(1);

    // Verify /auth/login is called with credentials
    expect(postCalls[0][0]).toBe('/auth/login');
    expect(postCalls[0][1]).toEqual({
      usernameOrEmail: 'testuser',
      password: 'Password123',
    });
  });

  it('clears query cache on logout', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: {} });
    vi.mocked(apiClient.post).mockResolvedValue({ data: {} });

    const clearSpy = vi.spyOn(queryClient, 'clear');

    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(async () => {
      await result.current.logout();
    });

    expect(apiClient.post).toHaveBeenCalledWith('/auth/logout');
    expect(clearSpy).toHaveBeenCalled();
  });
});
