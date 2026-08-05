import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import { shareServerFile } from '../utils/sharePdf';

type Segment = { id: number; itemCode: string; itemName: string; quantity: number; billableDays: number; baseRate: number; rentalType: string; amount: number; sourceIssueReference: string; sourceEndReference?: string };
type Run = { id: number; billingRunNumber: string; agreementNumber: string; partyName: string; siteName: string; periodStart: string; periodEnd: string; status: 'DRAFT' | 'CALCULATED' | 'FINALIZED' | 'CANCELLED'; rentalSubtotal: number; taxableAmount: number; totalTax: number; grandTotal: number; segments: Segment[]; charges: { id: number; selected: boolean; description: string; amount: number }[] };

const money = (value: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value || 0);

type Props = NativeStackScreenProps<RootStackParams, 'BillingRunDetail'>;

export function BillingRunDetailScreen({ route, navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const canFinalize = user?.roles.some(role => ['ROLE_ADMIN', 'ROLE_ACCOUNTS'].includes(role)) ?? false;
  
  const [selected, setSelected] = useState<Run>();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<Run>(`/billing-runs/${route.params.runId}`);
      setSelected(response.data);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load billing details.'));
    } finally {
      setLoading(false);
    }
  }, [route.params.runId]);

  useEffect(() => {
    void load();
  }, [load]);

  const finalize = () => {
    if (!selected) return;
    Alert.alert(
      'Finalize rental bill?',
      `This locks ${selected.billingRunNumber} and its challan timeline.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Finalize',
          onPress: async () => {
            setSubmitting(true);
            try {
              const response = await apiClient.post<Run>(`/billing-runs/${selected.id}/finalize`);
              setSelected(response.data);
            } catch (cause) {
              setError(apiErrorMessage(cause, 'Unable to finalize this bill.'));
            } finally {
              setSubmitting(false);
            }
          },
        },
      ]
    );
  };

  const invoiceAndDownload = async () => {
    if (!selected) return;
    setSubmitting(true);
    try {
      // 1. Create or get existing invoice from billing run
      const result = await apiClient.post<{ id: number; invoiceNumber: string; status: string }>(`/invoices/from-billing-run/${selected.id}`);
      
      // 2. If it's a draft, issue it so the PDF is generated
      let invoiceId = result.data.id;
      if (result.data.status === 'DRAFT') {
        await apiClient.post(`/invoices/${invoiceId}/issue`);
      }

      // 3. Download and share the PDF
      await shareServerFile(
        `/invoices/${invoiceId}/pdf`,
        `invoice-${result.data.invoiceNumber}.pdf`,
        'application/pdf',
        'Share SteelFab Invoice'
      );
    } catch (cause) {
      Alert.alert('Download Failed', apiErrorMessage(cause, 'Unable to create or download the invoice.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (!selected) {
    return (
      <ScrollView style={styles.page} contentContainerStyle={{ paddingTop: insets.top + 18 }} refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}>
        {error ? <Text style={styles.error}>{error}</Text> : <Text style={styles.lead}>Loading...</Text>}
      </ScrollView>
    );
  }

  return (
    <ScrollView style={styles.page} contentContainerStyle={{ paddingTop: insets.top + 18, paddingBottom: insets.bottom + 36 }} refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}>
      <PageHeader eyebrow={selected.agreementNumber} title={selected.billingRunNumber} />
      <Text style={styles.lead}>{selected.partyName} | {selected.siteName}</Text>
      
      <View style={styles.statusRow}>
        <StatusPill value={selected.status} />
        <Text style={styles.meta}>{selected.periodStart} to {selected.periodEnd}</Text>
      </View>
      
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Card style={styles.detail}>
        <Text style={styles.title}>Calculation Timeline</Text>
        
        {!selected.segments.length ? (
          <EmptyBlock title="No billable material" message="No issued material was deployed in this period." />
        ) : (
          selected.segments.map(line => (
            <View key={line.id} style={styles.line}>
              <Text style={styles.choiceTitle}>{line.itemCode} - {line.itemName}</Text>
              <Text style={styles.meta}>
                {line.quantity} units | {line.billableDays} days | {line.rentalType === 'PER_PIECE_PER_MONTH' ? 'monthly rate' : 'daily rate'} {money(line.baseRate)}
              </Text>
              <Text style={styles.meta}>From {line.sourceIssueReference}{line.sourceEndReference ? ` to ${line.sourceEndReference}` : '; still at site'}</Text>
              <Text style={styles.lineAmount}>{money(line.amount)}</Text>
            </View>
          ))
        )}
        
        <View style={styles.total}>
          <Text style={styles.meta}>Taxable {money(selected.taxableAmount)} + tax {money(selected.totalTax)}</Text>
          <Text style={styles.amount}>{money(selected.grandTotal)}</Text>
        </View>

        {canFinalize && selected.status === 'CALCULATED' ? (
          <AppButton title="Finalize billing run" onPress={finalize} loading={submitting} disabled={selected.grandTotal <= 0} />
        ) : null}
        
        {canFinalize && selected.status === 'FINALIZED' ? (
          <AppButton title="Issue & Download Invoice" onPress={() => void invoiceAndDownload()} loading={submitting} />
        ) : null}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.canvas, paddingHorizontal: 18 },
  lead: { color: colors.muted, lineHeight: 20, marginBottom: 8 },
  error: { color: colors.red, marginBottom: 12, fontFamily: fonts.semiBold },
  statusRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 },
  title: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold, marginBottom: 10 },
  meta: { color: colors.muted, fontSize: 12, lineHeight: 18, marginTop: 3 },
  amount: { color: colors.ink, fontSize: 22, fontFamily: fonts.extraBold, marginTop: 12 },
  detail: { marginTop: 4 },
  line: { paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: colors.line },
  choiceTitle: { color: colors.ink, fontFamily: fonts.bold },
  lineAmount: { color: colors.primaryDark, fontFamily: fonts.bold, marginTop: 6 },
  total: { paddingVertical: 16, alignItems: 'flex-end', gap: 2 },
});
