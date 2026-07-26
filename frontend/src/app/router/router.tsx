import { lazy, Suspense } from 'react';
import { createBrowserRouter } from 'react-router';
import { Spin } from 'antd';
import { AppLayout } from '../../layouts/AppLayout';
import { LoginPage } from '../../features/auth/pages/LoginPage';
import { ProtectedRoute } from '../../features/auth/components/ProtectedRoute';
import { UnauthorizedPage } from '../../features/auth/pages/UnauthorizedPage';
import { ChangePasswordPage } from '../../features/auth/pages/ChangePasswordPage';
import { DashboardPage } from '../../features/dashboard/pages/DashboardPage';
import { NotFoundPage } from '../../pages/NotFoundPage';

const UserListPage = lazy(() => import('../../features/users/pages/UserListPage').then((module) => ({ default: module.UserListPage })));
const AuditLogPage = lazy(() => import('../../features/audit/pages/AuditLogPage').then((module) => ({ default: module.AuditLogPage })));
const MasterDataPage = lazy(() => import('../../features/masterdata/pages/MasterDataPage').then((module) => ({ default: module.MasterDataPage })));
const DocumentsPage = lazy(() => import('../../features/documents/pages/DocumentsPage').then((module) => ({ default: module.DocumentsPage })));
const InventoryPage = lazy(() => import('../../features/inventory/pages/InventoryPage').then((module) => ({ default: module.InventoryPage })));
const OpeningStockImportPage = lazy(() => import('../../features/migration/pages/OpeningStockImportPage').then((module) => ({ default: module.OpeningStockImportPage })));
const QuotationTemplatesPage = lazy(() => import('../../features/quotation/pages/QuotationTemplatesPage').then((module) => ({ default: module.QuotationTemplatesPage })));
const QuotationsPage = lazy(() => import('../../features/quotation/pages/QuotationsPage').then((module) => ({ default: module.QuotationsPage })));
const deferred = (element: React.ReactNode) => <Suspense fallback={<Spin fullscreen />}>{element}</Suspense>;

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
            {deferred(<UserListPage />)}
          </ProtectedRoute>
        ),
      },
      {
        path: 'audit-logs',
        element: (
          <ProtectedRoute adminOnly>
            {deferred(<AuditLogPage />)}
          </ProtectedRoute>
        ),
      },
      { path: 'categories', element: deferred(<MasterDataPage kind="categories" />) },
      { path: 'items', element: deferred(<MasterDataPage kind="items" />) },
      { path: 'parties', element: deferred(<MasterDataPage kind="parties" />) },
      { path: 'sites', element: deferred(<MasterDataPage kind="sites" />) },
      { path: 'vendors', element: deferred(<MasterDataPage kind="vendors" />) },
      { path: 'documents', element: deferred(<DocumentsPage />) },
      { path: 'inventory', element: deferred(<InventoryPage />) },
      { path: 'opening-stock-imports', element: deferred(<OpeningStockImportPage />) },
      { path: 'quotation-templates', element: deferred(<QuotationTemplatesPage />) },
      { path: 'quotations', element: deferred(<QuotationsPage />) },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
