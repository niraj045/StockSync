import { App as AntApp, ConfigProvider } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router';
import { router } from '../router/router';
import { AuthProvider } from '../../features/auth/context/AuthContext';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});

export function AppProviders() {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#0f766e',
          colorInfo: '#0f766e',
          colorSuccess: '#2e7d32',
          colorWarning: '#d97706',
          colorError: '#c62828',
          colorText: '#182230',
          colorTextSecondary: '#667085',
          colorBorder: '#dce5e3',
          colorBgLayout: '#f5f7f7',
          fontFamily: 'Inter, "Segoe UI", Arial, sans-serif',
          borderRadius: 9,
          controlHeight: 40,
        },
        components: {
          Button: {
            fontWeight: 650,
          },
          Card: {
            headerFontSize: 16,
          },
          Menu: {
            darkItemBg: '#0c302f',
            darkSubMenuItemBg: '#0c302f',
            darkItemSelectedBg: '#0f766e',
          },
        },
      }}
    >
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  );
}
