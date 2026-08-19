import { afterEach, describe, expect, it, vi } from 'vitest';
import { reportsApi } from '../../reports/api';
import { exportSiteStockExcel } from './SiteDetailsPage';

describe('site details Excel export', () => {
  afterEach(() => vi.restoreAllMocks());

  it('exports the supported item-level site stock report for the selected site', async () => {
    const exportReport = vi.spyOn(reportsApi, 'export').mockResolvedValue({
      id: 77,
      reportType: 'SITE_PENDING_STOCK',
      format: 'EXCEL',
      filename: 'site-pending-stock.xlsx',
      status: 'SUCCESS',
      generatedAt: '2026-08-19T11:55:00Z',
    });

    await expect(exportSiteStockExcel(3)).resolves.toBe('/api/v1/reports/exports/77/download');
    expect(exportReport).toHaveBeenCalledWith('SITE_PENDING_STOCK', { siteId: 3 }, 'EXCEL');
  });
});
