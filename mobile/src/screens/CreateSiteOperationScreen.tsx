import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text } from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, DateField, Field, SelectField, ToggleField } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';
import type { OperationRequest, OperationResponse, Page, Site } from '../types/api';
import { localDate } from '../utils/format';

type Props = NativeStackScreenProps<RootStackParams, 'CreateSiteOperation'>;
const types = ['TRANSPORT', 'LOADING_LABOUR', 'UNLOADING_LABOUR', 'MATHADI', 'TPI', 'CONTRACT_LABOUR', 'SITE_EXPENSE'];
const directions = ['DELIVERY', 'RETURN', 'GENERAL'];
const statuses = ['PLANNED', 'CONFIRMED', 'COMPLETED', 'CANCELLED'];
const empty: OperationRequest = { operationDate: localDate(), operationType: 'TRANSPORT', direction: 'DELIVERY', siteId: 0, rate: 0, amount: 0, chargeToClient: false, status: 'PLANNED' };

export function CreateSiteOperationScreen({ navigation, route }: Props) {
  const id = route.params?.operationId;
  const [value, setValue] = useState<OperationRequest>(empty);
  const [sites, setSites] = useState<Site[]>([]);
  const [saving, setSaving] = useState(false);
  useEffect(() => { Promise.all([apiClient.get<Page<Site>>('/sites', { params: { size: 500 } }), apiClient.get<OperationResponse[]>('/client-workflow/operations')]).then(([siteData, operations]) => { setSites(siteData.data.content); if (id) { const found = operations.data.find((row) => row.id === id); if (found) setValue(found); } }).catch((cause) => Alert.alert('Operation unavailable', apiErrorMessage(cause, 'Unable to load operation details.'))); }, [id]);
  const change = <K extends keyof OperationRequest>(key: K, next: OperationRequest[K]) => setValue((current) => ({ ...current, [key]: next }));
  const number = (text: string) => text.trim() ? Number(text) : undefined;
  const submit = async () => { if (!value.siteId) { Alert.alert('Select a site', 'A site is required.'); return; } setSaving(true); try { if (id) await apiClient.put(`/client-workflow/operations/${id}`, value); else await apiClient.post('/client-workflow/operations', value); navigation.goBack(); } catch (cause) { Alert.alert('Operation not saved', apiErrorMessage(cause, 'Unable to save this operation.')); } finally { setSaving(false); } };
  return <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}><ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" automaticallyAdjustKeyboardInsets>
    <Text style={styles.intro}>Record transport, loading, unloading, Mathadi, TPI, contract labour or other site expenses.</Text>
    <DateField label="Operation date *" value={value.operationDate} onChange={(next) => change('operationDate', next)} />
    <SelectField label="Type *" value={value.operationType} options={types} onChange={(next) => change('operationType', next)} />
    <SelectField label="Direction *" value={value.direction} options={directions} onChange={(next) => change('direction', next)} />
    <SelectField label="Site *" value={value.siteId ? String(value.siteId) : ''} options={sites.map((site) => ({ value: String(site.id), label: `${site.siteName} — ${site.partyName}` }))} onChange={(next) => { const site = sites.find((row) => row.id === Number(next)); change('siteId', Number(next)); change('partyId', site?.partyId); }} />
    <Field label="Provider name" value={value.providerName ?? ''} onChangeText={(next) => change('providerName', next)} />
    <Field label="Transporter" value={value.transporterName ?? ''} onChangeText={(next) => change('transporterName', next)} />
    <Field label="Vehicle number" value={value.vehicleNumber ?? ''} autoCapitalize="characters" onChangeText={(next) => change('vehicleNumber', next)} />
    <Field label="Driver name" value={value.driverName ?? ''} onChangeText={(next) => change('driverName', next)} />
    <Field label="Worker count" value={value.workerCount?.toString() ?? ''} keyboardType="number-pad" onChangeText={(next) => change('workerCount', number(next))} />
    <Field label="Quantity" value={value.quantity?.toString() ?? ''} keyboardType="decimal-pad" onChangeText={(next) => change('quantity', number(next))} />
    <Field label="Rate *" value={String(value.rate)} keyboardType="decimal-pad" onChangeText={(next) => change('rate', Number(next) || 0)} />
    <Field label="Amount *" value={String(value.amount)} keyboardType="decimal-pad" onChangeText={(next) => change('amount', Number(next) || 0)} />
    <ToggleField label="Charge this amount to client" value={value.chargeToClient} onChange={(next) => change('chargeToClient', next)} />
    <SelectField label="Status *" value={value.status} options={statuses} onChange={(next) => change('status', next)} />
    <Field label="Reference number" value={value.referenceNumber ?? ''} onChangeText={(next) => change('referenceNumber', next)} />
    <Field label="Notes" value={value.notes ?? ''} multiline onChangeText={(next) => change('notes', next)} />
    <AppButton title={id ? 'Save operation' : 'Create operation'} onPress={submit} loading={saving} />
  </ScrollView></KeyboardAvoidingView>;
}
const styles = StyleSheet.create({ root: { flex: 1, backgroundColor: colors.canvas }, content: { padding: 18, paddingBottom: 40, gap: 15 }, intro: { color: colors.muted, lineHeight: 21 } });
