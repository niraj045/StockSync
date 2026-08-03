import type { IssuedChallan, ReceivingChallan } from '../types/api';

export type RootStackParams = {
  Main: undefined;
  Login: undefined;
  Stock: { focus?: 'all' | 'godown' | 'sites' } | undefined;
  GstExport: undefined;
  CreateParty: undefined;
  CreateSite: { partyId?: number } | undefined;
  CreateQuotation: { partyId?: number; siteId?: number } | undefined;
  AgreementFlow: { quotationId?: number; agreementId?: number };
  CreateOrder: { agreementId?: number } | undefined;
  CreateIssuedChallan: { orderId?: number } | undefined;
  CreateReceivingChallan: { issuedChallan?: IssuedChallan } | undefined;
  IssuedChallanDetail: { challan: IssuedChallan };
  ReceivingChallanDetail: { challan: ReceivingChallan };
};

export type MainTabParams = {
  Dashboard: undefined;
  Sales: undefined;
  Orders: undefined;
  Challans: undefined;
  More: undefined;
};
