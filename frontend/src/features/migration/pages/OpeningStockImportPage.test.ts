import { describe, expect, it } from 'vitest';
import { openingStockImportPermissions } from './OpeningStockImportPage';

describe('openingStockImportPermissions', () => {
  it('allows administrators to prepare, post, and reverse', () => {
    expect(openingStockImportPermissions(['ROLE_ADMIN'])).toEqual({
      canPrepare: true,
      canPostOrReverse: true,
    });
  });

  it('allows operations to prepare but not post or reverse', () => {
    expect(openingStockImportPermissions(['ROLE_OPERATIONS'])).toEqual({
      canPrepare: true,
      canPostOrReverse: false,
    });
  });

  it.each(['ROLE_ACCOUNTS', 'ROLE_VIEWER'])('keeps %s read-only', (role) => {
    expect(openingStockImportPermissions([role])).toEqual({
      canPrepare: false,
      canPostOrReverse: false,
    });
  });
});
