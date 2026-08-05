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
import { AppButton, Card, DateField, EmptyBlock, Field } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import type { IssuedChallan, Page, SiteOrder, StockBalance } from '../types/api';
import { localDate, quantity } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateIssuedChallan'>;

export function CreateIssuedChallanScreen({ navigation, route }: Props) {
  const [orders, setOrders] = useState<SiteOrder[]>([]);
  const [stock, setStock] = useState<StockBalance[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>([]);
  const [quantities, setQuantities] = useState<Record<number, string>>({});
  const [itemNotes, setItemNotes] = useState<Record<number, string>>({});
  const [dispatchDate, setDispatchDate] = useState(localDate());
  const [refNo, setRefNo] = useState('');
  const [vehicleNumber, setVehicleNumber] = useState('');
  const [driverName, setDriverName] = useState('');
  const [driverPhone, setDriverPhone] = useState('');
  const [notes, setNotes] = useState('');
  const [pickerOpen, setPickerOpen] = useState(false);
  const [addItemModalOpen, setAddItemModalOpen] = useState(false);
  const [itemSearchQuery, setItemSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [orderResponse, stockResponse] = await Promise.all([
          apiClient.get<Page<SiteOrder>>('/orders', { params: { size: 200, sort: 'id,desc' } }),
          apiClient.get<Page<StockBalance>>('/stock/balances', { params: { size: 500 } }),
        ]);
        const eligibleOrders = orderResponse.data.content.filter((order) =>
          order.status === 'CONFIRMED' || order.status === 'PARTIALLY_FULFILLED');
        const stockRows = stockResponse.data.content;
        setOrders(eligibleOrders);
        setStock(stockRows);
        const requestedOrder = eligibleOrders.find((order) => order.id === route.params?.orderId);
        if (requestedOrder) {
          chooseOrder(requestedOrder, stockRows);
        }
      } catch (cause) {
        setError(apiErrorMessage(cause, 'Unable to load confirmed orders.'));
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [route.params?.orderId]);

  const selected = orders.find((order) => order.id === selectedId);
  const allCandidateItems = useMemo(() => {
    if (!selected) return [];
    if (selected.items && selected.items.length > 0) {
      const remaining = selected.items.filter((item) => Number(item.remainingQuantity) > 0);
      if (remaining.length > 0) return remaining;
      return selected.items;
    }
    return stock.map((stk) => ({
      id: 0,
      itemId: stk.itemId,
      itemCode: stk.itemCode,
      itemName: stk.itemName,
      unit: stk.unit,
      orderedQuantity: '999999',
      issuedQuantity: '0',
      remainingQuantity: String(stk.availableQuantity),
      unitPrice: '0',
      version: 0,
    }));
  }, [selected, stock]);

  const activeDispatchItems = useMemo(() => {
    return allCandidateItems.filter((item) => selectedItemIds.includes(item.itemId));
  }, [allCandidateItems, selectedItemIds]);

  const searchableCandidateItems = useMemo(() => {
    if (!itemSearchQuery.trim()) return allCandidateItems;
    const query = itemSearchQuery.trim().toLowerCase();
    return allCandidateItems.filter((item) =>
      item.itemName.toLowerCase().includes(query) || item.itemCode.toLowerCase().includes(query)
    );
  }, [allCandidateItems, itemSearchQuery]);

  const chooseOrder = (order: SiteOrder, stockRows = stock) => {
    setSelectedId(order.id);
    const candidates = (order.items && order.items.length > 0)
      ? order.items.filter((item) => Number(item.remainingQuantity) > 0)
      : stockRows.map((stk) => ({ itemId: stk.itemId, remainingQuantity: String(stk.availableQuantity) }));

    const initialIds = candidates.length > 0 ? [candidates[0].itemId] : [];
    setSelectedItemIds(initialIds);

    const initialQty: Record<number, string> = {};
    candidates.forEach((item) => {
      const available = Number(stockRows.find((balance) => balance.itemId === item.itemId)?.availableQuantity ?? 0);
      initialQty[item.itemId] = String(Math.min(Number(item.remainingQuantity || available), available));
    });
    setQuantities(initialQty);
    setPickerOpen(false);
  };

  const addItemToDispatch = (itemId: number) => {
    if (!selectedItemIds.includes(itemId)) {
      setSelectedItemIds((prev) => [...prev, itemId]);
      if (!quantities[itemId]) {
        const item = allCandidateItems.find((i) => i.itemId === itemId);
        const available = Number(stock.find((b) => b.itemId === itemId)?.availableQuantity ?? 0);
        const defaultQty = item ? Math.min(Number(item.remainingQuantity || available), available) : 1;
        setQuantities((prev) => ({ ...prev, [itemId]: String(defaultQty) }));
      }
    }
    setAddItemModalOpen(false);
    setItemSearchQuery('');
  };

  const removeItemFromDispatch = (itemId: number) => {
    setSelectedItemIds((prev) => prev.filter((id) => id !== itemId));
  };

  const validate = () => {
    if (!selected) return 'Select a confirmed site order.';
    if (!/^\d{4}-\d{2}-\d{2}$/.test(dispatchDate)) return 'Dispatch date must use YYYY-MM-DD.';
    if (!activeDispatchItems.length) return 'Add at least one item to dispatch.';
    const positive = activeDispatchItems.filter((item) => Number(quantities[item.itemId]) > 0);
    if (!positive.length) return 'Enter a dispatch quantity greater than 0 for at least one item.';
    for (const item of positive) {
      const value = Number(quantities[item.itemId]);
      const available = Number(stock.find((balance) => balance.itemId === item.itemId)?.availableQuantity ?? 0);
      if (value > Number(item.remainingQuantity)) return `${item.itemName} exceeds the order balance.`;
      if (value > available) return `${item.itemName} exceeds godown stock (${quantity(available)}).`;
    }
    return null;
  };

  const submit = async () => {
    const validation = validate();
    if (validation) {
      Alert.alert('Check dispatch quantities', validation);
      return;
    }
    setSubmitting(true);
    try {
      const response = await apiClient.post<IssuedChallan>('/challans/issued', {
        refNo: refNo.trim() || null,
        siteOrderId: selectedId,
        dispatchDate,
        vehicleNumber: vehicleNumber.trim() || null,
        driverName: driverName.trim() || null,
        driverPhone: driverPhone.trim() || null,
        notes: notes.trim() || null,
        items: activeDispatchItems
          .filter((item) => Number(quantities[item.itemId]) > 0)
          .map((item) => ({
            itemId: item.itemId,
            quantity: Number(quantities[item.itemId]),
            notes: itemNotes[item.itemId]?.trim() || null,
          })),
      });
      navigation.replace('IssuedChallanDetail', { challan: response.data });
    } catch (cause) {
      Alert.alert('Challan not created', apiErrorMessage(cause, 'Unable to generate the issued challan.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'} automaticallyAdjustKeyboardInsets>
        {error ? <Text style={styles.error}>{error}</Text> : null}
        <Text style={styles.label}>Confirmed site order</Text>
        <Pressable style={styles.selector} onPress={() => setPickerOpen(true)} disabled={loading}>
          <View style={styles.selectorCopy}>
            <Text style={selected ? styles.selectorValue : styles.selectorPlaceholder}>
              {selected ? selected.orderNumber : loading ? 'Loading orders...' : 'Select order'}
            </Text>
            {selected ? <Text style={styles.selectorMeta}>{selected.partyName} | {selected.siteName}</Text> : null}
          </View>
          <Ionicons name="chevron-down" size={20} color={colors.muted} />
        </Pressable>

        <View style={styles.twoFields}>
          <View style={styles.half}><DateField label="Dispatch date" value={dispatchDate} onChange={setDispatchDate} /></View>
          <View style={styles.half}><Field label="Ref no" value={refNo} onChangeText={setRefNo} /></View>
        </View>
        <View style={styles.twoFields}>
          <View style={styles.half}><Field label="Vehicle number" value={vehicleNumber} onChangeText={setVehicleNumber} autoCapitalize="characters" /></View>
          <View style={styles.half}><Field label="Driver name" value={driverName} onChangeText={setDriverName} /></View>
        </View>
        <Field label="Driver phone" value={driverPhone} onChangeText={setDriverPhone} keyboardType="phone-pad" />
        <Field label="Challan notes" value={notes} onChangeText={setNotes} multiline />

        <View style={styles.headingRow}>
          <Text style={styles.heading}>Dispatch items ({activeDispatchItems.length})</Text>
          {selected ? (
            <Pressable style={styles.addButton} onPress={() => { setItemSearchQuery(''); setAddItemModalOpen(true); }}>
              <Ionicons name="add-circle-outline" size={18} color="#fff" />
              <Text style={styles.addButtonText}>Add item</Text>
            </Pressable>
          ) : null}
        </View>

        {!selected ? (
          <Card><EmptyBlock title="Select an order" message="Its items will be available for selection here." /></Card>
        ) : activeDispatchItems.length ? (
          activeDispatchItems.map((item) => {
            const available = Number(stock.find((balance) => balance.itemId === item.itemId)?.availableQuantity ?? 0);
            return (
              <Card key={item.itemId} style={styles.item}>
                <View style={styles.itemHeader}>
                  <View style={styles.itemTitleBlock}>
                    <Text style={styles.code}>{item.itemCode}</Text>
                    <Text style={styles.itemName}>{item.itemName}</Text>
                  </View>
                  <Pressable accessibilityLabel="Remove item" onPress={() => removeItemFromDispatch(item.itemId)} style={styles.removeBtn}>
                    <Ionicons name="trash-outline" size={20} color={colors.red} />
                  </Pressable>
                </View>

                <View style={styles.balanceRow}>
                  <Text style={styles.balance}>Order: {quantity(Number(item.remainingQuantity))}</Text>
                  <Text style={[styles.balance, available <= 0 && styles.noStock]}>Godown: {quantity(available)}</Text>
                </View>

                <View style={styles.quantityRow}>
                  <Text style={styles.quantityLabel}>Dispatch quantity</Text>
                  <TextInput
                    keyboardType="decimal-pad"
                    onChangeText={(value) => setQuantities((current) => ({ ...current, [item.itemId]: value }))}
                    selectTextOnFocus
                    style={styles.quantityInput}
                    value={quantities[item.itemId] ?? '0'}
                  />
                  <Text style={styles.unit}>{item.unit}</Text>
                </View>

                <View style={styles.itemNotesRow}>
                  <Text style={styles.itemNotesLabel}>Remarks</Text>
                  <TextInput
                    style={styles.itemNotesInput}
                    value={itemNotes[item.itemId] ?? ''}
                    onChangeText={(value) => setItemNotes((current) => ({ ...current, [item.itemId]: value }))}
                    placeholder="Optional notes"
                    placeholderTextColor={colors.muted}
                  />
                </View>
              </Card>
            );
          })
        ) : (
          <Card>
            <EmptyBlock title="No items added" message="Tap 'Add item' to pick items for this dispatch challan." />
            <AppButton title="+ Add item to dispatch" variant="secondary" onPress={() => { setItemSearchQuery(''); setAddItemModalOpen(true); }} />
          </Card>
        )}

        {selected && activeDispatchItems.length > 0 ? (
          <AppButton title="+ Add another item" variant="secondary" onPress={() => { setItemSearchQuery(''); setAddItemModalOpen(true); }} />
        ) : null}

        <AppButton title="Generate issued challan" onPress={submit} loading={submitting} disabled={loading || !!error || !selected} />
      </ScrollView>

      {/* Order Picker Modal */}
      <Modal animationType="slide" transparent visible={pickerOpen} onRequestClose={() => setPickerOpen(false)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.sheet}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>Select confirmed order</Text>
              <Pressable accessibilityLabel="Close" onPress={() => setPickerOpen(false)} style={styles.close}>
                <Ionicons name="close" size={24} color={colors.ink} />
              </Pressable>
            </View>
            <ScrollView contentContainerStyle={styles.orderList}>
              {orders.length ? orders.map((order) => (
                <Pressable key={order.id} onPress={() => chooseOrder(order)} style={styles.order}>
                  <View style={styles.orderCopy}>
                    <Text style={styles.orderNumber}>{order.orderNumber}</Text>
                    <Text style={styles.orderParty}>{order.partyName}</Text>
                    <Text style={styles.orderSite}>{order.siteName}</Text>
                  </View>
                  <Ionicons name="chevron-forward" size={20} color={colors.muted} />
                </Pressable>
              )) : <EmptyBlock title="No confirmed orders" message="Confirm a site order before issuing material." />}
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* Item Picker & Search Modal */}
      <Modal animationType="slide" transparent visible={addItemModalOpen} onRequestClose={() => setAddItemModalOpen(false)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.sheet}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>Select item to dispatch</Text>
              <Pressable accessibilityLabel="Close" onPress={() => setAddItemModalOpen(false)} style={styles.close}>
                <Ionicons name="close" size={24} color={colors.ink} />
              </Pressable>
            </View>

            <View style={styles.searchBarContainer}>
              <Ionicons name="search-outline" size={18} color={colors.muted} style={styles.searchIcon} />
              <TextInput
                style={styles.searchInput}
                placeholder="Search items by code or name..."
                placeholderTextColor={colors.muted}
                value={itemSearchQuery}
                onChangeText={setItemSearchQuery}
                autoFocus
              />
              {itemSearchQuery ? (
                <Pressable onPress={() => setItemSearchQuery('')}>
                  <Ionicons name="close-circle" size={18} color={colors.muted} />
                </Pressable>
              ) : null}
            </View>

            <ScrollView contentContainerStyle={styles.orderList} keyboardShouldPersistTaps="handled">
              {searchableCandidateItems.length ? searchableCandidateItems.map((item) => {
                const isSelected = selectedItemIds.includes(item.itemId);
                const available = Number(stock.find((b) => b.itemId === item.itemId)?.availableQuantity ?? 0);
                return (
                  <Pressable
                    key={item.itemId}
                    onPress={() => addItemToDispatch(item.itemId)}
                    style={[styles.itemPickRow, isSelected && styles.itemPickRowSelected]}
                  >
                    <View style={styles.orderCopy}>
                      <Text style={styles.code}>{item.itemCode}</Text>
                      <Text style={styles.itemName}>{item.itemName}</Text>
                      <Text style={styles.orderSite}>
                        Rem: {quantity(Number(item.remainingQuantity))} | Stock: {quantity(available)} {item.unit}
                      </Text>
                    </View>
                    {isSelected ? (
                      <Ionicons name="checkmark-circle" size={22} color={colors.primary} />
                    ) : (
                      <Ionicons name="add-circle-outline" size={22} color={colors.muted} />
                    )}
                  </Pressable>
                );
              }) : (
                <EmptyBlock title="No matching items" message="No stock or order item matched your search query." />
              )}
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
  label: { color: colors.ink, fontSize: 14, fontFamily: fonts.bold, marginBottom: -8 },
  selector: { minHeight: 58, borderRadius: 8, borderWidth: 1, borderColor: colors.line, backgroundColor: '#fff', paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center' },
  selectorCopy: { flex: 1 },
  selectorValue: { color: colors.ink, fontSize: 16, fontFamily: fonts.bold },
  selectorPlaceholder: { color: '#98A2B3', fontSize: 16 },
  selectorMeta: { color: colors.muted, fontSize: 11, marginTop: 3 },
  twoFields: { flexDirection: 'row', gap: 10 },
  half: { flex: 1 },
  headingRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 4 },
  heading: { color: colors.ink, fontSize: 18, fontFamily: fonts.bold },
  addButton: { flexDirection: 'row', alignItems: 'center', gap: 4, backgroundColor: colors.primary, paddingHorizontal: 12, paddingVertical: 6, borderRadius: 6 },
  addButtonText: { color: '#fff', fontSize: 12, fontFamily: fonts.bold },
  item: {},
  itemHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  itemTitleBlock: { flex: 1 },
  removeBtn: { padding: 4 },
  code: { color: colors.primary, fontSize: 11, fontFamily: fonts.bold },
  itemName: { color: colors.ink, fontSize: 16, fontFamily: fonts.bold, marginTop: 3 },
  balanceRow: { flexDirection: 'row', gap: 16, marginTop: 9 },
  balance: { color: colors.muted, fontSize: 12, fontFamily: fonts.semiBold },
  noStock: { color: colors.red },
  quantityRow: { flexDirection: 'row', alignItems: 'center', marginTop: 13, paddingTop: 12, borderTopWidth: 1, borderTopColor: colors.line },
  quantityLabel: { color: colors.ink, fontFamily: fonts.bold, flex: 1 },
  quantityInput: { width: 90, height: 44, borderWidth: 1, borderColor: colors.primary, borderRadius: 7, backgroundColor: '#fff', color: colors.ink, fontSize: 17, fontFamily: fonts.bold, textAlign: 'right', paddingHorizontal: 10 },
  unit: { color: colors.muted, fontSize: 11, width: 48, marginLeft: 7 },
  itemNotesRow: { flexDirection: 'row', alignItems: 'center', marginTop: 12 },
  itemNotesLabel: { color: colors.muted, fontSize: 12, fontFamily: fonts.semiBold, width: 60 },
  itemNotesInput: { flex: 1, height: 38, borderWidth: 1, borderColor: colors.line, borderRadius: 6, backgroundColor: '#fff', color: colors.ink, fontSize: 13, paddingHorizontal: 10 },
  modalBackdrop: { flex: 1, backgroundColor: 'rgba(4, 25, 23, 0.45)', justifyContent: 'flex-end' },
  sheet: { maxHeight: '82%', backgroundColor: colors.surface, borderTopLeftRadius: 12, borderTopRightRadius: 12, paddingBottom: 20 },
  sheetHeader: { minHeight: 64, paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sheetTitle: { color: colors.ink, fontSize: 18, fontFamily: fonts.bold },
  close: { padding: 8 },
  searchBarContainer: { flexDirection: 'row', alignItems: 'center', marginHorizontal: 14, marginTop: 12, paddingHorizontal: 10, height: 44, borderRadius: 8, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line },
  searchIcon: { marginRight: 8 },
  searchInput: { flex: 1, color: colors.ink, fontSize: 14, fontFamily: fonts.semiBold },
  orderList: { padding: 14 },
  order: { minHeight: 82, padding: 14, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center' },
  orderCopy: { flex: 1 },
  orderNumber: { color: colors.primary, fontFamily: fonts.bold },
  orderParty: { color: colors.ink, fontSize: 15, fontFamily: fonts.bold, marginTop: 4 },
  orderSite: { color: colors.muted, fontSize: 12, marginTop: 2 },
  itemPickRow: { minHeight: 70, padding: 12, borderRadius: 8, borderWidth: 1, borderColor: colors.line, backgroundColor: '#fff', marginBottom: 8, flexDirection: 'row', alignItems: 'center' },
  itemPickRowSelected: { backgroundColor: '#F0F9FF', borderColor: colors.primary },
});
