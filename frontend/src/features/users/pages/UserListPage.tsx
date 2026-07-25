import { useState, useEffect, useCallback } from 'react';
import { Table, Button, Card, Input, Select, Tag, Space, Modal, Tooltip, message } from 'antd';
import { EditOutlined, EyeOutlined, PlusOutlined, UserDeleteOutlined, UserAddOutlined } from '@ant-design/icons';
import { apiClient } from '../../../api/client';
import { User, useAuth } from '../../auth/context/AuthContext';
import { UserFormModal } from '../components/UserFormModal';
import { UserDetailsDrawer } from '../components/UserDetailsDrawer';

export function UserListPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [totalItems, setTotalItems] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Filters
  const [search, setSearch] = useState('');
  const [activeFilter, setActiveFilter] = useState<string>('all');
  const [roleFilter, setRoleFilter] = useState<string>('all');

  // Modals / Drawer State
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [drawerVisible, setDrawerVisible] = useState(false);

  const fetchUsers = useCallback(async (page: number, size: number) => {
    setLoading(true);
    try {
      const params: any = {
        page: page - 1,
        size: size,
        sort: 'createdAt,desc',
      };
      if (search) params.search = search;
      if (activeFilter !== 'all') params.active = activeFilter === 'active';
      if (roleFilter !== 'all') params.role = roleFilter;

      const response = await apiClient.get('/users', { params });
      setUsers(response.data.content);
      setTotalItems(response.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      console.error('Failed to fetch users', err);
      message.error('Failed to fetch user list');
    } finally {
      setLoading(false);
    }
  }, [search, activeFilter, roleFilter]);

  useEffect(() => {
    fetchUsers(1, pageSize);
  }, [fetchUsers, pageSize]);

  const handleStatusToggle = (user: User) => {
    if (user.id === currentUser?.id) {
      message.error('You cannot deactivate your own account.');
      return;
    }

    const actionText = user.active ? 'deactivate' : 'activate';
    Modal.confirm({
      title: `Confirm Account ${actionText.charAt(0).toUpperCase() + actionText.slice(1)}`,
      content: `Are you sure you want to ${actionText} the user account for ${user.fullName}?`,
      okText: 'Confirm',
      cancelText: 'Cancel',
      okType: user.active ? 'danger' : 'primary',
      onOk: async () => {
        try {
          if (user.active) {
            await apiClient.post(`/users/${user.id}/deactivate`);
            message.success(`User ${user.fullName} deactivated successfully.`);
          } else {
            await apiClient.post(`/users/${user.id}/activate`);
            message.success(`User ${user.fullName} activated successfully.`);
          }
          fetchUsers(currentPage, pageSize);
        } catch (err: any) {
          const code = err.response?.data?.code;
          if (code === 'LAST_ACTIVE_ADMIN_REQUIRED') {
            message.error('Cannot deactivate the last active Administrator.');
          } else if (code === 'CANNOT_DEACTIVATE_CURRENT_USER') {
            message.error('Cannot deactivate your own account.');
          } else {
            message.error(err.response?.data?.message || `Failed to ${actionText} user.`);
          }
        }
      },
    });
  };

  const handleEditClick = (user: User) => {
    setSelectedUser(user);
    setFormModalVisible(true);
  };

  const handleCreateClick = () => {
    setSelectedUser(null);
    setFormModalVisible(true);
  };

  const handleViewLogClick = (user: User) => {
    setSelectedUser(user);
    setDrawerVisible(true);
  };

  const columns = [
    {
      title: 'Full Name',
      dataIndex: 'fullName',
      key: 'fullName',
      sorter: true,
    },
    {
      title: 'Username',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Roles',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles: string[]) => (
        <Space size={[0, 4]} wrap>
          {roles.map((role) => (
            <Tag className="role-tag" key={role}>
              {role.replace('ROLE_', '')}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => (
        <Tag className="status-tag" color={active ? 'success' : 'error'}>
          {active ? 'Active' : 'Deactivated'}
        </Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: User) => {
        const isSelf = record.id === currentUser?.id;
        return (
          <Space size="middle">
            <Tooltip title="Edit Profile Details">
              <Button
                type="text"
                className="action-button"
                icon={<EditOutlined />}
                onClick={() => handleEditClick(record)}
              />
            </Tooltip>

            <Tooltip title="View Action Logs">
              <Button
                type="text"
                className="action-button"
                icon={<EyeOutlined />}
                onClick={() => handleViewLogClick(record)}
              />
            </Tooltip>

            {record.active ? (
              <Tooltip title={isSelf ? 'You cannot deactivate yourself' : 'Deactivate User Account'}>
                <Button
                  type="text"
                  danger
                  disabled={isSelf}
                  icon={<UserDeleteOutlined />}
                  onClick={() => handleStatusToggle(record)}
                />
              </Tooltip>
            ) : (
              <Tooltip title="Activate User Account">
                <Button
                  type="text"
                  style={{ color: '#52c41a' }}
                  icon={<UserAddOutlined />}
                  onClick={() => handleStatusToggle(record)}
                />
              </Tooltip>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">User management</h1>
          <p className="page-description">
            Control team access, account status and assigned operational roles.
          </p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleCreateClick}
          size="large"
        >
          Add user
        </Button>
      </div>

      <Card className="premium-card">
        <div className="filters-bar">
          <Input.Search
            placeholder="Search by name, email or username"
            onSearch={(value) => {
              setSearch(value);
              setCurrentPage(1);
            }}
            onChange={(e) => {
              if (e.target.value === '') {
                setSearch('');
                setCurrentPage(1);
              }
            }}
            style={{ width: 280 }}
            allowClear
          />

          <Select
            value={activeFilter}
            onChange={(value) => {
              setActiveFilter(value);
              setCurrentPage(1);
            }}
            style={{ width: 150 }}
            options={[
              { label: 'All Statuses', value: 'all' },
              { label: 'Active', value: 'active' },
              { label: 'Deactivated', value: 'inactive' },
            ]}
          />

          <Select
            value={roleFilter}
            onChange={(value) => {
              setRoleFilter(value);
              setCurrentPage(1);
            }}
            style={{ width: 180 }}
            options={[
              { label: 'All Roles', value: 'all' },
              { label: 'Admin', value: 'ROLE_ADMIN' },
              { label: 'Operations', value: 'ROLE_OPERATIONS' },
              { label: 'Accounts', value: 'ROLE_ACCOUNTS' },
              { label: 'Viewer', value: 'ROLE_VIEWER' },
            ]}
          />
        </div>

        <Table
          dataSource={users}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            current: currentPage,
            pageSize: pageSize,
            total: totalItems,
            onChange: (page, size) => {
              setCurrentPage(page);
              setPageSize(size);
            },
            showSizeChanger: true,
          }}
        />
      </Card>

      <UserFormModal
        visible={formModalVisible}
        onCancel={() => setFormModalVisible(false)}
        onSuccess={() => {
          setFormModalVisible(false);
          message.success(selectedUser ? 'User details updated successfully.' : 'User created successfully.');
          fetchUsers(currentPage, pageSize);
        }}
        userToEdit={selectedUser}
      />

      <UserDetailsDrawer
        visible={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        user={selectedUser}
      />
    </div>
  );
}
