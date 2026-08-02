import { Ionicons } from '@expo/vector-icons';
import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps, useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import type { MainTabParams, RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import type { Agreement, Page, Party, Quotation, Site } from '../types/api';
import { dateLabel, quantity } from '../utils/format';
import { shareServerPdf } from '../utils/sharePdf';

type Props = CompositeScreenProps<BottomTabScreenProps<MainTabParams, 'Sales'>, NativeStackScreenProps<RootStackParams>>;
type Mode = 'customers' | 'quotations' | 'agreements';

export function SalesScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  const canWrite = user?.roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_OPERATIONS') ?? false;
  const [mode, setMode] = useState<Mode>('customers');
  const [parties, setParties] = useState<Party[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [agreements, setAgreements] = useState<Agreement[]>([]);
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState<number | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [partyResponse, siteResponse, quotationResponse, agreementResponse] = await Promise.all([
        apiClient.get<Page<Party>>('/parties', { params: { size: 200, sort: 'id,desc' } }),
        apiClient.get<Page<Site>>('/sites', { params: { size: 300, sort: 'id,desc' } }),
        apiClient.get<Page<Quotation>>('/quotations', { params: { size: 100, sort: 'id,desc' } }),
        apiClient.get<Page<Agreement>>('/agreements', { params: { size: 100, sort: 'id,desc' } }),
      ]);
      setParties(partyResponse.data.content);
      setSites(siteResponse.data.content);
      setQuotations(quotationResponse.data.content);
      setAgreements(agreementResponse.data.content);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load the sales workflow.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => void load(), [load]));

  const transitionQuotation = (quotation: Quotation, action: 'send' | 'approve') => {
    Alert.alert(
      action === 'send' ? 'Send quotation?' : 'Approve quotation?',
      action === 'send'
        ? `${quotation.quotationNumber} will be locked for customer review.`
        : `${quotation.quotationNumber} can then be converted into an agreement.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: action === 'send' ? 'Send' : 'Approve',
          onPress: async () => {
            setWorkingId(quotation.id);
            try {
              const response = await apiClient.post<Quotation>(`/quotations/${quotation.id}/${action}`);
              setQuotations((current) => current.map((row) => row.id === quotation.id ? response.data : row));
            } catch (cause) {
              Alert.alert('Action failed', apiErrorMessage(cause, `Unable to ${action} this quotation.`));
            } finally {
              setWorkingId(null);
            }
          },
        },
      ],
    );
  };

  const shareQuotation = async (quotation: Quotation) => {
    setWorkingId(quotation.id);
    try {
      await shareServerPdf(
        `/quotations/${quotation.id}/pdf`,
        `quotation-${quotation.quotationNumber.replaceAll('/', '-')}.pdf`,
        quotation.quotationTemplateCode === 'STEELFAB_EXACT_HIRE_V1' ? 'Share SteelFab exact PDF' : 'Share quotation PDF',
      );
    } catch (cause) {
      Alert.alert('PDF unavailable', apiErrorMessage(cause, 'Unable to generate and share this quotation.'));
    } finally {
      setWorkingId(null);
    }
  };

  return (
    <ScrollView
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
    >
      <PageHeader
        eyebrow="Customer to contract"
        title="Sales workflow"
        action={(mode === 'customers' ? isAdmin : canWrite) ? (
          <Pressable
            accessibilityLabel={mode === 'customers' ? 'Create customer' : 'Create quotation'}
            onPress={() => navigation.navigate(mode === 'customers' ? 'CreateParty' : 'CreateQuotation')}
            style={styles.add}
          >
            <Ionicons name="add" color="#fff" size={26} />
          </Pressable>
        ) : undefined}
      />
      <Text style={styles.intro}>Prepare every prerequisite for an order and challan directly from the app.</Text>
      <View style={styles.segments}>
        {(['customers', 'quotations', 'agreements'] as Mode[]).map((value) => (
          <Pressable key={value} onPress={() => setMode(value)} style={[styles.segment, mode === value && styles.segmentActive]}>
            <Text style={[styles.segmentText, mode === value && styles.segmentTextActive]}>{value}</Text>
          </Pressable>
        ))}
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}

      {mode === 'customers' ? (
        parties.length ? parties.map((party) => {
          const partySites = sites.filter((site) => site.partyId === party.id);
          return (
            <Card key={party.id} style={styles.card}>
              <View style={styles.top}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.name}>{party.legalName}</Text>
                  {party.tradeName ? <Text style={styles.meta}>{party.tradeName}</Text> : null}
                </View>
                <StatusPill value={party.active ? 'ACTIVE' : 'INACTIVE'} />
              </View>
              <Text style={styles.count}>{partySites.length} site{partySites.length === 1 ? '' : 's'}</Text>
              {partySites.slice(0, 3).map((site) => (
                <View key={site.id} style={styles.siteRow}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.siteName}>{site.siteName}</Text>
                    <Text style={styles.meta}>{site.siteCode}</Text>
                  </View>
                  <StatusPill value={site.status} />
                </View>
              ))}
              {isAdmin ? <AppButton title="Add site" variant="secondary" onPress={() => navigation.navigate('CreateSite', { partyId: party.id })} /> : null}
            </Card>
          );
        }) : !loading ? <Card><EmptyBlock title="No customers" message="Create the first customer, then add their work site." />{isAdmin ? <AppButton title="Create customer" onPress={() => navigation.navigate('CreateParty')} /> : null}</Card> : null
      ) : null}

      {mode === 'quotations' ? (
        quotations.length ? quotations.map((quotation) => (
          <Card key={quotation.id} style={styles.card}>
            <View style={styles.top}>
              <Text style={styles.number}>{quotation.quotationNumber}</Text>
              <StatusPill value={quotation.status} />
            </View>
            <Text style={styles.name}>{quotation.partyName}</Text>
            <Text style={styles.meta}>{quotation.siteName} | valid to {dateLabel(quotation.validUntil)}</Text>
            <View style={styles.summary}>
              <Text style={styles.meta}>{quotation.items.length} material line{quotation.items.length === 1 ? '' : 's'}</Text>
              <Text style={styles.amount}>INR {quantity(quotation.grandTotal)}</Text>
            </View>
            <AppButton
              title={quotation.quotationTemplateCode === 'STEELFAB_EXACT_HIRE_V1' ? 'Generate SteelFab PDF' : 'Share quotation PDF'}
              variant="secondary"
              loading={workingId === quotation.id}
              onPress={() => void shareQuotation(quotation)}
            />
            {canWrite && quotation.status === 'DRAFT' ? <AppButton title="Send for approval" loading={workingId === quotation.id} onPress={() => transitionQuotation(quotation, 'send')} /> : null}
            {isAdmin && quotation.status === 'SENT' ? <AppButton title="Approve quotation" loading={workingId === quotation.id} onPress={() => transitionQuotation(quotation, 'approve')} /> : null}
            {canWrite && quotation.status === 'APPROVED' ? <AppButton title="Create agreement" onPress={() => navigation.navigate('AgreementFlow', { quotationId: quotation.id })} /> : null}
            {quotation.status === 'CONVERTED' ? (
              <AppButton
                title="Open agreement"
                variant="secondary"
                onPress={() => {
                  const agreement = agreements.find((row) => row.sourceQuotationId === quotation.id);
                  if (agreement) navigation.navigate('AgreementFlow', { agreementId: agreement.id });
                  else Alert.alert('Agreement unavailable', 'Refresh Sales and try again.');
                }}
              />
            ) : null}
          </Card>
        )) : !loading ? <Card><EmptyBlock title="No quotations" message="Create a quotation after setting up the customer and site." />{canWrite ? <AppButton title="Create quotation" onPress={() => navigation.navigate('CreateQuotation')} /> : null}</Card> : null
      ) : null}

      {mode === 'agreements' ? (
        agreements.length ? agreements.map((agreement) => (
          <Card key={agreement.id} style={styles.card}>
            <View style={styles.top}>
              <Text style={styles.number}>{agreement.agreementNumber}</Text>
              <StatusPill value={agreement.status} />
            </View>
            <Text style={styles.name}>{agreement.partyName}</Text>
            <Text style={styles.meta}>{agreement.siteName} | effective {dateLabel(agreement.effectiveDate)}</Text>
            <AppButton title={agreement.status === 'ACTIVE' ? 'Open agreement' : 'Continue setup'} variant="secondary" onPress={() => navigation.navigate('AgreementFlow', { agreementId: agreement.id })} />
            {canWrite && agreement.status === 'ACTIVE' ? <AppButton title="Create site order" onPress={() => navigation.navigate('CreateOrder', { agreementId: agreement.id })} /> : null}
          </Card>
        )) : !loading ? <Card><EmptyBlock title="No agreements" message="Approve a quotation and convert it into an agreement." /></Card> : null
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: 18, paddingBottom: 36, backgroundColor: colors.canvas, flexGrow: 1 },
  add: { width: 46, height: 46, borderRadius: 8, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  intro: { color: colors.muted, lineHeight: 21, marginTop: -8, marginBottom: 15 },
  segments: { flexDirection: 'row', borderWidth: 1, borderColor: colors.line, borderRadius: 8, backgroundColor: '#fff', padding: 3, marginBottom: 14 },
  segment: { flex: 1, minHeight: 40, alignItems: 'center', justifyContent: 'center', borderRadius: 6 },
  segmentActive: { backgroundColor: colors.primary },
  segmentText: { color: colors.muted, fontSize: 12, fontFamily: fonts.bold, textTransform: 'capitalize' },
  segmentTextActive: { color: '#fff' },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12, marginBottom: 12 },
  card: { marginBottom: 10, gap: 11 },
  top: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  name: { color: colors.ink, fontSize: 17, fontFamily: fonts.bold },
  number: { color: colors.primary, fontFamily: fonts.bold, flexShrink: 1 },
  meta: { color: colors.muted, fontSize: 12, marginTop: 3 },
  count: { color: colors.primaryDark, fontSize: 12, fontFamily: fonts.bold },
  siteRow: { flexDirection: 'row', alignItems: 'center', borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 10 },
  siteName: { color: colors.ink, fontFamily: fonts.semiBold },
  summary: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 10 },
  amount: { color: colors.ink, fontFamily: fonts.black },
});
