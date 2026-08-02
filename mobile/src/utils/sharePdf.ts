import { encode } from 'base64-arraybuffer';
import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import { apiClient } from '../api/client';

export async function shareServerPdf(path: string, filename: string, dialogTitle: string) {
  return shareServerFile(path, filename, 'application/pdf', dialogTitle);
}

export async function shareServerFile(path: string, filename: string, mimeType: string, dialogTitle: string) {
  const response = await apiClient.get<ArrayBuffer>(path, { responseType: 'arraybuffer' });
  const filePath = `${FileSystem.cacheDirectory}${filename.replaceAll('/', '-')}`;
  await FileSystem.writeAsStringAsync(filePath, encode(response.data), {
    encoding: FileSystem.EncodingType.Base64,
  });
  if (!(await Sharing.isAvailableAsync())) throw new Error('Sharing is not available on this device');
  await Sharing.shareAsync(filePath, { mimeType, dialogTitle });
}
