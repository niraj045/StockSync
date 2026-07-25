import {
  AppstoreOutlined,
  DashboardOutlined,
  FileTextOutlined,
  ShopOutlined,
  TeamOutlined,
  HistoryOutlined,
  UserOutlined,
  DownOutlined,
  KeyOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { Layout, Menu, Typography, Dropdown, Avatar, Space } from 'antd';
import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../features/auth/context/AuthContext';

const { Header, Content, Sider } = Layout;

export function AppLayout() {
  const { user, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  // Build dynamic navigation items based on role
  const navigationItems = [
    { key: '/', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/inventory', icon: <AppstoreOutlined />, label: 'Inventory', disabled: true },
    { key: '/parties', icon: <TeamOutlined />, label: 'Parties', disabled: true },
    { key: '/sites', icon: <ShopOutlined />, label: 'Sites', disabled: true },
    { key: '/reports', icon: <FileTextOutlined />, label: 'Reports', disabled: true },
  ];

  // If user is Admin, add user management and audit logs links
  if (user?.roles.includes('ROLE_ADMIN')) {
    navigationItems.push(
      { key: '/users', icon: <TeamOutlined />, label: 'User Management', disabled: false },
      { key: '/audit-logs', icon: <HistoryOutlined />, label: 'Audit Logs', disabled: false }
    );
  }

  const profileMenuItems = {
    items: [
      {
        key: 'change-password',
        label: 'Change Password',
        icon: <KeyOutlined />,
        onClick: () => navigate('/change-password'),
      },
      {
        type: 'divider' as const,
      },
      {
        key: 'sign-out',
        label: 'Sign Out',
        icon: <LogoutOutlined />,
        danger: true,
        onClick: handleLogout,
      },
    ],
  };

  const displayRole = user?.roles[0]?.replace('ROLE_', '') || 'User';

  return (
    <Layout className="app-shell">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
        theme="dark"
      >
        <div className="brand" aria-label="StockSync">
          {collapsed ? 'SS' : 'StockSync'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={navigationItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Typography.Title level={4} className="app-title">
            Shuttering Inventory Management
          </Typography.Title>

          <Dropdown menu={profileMenuItems} trigger={['click']}>
            <div className="user-profile-header">
              <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#0f766e' }} />
              <Space className="user-name-label" style={{ display: collapsed ? 'none' : 'inline-flex' }}>
                {user?.fullName}
                <span className="user-role-badge">{displayRole}</span>
                <DownOutlined style={{ fontSize: '10px', color: '#64748b' }} />
              </Space>
            </div>
          </Dropdown>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
