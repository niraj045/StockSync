import {
  AlertOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
  CalendarOutlined,
  DollarOutlined,
  ReloadOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import { Alert, Button, Card, Col, DatePicker, Empty, InputNumber, Row, Skeleton, Space, Statistic, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip as ChartTooltip, XAxis, YAxis } from 'recharts';
import { useNavigate } from 'react-router';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';

type Money = number;
type ChartPoint = { label: string; value: number };
type TrendPoint = { date: string; issued: number; received: number };
type AttentionItem = { key: string; severity: string; title: string; description: string; targetPath: string; count: number };
type DocumentItem = { type: string; id: number; number: string; documentDate: string; status: string; targetPath: string };
type ActivityItem = { id: number; action: string; username: string; createdAt: string; description: string };
type QuickAction = { key: string; label: string; targetPath: string; roleGroup: string };

type DashboardOverview = {
  stockSummary: { godownAvailable: Money; materialAtSites: Money; damaged: Money; lost: Money; scrapped: Money; underRepair: Money; physicalCurrentStock: Money; currentAccountableStock: Money };
  movementSummary: { issuedToday: Money; receivedToday: Money; issuedInPeriod: Money; receivedInPeriod: Money; siteTransfersInPeriod: number };
  orderSummary: { openSiteOrders: number; partiallyFulfilledSiteOrders: number; fulfilledSiteOrders: number };
  challanSummary: { draftIssuedChallans: number; draftReceivingChallans: number; pendingExtraReturnApprovals: number };
  agreementSummary: { activeAgreements: number; expiringSoon: number; warningDate: string };
  billingSummary: { draftBillingRuns: number; draftInvoices: number; issuedInvoices: number; totalInvoicedInPeriod: Money; totalOutstanding: Money };
  paymentSummary: { cashReceived: Money; tds: Money; depositAdjustments: Money; availableCustomerAdvance: Money; availableSecurityDeposits: Money };
  exceptionSummary: { lossAwaitingApproval: number; damageAwaitingAction: number; materialUnderRepair: Money; repairablePendingDecision: number; partiallyFulfilledOrders: number; extraReturnsAwaitingApproval: number; lowStockMaterials: number; overdueInvoices: number; highOutstandingParties: number; agreementsExpiringSoon: number };
  attentionItems: AttentionItem[];
  stockByStatus: ChartPoint[];
  movementTrend: TrendPoint[];
  topSites: ChartPoint[];
  outstandingAgeing: ChartPoint[];
  recentDocuments: DocumentItem[];
  recentActivity: ActivityItem[];
  quickActions: QuickAction[];
  roleMode: string;
  generatedAt: string;
};

const { RangePicker } = DatePicker;
const money = (value?: number) => `Rs ${Number(value ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;
const qty = (value?: number) => Number(value ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 });
const last30 = [dayjs().subtract(29, 'day'), dayjs()] as [Dayjs, Dayjs];

export function DashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [range, setRange] = useState<[Dayjs, Dayjs]>(last30);
  const [partyId, setPartyId] = useState<number | null>(null);
  const [siteId, setSiteId] = useState<number | null>(null);
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [from, to] = range;
  const overview = useQuery({
    queryKey: ['dashboard-overview', from.format('YYYY-MM-DD'), to.format('YYYY-MM-DD'), partyId, siteId, categoryId],
    queryFn: async () => (await apiClient.get<DashboardOverview>('/dashboard/overview', {
      params: {
        dateFrom: from.format('YYYY-MM-DD'),
        dateTo: to.format('YYYY-MM-DD'),
        partyId: partyId ?? undefined,
        siteId: siteId ?? undefined,
        categoryId: categoryId ?? undefined,
      },
    })).data,
  });

  const data = overview.data;
  const isViewer = user?.roles.includes('ROLE_VIEWER');

  const attentionColumns: ColumnsType<AttentionItem> = [
    { title: 'Needs attention', dataIndex: 'title', render: (text, row) => <Button type="link" onClick={() => navigate(row.targetPath)}>{text}</Button> },
    { title: 'Count', dataIndex: 'count', width: 90 },
    { title: 'Severity', dataIndex: 'severity', width: 110, render: (value) => <Tag color={value === 'danger' ? 'red' : value === 'warning' ? 'gold' : 'blue'}>{value}</Tag> },
  ];

  const documentColumns: ColumnsType<DocumentItem> = [
    { title: 'Type', dataIndex: 'type' },
    { title: 'Number', dataIndex: 'number', render: (text, row) => <Button type="link" onClick={() => navigate(row.targetPath)}>{text}</Button> },
    { title: 'Date', dataIndex: 'documentDate', width: 120 },
    { title: 'Status', dataIndex: 'status', width: 120, render: (value) => <Tag>{value}</Tag> },
  ];

  if (overview.isLoading) {
    return <div className="page-stack"><Skeleton active paragraph={{ rows: 10 }} /></div>;
  }

  if (overview.isError) {
    return <Alert type="error" showIcon message="Dashboard could not be loaded" action={<Button onClick={() => overview.refetch()}>Retry</Button>} />;
  }

  if (!data) return <Empty description="No dashboard data available" />;

  const operationalMetrics = [
    { title: 'Godown available', value: qty(data.stockSummary.godownAvailable), icon: <ArrowDownOutlined />, tip: 'Available godown quantity from stock balances.' },
    { title: 'At sites', value: qty(data.stockSummary.materialAtSites), icon: <BarChartOutlined />, tip: 'Pending site material from site stock balances. Not added to issued history again.' },
    { title: 'Issued in period', value: qty(data.movementSummary.issuedInPeriod), icon: <ArrowUpOutlined />, tip: 'OUT stock transactions in the selected date range.' },
    { title: 'Received in period', value: qty(data.movementSummary.receivedInPeriod), icon: <ArrowDownOutlined />, tip: 'IN stock transactions in the selected date range.' },
  ];

  const financeMetrics = [
    { title: 'Invoiced', value: money(data.billingSummary.totalInvoicedInPeriod) },
    { title: 'Cash received', value: money(data.paymentSummary.cashReceived) },
    { title: 'TDS', value: money(data.paymentSummary.tds) },
    { title: 'Outstanding', value: money(data.billingSummary.totalOutstanding) },
  ];

  return (
    <div className="page-stack dashboard-page">
      <section className="page-header">
        <div>
          <span className="page-kicker"><CalendarOutlined /> Operational dashboard</span>
          <Typography.Title level={2}>Stock, site work, billing and exceptions</Typography.Title>
          <p className="page-description">Live read-only overview from inventory, site balances, challans, agreements, invoices, payments and reports.</p>
        </div>
        <Space wrap>
          <RangePicker value={range} onChange={(value) => value?.[0] && value?.[1] && setRange([value[0], value[1]])} />
          <InputNumber min={1} placeholder="Party ID" value={partyId ?? undefined} onChange={(value) => setPartyId(value ?? null)} />
          <InputNumber min={1} placeholder="Site ID" value={siteId ?? undefined} onChange={(value) => setSiteId(value ?? null)} />
          <InputNumber min={1} placeholder="Category ID" value={categoryId ?? undefined} onChange={(value) => setCategoryId(value ?? null)} />
          <Button icon={<ReloadOutlined />} onClick={() => overview.refetch()}>Refresh</Button>
        </Space>
      </section>

      <Alert type="info" showIcon message={`Last updated ${dayjs(data.generatedAt).format('DD MMM YYYY, HH:mm')}. Role view: ${data.roleMode}${isViewer ? '. Quick actions hidden.' : ''}`} />

      <Row gutter={[16, 16]}>
        {operationalMetrics.map((metric) => (
          <Col xs={24} sm={12} lg={6} key={metric.title}>
            <Card className="metric-card compact-card">
              <Tooltip title={metric.tip}><div className="metric-icon">{metric.icon}</div></Tooltip>
              <Statistic title={metric.title} value={metric.value} />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Stock By Status" className="premium-card">
            <DashboardBar data={data.stockByStatus} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Issued Versus Received" className="premium-card">
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={data.movementTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <ChartTooltip />
                <Legend />
                <Bar dataKey="issued" fill="#0f766e" />
                <Bar dataKey="received" fill="#f59e0b" />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Top Sites By Pending Material" className="premium-card">
            <DashboardBar data={data.topSites} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Outstanding Ageing" className="premium-card">
            {data.outstandingAgeing.length ? <DashboardBar data={data.outstandingAgeing} /> : <Empty description="Financial ageing is visible to accounts roles" />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {financeMetrics.map((metric) => (
          <Col xs={24} sm={12} lg={6} key={metric.title}>
            <Card className="metric-card compact-card">
              <div className="metric-icon"><DollarOutlined /></div>
              <Statistic title={metric.title} value={metric.value} />
            </Card>
          </Col>
        ))}
      </Row>

      <Card title="Quick Actions" className="premium-card">
        {data.quickActions.length ? (
          <Space wrap>
            {data.quickActions.map((action) => <Button key={action.key} icon={<ToolOutlined />} onClick={() => navigate(action.targetPath)}>{action.label}</Button>)}
          </Space>
        ) : <Empty description="No create or approval actions for this role" />}
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title={<><AlertOutlined /> Attention Items</>} className="premium-card">
            <Table rowKey="key" size="small" columns={attentionColumns} dataSource={data.attentionItems} pagination={false} locale={{ emptyText: 'No urgent attention items' }} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Recent Documents" className="premium-card">
            <Table rowKey={(row) => `${row.type}-${row.id}`} size="small" columns={documentColumns} dataSource={data.recentDocuments} pagination={false} />
          </Card>
        </Col>
      </Row>

      <Card title="Recent Activity" className="premium-card">
        <Table
          rowKey="id"
          size="small"
          dataSource={data.recentActivity}
          pagination={false}
          columns={[
            { title: 'Action', dataIndex: 'action' },
            { title: 'User', dataIndex: 'username' },
            { title: 'When', dataIndex: 'createdAt', render: (value) => dayjs(value).format('DD MMM HH:mm') },
            { title: 'Description', dataIndex: 'description' },
          ]}
        />
      </Card>
    </div>
  );
}

function DashboardBar({ data }: { data: ChartPoint[] }) {
  if (!data.length) return <Empty description="No chart data" />;
  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="label" />
        <YAxis />
        <ChartTooltip />
        <Bar dataKey="value" fill="#0f766e" />
      </BarChart>
    </ResponsiveContainer>
  );
}
