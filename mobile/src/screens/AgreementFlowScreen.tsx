import { Ionicons } from '@expo/vector-icons';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, DateField, Field, LoadingBlock, StatusPill } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';
import type { Agreement } from '../types/api';
import { dateLabel, localDate, quantity } from '../utils/format';
import { shareServerPdf } from '../utils/sharePdf';

type Props = NativeStackScreenProps<RootStackParams, 'AgreementFlow'>;

export function AgreementFlowScreen({ navigation, route }: Props) {
  const { user } = useAuth();
  const [agreement, setAgreement] = useState<Agreement | null>(null);
  const [effectiveDate, setEffectiveDate] = useState(localDate());
  const [expiryDate, setExpiryDate] = useState('');
  const [securityDeposit, setSecurityDeposit] = useState('0');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(!!route.params.agreementId);
  const [working, setWorking] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!route.params.agreementId) return;
    apiClient.get<Agreement>(`/agreements/${route.params.agreementId}`)
      .then((response) => setAgreement(response.data))
      .catch((cause) => setError(apiErrorMessage(cause, 'Unable to load this agreement.')))
      .finally(() => setLoading(false));
  }, [route.params.agreementId]);

  const convert = async () => {
    if (!route.params.quotationId) return;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(effectiveDate) || (expiryDate && !/^\d{4}-\d{2}-\d{2}$/.test(expiryDate))) {
      Alert.alert('Check agreement dates', 'Use YYYY-MM-DD format.');
      return;
    }
    setWorking('convert');
    try {
      const response = await apiClient.post<Agreement>(`/agreements/from-quotation/${route.params.quotationId}`, {
        templateId: null,
        effectiveDate,
        expiryDate: expiryDate || null,
        securityDeposit: Number(securityDeposit) || 0,
        notes: notes.trim() || null,
      });
      setAgreement(response.data);
    } catch (cause) {
      Alert.alert('Agreement not created', apiErrorMessage(cause, 'Unable to convert this quotation.'));
    } finally {
      setWorking('');
    }
  };

  const runAction = async (action: 'generate-document' | 'ready-for-review' | 'activate') => {
    if (!agreement) return;
    const labels = {
      'generate-document': 'Generate agreement document?',
      'ready-for-review': 'Mark agreement ready for review?',
      activate: 'Activate agreement?',
    };
    Alert.alert(labels[action], action === 'activate' ? 'The agreement will become available for site orders and challans.' : 'The agreement record will move to its next preparation step.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Continue',
        onPress: async () => {
          setWorking(action);
          try {
            const response = await apiClient.post<Agreement>(`/agreements/${agreement.id}/${action}`);
            setAgreement(response.data);
          } catch (cause) {
            Alert.alert('Action failed', apiErrorMessage(cause, 'Unable to update this agreement.'));
          } finally {
            setWorking('');
          }
        },
      },
    ]);
  };

  const shareDocument = async () => {
    if (!agreement) return;
    setWorking('share');
    try {
      await shareServerPdf(
        `/agreements/${agreement.id}/document`,
        agreement.generatedFilename ?? `agreement-${agreement.agreementNumber}.pdf`,
        'Share agreement',
      );
    } catch (cause) {
      Alert.alert('Document unavailable', apiErrorMessage(cause, 'Unable to download and share the agreement.'));
    } finally {
      setWorking('');
    }
  };

  if (loading) return <LoadingBlock />;

  if (!agreement) {
    return (
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.root}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'} automaticallyAdjustKeyboardInsets>
          {error ? <Text style={styles.error}>{error}</Text> : null}
          <Text style={styles.title}>Convert approved quotation</Text>
          <Text style={styles.intro}>Set the contract period and deposit. Commercial lines and terms are copied from the approved quotation.</Text>
          <DateField label="Effective date *" value={effectiveDate} onChange={setEffectiveDate} />
          <DateField label="Expiry date" value={expiryDate} onChange={setExpiryDate} optional minimumDate={new Date(`${effectiveDate}T12:00:00`)} />
          <Field label="Security deposit (INR)" value={securityDeposit} onChangeText={setSecurityDeposit} keyboardType="decimal-pad" />
          <Field label="Agreement notes" value={notes} onChangeText={setNotes} multiline />
          <AppButton title="Create draft agreement" onPress={convert} loading={working === 'convert'} disabled={!!error} />
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }

  const generated = !!agreement.generatedDocumentAttachmentId;
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Card>
        <View style={styles.top}>
          <Text style={styles.number}>{agreement.agreementNumber}</Text>
          <StatusPill value={agreement.status} />
        </View>
        <Text style={styles.party}>{agreement.partyName}</Text>
        <Text style={styles.site}>{agreement.siteName} ({agreement.siteCode})</Text>
        <View style={styles.detailGrid}>
          <Detail label="Effective" value={dateLabel(agreement.effectiveDate)} />
          <Detail label="Expiry" value={agreement.expiryDate ? dateLabel(agreement.expiryDate) : 'Open ended'} />
          <Detail label="Quotation" value={agreement.sourceQuotationNumber} />
          <Detail label="Deposit" value={`INR ${quantity(agreement.securityDeposit)}`} />
        </View>
      </Card>

      <Text style={styles.heading}>Agreement readiness</Text>
      <Card style={styles.steps}>
        <Step number="1" title="Agreement created" done />
        <Step number="2" title="Document generated" done={generated} />
        <Step number="3" title="Ready for review" done={agreement.status !== 'DRAFT'} />
        <Step number="4" title="Agreement active" done={agreement.status === 'ACTIVE'} />
      </Card>

      {agreement.status === 'DRAFT' && !generated ? (
        <AppButton title="Generate agreement document" onPress={() => runAction('generate-document')} loading={working === 'generate-document'} />
      ) : null}
      {agreement.status === 'DRAFT' && generated ? (
        <AppButton title="Mark ready for review" onPress={() => runAction('ready-for-review')} loading={working === 'ready-for-review'} />
      ) : null}
      {agreement.status === 'READY_FOR_REVIEW' && !generated ? (
        <AppButton title="Generate agreement document" onPress={() => runAction('generate-document')} loading={working === 'generate-document'} />
      ) : null}
      {agreement.status === 'READY_FOR_REVIEW' && generated && isAdmin ? (
        <AppButton title="Activate agreement" onPress={() => runAction('activate')} loading={working === 'activate'} />
      ) : null}
      {agreement.status === 'READY_FOR_REVIEW' && generated && !isAdmin ? (
        <Text style={styles.notice}>Document is ready. An administrator must activate this agreement.</Text>
      ) : null}
      {generated ? (
        <AppButton
          title="Share agreement PDF"
          variant="secondary"
          onPress={shareDocument}
          loading={working === 'share'}
          icon={<Ionicons name="share-social-outline" size={20} color={colors.primary} />}
        />
      ) : null}
      {agreement.status === 'ACTIVE' ? (
        <AppButton title="Create site order" onPress={() => navigation.replace('CreateOrder', { agreementId: agreement.id })} />
      ) : null}
      {agreement.status === 'ACTIVE' ? <Text style={styles.helper}>The order screen will preselect this agreement and continue to challan dispatch.</Text> : null}
    </ScrollView>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return <View style={styles.detail}><Text style={styles.detailLabel}>{label}</Text><Text style={styles.detailValue}>{value}</Text></View>;
}

function Step({ number, title, done }: { number: string; title: string; done?: boolean }) {
  return (
    <View style={styles.step}>
      <View style={[styles.stepNumber, done && styles.stepDone]}>
        {done ? <Ionicons name="checkmark" size={17} color="#fff" /> : <Text style={styles.stepNumberText}>{number}</Text>}
      </View>
      <Text style={[styles.stepTitle, done && styles.stepTitleDone]}>{title}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.canvas },
  content: { padding: 18, paddingBottom: 36, backgroundColor: colors.canvas, gap: 14, flexGrow: 1 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12 },
  title: { color: colors.ink, fontSize: 23, fontWeight: '900' },
  intro: { color: colors.muted, lineHeight: 21 },
  top: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 10 },
  number: { color: colors.primary, fontSize: 17, fontWeight: '900', flexShrink: 1 },
  party: { color: colors.ink, fontSize: 21, fontWeight: '900', marginTop: 14 },
  site: { color: colors.muted, marginTop: 4 },
  detailGrid: { flexDirection: 'row', flexWrap: 'wrap', borderTopWidth: 1, borderTopColor: '#E9EFEE', marginTop: 16, paddingTop: 14, rowGap: 14 },
  detail: { width: '50%', paddingRight: 8 },
  detailLabel: { color: colors.muted, fontSize: 11, marginBottom: 3 },
  detailValue: { color: colors.ink, fontSize: 13, fontWeight: '800' },
  heading: { color: colors.ink, fontSize: 19, fontWeight: '900', marginTop: 4 },
  steps: { gap: 13 },
  step: { flexDirection: 'row', alignItems: 'center', minHeight: 34 },
  stepNumber: { width: 30, height: 30, borderRadius: 15, backgroundColor: '#D7E1DF', alignItems: 'center', justifyContent: 'center' },
  stepDone: { backgroundColor: colors.green },
  stepNumberText: { color: colors.muted, fontWeight: '900' },
  stepTitle: { color: colors.muted, fontWeight: '800', marginLeft: 11 },
  stepTitleDone: { color: colors.ink },
  helper: { color: colors.muted, fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: -5 },
  notice: { color: colors.amber, backgroundColor: colors.amberSoft, borderRadius: 8, padding: 12, lineHeight: 19 },
});
