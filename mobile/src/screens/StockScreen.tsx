import { useCallback, useEffect, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, TextInput, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { Card, EmptyBlock, PageHeader } from '../components/ui';
import { ExcelExportButton } from '../components/ExcelExportButton';
import { colors, fonts } from '../theme';
import type { Page, StockBalance } from '../types/api';
import type { RootStackParams } from '../navigation/types';
import { quantity } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'Stock'>;
type StockFocus = 'all' | 'godown' | 'sites';

export function StockScreen({ route }: Props) {
  const insets = useSafeAreaInsets();
  const [items, setItems] = useState<StockBalance[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [focus, setFocus] = useState<StockFocus>(route.params?.focus ?? 'all');

  useEffect(() => {
    setFocus(route.params?.focus ?? 'all');
  }, [route.params?.focus]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<Page<StockBalance>>('/stock/balances', {
        params: { search: search.trim() || undefined, size: 200 },
      });
      setItems(response.data.content);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load stock.'));
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    const timer = setTimeout(() => void load(), 250);
    return () => clearTimeout(timer);
  }, [load]);

  const visibleItems = items.filter((item) => focus === 'godown'
    ? Number(item.availableQuantity) > 0
    : focus === 'sites' ? Number(item.issuedQuantity) > 0 : true);

  return (
    <FlatList
      data={visibleItems}
      keyExtractor={(item) => String(item.itemId)}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
      ListHeaderComponent={
        <>
          <PageHeader
            eyebrow="Live inventory"
            title="Stock overview"
            action={<ExcelExportButton reportType={focus === 'godown' ? 'GODOWN_STOCK' : focus === 'sites' ? 'SITE_PENDING_STOCK' : 'CURRENT_STOCK_SUMMARY'} />}
          />
          <View accessibilityRole="tablist" style={styles.focusTabs}>
            <FocusTab active={focus === 'all'} label="All stock" onPress={() => setFocus('all')} />
            <FocusTab active={focus === 'godown'} label="Godown" onPress={() => setFocus('godown')} />
            <FocusTab active={focus === 'sites'} label="At sites" onPress={() => setFocus('sites')} />
          </View>
          <View style={styles.search}>
            <Ionicons name="search-outline" size={20} color={colors.muted} />
            <TextInput
              autoCapitalize="none"
              onChangeText={setSearch}
              placeholder="Search code or material"
              placeholderTextColor="#98A2B3"
              returnKeyType="search"
              style={styles.searchInput}
              value={search}
            />
          </View>
          {error ? <Text style={styles.error}>{error}</Text> : null}
        </>
      }
      ListEmptyComponent={!loading ? <Card><EmptyBlock title="No stock found" message={focus === 'all'
        ? 'Try another item name or code.'
        : `There are no materials with ${focus === 'godown' ? 'godown' : 'site'} balance.`} /></Card> : null}
      renderItem={({ item }) => (
        <Card style={styles.item}>
          <View style={styles.itemTop}>
            <View style={styles.itemCopy}>
              <Text style={styles.code}>{item.itemCode}</Text>
              <Text style={styles.name}>{item.itemName}</Text>
              <Text style={styles.meta}>{item.categoryName} | {item.unit}</Text>
            </View>
            {item.belowMinimum ? <View style={styles.low}><Text style={styles.lowText}>LOW</Text></View> : null}
          </View>
          <View style={styles.quantities}>
            <StockValue label="Godown" value={item.availableQuantity} strong />
            <StockValue label="At sites" value={item.issuedQuantity} />
            <StockValue label="Lost" value={item.lostQuantity} />
          </View>
        </Card>
      )}
    />
  );
}

function FocusTab({ active, label, onPress }: { active: boolean; label: string; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="tab"
      accessibilityState={{ selected: active }}
      onPress={onPress}
      style={({ pressed }) => [styles.focusTab, active && styles.focusTabActive, pressed && styles.focusTabPressed]}
    >
      <Text style={[styles.focusTabText, active && styles.focusTabTextActive]}>{label}</Text>
    </Pressable>
  );
}

function StockValue({ label, value, strong }: { label: string; value: number; strong?: boolean }) {
  return (
    <View style={styles.stockValue}>
      <Text style={styles.stockLabel}>{label}</Text>
      <Text style={[styles.stockNumber, strong && styles.stockStrong]}>{quantity(value)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  content: { padding: 18, paddingBottom: 34, backgroundColor: colors.canvas, flexGrow: 1 },
  focusTabs: { flexDirection: 'row', padding: 3, marginBottom: 12, borderRadius: 8, backgroundColor: colors.neutralSoft },
  focusTab: { flex: 1, minHeight: 42, alignItems: 'center', justifyContent: 'center', borderRadius: 6 },
  focusTabActive: { backgroundColor: colors.primary },
  focusTabPressed: { opacity: 0.75 },
  focusTabText: { color: colors.muted, fontSize: 12, fontFamily: fonts.semiBold },
  focusTabTextActive: { color: '#fff', fontFamily: fonts.bold },
  search: { flexDirection: 'row', alignItems: 'center', gap: 9, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 14, marginBottom: 14 },
  searchInput: { flex: 1, height: 50, color: colors.ink, fontSize: 16 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12, marginBottom: 12 },
  item: { marginBottom: 10 },
  itemTop: { flexDirection: 'row', alignItems: 'flex-start' },
  itemCopy: { flex: 1 },
  code: { color: colors.primary, fontSize: 11, fontFamily: fonts.bold },
  name: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold, marginTop: 3 },
  meta: { color: colors.muted, fontSize: 12, marginTop: 5 },
  low: { backgroundColor: colors.redSoft, paddingHorizontal: 9, paddingVertical: 5, borderRadius: 8 },
  lowText: { color: colors.red, fontSize: 10, fontFamily: fonts.bold },
  quantities: { flexDirection: 'row', borderTopWidth: 1, borderTopColor: colors.line, marginTop: 14, paddingTop: 13 },
  stockValue: { flex: 1 },
  stockLabel: { color: colors.muted, fontSize: 11 },
  stockNumber: { color: colors.ink, fontSize: 17, fontFamily: fonts.semiBold, marginTop: 3 },
  stockStrong: { color: colors.primary, fontFamily: fonts.black },
});
