import { useCallback, useEffect, useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyBlock } from '../components/ui';
import { colors, fonts } from '../theme';
import type { DashboardOverview } from '../types/api';
import type { MainTabParams, RootStackParams } from '../navigation/types';
import { localDate, quantity } from '../utils/format';

type Props = CompositeScreenProps<
  BottomTabScreenProps<MainTabParams, 'Dashboard'>,
  NativeStackScreenProps<RootStackParams>
>;

export function DashboardScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const [data, setData] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    const from = new Date();
    from.setDate(from.getDate() - 29);
    try {
      const response = await apiClient.get<DashboardOverview>('/dashboard/overview', {
        params: { dateFrom: localDate(from), dateTo: localDate() },
      });
      setData(response.data);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load dashboard.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => void load(), [load]);

  return (
    <ScrollView
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
    >
      <View style={styles.welcome}>
        <View>
          <Text style={styles.hello}>Hello!</Text>
          <Text style={styles.userName}>{user?.fullName ?? 'SteelFab user'}</Text>
        </View>
        <View style={styles.avatar}><Text style={styles.avatarText}>{user?.fullName?.charAt(0).toUpperCase() ?? 'S'}</Text></View>
      </View>
      <View style={styles.overviewHeader}>
        <Text style={styles.overviewTitle}>Operations overview</Text>
        <View style={styles.datePill}>
          <Ionicons name="calendar-outline" size={15} color={colors.primary} />
          <Text style={styles.dateText}>{new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short' })}</Text>
        </View>
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {data ? (
        <>
          <View style={styles.pulseCard}>
            <View style={styles.pulseTop}>
              <View>
                <Text style={styles.pulseEyebrow}>CURRENT ACCOUNTABLE STOCK</Text>
                <Text style={styles.pulseValue}>{quantity(data.stockSummary.currentAccountableStock)}</Text>
              </View>
              <View style={styles.pulseIcon}><Ionicons name="cube-outline" size={24} color="#fff" /></View>
            </View>
            <View style={styles.pulseFooter}>
              <Text style={styles.pulseMeta}>{quantity(data.movementSummary.issuedInPeriod)} issued</Text>
              <View style={styles.pulseDot} />
              <Text style={styles.pulseMeta}>{quantity(data.movementSummary.receivedInPeriod)} received</Text>
              <Text style={styles.pulsePeriod}>30 days</Text>
            </View>
          </View>

          <Text style={styles.sectionLabel}>Inventory position</Text>
          <View style={styles.metricGrid}>
            <View style={styles.metricRow}>
              <Metric
                icon="business-outline"
                label="Godown available"
                onPress={() => navigation.navigate('Stock', { focus: 'godown' })}
                side="left"
                tone="green"
                value={quantity(data.stockSummary.godownAvailable)}
              />
              <Metric
                icon="location-outline"
                label="At sites"
                onPress={() => navigation.navigate('Stock', { focus: 'sites' })}
                side="right"
                tone="blue"
                value={quantity(data.stockSummary.materialAtSites)}
              />
            </View>
            <View style={styles.metricRow}>
              <Metric icon="document-text-outline" label="Open orders" side="left" tone="purple" value={quantity(data.orderSummary.openSiteOrders)} />
              <Metric icon="briefcase-outline" label="Active agreements" side="right" tone="saffron" value={quantity(data.agreementSummary.activeAgreements)} />
            </View>
          </View>

          <Card style={styles.controlCard}>
            <Text style={styles.cardTitle}>Work requiring attention</Text>
            <View style={styles.controlRow}>
              <ControlValue label="Partial orders" value={data.orderSummary.partiallyFulfilledSiteOrders} />
              <ControlValue label="Expiring soon" value={data.agreementSummary.expiringSoon} />
              <ControlValue label="Low stock" value={data.exceptionSummary.lowStockMaterials} />
            </View>
          </Card>

          <Text style={styles.sectionTitle}>Attention centre</Text>
          {data.attentionItems.length ? (
            data.attentionItems.slice(0, 5).map((item) => (
              <Card key={item.key} style={styles.attention}>
                <View style={[styles.alertIcon, item.severity === 'HIGH' && styles.alertHigh]}>
                  <Ionicons name="alert-outline" size={20} color={item.severity === 'HIGH' ? colors.red : colors.amber} />
                </View>
                <View style={styles.attentionCopy}>
                  <Text style={styles.attentionTitle}>{item.title}</Text>
                  <Text style={styles.attentionMessage}>{item.description}</Text>
                </View>
                <Text style={styles.attentionCount}>{item.count}</Text>
              </Card>
            ))
          ) : (
            <Card><EmptyBlock title="All clear" message="There are no operational alerts right now." /></Card>
          )}
        </>
      ) : !loading ? (
        <Card><EmptyBlock title="No dashboard data" message="Pull down to try again." /></Card>
      ) : null}
    </ScrollView>
  );
}

function Metric({ icon, label, value, side, tone, onPress }: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  value: string;
  side: 'left' | 'right';
  tone: 'blue' | 'green' | 'purple' | 'saffron';
  onPress?: () => void;
}) {
  const content = (
    <Card style={[styles.metric, onPress && styles.metricInteractive]}>
      <View style={[styles.metricIcon, tone === 'blue' && styles.metricBlue, tone === 'green' && styles.metricGreen,
        tone === 'purple' && styles.metricPurple, tone === 'saffron' && styles.metricSaffron]}>
        <Ionicons name={icon} size={21} color={tone === 'green' ? colors.green : tone === 'blue' ? colors.blue
          : tone === 'purple' ? colors.purple : colors.saffron} />
      </View>
      <Text style={styles.metricLabel}>{label}</Text>
      <Text numberOfLines={1} adjustsFontSizeToFit style={styles.metricValue}>{value}</Text>
    </Card>
  );
  const slotStyle = [styles.metricSlot, side === 'left' ? styles.metricSlotLeft : styles.metricSlotRight];
  if (!onPress) return <View style={slotStyle}>{content}</View>;
  return (
    <Pressable
      accessibilityHint={`Opens ${label.toLowerCase()} item balances`}
      accessibilityLabel={`View ${label}`}
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [slotStyle, pressed && styles.metricPressed]}
    >
      {content}
    </Pressable>
  );
}

function ControlValue({ label, value }: { label: string; value: number }) {
  return <View style={styles.controlValue}><Text style={styles.controlNumber}>{value}</Text><Text style={styles.controlLabel}>{label}</Text></View>;
}

const styles = StyleSheet.create({
  content: { padding: 18, paddingBottom: 34, backgroundColor: colors.canvas },
  welcome: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 22 },
  hello: { color: colors.muted, fontSize: 14 },
  userName: { color: colors.ink, fontSize: 23, fontFamily: fonts.extraBold, marginTop: 2 },
  avatar: { width: 46, height: 46, borderRadius: 23, backgroundColor: colors.primaryDark, alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: '#fff', fontSize: 18, fontFamily: fonts.bold },
  overviewHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
  overviewTitle: { color: colors.ink, fontSize: 18, fontFamily: fonts.bold },
  datePill: { minHeight: 36, flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 11, borderRadius: 8, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line },
  dateText: { color: colors.ink, fontSize: 12, fontFamily: fonts.semiBold },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12, marginBottom: 14 },
  pulseCard: { backgroundColor: colors.primary, borderRadius: 8, padding: 18, marginBottom: 20 },
  pulseTop: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between' },
  pulseEyebrow: { color: '#D8E2EE', fontSize: 10, fontFamily: fonts.bold },
  pulseValue: { color: '#fff', fontSize: 34, fontFamily: fonts.black, marginTop: 6 },
  pulseIcon: { width: 44, height: 44, borderRadius: 8, backgroundColor: 'rgba(255,255,255,0.18)', alignItems: 'center', justifyContent: 'center' },
  pulseFooter: { flexDirection: 'row', alignItems: 'center', marginTop: 18 },
  pulseMeta: { color: '#fff', fontSize: 11, fontFamily: fonts.semiBold },
  pulseDot: { width: 4, height: 4, borderRadius: 2, backgroundColor: colors.saffron, marginHorizontal: 8 },
  pulsePeriod: { marginLeft: 'auto', color: '#D8E2EE', fontSize: 11, fontFamily: fonts.semiBold },
  sectionLabel: { color: colors.ink, fontSize: 16, fontFamily: fonts.bold, marginBottom: 10 },
  metricGrid: { width: '100%' },
  metricRow: { flexDirection: 'row', width: '100%', marginBottom: 10 },
  metricSlot: { flex: 1, minWidth: 0 },
  metricSlotLeft: { marginRight: 5 },
  metricSlotRight: { marginLeft: 5 },
  metric: { width: '100%', minHeight: 142, justifyContent: 'space-between' },
  metricInteractive: { borderColor: '#C5D5E5' },
  metricPressed: { opacity: 0.72, transform: [{ scale: 0.98 }] },
  metricIcon: { width: 42, height: 42, borderRadius: 8, backgroundColor: colors.primarySoft, alignItems: 'center', justifyContent: 'center' },
  metricBlue: { backgroundColor: colors.blueSoft },
  metricGreen: { backgroundColor: colors.greenSoft },
  metricPurple: { backgroundColor: colors.purpleSoft },
  metricSaffron: { backgroundColor: colors.saffronSoft },
  metricLabel: { color: colors.muted, marginTop: 13 },
  metricValue: { color: colors.ink, fontSize: 27, fontFamily: fonts.black, marginTop: 4 },
  controlCard: { marginTop: 14 },
  cardTitle: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold, marginBottom: 15 },
  controlRow: { flexDirection: 'row' },
  controlValue: { flex: 1, paddingHorizontal: 5 },
  controlNumber: { color: colors.primary, fontSize: 25, fontFamily: fonts.black, textAlign: 'center' },
  controlLabel: { color: colors.muted, fontSize: 12, lineHeight: 16, textAlign: 'center', marginTop: 4 },
  sectionTitle: { color: colors.ink, fontSize: 19, fontFamily: fonts.bold, marginTop: 24, marginBottom: 12 },
  attention: { flexDirection: 'row', alignItems: 'center', marginBottom: 10 },
  alertIcon: { width: 40, height: 40, borderRadius: 8, backgroundColor: colors.amberSoft, alignItems: 'center', justifyContent: 'center' },
  alertHigh: { backgroundColor: colors.redSoft },
  attentionCopy: { flex: 1, paddingHorizontal: 12 },
  attentionTitle: { color: colors.ink, fontWeight: '800' },
  attentionMessage: { color: colors.muted, fontSize: 12, lineHeight: 17, marginTop: 3 },
  attentionCount: { color: colors.ink, fontSize: 20, fontWeight: '900' },
});
