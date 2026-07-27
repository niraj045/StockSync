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
const AgreementsPage = lazy(() => import('../../features/agreement/pages/AgreementsPage').then((module) => ({ default: module.AgreementsPage })));
const BillingRunsPage = lazy(() => import('../../features/billing/pages/BillingRunsPage').then((module) => ({ default: module.BillingRunsPage })));
const InvoicesPage = lazy(() => import('../../features/billing/pages/InvoicesPage').then((module) => ({ default: module.InvoicesPage })));
const InvoiceDetailsPage = lazy(() => import('../../features/billing/pages/InvoiceDetailsPage').then((module) => ({ default: module.InvoiceDetailsPage })));
const PaymentsPage = lazy(() => import('../../features/payment/pages/PaymentsPage').then((module) => ({ default: module.PaymentsPage })));
const SecurityDepositsPage = lazy(() => import('../../features/payment/pages/SecurityDepositsPage').then((module) => ({ default: module.SecurityDepositsPage })));
const OutstandingPage = lazy(() => import('../../features/payment/pages/OutstandingPage').then((module) => ({ default: module.OutstandingPage })));
const ReportsPage = lazy(() => import('../../features/reports/pages/ReportsPage').then((module) => ({ default: module.ReportsPage })));
const SiteOrdersPage = lazy(() => import('../../features/order/pages/SiteOrdersPage').then((module) => ({ default: module.SiteOrdersPage })));
const IssuedChallansPage = lazy(() => import('../../features/challan/pages/IssuedChallansPage').then((module) => ({ default: module.IssuedChallansPage })));
const ReceivingChallansPage = lazy(() => import('../../features/challan/pages/ReceivingChallansPage').then((module) => ({ default: module.ReceivingChallansPage })));
const StockLossesPage = lazy(() => import('../../features/exception/pages/StockLossesPage').then((module) => ({ default: module.StockLossesPage })));
const StockDamagesPage = lazy(() => import('../../features/exception/pages/StockDamagesPage').then((module) => ({ default: module.StockDamagesPage })));
const ItemExchangesPage = lazy(() => import('../../features/exception/pages/ItemExchangesPage').then((module) => ({ default: module.ItemExchangesPage })));
const SiteTransfersPage = lazy(() => import('../../features/exception/pages/SiteTransfersPage').then((module) => ({ default: module.SiteTransfersPage })));
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
      { path: 'quotations/new', element: deferred(<QuotationsPage />) },
      { path: 'quotations/:quotationId/edit', element: deferred(<QuotationsPage />) },
      { path: 'agreements', element: deferred(<AgreementsPage />) },
      { path: 'billing-runs', element: deferred(<BillingRunsPage />) },
      { path: 'invoices', element: deferred(<InvoicesPage />) },
      { path: 'invoices/:id', element: deferred(<InvoiceDetailsPage />) },
      { path: 'payments', element: deferred(<PaymentsPage />) },
      { path: 'security-deposits', element: deferred(<SecurityDepositsPage />) },
      { path: 'outstanding', element: deferred(<OutstandingPage />) },
      { path: 'reports', element: deferred(<ReportsPage />) },
      { path: 'orders', element: deferred(<SiteOrdersPage />) },
      { path: 'challans/issued', element: deferred(<IssuedChallansPage />) },
      { path: 'challans/receiving', element: deferred(<ReceivingChallansPage />) },
      { path: 'stock-losses', element: deferred(<StockLossesPage />) },
      { path: 'stock-damages', element: deferred(<StockDamagesPage />) },
      { path: 'item-exchanges', element: deferred(<ItemExchangesPage />) },
      { path: 'site-transfers', element: deferred(<SiteTransfersPage />) },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
