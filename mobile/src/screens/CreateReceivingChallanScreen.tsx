import { Ionicons } from '@expo/vector-icons';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
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
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, EmptyBlock, Field } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import type { Page, Party, ReceivingChallan, Site, SiteOrder, SitePendingBalance } from '../types/api';
import { localDate, quantity } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateReceivingChallan'>;
type ReturnValues = { good: string; damaged: string; lost: string; extra: string };
type Picker = 'party' | 'site' | null;

const emptyValues = (): ReturnValues => ({ good: '0', damaged: '0', lost: '0', extra: '0' });

export function CreateReceivingChallanScreen({ navigation, route }: Props) {
  const issued = route.params?.issuedChallan;
  const { user } = useAuth();
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  const [parties, setParties] = useState<Party[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [partyId, setPartyId] = useState<number | null>(issued?.partyId ?? null);
  const [siteId, setSiteId] = useState<number | null>(issued?.siteId ?? null);
  const [agreementId, setAgreementId] = useState<number | null>(null);
  const [balances, setBalances] = useState<SitePendingBalance[]>([]);
  const [values, setValues] = useState<Record<number, ReturnValues>>({});
  const [receiveDate, setReceiveDate] = useState(localDate());
  const [vehicleNumber, setVehicleNumber] = useState(issued?.vehicleNumber ?? '');
  const [driverName, setDriverName] = useState(issued?.driverName ?? '');
  const [driverPhone, setDriverPhone] = useState('');
  const [notes, setNotes] = useState('');
  const [picker, setPicker] = useState<Picker>(null);
  const [loading, setLoading] = useState(true);
  const [loadingStock, setLoadingStock] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [partyResponse, siteResponse, orderResponse] = await Promise.all([
          apiClient.get<Page<Party>>('/parties', { params: { active: true, size: 200 } }),
          apiClient.get<Page<Site>>('/sites', { params: { size: 300 } }),
          issued?.siteOrderId ? apiClient.get<SiteOrder>(`/orders/${issued.siteOrderId}`) : Promise.resolve(null),
        ]);
        setParties(partyResponse.data.content);
        setSites(siteResponse.data.content);
        if (orderResponse) setAgreementId(orderResponse.data.agreementId);
      } catch (cause) {
        setError(apiErrorMessage(cause, 'Unable to load return setup data.'));
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [issued?.siteOrderId]);

  useEffect(() => {
    if (!siteId) {
      setBalances([]);
      setValues({});
      return;
    }
    setLoadingStock(true);
    apiClient.get<SitePendingBalance[]>('/challans/receiving/site-pending-balances', { params: { siteId } })
      .then((response) => {
        setBalances(response.data);
        setValues(Object.fromEntries(response.data.map((row) => [row.itemId, emptyValues()])));
      })
      .catch((cause) => setError(apiErrorMessage(cause, 'Unable to load material pending at this site.')))
      .finally(() => setLoadingStock(false));
  }, [siteId]);

  const selectedParty = parties.find((row) => row.id === partyId);
  const selectedSite = sites.find((row) => row.id === siteId);
  const filteredSites = sites.filter((row) => row.partyId === partyId && row.status !== 'CLOSED');

  const totals = useMemo(() => balances.reduce((result, row) => {
    const line = values[row.itemId] ?? emptyValues();
    result.good += Number(line.good) || 0;
    result.damaged += Number(line.damaged) || 0;
    result.lost += Number(line.lost) || 0;
    result.extra += Number(line.extra) || 0;
    return result;
  }, { good: 0, damaged: 0, lost: 0, extra: 0 }), [balances, values]);

  const updateValue = (itemId: number, field: keyof ReturnValues, value: string) => {
    setValues((current) => ({
      ...current,
      [itemId]: { ...(current[itemId] ?? emptyValues()), [field]: value },
    }));
  };

  const validate = () => {
    if (!partyId || !siteId) return 'Select the customer and site.';
    if (!/^\d{4}-\d{2}-\d{2}$/.test(receiveDate)) return 'Use YYYY-MM-DD format for the receive date.';
    if (totals.good + totals.damaged + totals.lost + totals.extra <= 0) return 'Enter a returned quantity for at least one item.';
    for (const balance of balances) {
      const line = values[balance.itemId] ?? emptyValues();
      const normal = (Number(line.good) || 0) + (Number(line.damaged) || 0) + (Number(line.lost) || 0);
      if ([line.good, line.damaged, line.lost, line.extra].some((value) => Number(value) < 0)) return 'Return quantities cannot be negative.';
      if (normal > Number(balance.pendingQuantity)) {
        return `${balance.itemName} has only ${quantity(balance.pendingQuantity)} pending at the site.`;
      }
    }
    return '';
  };

  const submit = () => {
    const validation = validate();
    if (validation) {
      Alert.alert('Check return quantities', validation);
      return;
    }
    const hasExtra = totals.extra > 0;
    Alert.alert(
      hasExtra ? 'Record return with extra stock?' : 'Post material return?',
      hasExtra
        ? `This return includes ${quantity(totals.extra)} extra units and requires administrator approval.`
        : `${quantity(totals.good)} good, ${quantity(totals.damaged)} damaged, and ${quantity(totals.lost)} lost units will be reconciled.`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Continue', onPress: () => void createAndPost() },
      ],
    );
  };

  const createAndPost = async () => {
    if (!partyId || !siteId) return;
    setSubmitting(true);
    try {
      const itemLines = balances.flatMap((balance) => {
        const line = values[balance.itemId] ?? emptyValues();
        const good = Number(line.good) || 0;
        const damaged = Number(line.damaged) || 0;
        const lost = Number(line.lost) || 0;
        const extra = Number(line.extra) || 0;
        if (good + damaged + lost + extra <= 0) return [];
        return [{
          itemId: balance.itemId,
          linkedIssuedChallanItemId: null,
          openingImportTransactionId: null,
          goodReturnedQuantity: good,
          damagedReturnedQuantity: damaged,
          lostQuantity: lost,
          extraReturnedQuantity: extra,
          exchangedFromItemId: null,
          exchangedToItemId: null,
          exchangedQuantity: 0,
          notes: null,
        }];
      });
      const created = await apiClient.post<ReceivingChallan>('/challans/receiving', {
        agreementId,
        partyId,
        siteId,
        linkedIssuedChallanId: issued?.id ?? null,
        receiveDate,
        vehicleNumber: vehicleNumber.trim() || null,
        driverName: driverName.trim() || null,
        driverPhone: driverPhone.trim() || null,
        transporterId: null,
        sourceType: issued ? 'ISSUED_CHALLAN' : 'SITE_PENDING_BALANCE',
        notes: notes.trim() || null,
        items: itemLines,
      });
      let result = created.data;
      if (result.status === 'EXTRA_APPROVAL_REQUIRED' && isAdmin) {
        result = (await apiClient.post<ReceivingChallan>(`/challans/receiving/${result.id}/approve-extra`)).data;
      }
      if (result.status === 'DRAFT' || result.status === 'APPROVED_FOR_POSTING') {
        result = (await apiClient.post<ReceivingChallan>(`/challans/receiving/${result.id}/post`)).data;
      }
      navigation.replace('ReceivingChallanDetail', { challan: result });
      if (result.status === 'EXTRA_APPROVAL_REQUIRED') {
        Alert.alert('Administrator approval required', `${result.receivingChallanNumber} is saved. An administrator must approve the extra quantity before posting.`);
      }
    } catch (cause) {
      Alert.alert('Return not completed', apiErrorMessage(cause, 'Unable to create and post the receiving challan.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {issued ? (
          <Card style={styles.sourceCard}>
            <Text style={styles.sourceLabel}>RETURN AGAINST ISSUED CHALLAN</Text>
            <Text style={styles.sourceNumber}>{issued.challanNumber}</Text>
            <Text style={styles.sourceMeta}>{issued.partyName} | {issued.siteName}</Text>
          </Card>
        ) : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        <Text style={styles.section}>Return location</Text>
        <Selector label="Customer *" value={selectedParty?.legalName ?? ''} disabled={!!issued || loading} onPress={() => setPicker('party')} />
        <Selector label="Site *" value={selectedSite ? `${selectedSite.siteCode} | ${selectedSite.siteName}` : ''} disabled={!!issued || !partyId || loading} onPress={() => setPicker('site')} />
        <View style={styles.twoColumns}>
          <View style={styles.column}><Field label="Receive date *" value={receiveDate} onChangeText={setReceiveDate} /></View>
          <View style={styles.column}><Field label="Vehicle number" value={vehicleNumber} onChangeText={setVehicleNumber} autoCapitalize="characters" /></View>
        </View>
        <View style={styles.twoColumns}>
          <View style={styles.column}><Field label="Driver name" value={driverName} onChangeText={setDriverName} /></View>
          <View style={styles.column}><Field label="Driver phone" value={driverPhone} onChangeText={setDriverPhone} keyboardType="phone-pad" /></View>
        </View>

        <View style={styles.sectionRow}>
          <Text style={styles.section}>Material received</Text>
          {loadingStock ? <Text style={styles.loadingText}>Loading stock...</Text> : null}
        </View>
        {!siteId ? <Card><EmptyBlock title="Select a site" message="Pending site material will appear here." /></Card> : null}
        {siteId && !loadingStock && !balances.length ? <Card><EmptyBlock title="No pending stock" message="This site has no material available to return." /></Card> : null}
        {balances.map((balance) => {
          const line = values[balance.itemId] ?? emptyValues();
          const normal = (Number(line.good) || 0) + (Number(line.damaged) || 0) + (Number(line.lost) || 0);
          return (
            <Card key={balance.itemId} style={styles.itemCard}>
              <View style={styles.itemHeader}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.code}>{balance.itemCode}</Text>
                  <Text style={styles.itemName}>{balance.itemName}</Text>
                </View>
                <View>
                  <Text style={styles.pendingLabel}>SITE PENDING</Text>
                  <Text style={styles.pending}>{quantity(balance.pendingQuantity)}</Text>
                </View>
              </View>
              <View style={styles.quantityGrid}>
                <QuantityInput label="Good" value={line.good} onChange={(value) => updateValue(balance.itemId, 'good', value)} />
                <QuantityInput label="Damaged" value={line.damaged} onChange={(value) => updateValue(balance.itemId, 'damaged', value)} />
                <QuantityInput label="Lost" value={line.lost} onChange={(value) => updateValue(balance.itemId, 'lost', value)} />
                <QuantityInput label="Extra" value={line.extra} onChange={(value) => updateValue(balance.itemId, 'extra', value)} />
              </View>
              {normal > Number(balance.pendingQuantity) ? <Text style={styles.lineError}>Normal return exceeds site pending quantity.</Text> : null}
            </Card>
          );
        })}

        <Field label="Return notes" value={notes} onChangeText={setNotes} multiline />
        <Card style={styles.summary}>
          <SummaryValue label="Good" value={totals.good} />
          <SummaryValue label="Damaged" value={totals.damaged} />
          <SummaryValue label="Lost" value={totals.lost} />
          <SummaryValue label="Extra" value={totals.extra} />
        </Card>
        <AppButton title={totals.extra > 0 && !isAdmin ? 'Save for administrator approval' : 'Save and post return'} onPress={submit} loading={submitting} disabled={loading || loadingStock || !!error || !balances.length} />
      </ScrollView>

      <Modal animationType="slide" transparent visible={!!picker} onRequestClose={() => setPicker(null)}>
        <View style={styles.backdrop}>
          <View style={styles.sheet}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>Select {picker}</Text>
              <Pressable onPress={() => setPicker(null)}><Ionicons name="close" size={25} color={colors.ink} /></Pressable>
            </View>
            <ScrollView contentContainerStyle={styles.optionList}>
              {(picker === 'party' ? parties : filteredSites).map((row) => (
                <Pressable
                  key={row.id}
                  style={styles.option}
                  onPress={() => {
                    if (picker === 'party') {
                      setPartyId(row.id);
                      setSiteId(null);
                    } else {
                      setSiteId(row.id);
                    }
                    setPicker(null);
                  }}
                >
                  <Text style={styles.optionText}>{picker === 'party' ? (row as Party).legalName : `${(row as Site).siteCode} | ${(row as Site).siteName}`}</Text>
                  <Ionicons name="chevron-forward" size={20} color={colors.muted} />
                </Pressable>
              ))}
            </ScrollView>
          </View>
        </View>
      </Modal>
    </KeyboardAvoidingView>
  );
}

function Selector({ label, value, disabled, onPress }: { label: string; value: string; disabled?: boolean; onPress: () => void }) {
  return (
    <View style={styles.selectorGroup}>
      <Text style={styles.label}>{label}</Text>
      <Pressable style={[styles.selector, disabled && styles.disabled]} disabled={disabled} onPress={onPress}>
        <Text style={value ? styles.selectorValue : styles.placeholder}>{value || 'Select'}</Text>
        <Ionicons name="chevron-down" size={20} color={colors.muted} />
      </Pressable>
    </View>
  );
}

function QuantityInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <View style={styles.quantityField}>
      <Text style={styles.quantityLabel}>{label}</Text>
      <TextInput value={value} onChangeText={onChange} keyboardType="decimal-pad" selectTextOnFocus style={styles.quantityInput} />
    </View>
  );
}

function SummaryValue({ label, value }: { label: string; value: number }) {
  return <View style={styles.summaryValue}><Text style={styles.summaryLabel}>{label}</Text><Text style={styles.summaryNumber}>{quantity(value)}</Text></View>;
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.canvas },
  content: { padding: 18, paddingBottom: 36, gap: 14 },
  sourceCard: { backgroundColor: colors.primarySoft, borderColor: '#FFD7CC' },
  sourceLabel: { color: colors.primaryDark, fontSize: 10, fontFamily: fonts.bold },
  sourceNumber: { color: colors.ink, fontSize: 19, fontFamily: fonts.extraBold, marginTop: 5 },
  sourceMeta: { color: colors.muted, fontSize: 12, marginTop: 4 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12 },
  section: { color: colors.ink, fontSize: 19, fontFamily: fonts.bold, marginTop: 3 },
  sectionRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  loadingText: { color: colors.muted, fontSize: 11 },
  selectorGroup: { gap: 7 },
  label: { color: colors.ink, fontSize: 13, fontFamily: fonts.semiBold },
  selector: { minHeight: 52, borderWidth: 1, borderColor: colors.line, borderRadius: 8, backgroundColor: '#fff', paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center' },
  disabled: { backgroundColor: '#F4F5F6', opacity: 0.78 },
  selectorValue: { flex: 1, color: colors.ink, fontSize: 15, fontFamily: fonts.semiBold },
  placeholder: { flex: 1, color: '#98A2B3', fontSize: 15 },
  twoColumns: { flexDirection: 'row', gap: 10 },
  column: { flex: 1 },
  itemCard: { gap: 13 },
  itemHeader: { flexDirection: 'row', alignItems: 'flex-start' },
  code: { color: colors.primary, fontSize: 11, fontFamily: fonts.bold },
  itemName: { color: colors.ink, fontSize: 16, fontFamily: fonts.bold, marginTop: 3 },
  pendingLabel: { color: colors.muted, fontSize: 9, fontFamily: fonts.bold, textAlign: 'right' },
  pending: { color: colors.ink, fontSize: 20, fontFamily: fonts.black, textAlign: 'right', marginTop: 3 },
  quantityGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 9, borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 13 },
  quantityField: { width: '48.5%', gap: 6 },
  quantityLabel: { color: colors.muted, fontSize: 11, fontFamily: fonts.semiBold },
  quantityInput: { height: 45, borderWidth: 1, borderColor: colors.line, borderRadius: 7, backgroundColor: '#fff', color: colors.ink, fontSize: 16, fontFamily: fonts.bold, textAlign: 'right', paddingHorizontal: 11 },
  lineError: { color: colors.red, fontSize: 11 },
  summary: { flexDirection: 'row', backgroundColor: colors.ink },
  summaryValue: { flex: 1, alignItems: 'center' },
  summaryLabel: { color: '#B8BDC4', fontSize: 10, fontFamily: fonts.semiBold },
  summaryNumber: { color: '#fff', fontSize: 17, fontFamily: fonts.bold, marginTop: 4 },
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.42)', justifyContent: 'flex-end' },
  sheet: { maxHeight: '78%', backgroundColor: '#fff', borderTopLeftRadius: 12, borderTopRightRadius: 12 },
  sheetHeader: { minHeight: 64, paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sheetTitle: { color: colors.ink, fontSize: 19, fontFamily: fonts.bold, textTransform: 'capitalize' },
  optionList: { padding: 14, paddingBottom: 28 },
  option: { minHeight: 62, borderBottomWidth: 1, borderBottomColor: colors.line, paddingHorizontal: 8, flexDirection: 'row', alignItems: 'center' },
  optionText: { flex: 1, color: colors.ink, fontFamily: fonts.semiBold },
});
