import {
  ApartmentOutlined,
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  CalculatorOutlined,
  CreditCardOutlined,
  DashboardOutlined,
  FileTextOutlined,
  SettingOutlined,
  ShopOutlined,
  SwapOutlined,
  TeamOutlined,
  CloudUploadOutlined,
} from '@ant-design/icons';
import type { ItemType } from 'antd/es/menu/interface';
import type { ReactNode } from 'react';

export type NavigationSection = {
  key: string;
  label: string;
  icon: ReactNode;
  roles: string[];
  children: Array<{ key: string; label: string }>;
  pinned?: boolean;
};

const ADMIN = 'ROLE_ADMIN';
const OPERATIONS = 'ROLE_OPERATIONS';
const ACCOUNTS = 'ROLE_ACCOUNTS';
const VIEWER = 'ROLE_VIEWER';
const EVERYONE = [ADMIN, OPERATIONS, ACCOUNTS, VIEWER];

export const navigationSections: NavigationSection[] = [
  {
    key: 'daily-operations',
    label: 'Daily Operations',
    icon: <SwapOutlined />,
    roles: [ADMIN, OPERATIONS],
    children: [
      { key: '/client-workflow', label: 'Inquiries & Site Costs' },
      { key: '/challans/issued', label: 'Issued Challans' },
      { key: '/challans/receiving', label: 'Receiving Challans' },
      { key: '/challans/import-client-excel', label: 'Historical D&R Import' },
      { key: '/site-transfers', label: 'Site Transfers' },
      { key: '/stock-losses', label: 'Stock Losses' },
      { key: '/stock-damages', label: 'Stock Damages' },
      { key: '/item-exchanges', label: 'Item Exchanges' },
    ],
  },
  {
    key: 'sales-agreements',
    label: 'Sales & Agreements',
    icon: <FileTextOutlined />,
    roles: [ADMIN, OPERATIONS, ACCOUNTS],
    children: [
      { key: '/quotations', label: 'Quotations' },
      { key: '/quotation-templates', label: 'Quotation Templates' },
      { key: '/agreements', label: 'Agreements' },
      { key: '/agreement-templates', label: 'Agreement Templates' },
      { key: '/orders', label: 'Site Orders' },
    ],
  },
  {
    key: 'stock-management',
    label: 'Stock Management',
    icon: <AppstoreOutlined />,
    roles: [ADMIN, OPERATIONS, VIEWER],
    children: [
      { key: '/inventory', label: 'Stock Overview' },
      { key: '/items', label: 'Items' },
      { key: '/opening-stock-imports', label: 'Opening Stock Import' },
    ],
  },
  {
    key: 'billing-payments',
    label: 'Billing & Payments',
    icon: <CreditCardOutlined />,
    roles: [ADMIN, ACCOUNTS],
    children: [
      { key: '/billing-runs', label: 'Billing Runs' },
      { key: '/invoices', label: 'Invoices' },
      { key: '/payments', label: 'Payments' },
      { key: '/security-deposits', label: 'Security Deposits' },
      { key: '/outstanding', label: 'Outstanding Payments' },
    ],
  },
  {
    key: 'customers-setup',
    label: 'Customers & Setup',
    icon: <ShopOutlined />,
    roles: EVERYONE,
    children: [
      { key: '/parties', label: 'Parties' },
      { key: '/sites', label: 'Sites' },
      { key: '/vendors', label: 'Vendors' },
      { key: '/categories', label: 'Categories' },
      { key: '/documents', label: 'Documents' },
    ],
  },
  {
    key: 'administration',
    label: 'Administration',
    icon: <SettingOutlined />,
    roles: [ADMIN],
    pinned: true,
    children: [
      { key: '/users', label: 'User Management' },
      { key: '/audit-logs', label: 'Activity Logs' },
    ],
  },
];

const routeIcons: Record<string, ReactNode> = {
  '/': <DashboardOutlined />,
  '/reports': <BarChartOutlined />,
  '/challans/issued': <FileTextOutlined />,
  '/challans/receiving': <FileTextOutlined />,
  '/challans/import-client-excel': <CloudUploadOutlined />,
  '/site-transfers': <SwapOutlined />,
  '/inventory': <AppstoreOutlined />,
  '/items': <AppstoreOutlined />,
  '/parties': <TeamOutlined />,
  '/sites': <ApartmentOutlined />,
  '/payments': <CreditCardOutlined />,
  '/outstanding': <CalculatorOutlined />,
  '/users': <TeamOutlined />,
  '/audit-logs': <AuditOutlined />,
};

export function visibleSections(roles: string[]) {
  return navigationSections.filter((section) => section.roles.some((role) => roles.includes(role)));
}

export function sectionMenuItem(section: NavigationSection): ItemType {
  return {
    key: section.key,
    icon: section.icon,
    label: section.label,
    children: section.children.map((child) => ({
      ...child,
      icon: routeIcons[child.key] ?? <FileTextOutlined />,
    })),
  };
}

export function activeRoute(pathname: string) {
  const routes = [
    '/',
    '/reports',
    ...navigationSections.flatMap((section) => section.children.map((child) => child.key)),
  ];
  return routes
    .filter((route) => route === '/' ? pathname === '/' : pathname === route || pathname.startsWith(`${route}/`))
    .sort((a, b) => b.length - a.length)[0] ?? pathname;
}

export function activeSection(pathname: string, sections: NavigationSection[]) {
  const route = activeRoute(pathname);
  return sections.find((section) => section.children.some((child) => child.key === route))?.key;
}

export const dashboardItem: ItemType = { key: '/', icon: <DashboardOutlined />, label: 'Dashboard' };
export const reportsItem: ItemType = { key: '/reports', icon: <BarChartOutlined />, label: 'Reports' };
