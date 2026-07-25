import { createBrowserRouter } from 'react-router';
import { AppLayout } from '../../layouts/AppLayout';
import { LoginPage } from '../../features/auth/pages/LoginPage';
import { ProtectedRoute } from '../../features/auth/components/ProtectedRoute';
import { UnauthorizedPage } from '../../features/auth/pages/UnauthorizedPage';
import { ChangePasswordPage } from '../../features/auth/pages/ChangePasswordPage';
import { UserListPage } from '../../features/users/pages/UserListPage';
import { AuditLogPage } from '../../features/audit/pages/AuditLogPage';
import { DashboardPage } from '../../features/dashboard/pages/DashboardPage';
import { NotFoundPage } from '../../pages/NotFoundPage';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: 'change-password',
        element: <ChangePasswordPage />,
      },
      {
        path: 'unauthorized',
        element: <UnauthorizedPage />,
      },
      {
        path: 'users',
        element: (
          <ProtectedRoute adminOnly>
            <UserListPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'audit-logs',
        element: (
          <ProtectedRoute adminOnly>
            <AuditLogPage />
          </ProtectedRoute>
        ),
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
