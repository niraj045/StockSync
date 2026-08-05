import type { IssuedChallan, ReceivingChallan } from '../types/api';

export type RootStackParams = {
  Main: undefined;
  Login: undefined;
  Stock: { focus?: 'all' | 'godown' | 'sites' } | undefined;
  GstExport: undefined;
  ExcelReports: undefined;
  Billing: undefined;
  BillingRunDetail: { runId: number };
  CreateParty: undefined;
  CreateSite: { partyId?: number } | undefined;
  CreateQuotation: { partyId?: number; siteId?: number; quotationId?: number } | undefined;
  AgreementFlow: { quotationId?: number; agreementId?: number; mode?: 'edit' | 'view' };
  CreateOrder: { agreementId?: number } | undefined;
  CreateIssuedChallan: { orderId?: number } | undefined;
  CreateReceivingChallan: { issuedChallan?: IssuedChallan } | undefined;
  IssuedChallanDetail: { challan: IssuedChallan };
  ReceivingChallanDetail: { challan: ReceivingChallan };
  Inquiries: undefined;
  CreateInquiry: { inquiryId?: number } | undefined;
  SiteOperations: undefined;
  CreateSiteOperation: { operationId?: number } | undefined;
  OpeningStockImport: undefined;
  SiteLedgerImport: undefined;
  ClientExcelImport: undefined;
  SiteDetail: { siteId: number; siteName?: string };
};

export type MainTabParams = {
  Dashboard: undefined;
  Sales: undefined;
  Orders: undefined;
  Challans: undefined;
  Sites: undefined;
  More: undefined;
};
