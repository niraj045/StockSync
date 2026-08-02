const buildDefault = process.env.EXPO_PUBLIC_API_URL ?? 'https://stocksync.caretakers.ind.in/api/v1';

export function normalizeApiUrl(value: string) {
  const trimmed = value.trim().replace(/\/+$/, '');
  if (!/^https?:\/\//i.test(trimmed)) {
    throw new Error('Enter a full address beginning with http:// or https://');
  }
  return trimmed.endsWith('/api/v1') ? trimmed : `${trimmed}/api/v1`;
}

export async function loadApiUrl() {
  return normalizeApiUrl(buildDefault);
}
