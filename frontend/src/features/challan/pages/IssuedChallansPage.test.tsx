import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { IssuedChallan } from '../types';
import { ChallanActions } from './IssuedChallansPage';

const challan: IssuedChallan = {
  id: 1,
  challanNumber: 'IC/2026-27/0001',
  siteOrderId: 10,
  siteOrderNumber: 'ORD/2026-27/0001',
  siteId: 20,
  siteName: 'Site Name',
  partyId: 30,
  partyName: 'Party Name',
  dispatchDate: '2026-07-27',
  vehicleNumber: 'MH-12-AB-1234',
  driverName: 'Rajesh',
  createdBy: 'test',
  createdAt: '2026-07-27T10:00:00Z',
  items: [],
};

const callbacks = () => ({
  onView: vi.fn(),
  onPdf: vi.fn(),
});

describe('challan list actions', () => {
  it('renders View and PDF actions successfully', () => {
    render(<ChallanActions challan={challan} {...callbacks()} />);
    expect(screen.getByRole('button', { name: 'View' })).toBeVisible();
    expect(screen.getByRole('button', { name: /PDF/i })).toBeVisible();
  });
});
