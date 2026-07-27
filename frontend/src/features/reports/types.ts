export type ExportFormat = 'PDF' | 'EXCEL' | 'CSV';

export type ReportCategory =
  | 'Inventory'
  | 'Sites and Parties'
  | 'Operations'
  | 'Commercial'
  | 'Financial'
  | 'GST'
  | 'Audit';

export interface ReportDefinition {
  reportType: string;
  name: string;
  category: ReportCategory;
  formats: ExportFormat[];
  roles: string[];
  gstPreparation: boolean;
  description: string;
}

export interface ReportFilter {
  startDate?: string;
  endDate?: string;
  partyId?: number;
  siteId?: number;
  agreementId?: number;
  itemId?: number;
  categoryId?: number;
  status?: string;
  documentNumber?: string;
  user?: string;
  month?: string;
  page?: number;
  size?: number;
}

export type ReportCell = string | number | boolean | null;
export type ReportRow = Record<string, ReportCell>;

export interface ReportPreview {
  reportType: string;
  columns: string[];
  rows: ReportRow[];
  totals: Record<string, number>;
  page: number;
  size: number;
  totalElements: number;
  warning?: string;
}

export interface ReportExport {
  id: number;
  reportType: string;
  format: ExportFormat;
  filename: string;
  status: string;
  generatedAt: string;
}

export interface ExportHistory {
  id: number;
  reportType: string;
  format: ExportFormat;
  filename?: string;
  status: string;
  errorMessage?: string;
  generatedBy: string;
  generatedAt: string;
  fileSize?: number;
}

export interface SavedReportFilter {
  id: number;
  name: string;
  reportType: string;
  filters: ReportFilter;
  ownerUsername: string;
  shared: boolean;
  createdAt: string;
  updatedAt: string;
}
