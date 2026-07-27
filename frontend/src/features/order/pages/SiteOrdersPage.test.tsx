import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { SiteOrder } from '../types';
import { OrderActions } from './SiteOrdersPage';

const order = (status: 'DRAFT' | 'CONFIRMED' | 'PARTIALLY_FULFILLED' | 'FULFILLED' | 'CANCELLED'): SiteOrder => ({
  id: 1,
  orderNumber: 'ORD/2026-27/0001',
  agreementId: 2,
  agreementNumber: 'AGR/2026-27/0001',
  partyId: 10,
  partyName: 'Party Name',
  siteId: 20,
  siteName: 'Site Name',
  orderDate: '2026-07-27',
  status,
  items: [],
  version: 0,
  createdAt: '2026-07-27T10:00:00Z',
  updatedAt: '2026-07-27T10:00:00Z',
});

const callbacks = () => ({
  onView: vi.fn(),
  onEdit: vi.fn(),
  onConfirm: vi.fn(),
  onCancel: vi.fn(),
  onPdf: vi.fn(),
});

describe('order workflow permissions', () => {
  it('OPERATIONS can write, see edit, confirm and cancel actions on draft', () => {
    render(<OrderActions order={order('DRAFT')} canWrite={true} {...callbacks()} />);
    expect(screen.getByRole('button', { name: 'Edit' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Confirm' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
  });

  it('OPERATIONS cannot edit or confirm but can cancel a confirmed order', () => {
    render(<OrderActions order={order('CONFIRMED')} canWrite={true} {...callbacks()} />);
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
  });

  it('VIEWER sees only view and PDF download actions', () => {
    render(<OrderActions order={order('DRAFT')} canWrite={false} {...callbacks()} />);
    expect(screen.getByRole('button', { name: 'View' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();
  });
});
