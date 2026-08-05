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
import { colors } from '../theme';
import type { Agreement, ItemOption, Page, SiteOrder } from '../types/api';
import { localDate, quantity } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateOrder'>;

type OrderLine = {
  itemId: number;
  itemCode: string;
  itemName: string;
  unit: string;
  quantity: string;
};

export function CreateOrderScreen({ navigation, route }: Props) {
  const [agreements, setAgreements] = useState<Agreement[]>([]);
  const [allItems, setAllItems] = useState<ItemOption[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(route.params?.agreementId ?? null);
  const [orderLines, setOrderLines] = useState<OrderLine[]>([]);
  const [orderDate, setOrderDate] = useState(localDate());
  const [notes, setNotes] = useState('');
  const [pickerOpen, setPickerOpen] = useState(false);
  const [itemPickerOpen, setItemPickerOpen] = useState(false);
  const [itemSearch, setItemSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [agreementResponse, itemsResponse] = await Promise.all([
          apiClient.get<Page<Agreement>>('/agreements', {
            params: { status: 'ACTIVE', size: 200, sort: 'id,desc' },
          }),
          apiClient.get<Page<ItemOption>>('/items', { params: { active: true, size: 500 } }),
        ]);
        setAgreements(agreementResponse.data.content);
        setAllItems(itemsResponse.data.content);
        const preselected = agreementResponse.data.content.find((row) => row.id === route.params?.agreementId);
        if (preselected) {
          setSelectedId(preselected.id);
          populateLines(preselected);
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

  const populateLines = (agreement: Agreement) => {
    if (agreement.items && agreement.items.length > 0) {
      setOrderLines(agreement.items.map((item) => ({
        itemId: item.itemId,
        itemCode: item.itemCode,
        itemName: item.itemName,
        unit: item.unit,
        quantity: '0',
      })));
    } else {
      setOrderLines([]);
    }
  };

  const chooseAgreement = (agreement: Agreement) => {
    setSelectedId(agreement.id);
    populateLines(agreement);
    setPickerOpen(false);
  };

  const addItem = (item: ItemOption) => {
    if (orderLines.some((line) => line.itemId === item.id)) {
      Alert.alert('Already added', `${item.itemName} is already in the order.`);
      return;
    }
    setOrderLines((current) => [...current, {
      itemId: item.id,
      itemCode: item.itemCode,
      itemName: item.itemName,
      unit: item.unit,
      quantity: '0',
    }]);
    setItemPickerOpen(false);
    setItemSearch('');
  };

  const removeItem = (itemId: number) => {
    setOrderLines((current) => current.filter((line) => line.itemId !== itemId));
  };

  const setLineQuantity = (itemId: number, value: string) => {
    setOrderLines((current) => current.map((line) =>
      line.itemId === itemId ? { ...line, quantity: value } : line
    ));
  };

  const filteredItems = useMemo(() => {
    if (!itemSearch.trim()) return allItems.filter((item) => item.active);
    const search = itemSearch.toLowerCase();
    return allItems.filter((item) =>
      item.active &&
      (item.itemName.toLowerCase().includes(search) ||
       item.itemCode.toLowerCase().includes(search))
    );
  }, [allItems, itemSearch]);

  const submit = async () => {
    if (!selected) {
      Alert.alert('Select agreement', 'Choose the active agreement for this site order.');
      return;
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(orderDate)) {
      Alert.alert('Check order date', 'Use YYYY-MM-DD format.');
      return;
    }
    const items = orderLines
      .filter((line) => Number(line.quantity) > 0)
      .map((line) => ({ itemId: line.itemId, orderedQuantity: Number(line.quantity) }));
    if (!items.length) {
      Alert.alert('Add material', 'Enter a quantity for at least one item.');
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
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'} automaticallyAdjustKeyboardInsets>
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
        <DateField label="Order date" value={orderDate} onChange={setOrderDate} />
        <Field label="Dispatch instructions" value={notes} onChangeText={setNotes} multiline />

        <View style={styles.headingRow}>
          <Text style={styles.heading}>Required material</Text>
          {selected ? (
            <Pressable style={styles.addButton} onPress={() => { setItemSearch(''); setItemPickerOpen(true); }}>
              <Ionicons name="add-circle" size={18} color="#fff" />
              <Text style={styles.addButtonText}>Add item</Text>
            </Pressable>
          ) : null}
        </View>

        {!selected ? (
          <Card><EmptyBlock title="Select an agreement" message="Its contracted items will appear here." /></Card>
        ) : orderLines.length === 0 ? (
          <Card>
            <EmptyBlock title="No items yet" message="Tap 'Add item' above to pick materials from the inventory." />
          </Card>
        ) : orderLines.map((line) => (
          <Card key={line.itemId} style={styles.item}>
            <View style={styles.itemHeader}>
              <View style={styles.itemInfo}>
                <Text style={styles.code}>{line.itemCode}</Text>
                <Text style={styles.itemName}>{line.itemName}</Text>
              </View>
              <Pressable style={styles.removeButton} onPress={() => removeItem(line.itemId)} hitSlop={8}>
                <Ionicons name="close-circle" size={22} color={colors.red} />
              </Pressable>
            </View>
            <View style={styles.quantityRow}>
              <Text style={styles.quantityLabel}>Order quantity</Text>
              <TextInput
                keyboardType="decimal-pad"
                onChangeText={(value) => setLineQuantity(line.itemId, value)}
                selectTextOnFocus
                style={styles.quantityInput}
                value={line.quantity}
              />
              <Text style={styles.unit}>{line.unit}</Text>
            </View>
          </Card>
        ))}
        <AppButton
          title="Create order and continue"
          onPress={submit}
          loading={submitting}
          disabled={loading || !!error || !agreements.length}
        />
        <Text style={styles.helper}>The app confirms the order and opens challan dispatch quantities next.</Text>
      </ScrollView>

      {/* Agreement Picker Modal */}
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

      {/* Item Picker Modal */}
      <Modal animationType="slide" transparent visible={itemPickerOpen} onRequestClose={() => setItemPickerOpen(false)}>
        <View style={styles.backdrop}>
          <View style={styles.sheet}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>Add material</Text>
              <Pressable accessibilityLabel="Close" onPress={() => setItemPickerOpen(false)} style={styles.close}>
                <Ionicons name="close" size={24} color={colors.ink} />
              </Pressable>
            </View>
            <View style={styles.searchBar}>
              <Ionicons name="search" size={18} color={colors.muted} />
              <TextInput
                style={styles.searchInput}
                placeholder="Search items by name or code..."
                placeholderTextColor={colors.muted}
                value={itemSearch}
                onChangeText={setItemSearch}
                autoFocus
              />
            </View>
            <ScrollView contentContainerStyle={styles.agreementList} keyboardShouldPersistTaps="handled">
              {filteredItems.length ? filteredItems.map((item) => {
                const alreadyAdded = orderLines.some((line) => line.itemId === item.id);
                return (
                  <Pressable
                    key={item.id}
                    onPress={() => !alreadyAdded && addItem(item)}
                    style={[styles.itemRow, alreadyAdded && styles.itemRowDisabled]}
                  >
                    <View style={styles.agreementCopy}>
                      <Text style={[styles.agreementNumber, alreadyAdded && styles.textDisabled]}>{item.itemCode}</Text>
                      <Text style={[styles.agreementParty, alreadyAdded && styles.textDisabled]}>{item.itemName}</Text>
                      <Text style={styles.agreementSite}>{item.unit}{item.categoryName ? ` · ${item.categoryName}` : ''}</Text>
                    </View>
                    {alreadyAdded ? (
                      <Text style={styles.addedBadge}>Added</Text>
                    ) : (
                      <Ionicons name="add-circle" size={24} color={colors.primary} />
                    )}
                  </Pressable>
                );
              }) : <EmptyBlock title="No items found" message="Try a different search term." />}
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
  headingRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 4 },
  heading: { color: colors.ink, fontSize: 20, fontWeight: '900' },
  addButton: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.primary, paddingHorizontal: 12, paddingVertical: 7, borderRadius: 8, gap: 5 },
  addButtonText: { color: '#fff', fontWeight: '800', fontSize: 13 },
  item: {},
  itemHeader: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between' },
  itemInfo: { flex: 1 },
  code: { color: colors.primary, fontSize: 11, fontWeight: '900' },
  itemName: { color: colors.ink, fontSize: 16, fontWeight: '800', marginTop: 3 },
  removeButton: { padding: 2, marginTop: -2, marginRight: -4 },
  quantityRow: { flexDirection: 'row', alignItems: 'center', marginTop: 13, paddingTop: 12, borderTopWidth: 1, borderTopColor: colors.line },
  quantityLabel: { color: colors.ink, fontWeight: '700', flex: 1 },
  quantityInput: { width: 90, height: 44, borderWidth: 1, borderColor: colors.primary, borderRadius: 7, backgroundColor: '#fff', color: colors.ink, fontSize: 17, fontWeight: '800', textAlign: 'right', paddingHorizontal: 10 },
  unit: { color: colors.muted, fontSize: 11, width: 48, marginLeft: 7 },
  helper: { color: colors.muted, fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: -5 },
  backdrop: { flex: 1, backgroundColor: 'rgba(4, 25, 23, 0.45)', justifyContent: 'flex-end' },
  sheet: { maxHeight: '78%', backgroundColor: colors.surface, borderTopLeftRadius: 12, borderTopRightRadius: 12, paddingBottom: 20 },
  sheetHeader: { minHeight: 64, paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sheetTitle: { color: colors.ink, fontSize: 19, fontWeight: '900' },
  close: { padding: 8 },
  searchBar: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 14, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: colors.line, gap: 8, backgroundColor: '#F7F9F8' },
  searchInput: { flex: 1, fontSize: 15, color: colors.ink, padding: 0 },
  agreementList: { padding: 14 },
  agreement: { minHeight: 82, padding: 14, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center' },
  agreementCopy: { flex: 1 },
  agreementNumber: { color: colors.primary, fontWeight: '900' },
  agreementParty: { color: colors.ink, fontSize: 15, fontWeight: '800', marginTop: 4 },
  agreementSite: { color: colors.muted, fontSize: 12, marginTop: 2 },
  itemRow: { minHeight: 66, padding: 14, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center' },
  itemRowDisabled: { opacity: 0.5 },
  textDisabled: { color: colors.muted },
  addedBadge: { color: colors.muted, fontSize: 12, fontWeight: '700', paddingHorizontal: 8, paddingVertical: 4, backgroundColor: '#F1F4F3', borderRadius: 6 },
});
