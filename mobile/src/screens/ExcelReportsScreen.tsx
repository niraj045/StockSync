import { useCallback, useEffect, useState } from 'react';
import { RefreshControl, SectionList, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { ExcelExportButton } from '../components/ExcelExportButton';
import { Card, EmptyBlock, PageHeader } from '../components/ui';
import { colors, fonts } from '../theme';

type ReportDefinition = {
  reportType: string;
  name: string;
  category: string;
  formats: string[];
  description: string;
};

export function ExcelReportsScreen() {
  const insets = useSafeAreaInsets();
  const [reports, setReports] = useState<ReportDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<{ reports: ReportDefinition[] }>('/reports/catalog');
      setReports(response.data.reports.filter((report) => report.formats.includes('EXCEL')));
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load Excel reports.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => void load(), [load]);

  const categories = [...new Set(reports.map((report) => report.category))];
  const sections = categories.map((category) => ({
    title: category,
    data: reports.filter((report) => report.category === category),
  }));

  return (
    <SectionList
      sections={sections}
      keyExtractor={(item) => item.reportType}
      contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 28 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
      ListHeaderComponent={<>
        <PageHeader eyebrow="SteelFab records" title="Excel reports" />
        <Text style={styles.intro}>Download the approved workbook format for operational, commercial and financial records.</Text>
        {error ? <Text style={styles.error}>{error}</Text> : null}
      </>}
      ListEmptyComponent={!loading ? <Card><EmptyBlock title="No reports available" message="Your account does not have access to an Excel report." /></Card> : null}
      renderSectionHeader={({ section }) => <Text style={styles.category}>{section.title}</Text>}
      renderItem={({ item }) => (
        <View style={styles.row}>
          <View style={styles.copy}>
            <Text style={styles.name}>{item.name}</Text>
            <Text style={styles.description}>{item.description}</Text>
          </View>
          <ExcelExportButton reportType={item.reportType} />
        </View>
      )}
    />
  );
}

const styles = StyleSheet.create({
  content: { padding: 18, backgroundColor: colors.canvas, flexGrow: 1 },
  intro: { color: colors.muted, lineHeight: 20, marginBottom: 12 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12 },
  category: { color: colors.primaryDark, fontFamily: fonts.bold, fontSize: 13, marginTop: 18, marginBottom: 7, backgroundColor: colors.canvas },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 14, marginBottom: 8, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 8 },
  copy: { flex: 1 },
  name: { color: colors.ink, fontFamily: fonts.bold, fontSize: 15 },
  description: { color: colors.muted, fontSize: 11, lineHeight: 16, marginTop: 4 },
});
