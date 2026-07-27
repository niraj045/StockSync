export type Page<T> = { content: T[]; totalElements: number };

export type PaymentAllocation = {
  id: number;
  invoiceId: number;
  invoiceNumber: string;
  cashAllocated: number;
  tdsAllocated: number;
  totalAllocated: number;
};

export type TdsDetails = {
  id: number;
  tdsAmount: number;
  deductionDate?: string;
  section?: string;
  certificateNumber?: string;
  certificateDate?: string;
  certificateAttachmentId?: number;
  verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
  rejectionReason?: string;
  version: number;
};

export type PaymentReceipt = {
  id: number;
  receiptNumber: string;
  partyId: number;
  partyName: string;
  siteId?: number;
  siteName?: string;
  paymentDate: string;
  paymentMode: string;
  referenceNumber?: string;
  bankName?: string;
  chequeNumber?: string;
  chequeDate?: string;
  cashAmount: number;
  tdsAmount: number;
  totalSettlementAmount: number;
  unallocatedAmount: number;
  status: 'DRAFT' | 'POSTED' | 'REVERSED' | 'CANCELLED';
  notes?: string;
  attachmentId?: number;
  postedAt?: string;
  postedBy?: string;
  reversedAt?: string;
  reversedBy?: string;
  reversalReason?: string;
  version: number;
  allocations: PaymentAllocation[];
  tdsDetails?: TdsDetails;
};

export type EligibleInvoice = {
  id: number;
  invoiceNumber: string;
  agreementId: number;
  agreementNumber: string;
  siteId: number;
  siteName: string;
  invoiceDate: string;
  dueDate: string;
  grandTotal: number;
  outstandingAmount: number;
  paymentStatus: 'UNPAID' | 'PARTIALLY_PAID' | 'PAID';
};

export type SecurityDepositTransaction = {
  id: number;
  depositNumber: string;
  agreementId: number;
  agreementNumber: string;
  partyId: number;
  partyName: string;
  siteId: number;
  siteName: string;
  transactionType: 'RECEIPT' | 'REFUND' | 'ADJUSTMENT_TO_INVOICE' | 'REVERSAL';
  transactionDate: string;
  amount: number;
  paymentMode?: string;
  referenceNumber?: string;
  relatedInvoiceId?: number;
  relatedInvoiceNumber?: string;
  status: 'POSTED' | 'REVERSED';
  notes?: string;
  version: number;
};

export type DepositSummary = {
  agreementId: number;
  agreementNumber: string;
  requiredDeposit: number;
  received: number;
  adjusted: number;
  refunded: number;
  available: number;
  shortfallOrExcess: number;
};

export type OutstandingSummary = {
  totalBilled: number;
  cashReceived: number;
  tds: number;
  depositAdjustments: number;
  outstanding: number;
  availableAdvance: number;
  availableSecurityDeposit: number;
};

export type InvoiceOutstanding = {
  invoiceId: number;
  invoiceNumber: string;
  invoiceTotal: number;
  cashAllocated: number;
  tdsAllocated: number;
  depositAdjusted: number;
  outstanding: number;
  paymentStatus: 'UNPAID' | 'PARTIALLY_PAID' | 'PAID';
};

export type AgreementOutstanding = {
  totalInvoiced: number;
  totalSettled: number;
  outstanding: number;
  requiredDeposit: number;
  availableDeposit: number;
};
