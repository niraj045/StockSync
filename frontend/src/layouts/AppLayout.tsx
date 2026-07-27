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
  BuildOutlined,
  TagsOutlined,
  ToolOutlined,
  FormOutlined,
  ImportOutlined,
} from '@ant-design/icons';
import { Layout, Menu, Typography, Dropdown, Avatar, Tooltip } from 'antd';
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
    { key: '/categories', icon: <TagsOutlined />, label: 'Categories' },
    { key: '/items', icon: <AppstoreOutlined />, label: 'Items' },
    { key: '/parties', icon: <TeamOutlined />, label: 'Parties' },
    { key: '/sites', icon: <ShopOutlined />, label: 'Sites' },
    { key: '/vendors', icon: <ToolOutlined />, label: 'Vendors' },
    { key: '/documents', icon: <FileTextOutlined />, label: 'Documents' },
    { key: '/inventory', icon: <AppstoreOutlined />, label: 'Inventory' },
    { key: '/opening-stock-imports', icon: <ImportOutlined />, label: <Tooltip placement="right" title="Opening Stock Import"><span>Opening Stock Import</span></Tooltip> },
    { key: '/quotations', icon: <FormOutlined />, label: 'Quotations' },
    { key: '/agreements', icon: <FileTextOutlined />, label: 'Agreements' },
    { key: '/orders', icon: <FileTextOutlined />, label: 'Site Orders' },
    { key: '/challans/issued', icon: <FileTextOutlined />, label: 'Issued Challans' },
    { key: '/challans/receiving', icon: <FileTextOutlined />, label: 'Receiving Challans' },
    { key: '/quotation-templates', icon: <FileTextOutlined />, label: 'Quotation Templates' },
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
    <Layout className={`app-shell ${collapsed ? 'sider-collapsed' : ''}`}>
      <Sider
        className="app-sider"
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
        theme="dark"
        width={238}
        collapsedWidth={72}
      >
        <div className="brand" aria-label="StockSync">
          <span className="brand-mark"><BuildOutlined /></span>
          {!collapsed && (
            <span className="brand-copy">
              <span className="brand-name">StockSync</span>
              <span className="brand-caption">Shuttering control</span>
            </span>
          )}
        </div>
        <Menu
          className="app-menu"
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={navigationItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <div className="header-context">
            <span className="header-eyebrow">Operations workspace</span>
            <Typography.Title level={4} className="app-title">
              Shuttering Inventory Management
            </Typography.Title>
          </div>

          <Dropdown menu={profileMenuItems} trigger={['click']}>
            <div className="user-profile-header">
              <Avatar className="profile-avatar" icon={<UserOutlined />} />
              <span className="profile-copy">
                <span className="user-name-label">{user?.fullName}</span>
                <span className="user-role-label">{displayRole.toLowerCase()}</span>
              </span>
              <DownOutlined style={{ fontSize: 10, color: '#64736f' }} />
            </div>
          </Dropdown>
        </Header>
        <Content className="app-content">
          <div className="content-frame">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
