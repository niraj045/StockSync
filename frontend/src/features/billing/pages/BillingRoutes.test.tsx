import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it, vi } from 'vitest';
import { ProtectedRoute } from '../../auth/components/ProtectedRoute';

vi.mock('../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: null, loading: false }),
}));

describe('billing route protection', () => {
  it('redirects unauthenticated billing run routes to login', () => {
    render(
      <MemoryRouter initialEntries={['/billing-runs']}>
        <Routes>
          <Route path="/login" element={<div>Login required</div>} />
          <Route path="/billing-runs" element={<ProtectedRoute><div>Billing runs</div></ProtectedRoute>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Login required')).toBeVisible();
    expect(screen.queryByText('Billing runs')).not.toBeInTheDocument();
  });

  it('redirects unauthenticated invoice routes to login', () => {
    render(
      <MemoryRouter initialEntries={['/invoices']}>
        <Routes>
          <Route path="/login" element={<div>Login required</div>} />
          <Route path="/invoices" element={<ProtectedRoute><div>Invoices</div></ProtectedRoute>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Login required')).toBeVisible();
    expect(screen.queryByText('Invoices')).not.toBeInTheDocument();
  });
});
