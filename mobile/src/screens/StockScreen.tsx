import { useCallback, useEffect, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, Text, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { Card, EmptyBlock, PageHeader } from '../components/ui';
import { colors, fonts } from '../theme';
import type { Page, StockBalance } from '../types/api';
import { quantity } from '../utils/format';

export function StockScreen() {
  const insets = useSafeAreaInsets();
  const [items, setItems] = useState<StockBalance[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

  return (
    <FlatList
      data={items}
      keyExtractor={(item) => String(item.itemId)}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
      ListHeaderComponent={
        <>
          <PageHeader eyebrow="Live inventory" title="Stock overview" />
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
      ListEmptyComponent={!loading ? <Card><EmptyBlock title="No stock found" message="Try another item name or code." /></Card> : null}
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
