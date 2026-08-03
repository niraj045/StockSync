import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps, useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useCallback, useState } from 'react';
import {
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import { ExcelExportButton } from '../components/ExcelExportButton';
import type { MainTabParams, RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import type { IssuedChallan, Page, ReceivingChallan } from '../types/api';
import { dateLabel, quantity } from '../utils/format';

type Props = CompositeScreenProps<
  BottomTabScreenProps<MainTabParams, 'Challans'>,
  NativeStackScreenProps<RootStackParams>
>;

type Mode = 'issued' | 'received';

export function ChallansScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const canWrite = user?.roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_OPERATIONS');
  const [mode, setMode] = useState<Mode>('issued');
  const [issued, setIssued] = useState<IssuedChallan[]>([]);
  const [received, setReceived] = useState<ReceivingChallan[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const endpoint = mode === 'issued' ? '/challans/issued' : '/challans/receiving';
      const response = await apiClient.get<Page<IssuedChallan | ReceivingChallan>>(endpoint, {
        params: { search: search.trim() || undefined, size: 100, sort: 'id,desc' },
      });
      if (mode === 'issued') setIssued(response.data.content as IssuedChallan[]);
      else setReceived(response.data.content as ReceivingChallan[]);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load challans.'));
    } finally {
      setLoading(false);
    }
  }, [mode, search]);

  useFocusEffect(useCallback(() => {
    const timer = setTimeout(() => void load(), 150);
    return () => clearTimeout(timer);
  }, [load]));

  const data: Array<IssuedChallan | ReceivingChallan> = mode === 'issued' ? issued : received;

  return (
    <FlatList<IssuedChallan | ReceivingChallan>
      data={data}
      keyExtractor={(item) => `${mode}-${item.id}`}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
      ListHeaderComponent={
        <>
          <PageHeader
            eyebrow="Daily operations"
            title="Challans"
            action={<View style={styles.headerActions}>
              <ExcelExportButton
                reportType={mode === 'issued' ? 'ISSUED_CHALLANS_REGISTER' : 'RECEIVING_CHALLANS_REGISTER'}
                filters={{ documentNumber: search.trim() || undefined }}
              />
              {canWrite ? (
                <Pressable
                  accessibilityLabel={mode === 'issued' ? 'Issue challan' : 'Record material return'}
                  onPress={() => navigation.navigate(mode === 'issued' ? 'CreateIssuedChallan' : 'CreateReceivingChallan')}
                  style={styles.add}
                >
                  <Ionicons name="add" color="#fff" size={26} />
                </Pressable>
              ) : null}
            </View>}
          />
          <View style={styles.segment}>
            <Segment label="Issued" active={mode === 'issued'} onPress={() => setMode('issued')} />
            <Segment label="Received" active={mode === 'received'} onPress={() => setMode('received')} />
          </View>
          <View style={styles.search}>
            <Ionicons name="search-outline" size={20} color={colors.muted} />
            <TextInput
              onChangeText={setSearch}
              placeholder="Search number, party or site"
              placeholderTextColor="#98A2B3"
              style={styles.searchInput}
              value={search}
            />
          </View>
          {error ? <Text style={styles.error}>{error}</Text> : null}
        </>
      }
      ListEmptyComponent={!loading ? (
        <Card><EmptyBlock title={`No ${mode} challans`} message="New challans will appear here." /></Card>
      ) : null}
      renderItem={({ item }) => {
        if (mode === 'issued') {
          const challan = item as IssuedChallan;
          const total = challan.items.reduce((sum, row) => sum + Number(row.quantity), 0);
          return (
            <Pressable onPress={() => navigation.navigate('IssuedChallanDetail', { challan })}>
              <Card style={styles.row}>
                <View style={styles.rowTop}>
                  <Text style={styles.number}>{challan.challanNumber}</Text>
                  <Ionicons name="chevron-forward" size={20} color={colors.muted} />
                </View>
                <Text style={styles.party}>{challan.partyName}</Text>
                <Text style={styles.site}>{challan.siteName}</Text>
                <View style={styles.rowMeta}>
                  <Text style={styles.meta}>{dateLabel(challan.dispatchDate)}</Text>
                  <Text style={styles.total}>{quantity(total)} units</Text>
                </View>
              </Card>
            </Pressable>
          );
        }
        const challan = item as ReceivingChallan;
        return (
          <Pressable onPress={() => navigation.navigate('ReceivingChallanDetail', { challan })}>
            <Card style={styles.row}>
              <View style={styles.rowTop}>
                <Text style={styles.number}>{challan.receivingChallanNumber}</Text>
                <StatusPill value={challan.status} />
              </View>
              <Text style={styles.party}>{challan.partyName}</Text>
              <Text style={styles.site}>{challan.siteName}</Text>
              <View style={styles.rowMeta}>
                <Text style={styles.meta}>{dateLabel(challan.receiveDate)}</Text>
                <Ionicons name="chevron-forward" size={20} color={colors.muted} />
              </View>
            </Card>
          </Pressable>
        );
      }}
    />
  );
}

function Segment({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={[styles.segmentButton, active && styles.segmentActive]}>
      <Text style={[styles.segmentText, active && styles.segmentTextActive]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  headerActions: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  content: { padding: 18, paddingBottom: 34, backgroundColor: colors.canvas, flexGrow: 1 },
  add: { width: 46, height: 46, borderRadius: 8, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  segment: { flexDirection: 'row', backgroundColor: '#E3E6E9', borderRadius: 8, padding: 4, marginBottom: 12 },
  segmentButton: { flex: 1, minHeight: 42, alignItems: 'center', justifyContent: 'center', borderRadius: 6 },
  segmentActive: { backgroundColor: colors.primary },
  segmentText: { color: colors.muted, fontFamily: fonts.bold },
  segmentTextActive: { color: '#fff' },
  search: { flexDirection: 'row', alignItems: 'center', gap: 9, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 14, marginBottom: 14 },
  searchInput: { flex: 1, height: 50, color: colors.ink, fontSize: 15 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12, marginBottom: 12 },
  row: { marginBottom: 10 },
  rowTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  number: { color: colors.primary, fontSize: 13, fontFamily: fonts.bold, flexShrink: 1 },
  party: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold, marginTop: 10 },
  site: { color: colors.muted, fontSize: 13, marginTop: 3 },
  rowMeta: { borderTopWidth: 1, borderTopColor: colors.line, marginTop: 13, paddingTop: 11, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  meta: { color: colors.muted, fontSize: 12 },
  total: { color: colors.ink, fontWeight: '800' },
});
