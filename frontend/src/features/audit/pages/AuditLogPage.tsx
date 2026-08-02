import { useState, useEffect, useCallback } from 'react';
import { Table, Card, Input, Select, Tag, Space, DatePicker, Button, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { apiClient } from '../../../api/client';

const { RangePicker } = DatePicker;

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

const ACTION_OPTIONS = [
  { label: 'All Actions', value: 'all' },
  { label: 'Login Success', value: 'LOGIN_SUCCESS' },
  { label: 'Login Failure', value: 'LOGIN_FAILURE' },
  { label: 'Logout', value: 'LOGOUT' },
  { label: 'Password Changed', value: 'PASSWORD_CHANGED' },
  { label: 'User Created', value: 'USER_CREATED' },
  { label: 'User Updated', value: 'USER_UPDATED' },
  { label: 'User Roles Updated', value: 'USER_ROLES_UPDATED' },
  { label: 'User Activated', value: 'USER_ACTIVATED' },
  { label: 'User Deactivated', value: 'USER_DEACTIVATED' },
];

export function AuditLogPage() {
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [totalItems, setTotalItems] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Filters
  const [usernameSearch, setUsernameSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('all');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs | null, dayjs.Dayjs | null] | null>(null);

  const fetchLogs = useCallback(async (page: number, size: number) => {
    setLoading(true);
    try {
      const params: any = {
        page: page - 1,
        size: size,
        sort: 'createdAt,desc',
      };

      if (usernameSearch) params.username = usernameSearch;
      if (actionFilter !== 'all') params.action = actionFilter;
      if (dateRange && dateRange[0] && dateRange[1]) {
        params.from = dateRange[0].startOf('day').toISOString();
        params.to = dateRange[1].endOf('day').toISOString();
      }

      const response = await apiClient.get('/audit-logs', { params });
      setLogs(response.data.content);
      setTotalItems(response.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      console.error('Failed to fetch audit logs', err);
      message.error('Failed to load audit log records');
    } finally {
      setLoading(false);
    }
  }, [usernameSearch, actionFilter, dateRange]);

  useEffect(() => {
    fetchLogs(1, pageSize);
  }, [fetchLogs, pageSize]);

  const handleReset = () => {
    setUsernameSearch('');
    setActionFilter('all');
    setDateRange(null);
    setCurrentPage(1);
  };

  const getActionColor = (action: string) => {
    switch (action) {
      case 'LOGIN_SUCCESS':
        return 'green';
      case 'LOGIN_FAILURE':
        return 'red';
      case 'PASSWORD_CHANGED':
        return 'orange';
      case 'USER_DEACTIVATED':
        return 'volcano';
      case 'USER_CREATED':
      case 'USER_ACTIVATED':
        return 'cyan';
      default:
        return 'blue';
    }
  };

  const columns = [
    {
      title: 'Timestamp',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm:ss'),
      width: 170,
    },
    {
      title: 'Username Snapshot',
      dataIndex: 'usernameSnapshot',
      key: 'usernameSnapshot',
      render: (username: string) => <strong>{username}</strong>,
      width: 180,
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      render: (action: string) => (
        <Tag color={getActionColor(action)}>
          {action.replace('_', ' ')}
        </Tag>
      ),
      width: 180,
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
      width: 130,
    },
  ];

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Activity audit</h1>
          <p className="page-description">
            Review security events and administrative changes across SteelFab.
          </p>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => fetchLogs(currentPage, pageSize)}>
            Refresh
          </Button>
          <Button onClick={handleReset}>Reset Filters</Button>
        </Space>
      </div>

      <Card className="premium-card">
        <div className="filters-bar">
          <Input.Search
            placeholder="Search by username snapshot"
            value={usernameSearch}
            onChange={(e) => setUsernameSearch(e.target.value)}
            onSearch={() => {
              setCurrentPage(1);
              fetchLogs(1, pageSize);
            }}
            style={{ width: 240 }}
            allowClear
          />

          <Select
            value={actionFilter}
            onChange={(value) => {
              setActionFilter(value);
              setCurrentPage(1);
            }}
            style={{ width: 180 }}
            options={ACTION_OPTIONS}
          />

          <RangePicker
            value={dateRange}
            onChange={(values) => {
              setDateRange(values as any);
              setCurrentPage(1);
            }}
          />
        </div>

        <Table
          dataSource={logs}
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
    </div>
  );
}
