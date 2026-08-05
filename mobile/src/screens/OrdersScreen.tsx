import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps, useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useCallback, useState } from 'react';
import { Alert, FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import { ExcelExportButton } from '../components/ExcelExportButton';
import type { MainTabParams, RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import type { Page, SiteOrder } from '../types/api';
import { dateLabel, quantity } from '../utils/format';

type Props = CompositeScreenProps<
  BottomTabScreenProps<MainTabParams, 'Orders'>,
  NativeStackScreenProps<RootStackParams>
>;

export function OrdersScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const canWrite = user?.roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_OPERATIONS');
  const [orders, setOrders] = useState<SiteOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [workingId, setWorkingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<Page<SiteOrder>>('/orders', {
        params: { size: 100, sort: 'id,desc' },
      });
      setOrders(response.data.content);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load site orders.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => void load(), [load]));

  const confirm = (order: SiteOrder) => {
    Alert.alert(
      'Confirm site order?',
      `${order.orderNumber} will be locked and ready for dispatch.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Confirm',
          onPress: async () => {
            setWorkingId(order.id);
            try {
              const response = await apiClient.post<SiteOrder>(`/orders/${order.id}/confirm`);
              setOrders((current) => current.map((row) => row.id === order.id ? response.data : row));
            } catch (cause) {
              Alert.alert('Order not confirmed', apiErrorMessage(cause, 'Unable to confirm this order.'));
            } finally {
              setWorkingId(null);
            }
          },
        },
      ],
    );
  };

  return (
    <FlatList
      data={orders}
      keyExtractor={(item) => String(item.id)}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
      ListHeaderComponent={
        <>
          <PageHeader
            eyebrow="Order to dispatch"
            title="Site orders"
            action={<View style={styles.headerActions}>
              <ExcelExportButton reportType="SITE_ORDERS_REGISTER" />
              {canWrite ? (
                <Pressable accessibilityLabel="Create site order" onPress={() => navigation.navigate('CreateOrder')} style={styles.add}>
                  <Ionicons name="add" color="#fff" size={26} />
                </Pressable>
              ) : null}
            </View>}
          />
          <Text style={styles.intro}>Create the material request, confirm it, then issue the challan without leaving the app.</Text>
          {error ? <Text style={styles.error}>{error}</Text> : null}
        </>
      }
      ListEmptyComponent={!loading ? (
        <Card>
          <EmptyBlock title="No site orders" message="Create the first order from an active agreement." />
          {canWrite ? <AppButton title="Create site order" onPress={() => navigation.navigate('CreateOrder')} /> : null}
        </Card>
      ) : null}
      renderItem={({ item }) => {
        const total = item.items.reduce((sum, row) => sum + Number(row.orderedQuantity), 0);
        const dispatchable = item.status === 'CONFIRMED' || item.status === 'PARTIALLY_FULFILLED';
        return (
          <Pressable onPress={() => item.siteId && navigation.navigate('SiteDetail', { siteId: item.siteId })}>
            <Card style={styles.order}>
              <View style={styles.top}>
                <Text style={styles.number}>{item.orderNumber}</Text>
                <StatusPill value={item.status} />
              </View>
              <Text style={styles.party}>{item.partyName}</Text>
              <Text style={styles.site}>{item.siteName} <Text style={styles.siteLink}>(Tap for site details)</Text></Text>
              <View style={styles.meta}>
                <Text style={styles.metaText}>{dateLabel(item.orderDate)}</Text>
                <Text style={styles.total}>{quantity(total)} ordered</Text>
              </View>
              {canWrite && item.status === 'DRAFT' ? (
                <AppButton
                  title="Confirm order"
                  variant="secondary"
                  loading={workingId === item.id}
                  onPress={() => confirm(item)}
                />
              ) : null}
              {canWrite && dispatchable ? (
                <AppButton
                  title="Issue challan"
                  onPress={() => navigation.navigate('CreateIssuedChallan', { orderId: item.id })}
                />
              ) : null}
            </Card>
          </Pressable>
        );
      }}
    />
  );
}

const styles = StyleSheet.create({
  headerActions: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  content: { padding: 18, paddingBottom: 34, backgroundColor: colors.canvas, flexGrow: 1 },
  add: { width: 46, height: 46, borderRadius: 8, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  intro: { color: colors.muted, lineHeight: 21, marginTop: -8, marginBottom: 16 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12, marginBottom: 12 },
  order: { marginBottom: 10, gap: 11 },
  top: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  number: { color: colors.primary, fontFamily: fonts.bold, flexShrink: 1 },
  party: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold },
  site: { color: colors.muted, fontSize: 13, marginTop: -7 },
  siteLink: { color: colors.primary, fontSize: 12, fontFamily: fonts.bold },
  meta: { flexDirection: 'row', justifyContent: 'space-between', borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 11 },
  metaText: { color: colors.muted, fontSize: 12 },
  total: { color: colors.ink, fontFamily: fonts.bold, fontSize: 12 },
});
