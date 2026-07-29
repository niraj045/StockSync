import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, EmptyBlock, Field } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';
import type { Page, Party, Site } from '../types/api';
import { localDate } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateSite'>;

export function CreateSiteScreen({ navigation, route }: Props) {
  const [parties, setParties] = useState<Party[]>([]);
  const [partyId, setPartyId] = useState<number | null>(route.params?.partyId ?? null);
  const [siteName, setSiteName] = useState('');
  const [siteCode, setSiteCode] = useState('');
  const [address, setAddress] = useState('');
  const [contactPerson, setContactPerson] = useState('');
  const [startDate, setStartDate] = useState(localDate());
  const [pickerOpen, setPickerOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    apiClient.get<Page<Party>>('/parties', { params: { active: true, size: 200, sort: 'legalName,asc' } })
      .then((response) => setParties(response.data.content))
      .catch((cause) => Alert.alert('Customers unavailable', apiErrorMessage(cause, 'Unable to load customers.')))
      .finally(() => setLoading(false));
  }, []);

  const selected = parties.find((party) => party.id === partyId);

  const submit = async () => {
    if (!partyId || !siteName.trim() || !siteCode.trim()) {
      Alert.alert('Complete required fields', 'Select a customer and enter the site name and code.');
      return;
    }
    if (startDate && !/^\d{4}-\d{2}-\d{2}$/.test(startDate)) {
      Alert.alert('Check start date', 'Use YYYY-MM-DD format.');
      return;
    }
    setSaving(true);
    try {
      const response = await apiClient.post<Site>('/sites', {
        partyId,
        siteName: siteName.trim(),
        siteCode: siteCode.trim().toUpperCase(),
        address: address.trim() || null,
        contactPerson: contactPerson.trim() || null,
        startDate: startDate || null,
        expectedEndDate: null,
        status: 'ACTIVE',
        defaulter: false,
        closedDate: null,
        notes: null,
      });
      navigation.replace('CreateQuotation', { partyId, siteId: response.data.id });
    } catch (cause) {
      Alert.alert('Site not created', apiErrorMessage(cause, 'Unable to create this site.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.intro}>Link the work site to its customer. The next step will prepare the quotation.</Text>
        <Text style={styles.label}>Customer *</Text>
        <Pressable style={styles.selector} disabled={loading} onPress={() => setPickerOpen(true)}>
          <Text style={selected ? styles.value : styles.placeholder}>
            {selected?.legalName ?? (loading ? 'Loading customers...' : 'Select customer')}
          </Text>
          <Ionicons name="chevron-down" size={20} color={colors.muted} />
        </Pressable>
        <Field label="Site name *" value={siteName} onChangeText={setSiteName} />
        <Field label="Site code *" value={siteCode} onChangeText={setSiteCode} autoCapitalize="characters" />
        <Field label="Site contact" value={contactPerson} onChangeText={setContactPerson} />
        <Field label="Address" value={address} onChangeText={setAddress} multiline />
        <Field label="Start date" value={startDate} onChangeText={setStartDate} placeholder="YYYY-MM-DD" />
        <AppButton title="Save and create quotation" onPress={submit} loading={saving} />
      </ScrollView>
      <Modal animationType="slide" transparent visible={pickerOpen} onRequestClose={() => setPickerOpen(false)}>
        <View style={styles.backdrop}>
          <View style={styles.sheet}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>Select customer</Text>
              <Pressable onPress={() => setPickerOpen(false)}><Ionicons name="close" size={25} color={colors.ink} /></Pressable>
            </View>
            <ScrollView contentContainerStyle={styles.list}>
              {parties.length ? parties.map((party) => (
                <Pressable key={party.id} style={styles.option} onPress={() => { setPartyId(party.id); setPickerOpen(false); }}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.optionTitle}>{party.legalName}</Text>
                    {party.tradeName ? <Text style={styles.optionMeta}>{party.tradeName}</Text> : null}
                  </View>
                  <Ionicons name="chevron-forward" size={20} color={colors.muted} />
                </Pressable>
              )) : <EmptyBlock title="No customers" message="Create a customer before adding a site." />}
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
  intro: { color: colors.muted, lineHeight: 21 },
  label: { color: colors.ink, fontSize: 14, fontWeight: '700', marginBottom: -8 },
  selector: { minHeight: 52, borderWidth: 1, borderColor: colors.line, borderRadius: 8, backgroundColor: '#fff', paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center' },
  value: { color: colors.ink, fontSize: 16, fontWeight: '800', flex: 1 },
  placeholder: { color: '#98A2B3', fontSize: 16, flex: 1 },
  backdrop: { flex: 1, backgroundColor: 'rgba(4,25,23,0.45)', justifyContent: 'flex-end' },
  sheet: { maxHeight: '78%', backgroundColor: '#fff', borderTopLeftRadius: 12, borderTopRightRadius: 12 },
  sheetHeader: { minHeight: 64, paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sheetTitle: { color: colors.ink, fontSize: 19, fontWeight: '900' },
  list: { padding: 14, paddingBottom: 28 },
  option: { minHeight: 68, padding: 13, borderBottomWidth: 1, borderBottomColor: colors.line, flexDirection: 'row', alignItems: 'center' },
  optionTitle: { color: colors.ink, fontSize: 15, fontWeight: '800' },
  optionMeta: { color: colors.muted, fontSize: 12, marginTop: 3 },
});
