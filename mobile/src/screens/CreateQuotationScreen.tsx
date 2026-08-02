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
import { AppButton, Card, DateField, EmptyBlock, Field } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';
import type { ItemOption, Page, Party, Quotation, QuotationTemplate, Site } from '../types/api';
import { localDate, quantity } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateQuotation'>;
type ExactSlotKey = 'hframe' | 'bracing' | 'mspipe' | 'platepipe' | 'basejack' | 'platform' | 'coupler';
type Line = {
  itemId: number;
  quantity: string;
  rate: string;
  requiredQuantity?: string;
  hireMonths?: string;
  replacementRate?: string;
};
type ExactHireForm = {
  partyAddress: string; subject: string; validityDays: string; minimumHirePeriod: string;
  minimumHireDays: string; siteLengthRmt: string; siteHeightMtr: string; gstPercentage: string;
  advanceRent: string; paymentDueDays: string; authorizedPerson: string; authorizedDesignation: string;
  authorizedPhone: string; acceptedBy: string; acceptedDesignation: string; acceptedPhone: string; acceptedDate: string;
};
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

const exactTemplateCode = 'STEELFAB_EXACT_HIRE_V1';
const exactSlots: { key: ExactSlotKey; label: string }[] = [
  { key: 'hframe', label: 'H Frame' }, { key: 'bracing', label: 'Bracing' },
  { key: 'mspipe', label: '20 Ft / MS Pipe' }, { key: 'platepipe', label: 'Plate Pipe' },
  { key: 'basejack', label: 'Base Jack' }, { key: 'platform', label: 'Platform' },
  { key: 'coupler', label: 'Coupler' },
];
const initialExactHire: ExactHireForm = {
  partyAddress: '', subject: '', validityDays: '7', minimumHirePeriod: '6 Months (180 days)',
  minimumHireDays: '90', siteLengthRmt: '', siteHeightMtr: '', gstPercentage: '18', advanceRent: '',
  paymentDueDays: '3', authorizedPerson: '', authorizedDesignation: '', authorizedPhone: '',
  acceptedBy: '', acceptedDesignation: '', acceptedPhone: '', acceptedDate: '',
};
const normalize = (value?: string) => value?.toLowerCase().replace(/[^a-z0-9]/g, '') ?? '';
const matchesExactSlot = (item: ItemOption, key: ExactSlotKey) => {
  const value = normalize(`${item.itemCode} ${item.itemName}`);
  if (key === 'hframe') return value.includes('hframe');
  if (key === 'bracing') return value.includes('bracing') || value.includes('crossbrace');
  if (key === 'mspipe') return value.includes('20ftpipe') || value.includes('mspipe') || value.includes('steelpipe');
  if (key === 'platepipe') return value.includes('platepipe');
  if (key === 'basejack') return value.includes('basejack');
  if (key === 'platform') return value.includes('platform') || value.includes('walkway');
  return value.includes('coupler') || value.includes('clamp');
};
const exactDefaultLines = (items: ItemOption[]): Line[] => exactSlots.flatMap((slot) => {
  const item = items.find((candidate) => matchesExactSlot(candidate, slot.key));
  return item ? [{
    itemId: item.id,
    requiredQuantity: '', quantity: '', rate: '', hireMonths: '6', replacementRate: '',
  }] : [];
});
const stateCodes: Record<string, string> = {
  andhrapradesh: '37', arunachalpradesh: '12', assam: '18', bihar: '10', chhattisgarh: '22', goa: '30',
  gujarat: '24', haryana: '06', himachalpradesh: '02', jharkhand: '20', karnataka: '29', kerala: '32',
  madhyapradesh: '23', maharashtra: '27', manipur: '14', meghalaya: '17', mizoram: '15', nagaland: '13',
  odisha: '21', punjab: '03', rajasthan: '08', sikkim: '11', tamilnadu: '33', telangana: '36', tripura: '16',
  uttarpradesh: '09', uttarakhand: '05', westbengal: '19', delhi: '07', jammuandkashmir: '01', ladakh: '38',
};
const stateCode = (gstin?: string, location?: string) => {
  const value = gstin?.trim().toUpperCase();
  if (value && /^\d{2}[0-9A-Z]{13}$/.test(value)) return value.slice(0, 2);
  const normalized = normalize(location);
  return Object.entries(stateCodes).find(([state]) => normalized.includes(state))?.[1];
};
const addDays = (date: string, days: number) => {
  const value = new Date(`${date}T00:00:00`);
  value.setDate(value.getDate() + days);
  return value.toISOString().slice(0, 10);
};

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
  const [exactHire, setExactHire] = useState<ExactHireForm>(initialExactHire);
  const [itemPickerIndex, setItemPickerIndex] = useState<number | null>(null);
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
        if (availableTemplates[0].templateCode === exactTemplateCode) {
          setValidUntil(addDays(localDate(), Number(initialExactHire.validityDays)));
          setLines(exactDefaultLines(itemResponse.data.content));
        }
      }
    }).catch((cause) => setError(apiErrorMessage(cause, 'Unable to load quotation setup data.')))
      .finally(() => setLoading(false));
  }, []);

  const selectedTemplate = templates.find((row) => row.id === templateId);
  const selectedParty = parties.find((row) => row.id === partyId);
  const selectedSite = sites.find((row) => row.id === siteId);
  const filteredSites = sites.filter((row) => row.partyId === partyId && row.status !== 'CLOSED');
  const isExact = selectedTemplate?.templateCode === exactTemplateCode;
  const availableItems = items.filter((item) => !lines.some((line, index) => line.itemId === item.id && index !== itemPickerIndex));
  const subtotal = useMemo(
    () => lines.reduce((sum, line) => sum + (Number(line.quantity) || 0) * (Number(line.rate) || 0) * (isExact ? (Number(line.hireMonths) || 0) : 1), 0),
    [lines, isExact],
  );
  const automaticGst = useMemo(() => {
    const total = Number(exactHire.gstPercentage) || 0;
    const supplier = stateCode(selectedTemplate?.companyGstin, selectedTemplate?.companyAddress);
    const customer = stateCode(selectedParty?.gstin, selectedParty?.state);
    return supplier && supplier === customer
      ? { cgst: total / 2, sgst: total / 2, igst: 0, label: 'Same state' }
      : { cgst: 0, sgst: 0, igst: total, label: 'Interstate / state unavailable' };
  }, [exactHire.gstPercentage, selectedParty, selectedTemplate]);
  const appliedTaxRate = isExact ? automaticGst.cgst + automaticGst.sgst + automaticGst.igst
    : (Number(cgstRate) || 0) + (Number(sgstRate) || 0) + (Number(igstRate) || 0);
  const estimate = subtotal * (1 + appliedTaxRate / 100);
  const oneMonthRent = lines.reduce((sum, line) => sum + (Number(line.quantity) || 0) * (Number(line.rate) || 0), 0);

  const selectTemplate = (template: QuotationTemplate) => {
    const wasExact = selectedTemplate?.templateCode === exactTemplateCode;
    setTemplateId(template.id);
    setTerms(template.defaultTerms ?? '');
    setNotes(template.defaultNotes ?? '');
    if (template.templateCode === exactTemplateCode) {
      const validityDays = Number(initialExactHire.validityDays);
      setRentalType('PER_PIECE_PER_DAY');
      setValidUntil(addDays(quotationDate, validityDays));
      setExactHire({
        ...initialExactHire,
        partyAddress: selectedParty?.address ?? '',
        subject: `Quotation for Supply of H frame Scaffolding Materials on Hire for ${selectedSite?.siteName ?? 'the selected site'}.`,
      });
      setLines(exactDefaultLines(items));
    } else if (wasExact) {
      setLines([]);
    }
    setPicker(null);
  };

  const addItem = (item: ItemOption) => {
    setLines((current) => [...current, isExact
      ? { itemId: item.id, requiredQuantity: '', quantity: '', rate: '', hireMonths: '6', replacementRate: '' }
      : { itemId: item.id, quantity: '1', rate: '' }]);
    setPicker(null);
  };

  const updateLine = (index: number, field: keyof Line, value: string | number) => {
    setLines((current) => current.map((line, lineIndex) => lineIndex === index ? { ...line, [field]: value } : line));
  };

  const updateExactHire = (field: keyof ExactHireForm, value: string) => {
    setExactHire((current) => ({ ...current, [field]: value }));
    if (field === 'validityDays' && Number(value) > 0) setValidUntil(addDays(quotationDate, Number(value)));
  };

  const selectQuotationDate = (value: string) => {
    setQuotationDate(value);
    if (isExact && Number(exactHire.validityDays) > 0) {
      setValidUntil(addDays(value, Number(exactHire.validityDays)));
    }
  };

  const selectParty = (party: Party) => {
    setPartyId(party.id); setSiteId(null); setPicker(null);
    if (isExact) setExactHire((current) => ({ ...current, partyAddress: party.address ?? '' }));
  };

  const selectSite = (site: Site) => {
    setSiteId(site.id); setPicker(null);
    if (isExact) setExactHire((current) => ({ ...current, subject: `Quotation for Supply of H frame Scaffolding Materials on Hire for ${site.siteName}.` }));
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
    if (!lines.length || lines.some((line) => !line.itemId || Number(line.quantity) <= 0 || Number(line.rate) <= 0)) {
      Alert.alert('Check material lines', 'Add at least one item and enter a quantity and rate greater than zero.');
      return;
    }
    if (isExact) {
      const requiredText = [exactHire.partyAddress, exactHire.subject, exactHire.minimumHirePeriod,
        exactHire.authorizedPerson, exactHire.authorizedDesignation, exactHire.authorizedPhone];
      const requiredNumbers = [exactHire.validityDays, exactHire.minimumHireDays, exactHire.siteLengthRmt,
        exactHire.siteHeightMtr, exactHire.gstPercentage, exactHire.paymentDueDays];
      if (requiredText.some((value) => !value.trim()) || requiredNumbers.some((value) => value === '' || Number(value) < 0)) {
        Alert.alert('Complete SteelFab details', 'Fill every required project, hire, GST, and authorization field.');
        return;
      }
      if (lines.some((line) => line.requiredQuantity === '' || Number(line.requiredQuantity) < 0
        || Number(line.hireMonths) <= 0 || line.replacementRate === '' || Number(line.replacementRate) < 0)) {
        Alert.alert('Complete SteelFab schedule', 'For every selected material, enter required quantity, offered quantity, monthly rate, months, and replacement rate.');
        return;
      }
    }
    setSaving(true);
    try {
      const advanceRent = exactHire.advanceRent === '' ? oneMonthRent : Number(exactHire.advanceRent);
      const response = await apiClient.post<Quotation>('/quotations', {
        quotationTemplateId: templateId,
        partyId,
        siteId,
        quotationDate,
        validUntil,
        rentalType,
        discountType: 'NONE',
        discountValue: 0,
        cgstRate: isExact ? automaticGst.cgst : Number(cgstRate) || 0,
        sgstRate: isExact ? automaticGst.sgst : Number(sgstRate) || 0,
        igstRate: isExact ? automaticGst.igst : Number(igstRate) || 0,
        transportCharge: 0,
        loadingCharge: 0,
        unloadingCharge: 0,
        otherCharge: 0,
        roundOff: 0,
        securityDeposit: Number(securityDeposit) || 0,
        terms: terms.trim() || null,
        notes: notes.trim() || null,
        exactHire: isExact ? {
          partyAddress: exactHire.partyAddress.trim(), subject: exactHire.subject.trim(),
          validityDays: Number(exactHire.validityDays), minimumHirePeriod: exactHire.minimumHirePeriod.trim(),
          minimumHireDays: Number(exactHire.minimumHireDays), siteLengthRmt: Number(exactHire.siteLengthRmt),
          siteHeightMtr: Number(exactHire.siteHeightMtr), gstPercentage: Number(exactHire.gstPercentage),
          advanceRent, paymentDueDays: Number(exactHire.paymentDueDays),
          authorizedPerson: exactHire.authorizedPerson.trim(), authorizedDesignation: exactHire.authorizedDesignation.trim(),
          authorizedPhone: exactHire.authorizedPhone.trim(), acceptedBy: exactHire.acceptedBy.trim() || null,
          acceptedDesignation: exactHire.acceptedDesignation.trim() || null,
          acceptedPhone: exactHire.acceptedPhone.trim() || null, acceptedDate: exactHire.acceptedDate || null,
        } : null,
        items: lines.map((line) => ({
          itemId: line.itemId,
          quantity: Number(line.quantity),
          rate: Number(line.rate),
          requiredQuantity: isExact ? Number(line.requiredQuantity) : null,
          hireMonths: isExact ? Number(line.hireMonths) : null,
          replacementRate: isExact ? Number(line.replacementRate) : null,
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
    if (picker === 'party') return parties.map((row) => ({ key: row.id, title: row.legalName, meta: row.tradeName, onPress: () => selectParty(row) }));
    if (picker === 'site') return filteredSites.map((row) => ({ key: row.id, title: row.siteName, meta: row.siteCode, onPress: () => selectSite(row) }));
    if (picker === 'item') return availableItems.map((row) => ({ key: row.id, title: row.itemName, meta: `${row.itemCode} | ${row.unit}`, onPress: () => {
      if (itemPickerIndex !== null) updateLine(itemPickerIndex, 'itemId', row.id); else addItem(row);
      setItemPickerIndex(null); setPicker(null);
    } }));
    if (picker === 'rental') return rentalTypes.map(([value, label]) => ({ key: value, title: label, onPress: () => { setRentalType(value); setPicker(null); } }));
    return [];
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'} automaticallyAdjustKeyboardInsets>
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
          <View style={styles.column}><DateField label="Quotation date *" value={quotationDate} onChange={selectQuotationDate} /></View>
          <View style={styles.column}><DateField label="Valid until *" value={validUntil} onChange={setValidUntil} minimumDate={new Date(`${quotationDate}T12:00:00`)} /></View>
        </View>
        <Text style={styles.section}>Customer and site</Text>
        <Selector label="Customer *" value={selectedParty?.legalName ?? ''} loading={loading} onPress={() => setPicker('party')} />
        <Selector label="Site *" value={selectedSite ? `${selectedSite.siteCode} | ${selectedSite.siteName}` : ''} disabled={!partyId} onPress={() => setPicker('site')} />
        {isExact ? <>
          <Text style={styles.section}>SteelFab project details</Text>
          <Field label="Customer address *" value={exactHire.partyAddress} onChangeText={(value) => updateExactHire('partyAddress', value)} multiline />
          <Field label="Quotation subject *" value={exactHire.subject} onChangeText={(value) => updateExactHire('subject', value)} multiline />
          <View style={styles.twoColumns}>
            <View style={styles.column}><Field label="Validity days *" value={exactHire.validityDays} onChangeText={(value) => updateExactHire('validityDays', value)} keyboardType="number-pad" /></View>
            <View style={styles.column}><Field label="Minimum hire days *" value={exactHire.minimumHireDays} onChangeText={(value) => updateExactHire('minimumHireDays', value)} keyboardType="number-pad" /></View>
          </View>
          <Field label="Quoted hire period *" value={exactHire.minimumHirePeriod} onChangeText={(value) => updateExactHire('minimumHirePeriod', value)} />
          <View style={styles.twoColumns}>
            <View style={styles.column}><Field label="Site length (RMT) *" value={exactHire.siteLengthRmt} onChangeText={(value) => updateExactHire('siteLengthRmt', value)} keyboardType="decimal-pad" /></View>
            <View style={styles.column}><Field label="Site height (MTR) *" value={exactHire.siteHeightMtr} onChangeText={(value) => updateExactHire('siteHeightMtr', value)} keyboardType="decimal-pad" /></View>
          </View>
        </> : null}
        <Selector label="Rental method *" value={rentalTypes.find(([value]) => value === rentalType)?.[1] ?? rentalType} onPress={() => setPicker('rental')} />

        <View style={styles.sectionRow}>
          <Text style={styles.section}>{isExact ? 'SteelFab material schedule' : 'Material and rates'}</Text>
          <Pressable style={styles.addItem} onPress={() => { setItemPickerIndex(null); setPicker('item'); }}>
            <Ionicons name="add" size={19} color="#fff" /><Text style={styles.addItemText}>Add item</Text>
          </Pressable>
        </View>
        {!lines.length ? <Card><EmptyBlock title="No material added" message="Add items from the inventory master and enter the agreed rates." /></Card> : null}
        {lines.map((line, index) => {
          const item = items.find((row) => row.id === line.itemId);
          return (
            <Card key={`${line.itemId}-${index}`} style={styles.line}>
              <View style={styles.lineHeader}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.code}>{isExact ? `${index + 1}. ${item?.itemCode ?? ''}` : item?.itemCode}</Text>
                  <Text style={styles.itemName}>{item?.itemName ?? ''}</Text>
                </View>
                {isExact ? <Pressable style={styles.mapButton} onPress={() => { setItemPickerIndex(index); setPicker('item'); }}>
                  <Ionicons name="swap-horizontal" size={18} color={colors.primary} /><Text style={styles.mapButtonText}>Change</Text>
                </Pressable> : null}
                <Pressable style={styles.removeButton} accessibilityLabel="Remove item" onPress={() => setLines((current) => current.filter((_, lineIndex) => lineIndex !== index))}>
                  <Ionicons name="trash-outline" size={22} color={colors.red} />
                </Pressable>
              </View>
              {isExact ? <>
                <View style={styles.twoColumns}>
                  <View style={styles.column}><Field label="Required qty *" value={line.requiredQuantity ?? ''} onChangeText={(value) => updateLine(index, 'requiredQuantity', value)} keyboardType="decimal-pad" /></View>
                  <View style={styles.column}><Field label={`Offered qty (${item?.unit ?? ''}) *`} value={line.quantity} onChangeText={(value) => updateLine(index, 'quantity', value)} keyboardType="decimal-pad" /></View>
                </View>
                <View style={styles.twoColumns}>
                  <View style={styles.column}><Field label="Monthly rate (INR) *" value={line.rate} onChangeText={(value) => updateLine(index, 'rate', value)} keyboardType="decimal-pad" /></View>
                  <View style={styles.column}><Field label="Hire months *" value={line.hireMonths ?? ''} onChangeText={(value) => updateLine(index, 'hireMonths', value)} keyboardType="decimal-pad" /></View>
                </View>
                <Field label="Replacement rate (INR) *" value={line.replacementRate ?? ''} onChangeText={(value) => updateLine(index, 'replacementRate', value)} keyboardType="decimal-pad" />
              </> : <View style={styles.twoColumns}>
                <View style={styles.column}><Field label={`Quantity (${item?.unit ?? ''})`} value={line.quantity} onChangeText={(value) => updateLine(index, 'quantity', value)} keyboardType="decimal-pad" /></View>
                <View style={styles.column}><Field label="Rate (INR)" value={line.rate} onChangeText={(value) => updateLine(index, 'rate', value)} keyboardType="decimal-pad" /></View>
              </View>}
            </Card>
          );
        })}

        <Text style={styles.section}>Taxes and terms</Text>
        {isExact ? <Card style={styles.gstCard}>
          <View style={styles.twoColumns}>
            <View style={styles.column}><Field label="GST rate % *" value={exactHire.gstPercentage} onChangeText={(value) => updateExactHire('gstPercentage', value)} keyboardType="decimal-pad" /></View>
            <View style={styles.column}><Text style={styles.gstMode}>{automaticGst.label}</Text><Text style={styles.gstBreakdown}>CGST {automaticGst.cgst}%  SGST {automaticGst.sgst}%  IGST {automaticGst.igst}%</Text></View>
          </View>
          <Text style={styles.gstHelp}>GST allocation is automatic from the company and customer state.</Text>
        </Card> : <View style={styles.threeColumns}>
          <View style={styles.smallColumn}><Field label="CGST %" value={cgstRate} onChangeText={setCgstRate} keyboardType="decimal-pad" /></View>
          <View style={styles.smallColumn}><Field label="SGST %" value={sgstRate} onChangeText={setSgstRate} keyboardType="decimal-pad" /></View>
          <View style={styles.smallColumn}><Field label="IGST %" value={igstRate} onChangeText={setIgstRate} keyboardType="decimal-pad" /></View>
        </View>}
        <Field label="Security deposit (INR)" value={securityDeposit} onChangeText={setSecurityDeposit} keyboardType="decimal-pad" />
        {isExact ? <>
          <Field label={`One month advance rent (INR, calculated ${quantity(oneMonthRent)})`} value={exactHire.advanceRent} onChangeText={(value) => updateExactHire('advanceRent', value)} keyboardType="decimal-pad" />
          <Text style={styles.fieldHelp}>Leave blank to use the calculated one-month rent.</Text>
          <Field label="Payment due days *" value={exactHire.paymentDueDays} onChangeText={(value) => updateExactHire('paymentDueDays', value)} keyboardType="number-pad" />
          <Text style={styles.section}>Authorization and acceptance</Text>
          <Field label="SteelFab authorized person *" value={exactHire.authorizedPerson} onChangeText={(value) => updateExactHire('authorizedPerson', value)} />
          <Field label="Designation *" value={exactHire.authorizedDesignation} onChangeText={(value) => updateExactHire('authorizedDesignation', value)} />
          <Field label="Phone *" value={exactHire.authorizedPhone} onChangeText={(value) => updateExactHire('authorizedPhone', value)} keyboardType="phone-pad" />
          <Field label="Accepted by" value={exactHire.acceptedBy} onChangeText={(value) => updateExactHire('acceptedBy', value)} />
          <Field label="Customer designation" value={exactHire.acceptedDesignation} onChangeText={(value) => updateExactHire('acceptedDesignation', value)} />
          <Field label="Customer phone" value={exactHire.acceptedPhone} onChangeText={(value) => updateExactHire('acceptedPhone', value)} keyboardType="phone-pad" />
          <DateField label="Acceptance date" value={exactHire.acceptedDate} onChange={(value) => updateExactHire('acceptedDate', value)} optional />
        </> : null}
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
  mapButton: { minHeight: 38, flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 10, borderWidth: 1, borderColor: colors.primary, borderRadius: 7 },
  mapButtonText: { color: colors.primary, fontWeight: '800' },
  removeButton: { minWidth: 40, minHeight: 40, alignItems: 'center', justifyContent: 'center', marginLeft: 4 },
  code: { color: colors.primary, fontSize: 11, fontWeight: '900' },
  itemName: { color: colors.ink, fontSize: 16, fontWeight: '800', marginTop: 3 },
  totalCard: { backgroundColor: colors.primarySoft },
  totalLabel: { color: colors.primaryDark, fontSize: 12, fontWeight: '800' },
  total: { color: colors.ink, fontSize: 25, fontWeight: '900', marginTop: 4 },
  totalHelp: { color: colors.muted, fontSize: 11, lineHeight: 17, marginTop: 5 },
  gstCard: { backgroundColor: colors.primarySoft, gap: 8 },
  gstMode: { color: colors.primaryDark, fontSize: 12, fontWeight: '900', marginTop: 8 },
  gstBreakdown: { color: colors.ink, fontSize: 12, fontWeight: '700', lineHeight: 18, marginTop: 5 },
  gstHelp: { color: colors.muted, fontSize: 11, lineHeight: 17 },
  fieldHelp: { color: colors.muted, fontSize: 11, marginTop: -9 },
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
