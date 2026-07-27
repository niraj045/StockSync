import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { StockLossesPage } from './StockLossesPage';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('../../auth/context/AuthContext', () => ({
  useAuth: () => ({
    user: { fullName: 'Test Admin', roles: ['ROLE_ADMIN'] },
  }),
}));

vi.mock('../../../api/client', () => ({
  apiClient: {
    get: vi.fn(() => Promise.resolve({ data: { content: [], totalElements: 0 } })),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('StockLossesPage', () => {
  it('renders page header and heading', () => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={qc}>
        <StockLossesPage />
      </QueryClientProvider>
    );
    expect(screen.getByText('Stock Losses')).toBeInTheDocument();
    expect(screen.getByText(/Record and reconcile missing/i)).toBeInTheDocument();
  });
});
