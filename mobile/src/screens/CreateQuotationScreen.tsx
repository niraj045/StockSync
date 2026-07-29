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
import { colors } from '../theme';
import type { ItemOption, Page, Party, Quotation, QuotationTemplate, Site } from '../types/api';
import { localDate, quantity } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateQuotation'>;
type Line = { itemId: number; quantity: string; rate: string };
type PickerKind = 'template' | 'party' | 'site' | 'item' | 'rental' | null;
type PickerRow = { key: number | string; title: string; meta?: string; onPress: () => void };

const rentalTypes = [
  ['PER_PIECE_PER_DAY', 'Per piece per day'],
  ['PLATE_AREA_PER_DAY', 'Plate area per day'],
  ['SCAFFOLD_AREA_PER_DAY', 'Scaffold area per day'],
  ['PLOT_AREA_PER_DAY', 'Plot area per day'],
  ['FIXED_RATE', 'Fixed rate'],
  ['SLAB_BASED', 'Slab based'],
] as const;

export function CreateQuotationScreen({ navigation, route }: Props) {
  const { user } = useAuth();
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  const [templates, setTemplates] = useState<QuotationTemplate[]>([]);
  const [parties, setParties] = useState<Party[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [items, setItems] = useState<ItemOption[]>([]);
  const [templateId, setTemplateId] = useState<number | null>(null);
  const [partyId, setPartyId] = useState<number | null>(route.params?.partyId ?? null);
  const [siteId, setSiteId] = useState<number | null>(route.params?.siteId ?? null);
  const [quotationDate, setQuotationDate] = useState(localDate());
  const [validUntil, setValidUntil] = useState(localDate(new Date(Date.now() + 30 * 86400000)));
  const [rentalType, setRentalType] = useState('PER_PIECE_PER_DAY');
  const [cgstRate, setCgstRate] = useState('9');
  const [sgstRate, setSgstRate] = useState('9');
  const [igstRate, setIgstRate] = useState('0');
  const [securityDeposit, setSecurityDeposit] = useState('0');
  const [terms, setTerms] = useState('');
  const [notes, setNotes] = useState('');
  const [templateCode, setTemplateCode] = useState('CLIENT-QUOTATION');
  const [templateName, setTemplateName] = useState('Client quotation');
  const [companyName, setCompanyName] = useState('');
  const [companyAddress, setCompanyAddress] = useState('');
  const [companyGstin, setCompanyGstin] = useState('');
  const [lines, setLines] = useState<Line[]>([]);
  const [picker, setPicker] = useState<PickerKind>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      apiClient.get<Page<QuotationTemplate>>('/quotation-templates', { params: { active: true, size: 100 } }),
      apiClient.get<Page<Party>>('/parties', { params: { active: true, size: 200 } }),
      apiClient.get<Page<Site>>('/sites', { params: { size: 300 } }),
      apiClient.get<Page<ItemOption>>('/items', { params: { active: true, size: 500 } }),
    ]).then(([templateResponse, partyResponse, siteResponse, itemResponse]) => {
      const availableTemplates = templateResponse.data.content;
      setTemplates(availableTemplates);
      setParties(partyResponse.data.content);
      setSites(siteResponse.data.content);
      setItems(itemResponse.data.content);
      if (availableTemplates.length === 1) {
        setTemplateId(availableTemplates[0].id);
        setTerms(availableTemplates[0].defaultTerms ?? '');
        setNotes(availableTemplates[0].defaultNotes ?? '');
      }
    }).catch((cause) => setError(apiErrorMessage(cause, 'Unable to load quotation setup data.')))
      .finally(() => setLoading(false));
  }, []);

  const selectedTemplate = templates.find((row) => row.id === templateId);
  const selectedParty = parties.find((row) => row.id === partyId);
  const selectedSite = sites.find((row) => row.id === siteId);
  const filteredSites = sites.filter((row) => row.partyId === partyId && row.status !== 'CLOSED');
  const availableItems = items.filter((item) => !lines.some((line) => line.itemId === item.id));
  const subtotal = useMemo(
    () => lines.reduce((sum, line) => sum + (Number(line.quantity) || 0) * (Number(line.rate) || 0), 0),
    [lines],
  );
  const estimate = subtotal * (1 + ((Number(cgstRate) || 0) + (Number(sgstRate) || 0) + (Number(igstRate) || 0)) / 100);

  const selectTemplate = (template: QuotationTemplate) => {
    setTemplateId(template.id);
    setTerms(template.defaultTerms ?? '');
    setNotes(template.defaultNotes ?? '');
    setPicker(null);
  };

  const addItem = (item: ItemOption) => {
    setLines((current) => [...current, { itemId: item.id, quantity: '1', rate: '' }]);
    setPicker(null);
  };

  const updateLine = (itemId: number, field: 'quantity' | 'rate', value: string) => {
    setLines((current) => current.map((line) => line.itemId === itemId ? { ...line, [field]: value } : line));
  };

  const createTemplate = async () => {
    if (!templateCode.trim() || !templateName.trim() || !companyName.trim()) {
      Alert.alert('Complete template details', 'Enter the template code, name, and company name.');
      return;
    }
    setSaving(true);
    try {
      const response = await apiClient.post<QuotationTemplate>('/quotation-templates', {
        templateCode: templateCode.trim().toUpperCase(),
        name: templateName.trim(),
        description: 'Primary client quotation template created from StockSync mobile',
        companyName: companyName.trim(),
        companyAddress: companyAddress.trim() || null,
        companyGstin: companyGstin.trim().toUpperCase() || null,
        headerText: null,
        footerText: null,
        defaultTerms: terms.trim() || null,
        defaultNotes: notes.trim() || null,
        logoAttachmentId: null,
      });
      setTemplates([response.data]);
      setTemplateId(response.data.id);
      Alert.alert('Template ready', 'The company quotation format is now selected.');
    } catch (cause) {
      Alert.alert('Template not created', apiErrorMessage(cause, 'Unable to create the quotation template.'));
    } finally {
      setSaving(false);
    }
  };

  const submit = async () => {
    if (!templateId || !partyId || !siteId) {
      Alert.alert('Complete quotation details', 'Select a template, customer, and site.');
      return;
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(quotationDate) || !/^\d{4}-\d{2}-\d{2}$/.test(validUntil)) {
      Alert.alert('Check dates', 'Use YYYY-MM-DD format for both dates.');
      return;
    }
    if (!lines.length || lines.some((line) => Number(line.quantity) <= 0 || Number(line.rate) <= 0)) {
      Alert.alert('Check material lines', 'Add at least one item and enter a quantity and rate greater than zero.');
      return;
    }
    setSaving(true);
    try {
      const response = await apiClient.post<Quotation>('/quotations', {
        quotationTemplateId: templateId,
        partyId,
        siteId,
        quotationDate,
        validUntil,
        rentalType,
        discountType: 'NONE',
        discountValue: 0,
        cgstRate: Number(cgstRate) || 0,
        sgstRate: Number(sgstRate) || 0,
        igstRate: Number(igstRate) || 0,
        transportCharge: 0,
        loadingCharge: 0,
        unloadingCharge: 0,
        otherCharge: 0,
        roundOff: 0,
        securityDeposit: Number(securityDeposit) || 0,
        terms: terms.trim() || null,
        notes: notes.trim() || null,
        items: lines.map((line) => ({
          itemId: line.itemId,
          quantity: Number(line.quantity),
          rate: Number(line.rate),
          rentalType,
          area: 0,
          weight: 0,
          description: null,
        })),
      });
      Alert.alert(
        'Quotation created',
        `${response.data.quotationNumber} is saved as a draft. Open it in Sales to send and approve it.`,
        [{ text: 'Open Sales', onPress: () => navigation.navigate('Main') }],
      );
    } catch (cause) {
      Alert.alert('Quotation not created', apiErrorMessage(cause, 'Unable to create this quotation.'));
    } finally {
      setSaving(false);
    }
  };

  const pickerRows = (): PickerRow[] => {
    if (picker === 'template') return templates.map((row) => ({ key: row.id, title: `${row.templateCode} | ${row.name}`, onPress: () => selectTemplate(row) }));
    if (picker === 'party') return parties.map((row) => ({ key: row.id, title: row.legalName, meta: row.tradeName, onPress: () => { setPartyId(row.id); setSiteId(null); setPicker(null); } }));
    if (picker === 'site') return filteredSites.map((row) => ({ key: row.id, title: row.siteName, meta: row.siteCode, onPress: () => { setSiteId(row.id); setPicker(null); } }));
    if (picker === 'item') return availableItems.map((row) => ({ key: row.id, title: row.itemName, meta: `${row.itemCode} | ${row.unit}`, onPress: () => addItem(row) }));
    if (picker === 'rental') return rentalTypes.map(([value, label]) => ({ key: value, title: label, onPress: () => { setRentalType(value); setPicker(null); } }));
    return [];
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!loading && !templates.length ? (
          <Card style={styles.setupCard}>
            <Text style={styles.setupTitle}>Set up the quotation format</Text>
            <Text style={styles.setupText}>The clean database has no quotation template. Create the client company format once; every future quotation can reuse it.</Text>
            {isAdmin ? (
              <>
                <Field label="Template code *" value={templateCode} onChangeText={setTemplateCode} autoCapitalize="characters" />
                <Field label="Template name *" value={templateName} onChangeText={setTemplateName} />
                <Field label="Company name *" value={companyName} onChangeText={setCompanyName} />
                <Field label="Company address" value={companyAddress} onChangeText={setCompanyAddress} multiline />
                <Field label="Company GSTIN" value={companyGstin} onChangeText={setCompanyGstin} autoCapitalize="characters" maxLength={15} />
                <AppButton title="Create company template" onPress={createTemplate} loading={saving} />
              </>
            ) : <Text style={styles.setupWarning}>An administrator must create the first quotation template.</Text>}
          </Card>
        ) : null}
        <Text style={styles.section}>Quotation details</Text>
        <Selector label="Template *" value={selectedTemplate ? `${selectedTemplate.templateCode} | ${selectedTemplate.name}` : ''} loading={loading} onPress={() => setPicker('template')} />
        <View style={styles.twoColumns}>
          <View style={styles.column}><Field label="Quotation date *" value={quotationDate} onChangeText={setQuotationDate} /></View>
          <View style={styles.column}><Field label="Valid until *" value={validUntil} onChangeText={setValidUntil} /></View>
        </View>
        <Text style={styles.section}>Customer and site</Text>
        <Selector label="Customer *" value={selectedParty?.legalName ?? ''} loading={loading} onPress={() => setPicker('party')} />
        <Selector label="Site *" value={selectedSite ? `${selectedSite.siteCode} | ${selectedSite.siteName}` : ''} disabled={!partyId} onPress={() => setPicker('site')} />
        <Selector label="Rental method *" value={rentalTypes.find(([value]) => value === rentalType)?.[1] ?? rentalType} onPress={() => setPicker('rental')} />

        <View style={styles.sectionRow}>
          <Text style={styles.section}>Material and rates</Text>
          <Pressable style={styles.addItem} onPress={() => setPicker('item')}>
            <Ionicons name="add" size={19} color="#fff" /><Text style={styles.addItemText}>Add item</Text>
          </Pressable>
        </View>
        {!lines.length ? <Card><EmptyBlock title="No material added" message="Add items from the inventory master and enter the agreed rates." /></Card> : null}
        {lines.map((line) => {
          const item = items.find((row) => row.id === line.itemId);
          return (
            <Card key={line.itemId} style={styles.line}>
              <View style={styles.lineHeader}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.code}>{item?.itemCode}</Text>
                  <Text style={styles.itemName}>{item?.itemName}</Text>
                </View>
                <Pressable accessibilityLabel="Remove item" onPress={() => setLines((current) => current.filter((row) => row.itemId !== line.itemId))}>
                  <Ionicons name="trash-outline" size={22} color={colors.red} />
                </Pressable>
              </View>
              <View style={styles.twoColumns}>
                <View style={styles.column}><Field label={`Quantity (${item?.unit ?? ''})`} value={line.quantity} onChangeText={(value) => updateLine(line.itemId, 'quantity', value)} keyboardType="decimal-pad" /></View>
                <View style={styles.column}><Field label="Rate (INR)" value={line.rate} onChangeText={(value) => updateLine(line.itemId, 'rate', value)} keyboardType="decimal-pad" /></View>
              </View>
            </Card>
          );
        })}

        <Text style={styles.section}>Taxes and terms</Text>
        <View style={styles.threeColumns}>
          <View style={styles.smallColumn}><Field label="CGST %" value={cgstRate} onChangeText={setCgstRate} keyboardType="decimal-pad" /></View>
          <View style={styles.smallColumn}><Field label="SGST %" value={sgstRate} onChangeText={setSgstRate} keyboardType="decimal-pad" /></View>
          <View style={styles.smallColumn}><Field label="IGST %" value={igstRate} onChangeText={setIgstRate} keyboardType="decimal-pad" /></View>
        </View>
        <Field label="Security deposit (INR)" value={securityDeposit} onChangeText={setSecurityDeposit} keyboardType="decimal-pad" />
        <Field label="Terms" value={terms} onChangeText={setTerms} multiline />
        <Field label="Notes" value={notes} onChangeText={setNotes} multiline />
        <Card style={styles.totalCard}>
          <Text style={styles.totalLabel}>Estimated total</Text>
          <Text style={styles.total}>INR {quantity(estimate)}</Text>
          <Text style={styles.totalHelp}>The server recalculates and stores the final authoritative amount.</Text>
        </Card>
        <AppButton title="Create draft quotation" onPress={submit} loading={saving} disabled={loading || !!error} />
      </ScrollView>

      <Modal animationType="slide" transparent visible={!!picker} onRequestClose={() => setPicker(null)}>
        <View style={styles.backdrop}>
          <View style={styles.sheet}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>Select {picker}</Text>
              <Pressable onPress={() => setPicker(null)}><Ionicons name="close" size={25} color={colors.ink} /></Pressable>
            </View>
            <ScrollView contentContainerStyle={styles.list}>
              {pickerRows().length ? pickerRows().map((row) => (
                <Pressable key={row.key} style={styles.option} onPress={row.onPress}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.optionTitle}>{row.title}</Text>
                    {row.meta ? <Text style={styles.optionMeta}>{row.meta}</Text> : null}
                  </View>
                  <Ionicons name="chevron-forward" size={20} color={colors.muted} />
                </Pressable>
              )) : <EmptyBlock title="No options available" message="Complete the previous setup step first." />}
            </ScrollView>
          </View>
        </View>
      </Modal>
    </KeyboardAvoidingView>
  );
}

function Selector({ label, value, onPress, loading, disabled }: { label: string; value: string; onPress: () => void; loading?: boolean; disabled?: boolean }) {
  return (
    <View style={styles.selectorGroup}>
      <Text style={styles.label}>{label}</Text>
      <Pressable style={[styles.selector, disabled && styles.selectorDisabled]} disabled={loading || disabled} onPress={onPress}>
        <Text numberOfLines={2} style={value ? styles.selectorValue : styles.placeholder}>{value || (loading ? 'Loading...' : 'Select')}</Text>
        <Ionicons name="chevron-down" size={20} color={colors.muted} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.canvas },
  content: { padding: 18, paddingBottom: 36, gap: 15 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12 },
  section: { color: colors.ink, fontSize: 19, fontWeight: '900', marginTop: 5 },
  sectionRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  selectorGroup: { gap: 7 },
  label: { color: colors.ink, fontSize: 14, fontWeight: '700' },
  selector: { minHeight: 52, borderWidth: 1, borderColor: colors.line, borderRadius: 8, backgroundColor: '#fff', paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center' },
  selectorDisabled: { backgroundColor: '#EEF2F1', opacity: 0.7 },
  selectorValue: { color: colors.ink, fontSize: 15, fontWeight: '800', flex: 1, paddingVertical: 8 },
  placeholder: { color: '#98A2B3', fontSize: 16, flex: 1 },
  twoColumns: { flexDirection: 'row', gap: 10 },
  column: { flex: 1 },
  threeColumns: { flexDirection: 'row', gap: 8 },
  smallColumn: { flex: 1 },
  addItem: { minHeight: 40, borderRadius: 7, backgroundColor: colors.primary, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, gap: 4 },
  addItemText: { color: '#fff', fontWeight: '800' },
  line: { gap: 12 },
  lineHeader: { flexDirection: 'row', alignItems: 'center' },
  code: { color: colors.primary, fontSize: 11, fontWeight: '900' },
  itemName: { color: colors.ink, fontSize: 16, fontWeight: '800', marginTop: 3 },
  totalCard: { backgroundColor: colors.primarySoft },
  totalLabel: { color: colors.primaryDark, fontSize: 12, fontWeight: '800' },
  total: { color: colors.ink, fontSize: 25, fontWeight: '900', marginTop: 4 },
  totalHelp: { color: colors.muted, fontSize: 11, lineHeight: 17, marginTop: 5 },
  setupCard: { gap: 13, backgroundColor: colors.amberSoft, borderColor: '#E5BE73' },
  setupTitle: { color: colors.ink, fontSize: 18, fontWeight: '900' },
  setupText: { color: colors.muted, lineHeight: 20 },
  setupWarning: { color: colors.amber, fontWeight: '800', lineHeight: 20 },
  backdrop: { flex: 1, backgroundColor: 'rgba(4,25,23,0.45)', justifyContent: 'flex-end' },
  sheet: { maxHeight: '80%', backgroundColor: '#fff', borderTopLeftRadius: 12, borderTopRightRadius: 12 },
  sheetHeader: { minHeight: 64, paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sheetTitle: { color: colors.ink, fontSize: 19, fontWeight: '900', textTransform: 'capitalize' },
  list: { padding: 14, paddingBottom: 28 },
  option: { minHeight: 66, padding: 13, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center' },
  optionTitle: { color: colors.ink, fontSize: 15, fontWeight: '800' },
  optionMeta: { color: colors.muted, fontSize: 12, marginTop: 3 },
});
