import { Ionicons } from '@expo/vector-icons';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import * as DocumentPicker from 'expo-document-picker';
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
  const [headerText, setHeaderText] = useState('');
  const [partATitle, setPartATitle] = useState('');
  const [partBTitle, setPartBTitle] = useState('');
  const [terms, setTerms] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (route.params.agreementId) {
      apiClient.get<Agreement>(`/agreements/${route.params.agreementId}`)
        .then((response) => setAgreement(response.data))
        .catch((cause) => setError(apiErrorMessage(cause, 'Unable to load this agreement.')))
        .finally(() => setLoading(false));
    } else if (route.params.quotationId) {
      // Need to fetch quotation to initialize terms
      apiClient.get<{headerText: string, partATitle: string, partBTitle: string, terms: string}>(`/quotations/${route.params.quotationId}`)
        .then((response) => { setHeaderText(response.data.headerText ?? ''); setPartATitle(response.data.partATitle ?? ''); setPartBTitle(response.data.partBTitle ?? ''); setTerms(response.data.terms ?? ''); })
        .catch((cause) => setError(apiErrorMessage(cause, 'Unable to load source quotation.')))
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [route.params.agreementId, route.params.quotationId]);

  const applyStandardTerms = async () => {
    const doFetch = async () => {
      try {
        const response = await apiClient.get<{headerText?:string, partATitle?:string, partBTitle?:string, terms:string}>('/settings/default-terms?documentType=AGREEMENT');
        setHeaderText(response.data.headerText ?? '');
        setPartATitle(response.data.partATitle ?? '');
        setPartBTitle(response.data.partBTitle ?? '');
        setTerms(response.data.terms ?? '');
      } catch (cause) {
        Alert.alert('Error', apiErrorMessage(cause, 'Failed to load standard terms.'));
      }
    };
    if ((terms && terms.trim()) || (headerText && headerText.trim())) {
      Alert.alert('Replace text?', 'This will replace the current header text and terms with the standard ones. Continue?', [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Replace', style: 'destructive', onPress: doFetch },
      ]);
    } else {
      doFetch();
    }
  };

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
        headerText: headerText.trim() || null,
        partATitle: partATitle.trim() || null,
        partBTitle: partBTitle.trim() || null,
        notes: notes.trim() || null,
        terms: terms.trim() || null,
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

  const shareSignedDocument = async () => {
    if (!agreement) return;
    setWorking('shareSigned');
    try {
      await shareServerPdf(
        `/agreements/${agreement.id}/signed-document`,
        agreement.signedFilename ?? `signed-agreement-${agreement.agreementNumber}.pdf`,
        'Share signed agreement',
      );
    } catch (cause) {
      Alert.alert('Document unavailable', apiErrorMessage(cause, 'Unable to download and share the signed agreement.'));
    } finally {
      setWorking('');
    }
  };

  const pickAndUploadSignedDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ['application/pdf', 'image/jpeg', 'image/png'],
        copyToCacheDirectory: true,
      });

      if (result.canceled) return;
      if (result.assets && result.assets.length > 0) {
        const file = result.assets[0];
        setWorking('upload');
        const formData = new FormData();
        formData.append('file', {
          uri: file.uri,
          name: file.name,
          type: file.mimeType ?? 'application/pdf',
        } as any);

        const response = await apiClient.post<Agreement>(`/agreements/${agreement!.id}/signed-document`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        setAgreement(response.data);
        Alert.alert('Success', 'Signed agreement uploaded.');
      }
    } catch (err) {
      Alert.alert('Error', apiErrorMessage(err, 'Failed to upload signed document.'));
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
          <View style={{flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4, marginTop: 8}}>
            <Text style={styles.label}>Header Intro</Text>
            <Pressable onPress={applyStandardTerms} hitSlop={12}>
              <Text style={{color: colors.primary, fontWeight: '700', fontSize: 13}}>Use Standard Text</Text>
            </Pressable>
          </View>
          <Field label="" value={headerText} onChangeText={setHeaderText} multiline />
          <Field label="Part A Title (optional)" value={partATitle} onChangeText={setPartATitle} />
          <Field label="Part B Title (optional)" value={partBTitle} onChangeText={setPartBTitle} />
          <Field label="Terms" value={terms} onChangeText={setTerms} multiline />
          <Text style={styles.fieldHelp}>Use the standard terms, edit them manually, or leave the field blank to generate the document without terms.</Text>
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
        <Step number="3" title="Signed document uploaded" done={!!agreement.signedDocumentAttachmentId} />
        <Step number="4" title="Ready for review" done={agreement.status !== 'DRAFT'} />
        <Step number="5" title="Agreement active" done={agreement.status === 'ACTIVE'} />
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
      {generated && (
        <AppButton
          title={agreement.signedDocumentAttachmentId ? "Re-upload signed agreement" : "Upload signed agreement"}
          variant="secondary"
          onPress={pickAndUploadSignedDocument}
          loading={working === 'upload'}
          icon={<Ionicons name="cloud-upload-outline" size={20} color={colors.primary} />}
        />
      )}
      {agreement.signedDocumentAttachmentId ? (
        <AppButton
          title="Share signed PDF"
          variant="secondary"
          onPress={shareSignedDocument}
          loading={working === 'shareSigned'}
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
  label: { color: colors.ink, fontSize: 14, fontWeight: '700' },
  fieldHelp: { color: colors.muted, fontSize: 11, marginTop: -9 },
});
