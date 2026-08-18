import { fireEvent, render, screen } from '@testing-library/react';
import { Form } from 'antd';
import { describe, expect, it, vi } from 'vitest';
import type { Quotation } from '../types';
import { apiErrorCode, apiFormErrors, quotationPermissions, requiresUnsavedConfirmation, sitesForParty, validateQuotationEditor, type QuotationEditor } from '../quotationForm';
import { ExactHireItemRows, QuotationActionButtons, QuotationItemRows, exactDefaultItems } from './QuotationsPage';

const quotation = (status: string): Quotation => ({
  id: 7, quotationNumber: 'QT/2026-27/0007', quotationTemplateId: 1, quotationTemplateName: 'Standard',
  partyId: 10, partyName: 'Snapshot Party', siteId: 20, siteName: 'Snapshot Site',
  quotationDate: '2026-07-26', validUntil: '2026-08-26', rentalType: 'PER_PIECE_PER_DAY', status,
  subtotal: 100, discountType: 'NONE', discountValue: 0, discountAmount: 0, taxableAmount: 100,
  cgstRate: 9, cgstAmount: 9, sgstRate: 9, sgstAmount: 9, igstRate: 0, igstAmount: 0, totalTax: 18,
  transportCharge: 0, loadingCharge: 0, unloadingCharge: 0, otherCharge: 0, roundOff: 0,
  grandTotal: 118, securityDeposit: 0, items: [], version: 0,
});
const editor = (changes: Partial<QuotationEditor> = {}): QuotationEditor => ({
  quotationTemplateId: 1, partyId: 10, siteId: 20, quotationDate: '2026-07-26', validUntil: '2026-08-26',
  rentalType: 'PER_PIECE_PER_DAY', discountType: 'NONE', discountValue: 0, cgstRate: 9, sgstRate: 9, igstRate: 0,
  transportCharge: 0, loadingCharge: 0, unloadingCharge: 0, otherCharge: 0, roundOff: 0, securityDeposit: 0,
  items: [{ itemId: 1, quantity: 1, rate: 10, rentalType: 'PER_PIECE_PER_DAY' }], ...changes,
});
const sites = [{ id: 20, partyId: 10, siteName: 'Own', status: 'ACTIVE' }, { id: 21, partyId: 11, siteName: 'Other', status: 'ACTIVE' }];

function Actions({ roles, status = 'DRAFT', onAction = vi.fn(), onPdf = vi.fn() }:{
  roles:string[];
  status?:string;
  onAction?:(name: string, needsReason?: boolean) => void;
  onPdf?:() => void;
}) {
  const permission = quotationPermissions(roles);
  return <QuotationActionButtons q={quotation(status)} {...permission} onView={vi.fn()} onEdit={vi.fn()} onAction={onAction} onPdf={onPdf}/>;
}

describe('quotation authorization and actions', () => {
  it('keeps ADMIN quotation actions available', () => {
    const { rerender } = render(<Actions roles={['ROLE_ADMIN']} />);
    expect(screen.getByRole('button', { name: 'Edit' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Send' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Clone' })).toBeVisible();
    rerender(<Actions roles={['ROLE_ADMIN']} status="SENT" />);
    ['Approve', 'Reject', 'Cancel'].forEach((name) => expect(screen.getByRole('button', { name })).toBeVisible());
  });
  it('allows OPERATIONS to edit drafts, send and clone but not approve', () => {
    render(<Actions roles={['ROLE_OPERATIONS']} />);
    ['Edit', 'Send', 'Clone'].forEach((name) => expect(screen.getByRole('button', { name })).toBeVisible());
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument();
  });
  it.each(['ROLE_ACCOUNTS', 'ROLE_VIEWER'])('keeps %s read-only with PDF', (role) => {
    render(<Actions roles={[role]} />);
    expect(screen.getByRole('button', { name: 'View' })).toBeVisible();
    expect(screen.getByRole('button', { name: /PDF/ })).toBeVisible();
    ['Edit', 'Send', 'Clone', 'Cancel'].forEach((name) => expect(screen.queryByRole('button', { name })).not.toBeInTheDocument());
  });
  it.each([['Send','send',false],['Approve','approve',false],['Reject','reject',true],['Cancel','cancel',true]])('%s delegates confirmation requirements', (button, action, reason) => {
    const callback = vi.fn();
    render(<Actions roles={['ROLE_ADMIN']} status={button === 'Send' ? 'DRAFT' : 'SENT'} onAction={callback}/>);
    fireEvent.click(screen.getByRole('button', { name: button }));
    expect(callback).toHaveBeenCalledWith(...(reason ? [action, true] : [action]));
  });
  it('clone delegates the mutation that refreshes the quotation list', () => {
    const callback = vi.fn(); render(<Actions roles={['ROLE_OPERATIONS']} onAction={callback}/>);
    fireEvent.click(screen.getByRole('button', { name: 'Clone' })); expect(callback).toHaveBeenCalledWith('clone');
  });
  it('PDF delegates authenticated download', () => {
    const callback = vi.fn(); render(<Actions roles={['ROLE_ACCOUNTS']} onPdf={callback}/>);
    fireEvent.click(screen.getByRole('button', { name: /PDF/ })); expect(callback).toHaveBeenCalledOnce();
  });
  it('offers preview and one-time finalization for an approved exact quotation', () => {
    const preview=vi.fn(),finalize=vi.fn();const exact={...quotation('APPROVED'),quotationTemplateCode:'STEELFAB_EXACT_HIRE_V1'};
    render(<QuotationActionButtons q={exact} isAdmin canWrite onView={vi.fn()} onEdit={vi.fn()} onAction={vi.fn()} onPdf={vi.fn()} onPreview={preview} onFinalize={finalize}/>);
    fireEvent.click(screen.getByRole('button',{name:/Preview/}));fireEvent.click(screen.getByRole('button',{name:/Finalize/}));
    expect(preview).toHaveBeenCalledOnce();expect(finalize).toHaveBeenCalledOnce();
  });
});

describe('quotation editor behavior', () => {
  it('filters sites by selected party and removes closed sites', () => {
    expect(sitesForParty([...sites, { id: 22, partyId: 10, status: 'CLOSED' }], 10).map((site) => site.id)).toEqual([20]);
  });
  it('blocks submission with a site owned by another party', () => {
    expect(validateQuotationEditor(editor({ siteId: 21 }), sites)[0].name).toBe('siteId');
  });
  it('adds and removes dynamic item rows', () => {
    render(<Form initialValues={{ items: [editor().items[0]] }}><QuotationItemRows items={[]}/></Form>);
    expect(screen.getByRole('combobox', { name: 'Search and select quotation material' })).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: 'Add line' }));
    expect(screen.getAllByRole('button', { name: 'Remove' })).toHaveLength(2);
    fireEvent.click(screen.getAllByRole('button', { name: 'Remove' })[1]);
    expect(screen.getAllByRole('button', { name: 'Remove' })).toHaveLength(1);
  });
  it('adds and removes dynamic SteelFab material rows', () => {
    render(<Form initialValues={{items:[]}}><ExactHireItemRows items={[]}/></Form>);
    expect(screen.getByText('No material added')).toBeVisible();
    fireEvent.click(screen.getByRole('button',{name:/Add item/}));
    expect(screen.getByLabelText('Stock item')).toBeVisible();
    fireEvent.click(screen.getByRole('button',{name:'Remove'}));
    expect(screen.getByText('No material added')).toBeVisible();
  });
  it('suggests only SteelFab materials that exist in the item master',()=>{
    const suggested=exactDefaultItems([{id:1,itemCode:'HF-1',itemName:'H Frame'},{id:2,itemCode:'OTHER',itemName:'Adjustable Prop'}]);
    expect(suggested).toHaveLength(1);
    expect(suggested[0].rentalType).toBe('PER_PIECE_PER_MONTH');
  });
  it('validates dynamic exact rows and their commercial fields', () => {
    const result=validateQuotationEditor(editor({exactHire:{gstPercentage:18},items:editor().items}),sites);
    expect(result.some(error=>String(error.name).includes('hireMonths'))).toBe(true);
  });
  it('allows fewer than seven complete SteelFab materials',()=>{
    const item={...editor().items[0],quantity:1,rate:1,requiredQuantity:0,hireMonths:6,replacementRate:0};
    expect(validateQuotationEditor(editor({exactHire:{gstPercentage:18},items:[item]}),sites).filter(error=>error.name==='items')).toEqual([]);
  });
  it('validates quantity greater than zero', () => expect(validateQuotationEditor(editor({ items: [{...editor().items[0], quantity: 0}] }), sites).some((e) => String(e.name).includes('quantity'))).toBe(true));
  it('validates negative rates', () => expect(validateQuotationEditor(editor({ items: [{...editor().items[0], rate: -1}] }), sites).some((e) => String(e.name).includes('rate'))).toBe(true));
  it('validates valid-until date', () => expect(validateQuotationEditor(editor({ validUntil: '2026-07-25' }), sites).some((e) => e.name === 'validUntil')).toBe(true));
  it('validates percentage discount range', () => expect(validateQuotationEditor(editor({ discountType: 'PERCENTAGE', discountValue: 101 }), sites).some((e) => e.name === 'discountValue')).toBe(true));
  it('rejects mixed CGST/SGST and IGST', () => expect(validateQuotationEditor(editor({ igstRate: 18 }), sites).some((e) => e.name === 'igstRate')).toBe(true));
  it('maps backend field errors to nested Ant Design form fields', () => {
    const error = { isAxiosError: true, response: { data: { fieldErrors: [{ field: 'items[0].quantity', message: 'must be greater than 0' }] } } };
    expect(apiFormErrors(error)).toEqual([{ name: ['items', 0, 'quantity'], errors: ['must be greater than 0'] }]);
  });
  it('recognizes optimistic-lock conflicts for the user-facing message', () => {
    expect(apiErrorCode({ isAxiosError: true, response: { data: { code: 'OPTIMISTIC_LOCK_CONFLICT' } } })).toBe('OPTIMISTIC_LOCK_CONFLICT');
  });
  it('warns only when the editor contains unsaved changes', () => {
    expect(requiresUnsavedConfirmation(true)).toBe(true); expect(requiresUnsavedConfirmation(false)).toBe(false);
  });
});
