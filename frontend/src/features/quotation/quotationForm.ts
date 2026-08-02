import axios from 'axios';
import type { NamePath } from 'antd/es/form/interface';
import type { ExactHireFields, Option, QuotationItem } from './types';

export type QuotationEditor = {
  quotationTemplateId: number; partyId: number; siteId: number; quotationDate: string; validUntil: string;
  rentalType: string; discountType: string; discountValue: number; cgstRate: number; sgstRate: number; igstRate: number;
  transportCharge: number; loadingCharge: number; unloadingCharge: number; otherCharge: number; roundOff: number;
  securityDeposit: number; terms?: string; notes?: string; items: QuotationItem[]; exactHire?: ExactHireFields; version?: number;
};
export type FormError = { name: NamePath; errors: string[] };

export function quotationPermissions(roles: string[]) {
  const isAdmin = roles.includes('ROLE_ADMIN');
  return { isAdmin, canWrite: isAdmin || roles.includes('ROLE_OPERATIONS') };
}
export function sitesForParty(sites: Option[], partyId?: number) {
  return sites.filter((site) => site.partyId === partyId && site.status !== 'CLOSED');
}
export function requiresUnsavedConfirmation(touched: boolean) {
  return touched;
}

export function validateQuotationEditor(values: QuotationEditor, sites: Option[]): FormError[] {
  const errors: FormError[] = [];
  const site = sites.find((candidate) => candidate.id === values.siteId);
  if (!site || site.partyId !== values.partyId) errors.push({ name: 'siteId', errors: ['Select a site belonging to the selected party'] });
  if (values.validUntil < values.quotationDate) errors.push({ name: 'validUntil', errors: ['Valid-until cannot be before quotation date'] });
  if (values.discountType === 'PERCENTAGE' && (values.discountValue < 0 || values.discountValue > 100)) errors.push({ name: 'discountValue', errors: ['Percentage discount must be between 0 and 100'] });
  if (values.igstRate > 0 && (values.cgstRate > 0 || values.sgstRate > 0)) errors.push({ name: 'igstRate', errors: ['IGST cannot be combined with CGST or SGST'] });
  values.items.forEach((item, index) => {
    if (!(item.quantity > 0)) errors.push({ name: ['items', index, 'quantity'], errors: ['Quantity must be greater than zero'] });
    if (item.rate < 0) errors.push({ name: ['items', index, 'rate'], errors: ['Rate cannot be negative'] });
  });
  if (values.exactHire) {
    if (values.items.length !== 7) errors.push({ name: 'items', errors: ['The exact SteelFab template requires all seven material rows'] });
    values.items.forEach((item, index) => {
      if (!(Number(item.requiredQuantity) >= 0)) errors.push({ name: ['items', index, 'requiredQuantity'], errors: ['Required quantity is required'] });
      if (!(Number(item.hireMonths) > 0)) errors.push({ name: ['items', index, 'hireMonths'], errors: ['Hire months must be greater than zero'] });
      if (!(Number(item.replacementRate) >= 0)) errors.push({ name: ['items', index, 'replacementRate'], errors: ['Replacement rate is required'] });
    });
  }
  return errors;
}

type ApiError = { code?: string; fieldErrors?: { field: string; message: string }[] };
export function apiFormErrors(error: unknown): FormError[] {
  if (!axios.isAxiosError<ApiError>(error)) return [];
  return (error.response?.data?.fieldErrors ?? []).map(({ field, message }) => ({
    name: field.split(/\.|\[|\]/).filter(Boolean).map((part) => /^\d+$/.test(part) ? Number(part) : part),
    errors: [message],
  }));
}
export function apiErrorCode(error: unknown) {
  return axios.isAxiosError<ApiError>(error) ? error.response?.data?.code : undefined;
}
