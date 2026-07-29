export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type User = {
  id: number;
  fullName: string;
  username: string;
  email: string;
  active: boolean;
  roles: string[];
  version: number;
};

export type DashboardOverview = {
  stockSummary: {
    godownAvailable: number;
    materialAtSites: number;
    damaged: number;
    lost: number;
    currentAccountableStock: number;
  };
  movementSummary: {
    issuedToday: number;
    receivedToday: number;
    issuedInPeriod: number;
    receivedInPeriod: number;
  };
  orderSummary: {
    openSiteOrders: number;
    partiallyFulfilledSiteOrders: number;
  };
  agreementSummary: {
    activeAgreements: number;
    expiringSoon: number;
  };
  exceptionSummary: {
    lowStockMaterials: number;
    overdueInvoices: number;
    extraReturnsAwaitingApproval: number;
  };
  attentionItems: {
    key: string;
    severity: string;
    title: string;
    description: string;
    count: number;
  }[];
  generatedAt: string;
};

export type StockBalance = {
  itemId: number;
  itemCode: string;
  itemName: string;
  categoryName: string;
  unit: string;
  availableQuantity: number;
  issuedQuantity: number;
  hiredQuantity: number;
  lostQuantity: number;
  scrappedQuantity: number;
  minimumStock: number;
  belowMinimum: boolean;
  updatedAt: string;
};

export type OrderItem = {
  id: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  unit: string;
  orderedQuantity: number;
  issuedQuantity: number;
  remainingQuantity: number;
};

export type SiteOrder = {
  id: number;
  orderNumber: string;
  agreementId: number;
  agreementNumber: string;
  partyId: number;
  partyName: string;
  siteId: number;
  siteName: string;
  orderDate: string;
  status: 'DRAFT' | 'CONFIRMED' | 'PARTIALLY_FULFILLED' | 'FULFILLED' | 'CANCELLED';
  notes?: string;
  items: OrderItem[];
  version: number;
};

export type AgreementItem = {
  itemId: number;
  itemCode: string;
  itemName: string;
  unit: string;
  contractedQuantity: number;
};

export type Agreement = {
  id: number;
  agreementNumber: string;
  sourceQuotationId: number;
  sourceQuotationNumber: string;
  partyId: number;
  partyName: string;
  siteId: number;
  siteName: string;
  siteCode: string;
  effectiveDate: string;
  expiryDate?: string;
  securityDeposit: number;
  grandTotal: number;
  generatedDocumentAttachmentId?: number;
  generatedFilename?: string;
  status: 'DRAFT' | 'READY_FOR_REVIEW' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED' | 'CLOSED' | 'CANCELLED';
  items: AgreementItem[];
};

export type Party = {
  id: number;
  legalName: string;
  tradeName?: string;
  gstin?: string;
  pan?: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  address?: string;
  state?: string;
  active: boolean;
  version: number;
};

export type Site = {
  id: number;
  partyId: number;
  partyName: string;
  siteName: string;
  siteCode: string;
  address?: string;
  contactPerson?: string;
  startDate?: string;
  expectedEndDate?: string;
  status: 'ACTIVE' | 'ON_HOLD' | 'DEFAULTER' | 'CLOSED';
  defaulter: boolean;
  version: number;
};

export type QuotationTemplate = {
  id: number;
  templateCode: string;
  name: string;
  defaultTerms?: string;
  defaultNotes?: string;
  active: boolean;
};

export type ItemOption = {
  id: number;
  itemCode: string;
  itemName: string;
  categoryName: string;
  size?: string;
  unit: string;
  active: boolean;
};

export type QuotationItem = {
  id: number;
  itemId: number;
  itemCodeSnapshot: string;
  itemNameSnapshot: string;
  unitSnapshot: string;
  quantity: number;
  rate: number;
  rentalType: string;
  amount: number;
};

export type Quotation = {
  id: number;
  quotationNumber: string;
  quotationTemplateId: number;
  quotationTemplateName: string;
  partyId: number;
  partyName: string;
  siteId: number;
  siteName: string;
  quotationDate: string;
  validUntil: string;
  rentalType: string;
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED' | 'CONVERTED';
  subtotal: number;
  grandTotal: number;
  securityDeposit: number;
  items: QuotationItem[];
  version: number;
};

export type IssuedChallanItem = {
  id: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  unit: string;
  quantity: number;
};

export type IssuedChallan = {
  id: number;
  challanNumber: string;
  siteOrderId: number;
  siteOrderNumber: string;
  siteId: number;
  siteName: string;
  partyId: number;
  partyName: string;
  dispatchDate: string;
  vehicleNumber?: string;
  driverName?: string;
  notes?: string;
  createdBy: string;
  createdAt: string;
  items: IssuedChallanItem[];
};

export type ReceivingChallan = {
  id: number;
  receivingChallanNumber: string;
  agreementId?: number;
  agreementNumber?: string;
  partyId: number;
  partyName: string;
  siteId: number;
  siteName: string;
  receiveDate: string;
  status: 'DRAFT' | 'EXTRA_APPROVAL_REQUIRED' | 'APPROVED_FOR_POSTING' | 'POSTED' | 'CANCELLED';
  vehicleNumber?: string;
  driverName?: string;
  driverPhone?: string;
  sourceType: 'ISSUED_CHALLAN' | 'SITE_PENDING_BALANCE' | 'OPENING_SITE_BALANCE';
  linkedIssuedChallanId?: number;
  linkedIssuedChallanNumber?: string;
  notes?: string;
  createdAt: string;
  items: {
    id: number;
    itemCode: string;
    itemName: string;
    unit: string;
    goodReturnedQuantity: number;
    damagedReturnedQuantity: number;
    lostQuantity: number;
    extraReturnedQuantity: number;
    pendingQuantitySnapshot: number;
  }[];
};

export type SitePendingBalance = {
  id: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  size?: string;
  unit: string;
  pendingQuantity: number;
};
