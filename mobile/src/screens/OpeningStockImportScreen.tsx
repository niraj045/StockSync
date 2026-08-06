import { useState } from 'react';
import { View, StyleSheet, ScrollView, Text, Alert } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import * as DocumentPicker from 'expo-document-picker';
import { Ionicons } from '@expo/vector-icons';
import { apiClient } from '../api/client';
import { AppButton, Card, PageHeader } from '../components/ui';
import { colors, fonts } from '../theme';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParams } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParams, 'OpeningStockImport'>;

export function OpeningStockImportScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const [selectedFile, setSelectedFile] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
  const [isPending, setIsPending] = useState(false);

  const importStock = async (file: DocumentPicker.DocumentPickerAsset) => {
    setIsPending(true);
    try {
      const formData = new FormData();
      formData.append('file', {
        uri: file.uri,
        name: file.name,
        type: file.mimeType ?? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      } as any);

      // 1. Upload workbook
      const uploadRes = await apiClient.post('/stock-imports/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      const uploadData = uploadRes.data;
      const batchId = uploadData.id;
      const fileChecksum = uploadData.fileChecksum;

      // 2. Auto-map items and party/site locations
      await apiClient.post(`/stock-imports/${batchId}/auto-map`);

      // 3. Post opening stock
      const postRes = await apiClient.post(`/stock-imports/${batchId}/post`, {
        confirmed: true,
        expectedChecksum: fileChecksum,
      });
      const postData = postRes.data;

      Alert.alert(
        'Import Successful',
        `Workbook "${dataName(uploadData, file)}" processed & posted successfully!\n\nBatch: ${uploadData.batchCode}\nTotal Posted Items & Stock: ${postData.postedCombinedTotal ?? uploadData.expectedCombinedTotal ?? 'Complete'}`,
        [{ text: 'OK', onPress: () => navigation.goBack() }]
      );
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to import stock data';
      Alert.alert('Import Failed', msg);
    } finally {
      setIsPending(false);
    }
  };

  const dataName = (data: any, file: DocumentPicker.DocumentPickerAsset) => data.fileName || file.name;

  const pickDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: [
          'application/vnd.ms-excel',
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        ],
        copyToCacheDirectory: true,
      });

      if (result.canceled) return;
      if (result.assets && result.assets.length > 0) {
        setSelectedFile(result.assets[0]);
      }
    } catch (err) {
      Alert.alert('Error', 'Failed to pick a document.');
    }
  };

  return (
    <ScrollView contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]} bounces={false}>
      <PageHeader eyebrow="Administration" title="Import opening stock" />
      
      <Card style={styles.card}>
        <Text style={styles.description}>
          Upload a SteelFab customer snapshot Excel file to automatically create the party, site, and opening stock records.
        </Text>

        {!selectedFile ? (
          <AppButton 
            title="Select Excel File" 
            onPress={pickDocument} 
            icon={<Ionicons name="document-attach-outline" size={20} color="#fff" />} 
          />
        ) : (
          <View style={styles.fileCard}>
            <View style={styles.fileInfo}>
              <Ionicons name="document-text" size={32} color={colors.primary} />
              <View style={styles.fileDetails}>
                <Text style={styles.fileName} numberOfLines={1}>{selectedFile.name}</Text>
                <Text style={styles.fileSize}>{(selectedFile.size ?? 0) / 1024 > 1024 ? ((selectedFile.size ?? 0) / 1024 / 1024).toFixed(2) + ' MB' : ((selectedFile.size ?? 0) / 1024).toFixed(0) + ' KB'}</Text>
              </View>
            </View>
            
            <View style={styles.actions}>
              <AppButton 
                title="Change File" 
                variant="secondary" 
                onPress={pickDocument} 
                disabled={isPending}
              />
              <View style={styles.spacer} />
              <AppButton 
                title="Upload & Import" 
                onPress={() => importStock(selectedFile)} 
                loading={isPending}
              />
            </View>
          </View>
        )}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: {
    padding: 16,
    gap: 16,
  },
  card: {
    gap: 20,
    padding: 20,
  },
  description: {
    color: colors.ink,
    fontSize: 15,
    lineHeight: 22,
    fontFamily: fonts.regular,
  },
  fileCard: {
    backgroundColor: colors.canvas,
    borderRadius: 8,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 20,
  },
  fileInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  fileDetails: {
    flex: 1,
  },
  fileName: {
    fontFamily: fonts.bold,
    fontSize: 16,
    color: colors.ink,
    marginBottom: 4,
  },
  fileSize: {
    fontFamily: fonts.regular,
    fontSize: 13,
    color: colors.muted,
  },
  actions: {
    flexDirection: 'row',
  },
  spacer: {
    width: 12,
  }
});
