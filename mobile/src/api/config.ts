import AsyncStorage from '@react-native-async-storage/async-storage';

const SERVER_KEY = 'stocksync.apiUrl';
const buildDefault = process.env.EXPO_PUBLIC_API_URL ?? 'http://10.0.2.2:8081/api/v1';

export function normalizeApiUrl(value: string) {
  const trimmed = value.trim().replace(/\/+$/, '');
  if (!/^https?:\/\//i.test(trimmed)) {
    throw new Error('Enter a full address beginning with http:// or https://');
  }
  return trimmed.endsWith('/api/v1') ? trimmed : `${trimmed}/api/v1`;
}

export async function loadApiUrl() {
  const saved = await AsyncStorage.getItem(SERVER_KEY);
  return normalizeApiUrl(saved ?? buildDefault);
}

export async function saveApiUrl(value: string) {
  const normalized = normalizeApiUrl(value);
  await AsyncStorage.setItem(SERVER_KEY, normalized);
  return normalized;
}
