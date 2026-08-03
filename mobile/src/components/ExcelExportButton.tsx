import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { ActivityIndicator, Alert, Pressable, StyleSheet } from 'react-native';
import { apiClient, apiErrorMessage } from '../api/client';
import { colors } from '../theme';
import type { ReportExport } from '../types/api';
import { shareServerFile } from '../utils/sharePdf';

type ReportFilters = Record<string, string | number | undefined>;

export function ExcelExportButton({ reportType, filters = {} }: { reportType: string; filters?: ReportFilters }) {
  const [loading, setLoading] = useState(false);

  const exportExcel = async () => {
    setLoading(true);
    try {
      const response = await apiClient.post<ReportExport>(`/reports/${reportType}/export`, {
        filters,
        format: 'EXCEL',
      });
      await shareServerFile(
        `/reports/exports/${response.data.id}/download`,
        response.data.filename,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'Share SteelFab Excel report',
      );
    } catch (cause) {
      Alert.alert('Excel unavailable', apiErrorMessage(cause, 'Unable to generate and share this Excel report.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Pressable
      accessibilityLabel="Export current list to Excel"
      accessibilityRole="button"
      disabled={loading}
      onPress={() => void exportExcel()}
      style={({ pressed }) => [styles.button, pressed && styles.pressed, loading && styles.disabled]}
    >
      {loading ? <ActivityIndicator color={colors.primary} size="small" /> : (
        <Ionicons name="document-text-outline" color={colors.primary} size={22} />
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    width: 46,
    height: 46,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: '#fff',
  },
  pressed: { opacity: 0.7 },
  disabled: { opacity: 0.55 },
});
