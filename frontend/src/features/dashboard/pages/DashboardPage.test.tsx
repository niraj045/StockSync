import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { DashboardPage } from './DashboardPage';

const authState = vi.hoisted(() => ({
  navigate: vi.fn(),
  get: vi.fn(),
  user: { id: 1, fullName: 'Ops User', username: 'ops', email: 'ops@test.local', active: true, roles: ['ROLE_OPERATIONS'], version: 0 },
}));

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return { ...actual, useNavigate: () => authState.navigate };
});

vi.mock('../../../api/client', () => ({
  apiClient: { get: authState.get },
}));

vi.mock('../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authState.user, loading: false, login: vi.fn(), logout: vi.fn(), refreshUser: vi.fn() }),
}));

const overview = {
  stockSummary: { godownAvailable: 80, materialAtSites: 40, damaged: 5, lost: 2, scrapped: 1, underRepair: 3, physicalCurrentStock: 125, currentAccountableStock: 127 },
  movementSummary: { issuedToday: 4, receivedToday: 2, issuedInPeriod: 22, receivedInPeriod: 13, siteTransfersInPeriod: 3 },
  orderSummary: { openSiteOrders: 5, partiallyFulfilledSiteOrders: 2, fulfilledSiteOrders: 8 },
  challanSummary: { draftIssuedChallans: 0, draftReceivingChallans: 1, pendingExtraReturnApprovals: 1 },
  agreementSummary: { activeAgreements: 12, expiringSoon: 2, warningDate: '2026-08-27' },
  billingSummary: { draftBillingRuns: 1, draftInvoices: 1, issuedInvoices: 6, totalInvoicedInPeriod: 120000, totalOutstanding: 35000 },
  paymentSummary: { cashReceived: 55000, tds: 5000, depositAdjustments: 1000, availableCustomerAdvance: 2500, availableSecurityDeposits: 8000 },
  exceptionSummary: { lossAwaitingApproval: 1, damageAwaitingAction: 2, materialUnderRepair: 3, repairablePendingDecision: 1, partiallyFulfilledOrders: 2, extraReturnsAwaitingApproval: 1, lowStockMaterials: 4, overdueInvoices: 3, highOutstandingParties: 1, agreementsExpiringSoon: 2 },
  attentionItems: [{ key: 'low-stock', severity: 'warning', title: 'Low-stock materials', description: 'Items below minimum', targetPath: '/inventory?belowMinimum=true', count: 4 }],
  stockByStatus: [{ label: 'Godown', value: 80 }, { label: 'At sites', value: 40 }],
  movementTrend: [{ date: '2026-07-28', issued: 5, received: 2 }],
  topSites: [{ label: 'Site A', value: 40 }],
  outstandingAgeing: [{ label: '1-30', value: 35000 }],
  recentDocuments: [{ type: 'Invoice', id: 9, number: 'INV-1', documentDate: '2026-07-28', status: 'ISSUED', targetPath: '/invoices/9' }],
  recentActivity: [{ id: 1, action: 'LOGIN', username: 'ops', createdAt: '2026-07-28T00:00:00Z', description: 'Signed in' }],
  quickActions: [{ key: 'order', label: 'Create site order', targetPath: '/orders', roleGroup: 'operations' }],
  roleMode: 'OPERATIONS',
  generatedAt: '2026-07-28T00:00:00Z',
};

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('Phase 10 DashboardPage', () => {
  beforeEach(() => {
    authState.navigate.mockClear();
    authState.get.mockResolvedValue({ data: overview });
    authState.user = { ...authState.user, roles: ['ROLE_OPERATIONS'] };
  });

  it('loads dashboard metrics and role-aware quick actions', async () => {
    renderPage();
    expect(await screen.findByText('Godown available')).toBeVisible();
    expect(screen.getByText('Create site order')).toBeVisible();
    expect(screen.getByText('Low-stock materials')).toBeVisible();
    expect(authState.get).toHaveBeenCalledWith('/dashboard/overview', expect.objectContaining({ params: expect.objectContaining({ dateFrom: expect.any(String), dateTo: expect.any(String) }) }));
  });

  it('navigates from attention items', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Low-stock materials' }));
    expect(authState.navigate).toHaveBeenCalledWith('/inventory?belowMinimum=true');
  });

  it('hides create actions for viewer role', async () => {
    authState.user = { ...authState.user, roles: ['ROLE_VIEWER'] };
    authState.get.mockResolvedValue({ data: { ...overview, quickActions: [], roleMode: 'READ_ONLY' } });
    renderPage();
    expect(await screen.findByText('READ ONLY view')).toBeVisible();
    expect(screen.queryByText('Create site order')).not.toBeInTheDocument();
  });
});
