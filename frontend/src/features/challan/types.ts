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

export type ReceivingChallanItem = {
  id?: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  size?: string;
  unit: string;
  linkedIssuedChallanItemId?: number;
  openingImportTransactionId?: number;
  pendingQuantitySnapshot: number;
  goodReturnedQuantity: number;
  damagedReturnedQuantity: number;
  lostQuantity: number;
  extraReturnedQuantity: number;
  exchangedFromItemId?: number;
  exchangedFromItemCode?: string;
  exchangedToItemId?: number;
  exchangedToItemCode?: string;
  exchangedQuantity: number;
  weightPerPieceSnapshot?: number;
  goodReturnedWeight?: number;
  damagedWeight?: number;
  lostWeight?: number;
  notes?: string;
  sequence?: number;
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
  linkedIssuedChallanId?: number;
  linkedIssuedChallanNumber?: string;
  receiveDate: string;
  status: string;
  vehicleNumber?: string;
  driverName?: string;
  driverPhone?: string;
  transporterId?: number;
  sourceType: string;
  notes?: string;
  postedAt?: string;
  postedBy?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  cancellationReason?: string;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
  items: ReceivingChallanItem[];
};

export type SiteStockBalance = {
  id: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  size?: string;
  unit: string;
  pendingQuantity: number;
};
