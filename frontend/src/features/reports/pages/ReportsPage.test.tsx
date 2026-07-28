import { render, screen, waitFor } from '@testing-library/react';
import { fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App } from 'antd';
import { describe, expect, it, vi } from 'vitest';
import { ReportsPage } from './ReportsPage';

const { post } = vi.hoisted(() => ({
  post: vi.fn((url: string) => {
  if (url === '/reports/saved-filters') {
    return Promise.resolve({ data: { id: 11, name: 'Demo preset', reportType: 'GST_SALES_REGISTER', filters: { page: 0, size: 25 }, ownerUsername: 'admin', shared: false, createdAt: '2026-07-28T00:00:00Z', updatedAt: '2026-07-28T00:00:00Z' } });
  }
  if (url.endsWith('/export')) {
    return Promise.resolve({ data: { id: 7, reportType: 'GST_SALES_REGISTER', format: 'EXCEL', filename: 'gst.xlsx', status: 'SUCCESS', generatedAt: '2026-07-28T00:00:00Z' } });
  }
  return Promise.resolve({
    data: {
      reportType: 'GST_SALES_REGISTER',
      columns: ['invoice_number', 'customer_name', 'validation_status', 'taxable_value'],
      rows: [{ invoice_number: 'INV/1', customer_name: 'Acme Infra', validation_status: 'MISSING_OR_INVALID_GSTIN', taxable_value: 1000 }],
      totals: { taxable_value: 1000 },
      page: 0,
      size: 25,
      totalElements: 1,
      warning: 'GST preparation exports are review files only; they do not file a GST return.',
    },
  });
}),
}));

vi.mock('../../../api/client', () => ({
  apiClient: {
    get: vi.fn((url: string) => {
      if (url === '/reports/catalog') {
        return Promise.resolve({
          data: {
            reports: [
              { reportType: 'GST_SALES_REGISTER', name: 'GST Sales Register', category: 'GST', formats: ['EXCEL', 'CSV'], roles: ['ROLE_ACCOUNTS'], gstPreparation: true, description: 'GST preparation' },
            ],
          },
        });
      }
      if (url === '/reports/saved-filters') return Promise.resolve({ data: [] });
      return Promise.resolve({ data: [] });
    }),
    post,
    delete: vi.fn(),
  },
}));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <App><ReportsPage /></App>
    </QueryClientProvider>
  );
}

describe('Phase 9 Report Centre', () => {
  it('previews GST reports, shows warnings and exports', async () => {
    const open = vi.spyOn(window, 'open').mockImplementation(() => null);
    renderPage();
    expect(await screen.findByText('GST Sales Register')).toBeVisible();
    expect(await screen.findByText('INV/1')).toBeVisible();
    expect(screen.getByText('Preparation only')).toBeVisible();
    expect(screen.getByText(/do not file a GST return/i)).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: /excel/i }));
    await waitFor(() => expect(post).toHaveBeenCalledWith('/reports/GST_SALES_REGISTER/export', expect.anything()));
    expect(open).toHaveBeenCalledWith('/api/v1/reports/exports/7/download', '_blank');
    open.mockRestore();
  });

  it('saves report filter presets from the modal OK button', async () => {
    renderPage();
    expect(await screen.findByText('GST Sales Register')).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: /save preset/i }));
    fireEvent.change(screen.getByLabelText(/preset name/i), { target: { value: 'Demo preset' } });
    fireEvent.click(screen.getByRole('button', { name: /^ok$/i }));
    await waitFor(() => expect(post).toHaveBeenCalledWith('/reports/saved-filters', expect.objectContaining({
      name: 'Demo preset',
      reportType: 'GST_SALES_REGISTER',
      shared: false,
    })));
  });
});
