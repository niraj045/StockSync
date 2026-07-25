import { useEffect, useState } from 'react';
import { Drawer, Descriptions, Table, Tag } from 'antd';
import dayjs from 'dayjs';
import { apiClient } from '../../../api/client';
import { User } from '../../auth/context/AuthContext';

interface UserDetailsDrawerProps {
  visible: boolean;
  onClose: () => void;
  user: User | null;
}

interface AuditLogEntry {
  id: number;
  userId: number | null;
  usernameSnapshot: string;
  action: string;
  entityType: string | null;
  entityId: string | null;
  description: string;
  requestMethod: string | null;
  requestPath: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export function UserDetailsDrawer({ visible, onClose, user }: UserDetailsDrawerProps) {
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const pageSize = 5;

  useEffect(() => {
    if (visible && user) {
      fetchUserLogs(1);
    } else {
      setLogs([]);
      setCurrentPage(1);
      setTotalItems(0);
    }
  }, [visible, user]);

  const fetchUserLogs = async (page: number) => {
    if (!user) return;
    setLoading(true);
    try {
      // Spring Data Pageable is 0-indexed, React table pagination is 1-indexed
      const response = await apiClient.get('/audit-logs', {
        params: {
          userId: user.id,
          page: page - 1,
          size: pageSize,
          sort: 'createdAt,desc',
        },
      });
      setLogs(response.data.content);
      setTotalItems(response.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      console.error('Failed to fetch user activity logs', err);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      render: (action: string) => <Tag color="blue">{action}</Tag>,
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: 'IP Address',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      render: (ip: string) => ip || 'N/A',
    },
    {
      title: 'Timestamp',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  return (
    <Drawer
      title="User Details & Audit History"
      placement="right"
      width={700}
      onClose={onClose}
      open={visible}
      destroyOnClose
    >
      {user && (
        <>
          <Descriptions title="Profile Information" bordered column={1} size="small" style={{ marginBottom: 32 }}>
            <Descriptions.Item label="Full Name">{user.fullName}</Descriptions.Item>
            <Descriptions.Item label="Username">{user.username}</Descriptions.Item>
            <Descriptions.Item label="Email">{user.email}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={user.active ? 'success' : 'error'}>
                {user.active ? 'Active' : 'Deactivated'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Roles">
              {user.roles.map((role) => (
                <Tag color="cyan" key={role}>
                  {role.replace('ROLE_', '')}
                </Tag>
              ))}
            </Descriptions.Item>
          </Descriptions>

          <h3 style={{ marginBottom: 16 }}>Activity Log History</h3>
          <Table
            dataSource={logs}
            columns={columns}
            rowKey="id"
            loading={loading}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: totalItems,
              onChange: (page) => fetchUserLogs(page),
              size: 'small',
              showSizeChanger: false,
            }}
          />
        </>
      )}
    </Drawer>
  );
}
