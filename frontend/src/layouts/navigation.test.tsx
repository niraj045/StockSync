import { describe, expect, it } from 'vitest';
import { activeRoute, activeSection, visibleSections } from './navigation';

const labelsFor = (roles: string[]) => visibleSections(roles).map((section) => section.label);

describe('grouped application navigation', () => {
  it('shows the correct sections for each employee role', () => {
    expect(labelsFor(['ROLE_OPERATIONS'])).toEqual([
      'Daily Operations',
      'Sales & Agreements',
      'Stock Management',
      'Customers & Setup',
    ]);
    expect(labelsFor(['ROLE_ACCOUNTS'])).toEqual([
      'Sales & Agreements',
      'Billing & Payments',
      'Customers & Setup',
    ]);
    expect(labelsFor(['ROLE_VIEWER'])).toEqual([
      'Stock Management',
      'Customers & Setup',
    ]);
    expect(labelsFor(['ROLE_ADMIN'])).toEqual([
      'Daily Operations',
      'Sales & Agreements',
      'Stock Management',
      'Billing & Payments',
      'Customers & Setup',
      'Administration',
    ]);
  });

  it('keeps every configured business page in its requested group', () => {
    const admin = visibleSections(['ROLE_ADMIN']);
    expect(admin.flatMap((section) => section.children.map((child) => child.key))).toEqual(
      expect.arrayContaining([
        '/challans/issued',
        '/challans/receiving',
        '/site-transfers',
        '/stock-losses',
        '/stock-damages',
        '/item-exchanges',
        '/quotations',
        '/quotation-templates',
        '/agreements',
        '/orders',
        '/inventory',
        '/items',
        '/opening-stock-imports',
        '/billing-runs',
        '/invoices',
        '/payments',
        '/security-deposits',
        '/outstanding',
        '/parties',
        '/sites',
        '/vendors',
        '/categories',
        '/documents',
        '/users',
        '/audit-logs',
      ]),
    );
  });

  it('resolves nested URLs to the correct active child and section after refresh', () => {
    const sections = visibleSections(['ROLE_ADMIN']);
    expect(activeRoute('/invoices/42')).toBe('/invoices');
    expect(activeSection('/invoices/42', sections)).toBe('billing-payments');
    expect(activeRoute('/quotations/8/edit')).toBe('/quotations');
    expect(activeSection('/challans/receiving', sections)).toBe('daily-operations');
  });
});
