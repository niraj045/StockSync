export type OrderItem = {
  id: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  unit: string;
  orderedQuantity: number;
  issuedQuantity: number;
  remainingQuantity: number;
  version: number;
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
  createdAt: string;
  updatedAt: string;
};
