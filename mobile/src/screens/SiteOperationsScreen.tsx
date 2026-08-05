import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, Card, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import type { OperationResponse } from '../types/api';
import { dateLabel, quantity } from '../utils/format';

import { ExcelExportButton } from '../components/ExcelExportButton';

type Props = NativeStackScreenProps<RootStackParams, 'SiteOperations'>;

export function SiteOperationsScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const [rows, setRows] = useState<OperationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setRows((await apiClient.get<OperationResponse[]>('/client-workflow/operations')).data);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load site operations.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => void load(), [load]));

  return (
    <ScrollView
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
    >
      <PageHeader
        eyebrow="Operations"
        title="Site costs"
        action={
          <View style={styles.headerActions}>
            <ExcelExportButton reportType="SITE_OPERATIONS_REGISTER" />
            <Pressable
              accessibilityLabel="Create site operation"
              onPress={() => navigation.navigate('CreateSiteOperation')}
              style={styles.add}
            >
              <Ionicons name="add" color="#fff" size={26} />
            </Pressable>
          </View>
        }
      />
      {error ? <Text style={styles.error}>{error}</Text> : null}
      
      {rows.length ? rows.map((row) => (
        <Card key={row.id} style={styles.card}>
          <View style={styles.top}>
            <Text style={styles.number}>{row.operationNumber}</Text>
            <StatusPill value={row.status} />
          </View>
          <Text style={styles.name}>{row.operationType.replaceAll('_', ' ')}</Text>
          <Text style={styles.meta}>{row.siteName} · {dateLabel(row.operationDate)} · {row.direction}</Text>
          <Text style={styles.meta}>Amount: ₹{quantity(row.amount)}{row.chargeToClient ? ' · Charge to client' : ''}</Text>
          
          <View style={styles.cardActions}>
            <View style={{ flex: 1 }}>
              <AppButton
                title="Edit operation"
                variant="secondary"
                onPress={() => navigation.navigate('CreateSiteOperation', { operationId: row.id })}
              />
            </View>
            <ExcelExportButton
              reportType="SITE_OPERATIONS_REGISTER"
              filters={{ siteId: String(row.siteId), docNo: row.operationNumber }}
            />
          </View>
        </Card>
      )) : !loading ? (
        <Card>
          <EmptyBlock title="No site operations" message="Record transport, labour or site expenses." />
          <AppButton title="Create operation" onPress={() => navigation.navigate('CreateSiteOperation')} />
        </Card>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: 18, paddingBottom: 40, backgroundColor: colors.canvas, flexGrow: 1 },
  headerActions: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  add: { width: 46, height: 46, borderRadius: 8, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  error: { color: colors.red, marginBottom: 12 },
  card: { marginBottom: 10, gap: 10 },
  top: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  number: { color: colors.primary, fontFamily: fonts.bold },
  name: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold },
  meta: { color: colors.muted, fontSize: 13 },
  cardActions: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 4 },
});
