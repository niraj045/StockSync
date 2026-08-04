import { useState } from 'react';
import { Button, Card, Select, Space, Table, Tag } from 'antd';
import { PlusOutlined, CalculatorOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import type { BillingRun } from '../types';
import { ReportExcelButton } from '../../../components/ReportExcelButton';

type Page<T> = { content: T[]; totalElements: number };

const money = (v?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(v ?? 0);

export function BillingRunsPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const accounts = roles.includes('ROLE_ACCOUNTS');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canPrepare = admin || accounts || operations;
  const canFinalize = admin || accounts;

  const navigate = useNavigate();
  const qc = useQueryClient();

  const [agreementId] = useState<number | undefined>();
  const [status, setStatus] = useState<string | undefined>();

  const billingRunsQuery = useQuery({
    queryKey: ['billing-runs', agreementId, status],
    queryFn: async () => (await apiClient.get<Page<BillingRun>>('/billing-runs', {
      params: { agreementId, status, size: 50, sort: 'id,desc' }
    })).data
  });

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['billing-runs'] });
  };

  const generateDueMutation = useMutation({
    mutationFn: async () => (await apiClient.post<{generated:number;skipped:number;failed:number;failures:string[]}>('/billing-runs/generate-due-drafts')).data,
    onSuccess: () => {
      refresh();
    },
  });

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Monthly Rental Billing</h1>
          <p className="page-description">Prepare rent from posted challan movements, review charges, then finalize and create the invoice.</p>
        </div>
        <Space wrap>
          <ReportExcelButton reportType="BILLING_RUN_REGISTER" filters={{ status }} />
          {canFinalize && <Button icon={<CalculatorOutlined/>} loading={generateDueMutation.isPending} onClick={()=>generateDueMutation.mutate()}>Generate due drafts</Button>}
          {canPrepare && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/billing-runs/new')}>
              New Billing Run
            </Button>
          )}
        </Space>
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Select
            allowClear
            placeholder="Filter by Status"
            style={{ width: 160 }}
            onChange={setStatus}
            options={['DRAFT', 'CALCULATED', 'FINALIZED', 'CANCELLED'].map(v => ({ value: v, label: v }))}
          />
        </Space>

        <Table
          rowKey="id"
          loading={billingRunsQuery.isLoading}
          dataSource={billingRunsQuery.data?.content}
          columns={[
            { title: 'Billing Run Number', dataIndex: 'billingRunNumber', width: 150 },
            { title: 'Agreement', dataIndex: 'agreementNumber', width: 150 },
            { title: 'Party', dataIndex: 'partyName' },
            { title: 'Site', dataIndex: 'siteName' },
            { title: 'Start Date', dataIndex: 'periodStart', width: 120 },
            { title: 'End Date', dataIndex: 'periodEnd', width: 120 },
            { title: 'Grand Total', dataIndex: 'grandTotal', render: money, width: 120 },
            { 
              title: 'Status', 
              dataIndex: 'status', 
              width: 120,
              render: (s: string) => {
                const color = s === 'FINALIZED' ? 'green' : s === 'CANCELLED' ? 'red' : s === 'CALCULATED' ? 'blue' : 'orange';
                const label = s === 'DRAFT' ? 'PREPARED' : s === 'CALCULATED' ? 'READY FOR REVIEW' : s;
                return <Tag color={color}>{label}</Tag>;
              }
            },
            {
              title: 'Actions',
              width: 100,
              render: (_, row) => (
                <Button onClick={() => navigate(`/billing-runs/${row.id}`)}>Review Details</Button>
              )
            }
          ]}
        />
      </Card>
    </div>
  );
}
