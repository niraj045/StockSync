import { useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { AppButton, Card, DateField, LoadingBlock } from '../components/ui';
import { colors, fonts } from '../theme';
import type { ReportExport, ReportPreview } from '../types/api';
import { quantity } from '../utils/format';
import { shareServerFile } from '../utils/sharePdf';

type ExportFormat = 'EXCEL' | 'CSV';
const currentMonth = () => new Date().toISOString().slice(0, 7);

export function GstExportScreen() {
  const [month, setMonth] = useState(currentMonth());
  const [format, setFormat] = useState<ExportFormat>('EXCEL');
  const [preview, setPreview] = useState<ReportPreview | null>(null);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);

  const validMonth = /^\d{4}-(0[1-9]|1[0-2])$/.test(month);
  const loadPreview = async () => {
    if (!validMonth) return;
    setLoading(true);
    try {
      const response = await apiClient.post<ReportPreview>('/reports/GST_TAX_SUMMARY/preview', {
        month, page: 0, size: 100,
      });
      setPreview(response.data);
    } catch (cause) {
      Alert.alert('GST summary unavailable', apiErrorMessage(cause, 'Unable to load this month.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void loadPreview(); }, []);

  const exportMonth = async () => {
    if (!validMonth) {
      Alert.alert('Check month', 'Enter the month in YYYY-MM format.');
      return;
    }
    setExporting(true);
    try {
      const response = await apiClient.post<ReportExport>('/reports/GSTR1_PREPARATION/export', {
        filters: { month, page: 0, size: 10000 }, format,
      });
      const mime = format === 'EXCEL'
        ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        : 'text/csv';
      await shareServerFile(`/reports/exports/${response.data.id}/download`, response.data.filename, mime, `Share ${month} GST export`);
    } catch (cause) {
      Alert.alert('GST export failed', apiErrorMessage(cause, 'Unable to generate the monthly GST file.'));
    } finally {
      setExporting(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <Text style={styles.heading}>Consolidated monthly GST</Text>
      <Text style={styles.intro}>One accountant-ready file containing all finalized invoices for the selected month. This is separate from individual quotation and challan PDFs.</Text>
      <Card style={styles.controls}>
        <DateField label="Tax month" value={month} onChange={setMonth} selection="month" />
        <View style={styles.formats}>
          {(['EXCEL', 'CSV'] as ExportFormat[]).map((value) => (
            <Pressable key={value} onPress={() => setFormat(value)} style={[styles.format, format === value && styles.formatActive]}>
              <Text style={[styles.formatText, format === value && styles.formatTextActive]}>{value === 'EXCEL' ? 'Excel' : 'CSV'}</Text>
            </Pressable>
          ))}
        </View>
        <AppButton title="Refresh monthly summary" variant="secondary" onPress={() => void loadPreview()} loading={loading} disabled={!validMonth} />
      </Card>

      {loading ? <LoadingBlock /> : preview ? <Card style={styles.summary}>
        <Text style={styles.summaryTitle}>Tax summary</Text>
        <Text style={styles.invoiceCount}>{preview.totalElements} tax group{preview.totalElements === 1 ? '' : 's'}</Text>
        {Object.entries(preview.totals ?? {}).map(([key, value]) => (
          <View key={key} style={styles.totalRow}>
            <Text style={styles.totalLabel}>{key.replaceAll('_', ' ')}</Text>
            <Text style={styles.totalValue}>INR {quantity(value)}</Text>
          </View>
        ))}
        {preview.warning ? <Text style={styles.warning}>{preview.warning}</Text> : null}
      </Card> : null}

      <AppButton title={`Generate complete ${format === 'EXCEL' ? 'Excel' : 'CSV'} export`} onPress={() => void exportMonth()} loading={exporting} disabled={!validMonth || loading} />
      <Text style={styles.disclaimer}>Preparation export only. Review it with the company accountant before filing GST returns.</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: 18, paddingBottom: 36, gap: 14, backgroundColor: colors.canvas, flexGrow: 1 },
  heading: { color: colors.ink, fontSize: 25, fontFamily: fonts.black },
  intro: { color: colors.muted, lineHeight: 21 },
  controls: { gap: 13 },
  error: { color: colors.red, fontSize: 12 },
  formats: { flexDirection: 'row', borderWidth: 1, borderColor: colors.line, borderRadius: 8, padding: 3 },
  format: { flex: 1, minHeight: 42, alignItems: 'center', justifyContent: 'center', borderRadius: 6 },
  formatActive: { backgroundColor: colors.primary },
  formatText: { color: colors.muted, fontFamily: fonts.bold },
  formatTextActive: { color: '#fff' },
  summary: { gap: 10 },
  summaryTitle: { color: colors.ink, fontSize: 18, fontFamily: fonts.extraBold },
  invoiceCount: { color: colors.primary, fontFamily: fonts.bold },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', gap: 12, borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 9 },
  totalLabel: { color: colors.muted, textTransform: 'capitalize', flex: 1 },
  totalValue: { color: colors.ink, fontFamily: fonts.bold },
  warning: { color: colors.amber, backgroundColor: colors.amberSoft, padding: 10, borderRadius: 7, lineHeight: 18 },
  disclaimer: { color: colors.muted, fontSize: 11, lineHeight: 17, textAlign: 'center' },
});
