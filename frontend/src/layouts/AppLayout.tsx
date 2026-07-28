import {
  UserOutlined,
  DownOutlined,
  KeyOutlined,
  LogoutOutlined,
  BuildOutlined,
  MenuOutlined,
} from '@ant-design/icons';
import { Layout, Menu, Typography, Dropdown, Avatar, Drawer, Button } from 'antd';
import type { ItemType } from 'antd/es/menu/interface';
import { useEffect, useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../features/auth/context/AuthContext';
import {
  activeRoute,
  activeSection,
  dashboardItem,
  reportsItem,
  sectionMenuItem,
  visibleSections,
} from './navigation';

const { Header, Content, Sider } = Layout;
const MOBILE_QUERY = '(max-width: 991px)';

export function AppLayout() {
  const { user, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(false);
  const [mobile, setMobile] = useState(() => window.matchMedia(MOBILE_QUERY).matches);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const sections = useMemo(() => visibleSections(user?.roles ?? []), [user?.roles]);
  const regularSections = sections.filter((section) => !section.pinned && section.key !== 'customers-setup');
  const customersSetup = sections.find((section) => section.key === 'customers-setup');
  const administration = sections.find((section) => section.pinned);
  const currentRoute = activeRoute(location.pathname);
  const currentSection = activeSection(location.pathname, sections);
  const [openSection, setOpenSection] = useState<string | undefined>(currentSection);

  useEffect(() => {
    const media = window.matchMedia(MOBILE_QUERY);
    const handleChange = (event: MediaQueryListEvent) => {
      setMobile(event.matches);
      if (!event.matches) setDrawerOpen(false);
    };
    setMobile(media.matches);
    media.addEventListener('change', handleChange);
    return () => media.removeEventListener('change', handleChange);
  }, []);

  useEffect(() => {
    setOpenSection(currentSection);
  }, [currentSection]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const onOpenChange = (keys: string[]) => {
    setOpenSection(keys.find((key) => key !== openSection));
  };

  const onNavigate = (key: string) => {
    navigate(key);
    setDrawerOpen(false);
  };

  const menu = (items: ItemType[], className = '') => (
    <Menu
      className={`app-menu ${className}`}
      theme="dark"
      mode="inline"
      inlineCollapsed={!mobile && collapsed}
      triggerSubMenuAction="click"
      selectedKeys={[currentRoute]}
      openKeys={collapsed && !mobile ? undefined : openSection ? [openSection] : []}
      items={items}
      onOpenChange={onOpenChange}
      onClick={({ key }) => onNavigate(key)}
    />
  );

  const navigation = (
    <>
      <div className="app-navigation-scroll">
        {menu([
          dashboardItem,
          ...regularSections.map(sectionMenuItem),
          reportsItem,
          ...(customersSetup ? [sectionMenuItem(customersSetup)] : []),
        ])}
      </div>
      {administration && (
        <div className="app-navigation-pinned">
          {menu([sectionMenuItem(administration)], 'app-menu-administration')}
        </div>
      )}
    </>
  );

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
      {!mobile && <Sider
          className="app-sider"
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          theme="dark"
          width={248}
          collapsedWidth={68}
        >
          <Brand collapsed={collapsed} />
          {navigation}
        </Sider>}
      <Drawer
        className="app-mobile-drawer"
        placement="left"
        width={288}
        open={mobile && drawerOpen}
        onClose={() => setDrawerOpen(false)}
        styles={{ body: { padding: 0, background: '#0c302f' }, header: { display: 'none' } }}
      >
        <Brand collapsed={false} />
        {navigation}
      </Drawer>
      <Layout>
        <Header className="app-header">
          <div className="header-leading">
            {mobile && <Button className="mobile-menu-button" aria-label="Open navigation" icon={<MenuOutlined />} onClick={() => setDrawerOpen(true)} />}
            <div className="header-context">
            <span className="header-eyebrow">Operations workspace</span>
            <Typography.Title level={4} className="app-title">
              Shuttering Inventory Management
            </Typography.Title>
            </div>
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

function Brand({ collapsed }: { collapsed: boolean }) {
  return (
    <div className="brand" aria-label="StockSync">
      <span className="brand-mark"><BuildOutlined /></span>
      {!collapsed && (
        <span className="brand-copy">
          <span className="brand-name">StockSync</span>
          <span className="brand-caption">Shuttering control</span>
        </span>
      )}
    </div>
  );
}
