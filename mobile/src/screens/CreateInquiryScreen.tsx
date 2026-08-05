import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text } from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, DateField, Field, SelectField } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';
import type { InquiryRequest, InquiryResponse } from '../types/api';
import { localDate } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateInquiry'>;
const sources = ['EMAIL', 'PHONE', 'JUSTDIAL', 'REFERENCE', 'WALK_IN', 'OTHER'];
const statuses = ['OPEN', 'FOLLOW_UP', 'QUOTED', 'WON', 'LOST', 'CANCELLED'];

export function CreateInquiryScreen({ navigation, route }: Props) {
  const id = route.params?.inquiryId;
  const [value, setValue] = useState<InquiryRequest>({ inquiryDate: localDate(), source: 'PHONE', companyName: '', contactName: '', requirement: '', status: 'OPEN' });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!id) return;
    apiClient.get<InquiryResponse[]>('/client-workflow/inquiries').then(({ data }) => {
      const found = data.find((row) => row.id === id);
      if (found) {
        const comp = found.companyName || found.partyName || (found.notes?.match(/Company:\s*([^\n]+)/i)?.[1]) || '';
        setValue({ ...found, companyName: comp });
      }
      else Alert.alert('Inquiry unavailable', 'This inquiry could not be found.');
    }).catch((cause) => Alert.alert('Inquiry unavailable', apiErrorMessage(cause, 'Unable to load this inquiry.')));
  }, [id]);

  const change = <K extends keyof InquiryRequest>(key: K, next: InquiryRequest[K]) => setValue((current) => ({ ...current, [key]: next }));
  const submit = async () => {
    if (!value.contactName.trim() || !value.requirement.trim()) {
      Alert.alert('Complete required fields', 'Enter the contact name and requirement.');
      return;
    }
    setSaving(true);
    try {
      const compName = value.companyName?.trim();
      const existingNotes = value.notes?.trim() || '';
      const notesWithComp = compName && !existingNotes.includes(`Company:`) ? `Company: ${compName}\n${existingNotes}`.trim() : existingNotes;

      const body = {
        ...value,
        companyName: compName || undefined,
        contactName: value.contactName.trim(),
        requirement: value.requirement.trim(),
        phone: value.phone?.trim() || undefined,
        email: value.email?.trim() || undefined,
        notes: notesWithComp || undefined,
      };
      if (id) await apiClient.put(`/client-workflow/inquiries/${id}`, body);
      else await apiClient.post('/client-workflow/inquiries', body);
      navigation.goBack();
    } catch (cause) {
      Alert.alert('Inquiry not saved', apiErrorMessage(cause, 'Unable to save this inquiry.'));
    } finally { setSaving(false); }
  };

  return <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}>
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" automaticallyAdjustKeyboardInsets>
      <Text style={styles.intro}>Record the lead source, contact and follow-up before preparing a quotation.</Text>
      <DateField label="Inquiry date *" value={value.inquiryDate} onChange={(next) => change('inquiryDate', next)} />
      <SelectField label="Source *" value={value.source} options={sources} onChange={(next) => change('source', next)} />
      <Field label="Company name" value={value.companyName ?? ''} onChangeText={(next) => change('companyName', next)} placeholder="e.g. M/S Ali Designers Pvt Ltd" />
      <Field label="Contact name *" value={value.contactName} onChangeText={(next) => change('contactName', next)} />
      <Field label="Phone" value={value.phone ?? ''} keyboardType="phone-pad" onChangeText={(next) => change('phone', next)} />
      <Field label="Email" value={value.email ?? ''} keyboardType="email-address" autoCapitalize="none" onChangeText={(next) => change('email', next)} />
      <Field label="Requirement *" value={value.requirement} multiline onChangeText={(next) => change('requirement', next)} />
      <DateField label="Follow-up date" value={value.followUpDate ?? ''} optional minimumDate={new Date(`${value.inquiryDate}T12:00:00`)} onChange={(next) => change('followUpDate', next || undefined)} />
      <SelectField label="Status *" value={value.status} options={statuses} onChange={(next) => change('status', next)} />
      <Field label="Linked quotation ID" value={value.quotationId?.toString() ?? ''} keyboardType="number-pad" onChangeText={(next) => change('quotationId', next ? Number(next) : undefined)} />
      <Field label="Notes" value={value.notes ?? ''} multiline onChangeText={(next) => change('notes', next)} />
      <AppButton title={id ? 'Save inquiry' : 'Create inquiry'} onPress={submit} loading={saving} />
    </ScrollView>
  </KeyboardAvoidingView>;
}

const styles = StyleSheet.create({ root: { flex: 1, backgroundColor: colors.canvas }, content: { padding: 18, paddingBottom: 40, gap: 15 }, intro: { color: colors.muted, lineHeight: 21 } });
