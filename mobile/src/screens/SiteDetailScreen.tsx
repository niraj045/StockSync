import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
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

  const aggregatedMaterials = useMemo(() => {
    const map = new Map<string, {
      key: string;
      itemId?: number;
      itemCode: string;
      itemName: string;
      unit: string;
      totalDispatched: number;
      totalReturned: number;
      totalLostOrDamaged: number;
      currentSiteQuantity: number;
    }>();

    // 1. Process Stock Balances from API if any
    for (const stk of stock) {
      const code = stk.itemCode || `ITEM-${stk.itemId}`;
      const key = code.toLowerCase();
      map.set(key, {
        key,
        itemId: stk.itemId,
        itemCode: code,
        itemName: stk.itemName || code,
        unit: stk.unit || 'pcs',
        totalDispatched: 0,
        totalReturned: 0,
        totalLostOrDamaged: 0,
        currentSiteQuantity: Number(stk.availableQuantity || stk.hiredQuantity || (stk as any).pendingQuantity || 0),
      });
    }

    // 2. Process Issued Challans (dispatched materials)
    for (const ch of issuedChallans) {
      if (!ch.items) continue;
      for (const itm of ch.items) {
        const code = itm.itemCode || `ITEM-${itm.itemId || itm.id}`;
        const key = code.toLowerCase();
        const qty = Number(itm.quantity || 0);
        const existing = map.get(key) || {
          key,
          itemId: itm.itemId,
          itemCode: code,
          itemName: itm.itemName || code,
          unit: itm.unit || 'pcs',
          totalDispatched: 0,
          totalReturned: 0,
          totalLostOrDamaged: 0,
          currentSiteQuantity: 0,
        };
        existing.totalDispatched += qty;
        map.set(key, existing);
      }
    }

    // 3. Process Receiving Challans (returned / loss / damage materials)
    for (const rch of receivingChallans) {
      if (rch.status === 'CANCELLED' || !rch.items) continue;
      for (const itm of rch.items) {
        const code = itm.itemCode || `ITEM-${itm.id}`;
        const key = code.toLowerCase();
        const retQty = Number(itm.goodReturnedQuantity || 0);
        const lossDmg = Number(itm.damagedReturnedQuantity || 0) + Number(itm.lostQuantity || 0);
        const existing = map.get(key) || {
          key,
          itemCode: code,
          itemName: itm.itemName || code,
          unit: itm.unit || 'pcs',
          totalDispatched: 0,
          totalReturned: 0,
          totalLostOrDamaged: 0,
          currentSiteQuantity: 0,
        };
        existing.totalReturned += retQty;
        existing.totalLostOrDamaged += lossDmg;
        map.set(key, existing);
      }
    }

    // 4. Process Orders items if not in challans yet
    for (const ord of orders) {
      if (!ord.items) continue;
      for (const itm of ord.items) {
        const code = itm.itemCode || `ITEM-${itm.itemId || itm.id}`;
        const key = code.toLowerCase();
        if (!map.has(key)) {
          map.set(key, {
            key,
            itemId: itm.itemId,
            itemCode: code,
            itemName: itm.itemName || code,
            unit: itm.unit || 'pcs',
            totalDispatched: Number(itm.issuedQuantity || 0),
            totalReturned: 0,
            totalLostOrDamaged: 0,
            currentSiteQuantity: Number(itm.issuedQuantity || 0),
          });
        }
      }
    }

    // 5. Final calculation of net current quantity
    const list = Array.from(map.values()).map((mat) => {
      const netCalculated = mat.totalDispatched - mat.totalReturned - mat.totalLostOrDamaged;
      const netSiteQty = mat.currentSiteQuantity > 0 ? mat.currentSiteQuantity : Math.max(0, netCalculated);
      return {
        ...mat,
        currentSiteQuantity: netSiteQty,
      };
    });

    return list.sort((a, b) => a.itemName.localeCompare(b.itemName));
  }, [stock, issuedChallans, receivingChallans, orders]);

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
            <Text style={styles.summaryVal}>{aggregatedMaterials.length}</Text>
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
              {tab === 'stock' ? `Materials (${aggregatedMaterials.length})` :
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
          <View style={styles.sectionHeaderRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.sectionHeading}>Site Material Inventory</Text>
              <Text style={styles.sectionSub}>Combined materials across all site challans & orders</Text>
            </View>
            <ExcelExportButton reportType="SITE_PENDING_STOCK" filters={{ siteId: String(siteId) }} />
          </View>

          {aggregatedMaterials.length ? aggregatedMaterials.map((mat) => (
            <Card key={mat.key} style={styles.itemCard}>
              <View style={styles.itemHeader}>
                <Text style={styles.itemCode}>{mat.itemCode}</Text>
                <Text style={styles.qtyText}>
                  {quantity(mat.currentSiteQuantity > 0 ? mat.currentSiteQuantity : mat.totalDispatched)} {mat.unit}
                </Text>
              </View>
              <Text style={styles.itemName}>{mat.itemName}</Text>

              <View style={styles.materialMetricsRow}>
                <View style={styles.metricBadge}>
                  <Text style={styles.metricLbl}>Dispatched</Text>
                  <Text style={styles.metricVal}>{quantity(mat.totalDispatched)}</Text>
                </View>
                <View style={styles.metricBadge}>
                  <Text style={styles.metricLbl}>Returned</Text>
                  <Text style={styles.metricVal}>{quantity(mat.totalReturned)}</Text>
                </View>
                {mat.totalLostOrDamaged > 0 ? (
                  <View style={[styles.metricBadge, { backgroundColor: colors.redSoft }]}>
                    <Text style={[styles.metricLbl, { color: colors.red }]}>Loss/Damage</Text>
                    <Text style={[styles.metricVal, { color: colors.red }]}>{quantity(mat.totalLostOrDamaged)}</Text>
                  </View>
                ) : null}
              </View>
            </Card>
          )) : !loading ? (
            <EmptyBlock title="No material stock" message="No materials recorded at this site." />
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
  materialMetricsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 6,
    borderTopWidth: 1,
    borderTopColor: colors.line,
    paddingTop: 8,
  },
  metricBadge: {
    backgroundColor: colors.surface,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    alignItems: 'center',
  },
  metricLbl: {
    fontSize: 10,
    color: colors.muted,
    fontFamily: fonts.semiBold,
  },
  metricVal: {
    fontSize: 12,
    color: colors.ink,
    fontFamily: fonts.bold,
  },
  sectionHeading: { color: colors.ink, fontSize: 15, fontFamily: fonts.bold },
  sectionHeaderRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 },
  sectionSub: { color: colors.muted, fontSize: 12, marginTop: 2 },
  emptySub: { color: colors.muted, fontSize: 13, fontStyle: 'italic', paddingVertical: 6 },
  cardActions: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 4 },
});
