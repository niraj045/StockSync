import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, Card, EmptyBlock, Field } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';
import type { Agreement, AgreementItem, Page, SiteOrder } from '../types/api';
import { localDate, quantity } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateOrder'>;

export function CreateOrderScreen({ navigation, route }: Props) {
  const [agreements, setAgreements] = useState<Agreement[]>([]);
  const [existingOrders, setExistingOrders] = useState<SiteOrder[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(route.params?.agreementId ?? null);
  const [quantities, setQuantities] = useState<Record<number, string>>({});
  const [orderDate, setOrderDate] = useState(localDate());
  const [notes, setNotes] = useState('');
  const [pickerOpen, setPickerOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [agreementResponse, orderResponse] = await Promise.all([
          apiClient.get<Page<Agreement>>('/agreements', {
            params: { status: 'ACTIVE', size: 200, sort: 'id,desc' },
          }),
          apiClient.get<Page<SiteOrder>>('/orders', { params: { size: 500 } }),
        ]);
        setAgreements(agreementResponse.data.content);
        setExistingOrders(orderResponse.data.content);
        const preselected = agreementResponse.data.content.find((row) => row.id === route.params?.agreementId);
        if (preselected) {
          setSelectedId(preselected.id);
          setQuantities(Object.fromEntries(preselected.items.map((item) => [item.itemId, '0'])));
        }
      } catch (cause) {
        setError(apiErrorMessage(cause, 'Unable to load active agreements.'));
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [route.params?.agreementId]);

  const selected = agreements.find((agreement) => agreement.id === selectedId);

  const confirmedByItem = useMemo(() => {
    const totals = new Map<number, number>();
    if (!selectedId) return totals;
    existingOrders
      .filter((order) =>
        order.agreementId === selectedId &&
        ['CONFIRMED', 'PARTIALLY_FULFILLED', 'FULFILLED'].includes(order.status))
      .forEach((order) => order.items.forEach((item) => {
        totals.set(item.itemId, (totals.get(item.itemId) ?? 0) + Number(item.orderedQuantity));
      }));
    return totals;
  }, [existingOrders, selectedId]);

  const remainingFor = (item: AgreementItem) =>
    Math.max(0, Number(item.contractedQuantity) - (confirmedByItem.get(item.itemId) ?? 0));

  const chooseAgreement = (agreement: Agreement) => {
    setSelectedId(agreement.id);
    setQuantities(Object.fromEntries(agreement.items.map((item) => [item.itemId, '0'])));
    setPickerOpen(false);
  };

  const submit = async () => {
    if (!selected) {
      Alert.alert('Select agreement', 'Choose the active agreement for this site order.');
      return;
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(orderDate)) {
      Alert.alert('Check order date', 'Use YYYY-MM-DD format.');
      return;
    }
    const items = selected.items
      .filter((item) => Number(quantities[item.itemId]) > 0)
      .map((item) => ({ itemId: item.itemId, orderedQuantity: Number(quantities[item.itemId]) }));
    if (!items.length) {
      Alert.alert('Add material', 'Enter a quantity for at least one agreement item.');
      return;
    }
    const exceeded = selected.items.find((item) => Number(quantities[item.itemId]) > remainingFor(item));
    if (exceeded) {
      Alert.alert('Quantity exceeds agreement', `${exceeded.itemName} has only ${quantity(remainingFor(exceeded))} remaining.`);
      return;
    }

    setSubmitting(true);
    try {
      const draft = await apiClient.post<SiteOrder>('/orders', {
        agreementId: selected.id,
        orderDate,
        notes: notes.trim() || null,
        items,
      });
      try {
        const confirmed = await apiClient.post<SiteOrder>(`/orders/${draft.data.id}/confirm`);
        navigation.replace('CreateIssuedChallan', { orderId: confirmed.data.id });
      } catch (cause) {
        Alert.alert(
          'Order saved as draft',
          `${draft.data.orderNumber} was created, but could not be confirmed. Open Orders and confirm it after checking the quantities.\n\n${apiErrorMessage(cause, '')}`,
          [{ text: 'View orders', onPress: () => navigation.navigate('Main') }],
        );
      }
    } catch (cause) {
      Alert.alert('Order not created', apiErrorMessage(cause, 'Unable to create the site order.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {error ? <Text style={styles.error}>{error}</Text> : null}
        <Text style={styles.label}>Active agreement</Text>
        <Pressable style={styles.selector} disabled={loading} onPress={() => setPickerOpen(true)}>
          <View style={styles.selectorCopy}>
            <Text style={selected ? styles.selectorValue : styles.selectorPlaceholder}>
              {selected ? selected.agreementNumber : loading ? 'Loading agreements...' : 'Select agreement'}
            </Text>
            {selected ? <Text style={styles.selectorMeta}>{selected.partyName} | {selected.siteName}</Text> : null}
          </View>
          <Ionicons name="chevron-down" size={20} color={colors.muted} />
        </Pressable>
        <Field label="Order date" value={orderDate} onChangeText={setOrderDate} placeholder="YYYY-MM-DD" />
        <Field label="Dispatch instructions" value={notes} onChangeText={setNotes} multiline />

        <Text style={styles.heading}>Required material</Text>
        {!selected ? (
          <Card><EmptyBlock title="Select an agreement" message="Its contracted items will appear here." /></Card>
        ) : selected.items.map((item) => {
          const remaining = remainingFor(item);
          return (
            <Card key={item.itemId} style={styles.item}>
              <Text style={styles.code}>{item.itemCode}</Text>
              <Text style={styles.itemName}>{item.itemName}</Text>
              <Text style={[styles.remaining, remaining <= 0 && styles.exhausted]}>
                Agreement remaining: {quantity(remaining)} {item.unit}
              </Text>
              <View style={styles.quantityRow}>
                <Text style={styles.quantityLabel}>Order quantity</Text>
                <TextInput
                  editable={remaining > 0}
                  keyboardType="decimal-pad"
                  onChangeText={(value) => setQuantities((current) => ({ ...current, [item.itemId]: value }))}
                  selectTextOnFocus
                  style={[styles.quantityInput, remaining <= 0 && styles.quantityDisabled]}
                  value={quantities[item.itemId] ?? '0'}
                />
                <Text style={styles.unit}>{item.unit}</Text>
              </View>
            </Card>
          );
        })}
        <AppButton
          title="Create order and continue"
          onPress={submit}
          loading={submitting}
          disabled={loading || !!error || !agreements.length}
        />
        <Text style={styles.helper}>The app confirms the order and opens challan dispatch quantities next.</Text>
      </ScrollView>

      <Modal animationType="slide" transparent visible={pickerOpen} onRequestClose={() => setPickerOpen(false)}>
        <View style={styles.backdrop}>
          <View style={styles.sheet}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>Select active agreement</Text>
              <Pressable accessibilityLabel="Close" onPress={() => setPickerOpen(false)} style={styles.close}>
                <Ionicons name="close" size={24} color={colors.ink} />
              </Pressable>
            </View>
            <ScrollView contentContainerStyle={styles.agreementList}>
              {agreements.length ? agreements.map((agreement) => (
                <Pressable key={agreement.id} onPress={() => chooseAgreement(agreement)} style={styles.agreement}>
                  <View style={styles.agreementCopy}>
                    <Text style={styles.agreementNumber}>{agreement.agreementNumber}</Text>
                    <Text style={styles.agreementParty}>{agreement.partyName}</Text>
                    <Text style={styles.agreementSite}>{agreement.siteName} ({agreement.siteCode})</Text>
                  </View>
                  <Ionicons name="chevron-forward" size={20} color={colors.muted} />
                </Pressable>
              )) : <EmptyBlock title="No active agreements" message="Activate an agreement before placing a site order." />}
            </ScrollView>
          </View>
        </View>
      </Modal>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.canvas },
  content: { padding: 18, paddingBottom: 34, gap: 15 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12 },
  label: { color: colors.ink, fontSize: 14, fontWeight: '700', marginBottom: -8 },
  selector: { minHeight: 58, borderRadius: 8, borderWidth: 1, borderColor: colors.line, backgroundColor: '#fff', paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center' },
  selectorCopy: { flex: 1 },
  selectorValue: { color: colors.ink, fontSize: 16, fontWeight: '800' },
  selectorPlaceholder: { color: '#98A2B3', fontSize: 16 },
  selectorMeta: { color: colors.muted, fontSize: 11, marginTop: 3 },
  heading: { color: colors.ink, fontSize: 20, fontWeight: '900', marginTop: 4 },
  item: {},
  code: { color: colors.primary, fontSize: 11, fontWeight: '900' },
  itemName: { color: colors.ink, fontSize: 16, fontWeight: '800', marginTop: 3 },
  remaining: { color: colors.muted, fontSize: 12, marginTop: 7 },
  exhausted: { color: colors.red },
  quantityRow: { flexDirection: 'row', alignItems: 'center', marginTop: 13, paddingTop: 12, borderTopWidth: 1, borderTopColor: colors.line },
  quantityLabel: { color: colors.ink, fontWeight: '700', flex: 1 },
  quantityInput: { width: 90, height: 44, borderWidth: 1, borderColor: colors.primary, borderRadius: 7, backgroundColor: '#fff', color: colors.ink, fontSize: 17, fontWeight: '800', textAlign: 'right', paddingHorizontal: 10 },
  quantityDisabled: { borderColor: colors.line, backgroundColor: '#F1F4F3', color: colors.muted },
  unit: { color: colors.muted, fontSize: 11, width: 48, marginLeft: 7 },
  helper: { color: colors.muted, fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: -5 },
  backdrop: { flex: 1, backgroundColor: 'rgba(4, 25, 23, 0.45)', justifyContent: 'flex-end' },
  sheet: { maxHeight: '78%', backgroundColor: colors.surface, borderTopLeftRadius: 12, borderTopRightRadius: 12, paddingBottom: 20 },
  sheetHeader: { minHeight: 64, paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sheetTitle: { color: colors.ink, fontSize: 19, fontWeight: '900' },
  close: { padding: 8 },
  agreementList: { padding: 14 },
  agreement: { minHeight: 82, padding: 14, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center' },
  agreementCopy: { flex: 1 },
  agreementNumber: { color: colors.primary, fontWeight: '900' },
  agreementParty: { color: colors.ink, fontSize: 15, fontWeight: '800', marginTop: 4 },
  agreementSite: { color: colors.muted, fontSize: 12, marginTop: 2 },
});
