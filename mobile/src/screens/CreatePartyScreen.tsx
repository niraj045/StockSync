import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text } from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, Field } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';
import type { Party } from '../types/api';

type Props = NativeStackScreenProps<RootStackParams, 'CreateParty'>;

export function CreatePartyScreen({ navigation }: Props) {
  const [legalName, setLegalName] = useState('');
  const [tradeName, setTradeName] = useState('');
  const [contactPerson, setContactPerson] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [gstin, setGstin] = useState('');
  const [pan, setPan] = useState('');
  const [address, setAddress] = useState('');
  const [state, setState] = useState('');
  const [saving, setSaving] = useState(false);

  const submit = async () => {
    if (!legalName.trim()) {
      Alert.alert('Customer name required', 'Enter the legal customer or company name.');
      return;
    }
    setSaving(true);
    try {
      const response = await apiClient.post<Party>('/parties', {
        legalName: legalName.trim(),
        tradeName: tradeName.trim() || null,
        contactPerson: contactPerson.trim() || null,
        phone: phone.trim() || null,
        email: email.trim() || null,
        gstin: gstin.trim().toUpperCase() || null,
        pan: pan.trim().toUpperCase() || null,
        address: address.trim() || null,
        state: state.trim() || null,
        notes: null,
        active: true,
      });
      navigation.replace('CreateSite', { partyId: response.data.id });
    } catch (cause) {
      Alert.alert('Customer not created', apiErrorMessage(cause, 'Unable to create this customer.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'} automaticallyAdjustKeyboardInsets>
        <Text style={styles.intro}>Create the customer record first. The app will continue to site setup.</Text>
        <Field label="Legal name *" value={legalName} onChangeText={setLegalName} autoCapitalize="words" />
        <Field label="Trade name" value={tradeName} onChangeText={setTradeName} autoCapitalize="words" />
        <Field label="Contact person" value={contactPerson} onChangeText={setContactPerson} autoCapitalize="words" />
        <Field label="Phone" value={phone} onChangeText={setPhone} keyboardType="phone-pad" />
        <Field label="Email" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" />
        <Field label="GSTIN" value={gstin} onChangeText={setGstin} autoCapitalize="characters" maxLength={15} />
        <Field label="PAN" value={pan} onChangeText={setPan} autoCapitalize="characters" maxLength={10} />
        <Field label="Address" value={address} onChangeText={setAddress} multiline />
        <Field label="State" value={state} onChangeText={setState} />
        <AppButton title="Save and add site" onPress={submit} loading={saving} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.canvas },
  content: { padding: 18, paddingBottom: 34, gap: 15 },
  intro: { color: colors.muted, lineHeight: 21, marginBottom: 2 },
});
