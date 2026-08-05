import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, Text, View, Pressable } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, Card, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import { ExcelExportButton } from '../components/ExcelExportButton';
import type { RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import type { IssuedChallan, OperationResponse, Page, ReceivingChallan, SiteOrder, StockBalance } from '../types/api';
import { dateLabel, quantity } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'SiteDetail'>;
type TabType = 'stock' | 'orders' | 'challans' | 'operations';

interface SiteProfile {
  id: number;
  siteName: string;
  siteCode: string;
  partyId: number;
  partyLegalName?: string;
  partyName?: string;
  addressLine1?: string;
  city?: string;
  state?: string;
  pincode?: string;
  status: string;
  defaulter?: boolean;
}

export function SiteDetailScreen({ route, navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { siteId } = route.params;

  const [site, setSite] = useState<SiteProfile | null>(null);
  const [stock, setStock] = useState<StockBalance[]>([]);
  const [orders, setOrders] = useState<SiteOrder[]>([]);
  const [issuedChallans, setIssuedChallans] = useState<IssuedChallan[]>([]);
  const [receivingChallans, setReceivingChallans] = useState<ReceivingChallan[]>([]);
  const [operations, setOperations] = useState<OperationResponse[]>([]);
  
  const [activeTab, setActiveTab] = useState<TabType>('stock');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [siteRes, stockRes, ordersRes, issuedRes, receivingRes, opsRes] = await Promise.all([
        apiClient.get<SiteProfile>(`/sites/${siteId}`).catch(() => null),
        apiClient.get<Page<StockBalance>>(`/stock/balances/site/${siteId}`, { params: { size: 500 } }).catch(() => null),
        apiClient.get<Page<SiteOrder>>('/orders', { params: { siteId, size: 200, sort: 'id,desc' } }).catch(() => null),
        apiClient.get<Page<IssuedChallan>>('/challans/issued', { params: { siteId, size: 200, sort: 'id,desc' } }).catch(() => null),
        apiClient.get<Page<ReceivingChallan>>('/challans/receiving', { params: { siteId, size: 200, sort: 'id,desc' } }).catch(() => null),
        apiClient.get<OperationResponse[]>('/client-workflow/operations', { params: { siteId } }).catch(() => null),
      ]);

      if (siteRes?.data) setSite(siteRes.data);
      if (stockRes?.data) setStock(stockRes.data.content || []);
      if (ordersRes?.data) setOrders(ordersRes.data.content || []);
      if (issuedRes?.data) setIssuedChallans(issuedRes.data.content || []);
      if (receivingRes?.data) setReceivingChallans(receivingRes.data.content || []);
      if (opsRes?.data) setOperations(opsRes.data || []);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load site details.'));
    } finally {
      setLoading(false);
    }
  }, [siteId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const partyName = site?.partyLegalName || site?.partyName || 'Customer';

  return (
    <ScrollView
      contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 34 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={loadData} colors={[colors.primary]} />}
    >
      <PageHeader
        eyebrow={partyName}
        title={site?.siteName || 'Site Detail'}
        action={<ExcelExportButton reportType="MONTHLY_SITE_STATEMENT" filters={{ siteId: String(siteId) }} />}
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}

      {/* Site Header Profile Card */}
      <Card style={styles.profileCard}>
        <View style={styles.topRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.siteTitle}>{site?.siteName}</Text>
            <Text style={styles.partySubtitle}>{partyName}</Text>
          </View>
          <StatusPill value={site?.status || 'ACTIVE'} />
        </View>

        <View style={styles.infoRow}>
          <Text style={styles.codeText}>Code: {site?.siteCode || `SITE-${siteId}`}</Text>
          {site?.defaulter ? <Text style={styles.defaulterBadge}>DEFAULTER</Text> : null}
        </View>

        {site?.addressLine1 ? (
          <Text style={styles.address}>
            {site.addressLine1}{site.city ? `, ${site.city}` : ''}{site.state ? `, ${site.state}` : ''}
          </Text>
        ) : null}

        <View style={styles.summaryBar}>
          <View style={styles.summaryItem}>
            <Text style={styles.summaryVal}>{stock.length}</Text>
            <Text style={styles.summaryLbl}>Material Items</Text>
          </View>
          <View style={styles.summaryDivider} />
          <View style={styles.summaryItem}>
            <Text style={styles.summaryVal}>{orders.length}</Text>
            <Text style={styles.summaryLbl}>Orders</Text>
          </View>
          <View style={styles.summaryDivider} />
          <View style={styles.summaryItem}>
            <Text style={styles.summaryVal}>{issuedChallans.length + receivingChallans.length}</Text>
            <Text style={styles.summaryLbl}>Challans</Text>
          </View>
        </View>
      </Card>

      {/* Segmented Navigation Tabs */}
      <View style={styles.tabsRow}>
        {(['stock', 'orders', 'challans', 'operations'] as TabType[]).map((tab) => (
          <Pressable
            key={tab}
            onPress={() => setActiveTab(tab)}
            style={[styles.tabBtn, activeTab === tab && styles.tabBtnActive]}
          >
            <Text style={[styles.tabTxt, activeTab === tab && styles.tabTxtActive]}>
              {tab === 'stock' ? `Stock (${stock.length})` :
               tab === 'orders' ? `Orders (${orders.length})` :
               tab === 'challans' ? `Challans (${issuedChallans.length + receivingChallans.length})` :
               `Operations (${operations.length})`}
            </Text>
          </Pressable>
        ))}
      </View>

      {/* Tab Contents */}
      {activeTab === 'stock' && (
        <View style={styles.tabContent}>
          {stock.length ? stock.map((stk) => (
            <Card key={stk.itemId} style={styles.itemCard}>
              <View style={styles.itemHeader}>
                <Text style={styles.itemCode}>{stk.itemCode}</Text>
                <Text style={styles.qtyText}>{quantity(stk.availableQuantity || stk.hiredQuantity)} {stk.unit}</Text>
              </View>
              <Text style={styles.itemName}>{stk.itemName}</Text>
            </Card>
          )) : !loading ? (
            <EmptyBlock title="No material stock" message="No pending materials recorded at this site." />
          ) : null}
        </View>
      )}

      {activeTab === 'orders' && (
        <View style={styles.tabContent}>
          {orders.length ? orders.map((ord) => (
            <Card key={ord.id} style={styles.itemCard}>
              <View style={styles.itemHeader}>
                <Text style={styles.itemCode}>{ord.orderNumber}</Text>
                <StatusPill value={ord.status} />
              </View>
              <Text style={styles.metaText}>Date: {dateLabel(ord.orderDate)}</Text>
              <Text style={styles.metaText}>{ord.items?.length ?? 0} line items ordered</Text>
              {(ord.status === 'CONFIRMED' || ord.status === 'PARTIALLY_FULFILLED') ? (
                <AppButton
                  title="Issue challan"
                  variant="secondary"
                  onPress={() => navigation.navigate('CreateIssuedChallan', { orderId: ord.id })}
                />
              ) : null}
            </Card>
          )) : !loading ? (
            <EmptyBlock title="No site orders" message="No site orders created for this site." />
          ) : null}
        </View>
      )}

      {activeTab === 'challans' && (
        <View style={styles.tabContent}>
          <Text style={styles.sectionHeading}>Issued Challans ({issuedChallans.length})</Text>
          {issuedChallans.length ? issuedChallans.map((ch) => (
            <Card key={ch.id} style={styles.itemCard}>
              <View style={styles.itemHeader}>
                <Text style={styles.itemCode}>{ch.challanNumber}</Text>
                <StatusPill value="ISSUED" />
              </View>
              <Text style={styles.metaText}>Date: {dateLabel(ch.dispatchDate)} | Vehicle: {ch.vehicleNumber || 'N/A'}</Text>
              <AppButton
                title="View challan"
                variant="secondary"
                onPress={() => navigation.navigate('IssuedChallanDetail', { challan: ch })}
              />
            </Card>
          )) : <Text style={styles.emptySub}>No issued challans.</Text>}

          <Text style={[styles.sectionHeading, { marginTop: 16 }]}>Receiving Challans ({receivingChallans.length})</Text>
          {receivingChallans.length ? receivingChallans.map((rch) => (
            <Card key={rch.id} style={styles.itemCard}>
              <View style={styles.itemHeader}>
                <Text style={styles.itemCode}>{rch.receivingChallanNumber}</Text>
                <StatusPill value={rch.status || 'RECEIVED'} />
              </View>
              <Text style={styles.metaText}>Date: {dateLabel(rch.receiveDate)} | Vehicle: {rch.vehicleNumber || 'N/A'}</Text>
              <AppButton
                title="View receiving challan"
                variant="secondary"
                onPress={() => navigation.navigate('ReceivingChallanDetail', { challan: rch })}
              />
            </Card>
          )) : <Text style={styles.emptySub}>No receiving challans.</Text>}
        </View>
      )}

      {activeTab === 'operations' && (
        <View style={styles.tabContent}>
          {operations.length ? operations.map((op) => (
            <Card key={op.id} style={styles.itemCard}>
              <View style={styles.itemHeader}>
                <Text style={styles.itemCode}>{op.operationNumber}</Text>
                <StatusPill value={op.status} />
              </View>
              <Text style={styles.itemName}>{op.operationType.replaceAll('_', ' ')}</Text>
              <Text style={styles.metaText}>Date: {dateLabel(op.operationDate)} | Amount: ₹{quantity(op.amount)}</Text>
              <View style={styles.cardActions}>
                <AppButton
                  title="Edit operation"
                  variant="secondary"
                  onPress={() => navigation.navigate('CreateSiteOperation', { operationId: op.id })}
                />
                <ExcelExportButton
                  reportType="SITE_OPERATIONS_REGISTER"
                  filters={{ siteId: String(siteId), docNo: op.operationNumber }}
                />
              </View>
            </Card>
          )) : !loading ? (
            <EmptyBlock title="No site operations" message="No operational expenses recorded for this site." />
          ) : null}
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: 18, backgroundColor: colors.canvas, flexGrow: 1, gap: 14 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12 },
  profileCard: { gap: 10, padding: 16 },
  topRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', gap: 10 },
  siteTitle: { color: colors.ink, fontSize: 20, fontFamily: fonts.extraBold },
  partySubtitle: { color: colors.primary, fontSize: 14, fontFamily: fonts.bold, marginTop: 2 },
  infoRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  codeText: { color: colors.muted, fontSize: 13, fontFamily: fonts.semiBold },
  defaulterBadge: { color: '#fff', backgroundColor: colors.red, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4, fontSize: 10, fontFamily: fonts.bold },
  address: { color: colors.muted, fontSize: 12, lineHeight: 18 },
  summaryBar: { flexDirection: 'row', alignItems: 'center', borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 12, marginTop: 4 },
  summaryItem: { flex: 1, alignItems: 'center' },
  summaryVal: { color: colors.primary, fontSize: 18, fontFamily: fonts.extraBold },
  summaryLbl: { color: colors.muted, fontSize: 11, marginTop: 2 },
  summaryDivider: { width: 1, height: 26, backgroundColor: colors.line },
  tabsRow: { flexDirection: 'row', backgroundColor: colors.surface, borderRadius: 8, padding: 4, borderWidth: 1, borderColor: colors.line },
  tabBtn: { flex: 1, paddingVertical: 9, alignItems: 'center', borderRadius: 6 },
  tabBtnActive: { backgroundColor: colors.primary },
  tabTxt: { color: colors.muted, fontSize: 11, fontFamily: fonts.bold },
  tabTxtActive: { color: '#fff' },
  tabContent: { gap: 10 },
  itemCard: { gap: 8, padding: 14 },
  itemHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  itemCode: { color: colors.primary, fontFamily: fonts.bold, fontSize: 13 },
  itemName: { color: colors.ink, fontSize: 16, fontFamily: fonts.bold },
  qtyText: { color: colors.ink, fontSize: 14, fontFamily: fonts.extraBold },
  metaText: { color: colors.muted, fontSize: 12 },
  sectionHeading: { color: colors.ink, fontSize: 15, fontFamily: fonts.bold },
  emptySub: { color: colors.muted, fontSize: 13, fontStyle: 'italic', paddingVertical: 6 },
  cardActions: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 4 },
});
