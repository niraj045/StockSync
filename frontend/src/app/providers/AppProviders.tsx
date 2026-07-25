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
          colorSuccess: '#27835f',
          colorWarning: '#c97a18',
          colorError: '#c2413a',
          colorText: '#17212b',
          colorTextSecondary: '#64736f',
          colorBorder: '#dfe7e4',
          colorBgLayout: '#f5f7f6',
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
            darkItemBg: '#102b2b',
            darkSubMenuItemBg: '#102b2b',
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
