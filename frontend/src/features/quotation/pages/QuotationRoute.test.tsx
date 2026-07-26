import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it, vi } from 'vitest';
import { ProtectedRoute } from '../../auth/components/ProtectedRoute';

vi.mock('../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: null, loading: false }),
}));

describe('quotation route protection', () => {
  it('redirects unauthenticated quotation routes to login', () => {
    render(<MemoryRouter initialEntries={['/quotations']}><Routes>
      <Route path="/login" element={<div>Login required</div>}/>
      <Route path="/quotations" element={<ProtectedRoute><div>Quotation screen</div></ProtectedRoute>}/>
    </Routes></MemoryRouter>);
    expect(screen.getByText('Login required')).toBeVisible();
    expect(screen.queryByText('Quotation screen')).not.toBeInTheDocument();
  });
});
