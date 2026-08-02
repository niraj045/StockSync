import axios, { AxiosError } from 'axios';
import { loadApiUrl } from './config';

type SessionExpiredListener = () => void;

let csrfToken: string | null = null;
let sessionExpiredListener: SessionExpiredListener | null = null;

export const apiClient = axios.create({
  timeout: 20_000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  if (csrfToken && config.method && !['get', 'head', 'options'].includes(config.method)) {
    config.headers['X-XSRF-TOKEN'] = csrfToken;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) sessionExpiredListener?.();
    return Promise.reject(error);
  },
);

export async function configureApi() {
  apiClient.defaults.baseURL = await loadApiUrl();
}

export function setApiUrl(url: string) {
  apiClient.defaults.baseURL = url;
  csrfToken = null;
}

export function onSessionExpired(listener: SessionExpiredListener | null) {
  sessionExpiredListener = listener;
}

export async function initializeCsrf() {
  const response = await apiClient.get('/auth/csrf');
  csrfToken =
    (response.headers['x-csrf-token'] as string | undefined) ??
    (response.headers['X-CSRF-TOKEN'] as string | undefined) ??
    null;
  if (!csrfToken) throw new Error('The server did not provide a mobile CSRF token');
}

export function apiErrorMessage(error: unknown, fallback: string) {
  if (!axios.isAxiosError(error)) return fallback;
  const data = error.response?.data as { message?: string } | undefined;
  if (!error.response) return 'Cannot reach the SteelFab server. Check the server address and network.';
  return data?.message ?? fallback;
}
