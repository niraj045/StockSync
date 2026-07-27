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
