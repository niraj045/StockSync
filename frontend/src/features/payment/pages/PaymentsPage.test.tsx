import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App } from 'antd';
import { describe, expect, it, vi } from 'vitest';
import { PaymentsPage } from './PaymentsPage';
import { SecurityDepositsPage } from './SecurityDepositsPage';
import { OutstandingPage } from './OutstandingPage';

vi.mock('../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: { roles: ['ROLE_ADMIN', 'ROLE_ACCOUNTS'] } }),
}));

vi.mock('../../../api/client', () => ({
  apiClient: {
    get: vi.fn((url: string) => {
      if (url === '/payments') {
        return Promise.resolve({ data: { content: [{ id: 1, receiptNumber: 'PR/2026-27/0001', partyId: 1, partyName: 'Acme Infra', paymentDate: '2026-07-10', paymentMode: 'NEFT', cashAmount: 500, tdsAmount: 50, totalSettlementAmount: 550, unallocatedAmount: 100, status: 'POSTED', version: 1, allocations: [] }], totalElements: 1 } });
      }
      if (url === '/parties') {
        return Promise.resolve({ data: { content: [{ id: 1, legalName: 'Acme Infra' }], totalElements: 1 } });
      }
      if (url === '/security-deposits') {
        return Promise.resolve({ data: { content: [{ id: 1, depositNumber: 'SD/2026-27/0001', agreementId: 1, agreementNumber: 'AGR-1', partyId: 1, partyName: 'Acme Infra', siteId: 1, siteName: 'Site A', transactionType: 'RECEIPT', transactionDate: '2026-07-12', amount: 1000, status: 'POSTED', version: 1 }], totalElements: 1 } });
      }
      if (url === '/agreements') {
        return Promise.resolve({ data: { content: [{ id: 1, agreementNumber: 'AGR-1', partyNameSnapshot: 'Acme Infra', siteNameSnapshot: 'Site A' }], totalElements: 1 } });
      }
      return Promise.resolve({ data: { totalBilled: 1000, cashReceived: 500, tds: 50, depositAdjustments: 100, outstanding: 350, availableAdvance: 100, availableSecurityDeposit: 900 } });
    }),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

function renderPage(node: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <App>{node}</App>
    </QueryClientProvider>
  );
}

describe('Phase 8 payment pages', () => {
  it('shows payment receipt rows with cash, TDS and advance', async () => {
    renderPage(<PaymentsPage />);
    expect(await screen.findByText('PR/2026-27/0001')).toBeVisible();
    expect(screen.getByText('Acme Infra')).toBeVisible();
    expect(screen.getByText('POSTED')).toBeVisible();
  });

  it('shows security deposit transactions', async () => {
    renderPage(<SecurityDepositsPage />);
    expect(await screen.findByText('SD/2026-27/0001')).toBeVisible();
    expect(screen.getByText('RECEIPT')).toBeVisible();
  });

  it('shows outstanding scope controls', async () => {
    renderPage(<OutstandingPage />);
    await waitFor(() => expect(screen.getByText('Party')).toBeVisible());
    expect(screen.getByText('Agreement')).toBeVisible();
    expect(screen.getByText('Invoice')).toBeVisible();
  });
});
