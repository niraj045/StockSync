import { Ionicons } from '@expo/vector-icons';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, Card, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';
import type { InquiryResponse } from '../types/api';
import { dateLabel } from '../utils/format';

import { ExcelExportButton } from '../components/ExcelExportButton';

type Props = NativeStackScreenProps<RootStackParams, 'Inquiries'>;

export function InquiriesScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const [inquiries, setInquiries] = useState<InquiryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<InquiryResponse[]>('/client-workflow/inquiries');
      setInquiries(response.data);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load inquiries.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => void load(), [load]));

  return (
    <ScrollView
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
    >
      <PageHeader
        eyebrow="Sales workflow"
        title="Inquiries"
        action={
          <View style={styles.headerActions}>
            <ExcelExportButton reportType="INQUIRY_REGISTER" />
            <Pressable
              accessibilityLabel="Create inquiry"
              onPress={() => navigation.navigate('CreateInquiry')}
              style={styles.add}
            >
              <Ionicons name="add" color="#fff" size={26} />
            </Pressable>
          </View>
        }
      />
      <Text style={styles.intro}>Track leads, customer company details and requirements before quoting.</Text>

      {error ? <Text style={styles.error}>{error}</Text> : null}

      {inquiries.length ? inquiries.map((inq) => {
        const companyName = inq.companyName || inq.partyName || (inq.notes?.match(/Company:\s*([^\n]+)/i)?.[1]) || 'Direct Lead';
        return (
          <Card key={inq.id} style={styles.card}>
            <View style={styles.top}>
              <Text style={styles.number}>{inq.inquiryNumber}</Text>
              <View style={styles.topRight}>
                <View style={styles.sourceTag}>
                  <Ionicons
                    name={inq.source === 'JUSTDIAL' ? 'call' : inq.source === 'EMAIL' ? 'mail' : 'globe-outline'}
                    size={12}
                    color={colors.primary}
                  />
                  <Text style={styles.sourceText}>{inq.source}</Text>
                </View>
                <StatusPill value={inq.status} />
              </View>
            </View>

            {/* Company Name Above Contact Name */}
            <View style={styles.namesBlock}>
              <Text style={styles.companyName}>{companyName}</Text>
              <Text style={styles.contactName}>
                <Ionicons name="person-outline" size={14} color={colors.muted} /> {inq.contactName}
              </Text>
            </View>

            <Text style={styles.meta}>Date: {dateLabel(inq.inquiryDate)}</Text>
            {inq.phone || inq.email ? (
              <Text style={styles.meta}>
                {[inq.phone, inq.email].filter(Boolean).join(' · ')}
              </Text>
            ) : null}
            {inq.siteName ? <Text style={styles.meta}>Site: {inq.siteName}</Text> : null}
            
            <View style={styles.summary}>
              <Text style={styles.meta}>Follow-up: {inq.followUpDate ? dateLabel(inq.followUpDate) : 'N/A'}</Text>
              {inq.quotationNumber ? <Text style={styles.meta}>Quote: {inq.quotationNumber}</Text> : null}
            </View>
            
            <AppButton
              title="Edit inquiry"
              variant="secondary"
              onPress={() => navigation.navigate('CreateInquiry', { inquiryId: inq.id })}
            />
          </Card>
        );
      }) : !loading ? (
        <Card>
          <EmptyBlock title="No inquiries" message="Register the first inquiry to begin tracking." />
          <AppButton title="Create inquiry" onPress={() => navigation.navigate('CreateInquiry')} />
        </Card>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: 18, paddingBottom: 36, backgroundColor: colors.canvas, flexGrow: 1 },
  headerActions: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  add: { width: 46, height: 46, borderRadius: 8, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  intro: { color: colors.muted, lineHeight: 21, marginTop: -8, marginBottom: 15 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12, marginBottom: 12 },
  card: { marginBottom: 10, gap: 10 },
  top: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  topRight: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  number: { color: colors.primary, fontFamily: fonts.bold, flexShrink: 1 },
  sourceTag: { flexDirection: 'row', alignItems: 'center', gap: 4, backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, paddingHorizontal: 7, paddingVertical: 3, borderRadius: 6 },
  sourceText: { color: colors.primary, fontSize: 11, fontFamily: fonts.bold },
  namesBlock: { gap: 2 },
  companyName: { color: colors.ink, fontSize: 18, fontFamily: fonts.extraBold },
  contactName: { color: colors.muted, fontSize: 14, fontFamily: fonts.semiBold },
  meta: { color: colors.muted, fontSize: 13 },
  summary: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 10 },
});
