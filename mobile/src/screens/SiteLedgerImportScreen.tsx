import { useState, useEffect } from 'react';
import { View, StyleSheet, ScrollView, Text, Alert } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import * as DocumentPicker from 'expo-document-picker';
import { Ionicons } from '@expo/vector-icons';
import { apiClient } from '../api/client';
import { AppButton, Card, PageHeader, SelectField } from '../components/ui';
import { colors, fonts } from '../theme';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParams } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParams, 'SiteLedgerImport'>;

interface Site {
  id: number;
  siteName: string;
}

export function SiteLedgerImportScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const [selectedFile, setSelectedFile] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
  const [siteId, setSiteId] = useState<string>('');
  const [sites, setSites] = useState<{label: string, value: string}[]>([]);
  const [isPending, setIsPending] = useState(false);

  useEffect(() => {
    apiClient.get('/sites', { params: { size: 200 } })
      .then(res => {
        setSites(res.data.content.map((s: Site) => ({ label: s.siteName, value: String(s.id) })));
      })
      .catch(() => Alert.alert('Error', 'Failed to load sites'));
  }, []);

  const importLedger = async () => {
    if (!selectedFile || !siteId) return;
    
    setIsPending(true);
    try {
      const formData = new FormData();
      formData.append('file', {
        uri: selectedFile.uri,
        name: selectedFile.name,
        type: selectedFile.mimeType ?? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      } as any);

      const response = await apiClient.post(`/sites/${siteId}/import-ledger`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      const items = response.data.length;
      Alert.alert(
        'Ledger Imported',
        `Successfully mapped ${items} material items and posted them as opening site balances.`,
        [{ text: 'Done', onPress: () => navigation.goBack() }]
      );
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to import ledger';
      Alert.alert('Import Failed', msg);
    } finally {
      setIsPending(false);
    }
  };

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
      <PageHeader eyebrow="Site Operations" title="Import Ledger" />
      
      <Card style={styles.card}>
        <Text style={styles.description}>
          Upload a Statement of Material Delivered & Returned (.xlsx) for a specific site to automatically create its opening balance.
        </Text>

        <SelectField 
          label="Select Site" 
          value={siteId} 
          options={sites} 
          onChange={setSiteId} 
        />

        {siteId ? (
          !selectedFile ? (
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
                  title="Change" 
                  variant="secondary" 
                  onPress={pickDocument} 
                  disabled={isPending}
                />
                <View style={styles.spacer} />
                <AppButton 
                  title="Import" 
                  onPress={importLedger} 
                  loading={isPending}
                />
              </View>
            </View>
          )
        ) : null}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: 16, gap: 16 },
  card: { gap: 20, padding: 20 },
  description: { color: colors.ink, fontSize: 15, lineHeight: 22, fontFamily: fonts.regular },
  fileCard: { backgroundColor: colors.canvas, borderRadius: 8, padding: 16, borderWidth: 1, borderColor: colors.line, gap: 20 },
  fileInfo: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  fileDetails: { flex: 1 },
  fileName: { fontFamily: fonts.bold, fontSize: 16, color: colors.ink, marginBottom: 4 },
  fileSize: { fontFamily: fonts.regular, fontSize: 13, color: colors.muted },
  actions: { flexDirection: 'row' },
  spacer: { width: 12 }
});
