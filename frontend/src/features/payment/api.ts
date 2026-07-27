import { apiClient } from '../../api/client';
import type {
  DepositSummary,
  AgreementOutstanding,
  EligibleInvoice,
  InvoiceOutstanding,
  OutstandingSummary,
  Page,
  PaymentReceipt,
  SecurityDepositTransaction,
  TdsDetails,
} from './types';

export type AllocationInput = { invoiceId: number; cashAllocated: number; tdsAllocated: number };
export type PaymentInput = {
  partyId: number;
  siteId?: number;
  paymentDate: string;
  paymentMode: string;
  referenceNumber?: string;
  bankName?: string;
  chequeNumber?: string;
  chequeDate?: string;
  cashAmount: number;
  tdsAmount: number;
  notes?: string;
  allocations: AllocationInput[];
  version?: number;
};

export const paymentsApi = {
  list: async (params: Record<string, string | number | undefined>) => (await apiClient.get<Page<PaymentReceipt>>('/payments', { params })).data,
  create: async (input: PaymentInput) => (await apiClient.post<PaymentReceipt>('/payments', input)).data,
  post: async (id: number) => (await apiClient.post<PaymentReceipt>(`/payments/${id}/post`)).data,
  allocate: async (id: number, allocations: AllocationInput[]) => (await apiClient.post<PaymentReceipt>(`/payments/${id}/allocate`, { allocations })).data,
  reverse: async (id: number, reason: string) => (await apiClient.post<PaymentReceipt>(`/payments/${id}/reverse`, { reason })).data,
  eligibleInvoices: async (partyId: number) => (await apiClient.get<EligibleInvoice[]>(`/payments/party/${partyId}/eligible-invoices`)).data,
  advance: async (partyId: number) => (await apiClient.get<{ partyId: number; availableAdvance: number }>(`/payments/party/${partyId}/available-advance`)).data,
  updateTds: async (id: number, input: { deductionDate?: string; section?: string; certificateNumber?: string; certificateDate?: string }) =>
    (await apiClient.put<TdsDetails>(`/payments/${id}/tds-details`, input)).data,
  verifyTds: async (id: number) => (await apiClient.post<TdsDetails>(`/payments/${id}/tds/verify`)).data,
  rejectTds: async (id: number, reason: string) => (await apiClient.post<TdsDetails>(`/payments/${id}/tds/reject`, { reason })).data,
  receiptUrl: (id: number) => `/api/v1/payments/${id}/receipt`,
};

export const depositsApi = {
  list: async (params: Record<string, string | number | undefined>) => (await apiClient.get<Page<SecurityDepositTransaction>>('/security-deposits', { params })).data,
  receipt: async (input: { agreementId: number; transactionDate: string; amount: number; paymentMode: string; referenceNumber?: string; notes?: string }) =>
    (await apiClient.post<SecurityDepositTransaction>('/security-deposits/receipt', input)).data,
  refund: async (input: { agreementId: number; transactionDate: string; amount: number; paymentMode: string; referenceNumber?: string; reason: string }) =>
    (await apiClient.post<SecurityDepositTransaction>('/security-deposits/refund', input)).data,
  adjust: async (input: { agreementId: number; invoiceId: number; transactionDate: string; amount: number; notes?: string }) =>
    (await apiClient.post<SecurityDepositTransaction>('/security-deposits/adjust-to-invoice', input)).data,
  reverse: async (id: number, reason: string) => (await apiClient.post<SecurityDepositTransaction>(`/security-deposits/${id}/reverse`, { reason })).data,
  summary: async (agreementId: number) => (await apiClient.get<DepositSummary>(`/security-deposits/agreement/${agreementId}/summary`)).data,
};

export const outstandingApi = {
  invoice: async (invoiceId: number) => (await apiClient.get<InvoiceOutstanding>(`/outstanding/invoices/${invoiceId}`)).data,
  site: async (siteId: number) => (await apiClient.get<OutstandingSummary>(`/outstanding/sites/${siteId}`)).data,
  party: async (partyId: number) => (await apiClient.get<OutstandingSummary>(`/outstanding/parties/${partyId}`)).data,
  agreement: async (agreementId: number) => (await apiClient.get<AgreementOutstanding>(`/outstanding/agreements/${agreementId}`)).data,
};
