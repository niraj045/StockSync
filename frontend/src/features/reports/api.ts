import { apiClient } from '../../api/client';
import type {
  ExportFormat,
  ExportHistory,
  ReportDefinition,
  ReportExport,
  ReportFilter,
  ReportPreview,
  SavedReportFilter,
} from './types';

export const reportsApi = {
  catalog: async () => (await apiClient.get<{ reports: ReportDefinition[] }>('/reports/catalog')).data.reports,
  preview: async (reportType: string, filters: ReportFilter) =>
    (await apiClient.post<ReportPreview>(`/reports/${reportType}/preview`, filters)).data,
  export: async (reportType: string, filters: ReportFilter, format: ExportFormat) =>
    (await apiClient.post<ReportExport>(`/reports/${reportType}/export`, { filters, format })).data,
  history: async () => (await apiClient.get<ExportHistory[]>('/reports/exports')).data,
  savedFilters: async () => (await apiClient.get<SavedReportFilter[]>('/reports/saved-filters')).data,
  createSavedFilter: async (input: { name: string; reportType: string; filters: ReportFilter; shared: boolean }) =>
    (await apiClient.post<SavedReportFilter>('/reports/saved-filters', input)).data,
  deleteSavedFilter: async (id: number) => {
    await apiClient.delete(`/reports/saved-filters/${id}`);
  },
  downloadUrl: (id: number) => `/api/v1/reports/exports/${id}/download`,
};
