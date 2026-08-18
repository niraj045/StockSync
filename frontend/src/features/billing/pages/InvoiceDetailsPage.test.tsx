import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App } from 'antd';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it, vi } from 'vitest';
import { apiClient } from '../../../api/client';
import type { Invoice } from '../types';
import { InvoiceDetailsPage } from './InvoiceDetailsPage';

vi.mock('../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: { roles: ['ROLE_ACCOUNTS'] } }),
}));

vi.mock('../../../api/client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}));

const issuedInvoice: Invoice = {
  id: 1,
  invoiceNumber: 'INV/2026-27/0001',
  billingRunId: 1,
  billingRunNumber: 'BR/2026-27/0001',
  agreementId: 1,
  agreementNumberSnapshot: 'AGR/2026-27/0001',
  partyNameSnapshot: 'Acme Infra',
  partyGstinSnapshot: '27AAAAA0000A1Z5',
  partyAddressSnapshot: 'Mumbai',
  partyStateSnapshot: 'Maharashtra',
  siteNameSnapshot: 'Site A',
  siteCodeSnapshot: 'SITE-A',
  siteAddressSnapshot: 'Mumbai',
  invoiceDate: '2026-08-17',
  dueDate: '2026-08-20',
  periodStart: '2026-07-01',
  periodEnd: '2026-07-31',
  status: 'ISSUED',
  subtotal: 1000,
  discountAmount: 0,
  taxableAmount: 1000,
  cgstRate: 9,
  cgstAmount: 90,
  sgstRate: 9,
  sgstAmount: 90,
  igstRate: 0,
  igstAmount: 0,
  totalTax: 180,
  roundOff: 0,
  grandTotal: 1180,
  generatedPdfAttachmentId: 10,
  version: 1,
  items: [],
};

function renderPage() {
  vi.mocked(apiClient.get).mockResolvedValue({ data: issuedInvoice });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <App>
        <MemoryRouter initialEntries={['/invoices/1']}>
          <Routes>
            <Route path="/invoices/:id" element={<InvoiceDetailsPage />} />
          </Routes>
        </MemoryRouter>
      </App>
    </QueryClientProvider>,
  );
}

describe('InvoiceDetailsPage', () => {
  it('allows Accounts users to regenerate an issued invoice PDF', async () => {
    renderPage();

    expect(await screen.findByRole('button', { name: /regenerate pdf/i })).toBeVisible();
    expect(screen.getByRole('button', { name: /download pdf/i })).toBeVisible();
  });
});
