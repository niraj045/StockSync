import { useState } from 'react';
import { App, Button, Card, Input, Space, Table, Tag } from 'antd';
import { DownloadOutlined, FileDoneOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { paymentsApi } from '../api';
import type { PaymentReceipt } from '../types';
import { useAuth } from '../../auth/context/AuthContext';
import { ReportExcelButton } from '../../../components/ReportExcelButton';

const money = (value?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value ?? 0);
const canPost = (roles: string[]) => roles.includes('ROLE_ADMIN') || roles.includes('ROLE_ACCOUNTS');
const canReverse = (roles: string[]) => roles.includes('ROLE_ADMIN');

export function PaymentsPage() {
  const { message, modal } = App.useApp();
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  const payments = useQuery({
    queryKey: ['payments', search],
    queryFn: () => paymentsApi.list({ search, size: 50, sort: 'id,desc' }),
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['payments'] });
  };

  const postPayment = useMutation({ mutationFn: paymentsApi.post, onSuccess: invalidate });
  const reversePayment = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => paymentsApi.reverse(id, reason),
    onSuccess: invalidate,
  });

  const openReverse = (record: PaymentReceipt) => {
    let reasonText = '';
    modal.confirm({
      title: `Reverse ${record.receiptNumber}`,
      content: <Input.TextArea placeholder="Required reason" rows={3} onChange={e => reasonText = e.target.value} />,
      okButtonProps: { danger: true },
      onOk: async () => {
        if (!reasonText.trim()) {
          message.error('Reversal reason is required');
          return Promise.reject();
        }
        await reversePayment.mutateAsync({ id: record.id, reason: reasonText.trim() });
      },
    });
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Payments</h1>
          <p className="page-description">Record receipts, TDS, invoice allocations and advances.</p>
        </div>
        <Space wrap>
          <ReportExcelButton reportType="PAYMENT_REGISTER" filters={{ documentNumber: search || undefined }} />
          <Button icon={<PlusOutlined />} type="primary" onClick={() => navigate('/payments/new')}>
            Create payment
          </Button>
        </Space>
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Input.Search allowClear placeholder="Search receipt or party" onSearch={setSearch} style={{ width: 280 }} />
        </Space>
        <Table<PaymentReceipt>
          rowKey="id"
          loading={payments.isLoading}
          dataSource={payments.data?.content ?? []}
          columns={[
            { title: 'Receipt', dataIndex: 'receiptNumber', width: 150 },
            { title: 'Party', dataIndex: 'partyName' },
            { title: 'Date', dataIndex: 'paymentDate', width: 120 },
            { title: 'Mode', dataIndex: 'paymentMode', width: 130 },
            { title: 'Cash', dataIndex: 'cashAmount', render: money, width: 130 },
            { title: 'TDS', dataIndex: 'tdsAmount', render: money, width: 120 },
            { title: 'Advance', dataIndex: 'unallocatedAmount', render: money, width: 130 },
            { title: 'Status', dataIndex: 'status', width: 110, render: (status: PaymentReceipt['status']) => <Tag color={status === 'POSTED' ? 'green' : status === 'REVERSED' ? 'red' : 'orange'}>{status}</Tag> },
            {
              title: 'Actions',
              width: 320,
              render: (_value: unknown, row) => (
                <Space>
                  <Button onClick={() => navigate(`/payments/${row.id}`)}>Details</Button>
                  {row.status === 'DRAFT' && canPost(roles) && <Button icon={<FileDoneOutlined />} onClick={() => postPayment.mutate(row.id)}>Post</Button>}
                  {row.status === 'POSTED' && canReverse(roles) && <Button danger icon={<StopOutlined />} onClick={() => openReverse(row)}>Reverse</Button>}
                  {row.status === 'POSTED' && <Button icon={<DownloadOutlined />} href={paymentsApi.receiptUrl(row.id)}>PDF</Button>}
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
