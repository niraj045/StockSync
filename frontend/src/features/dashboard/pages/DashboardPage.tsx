import {
  AlertOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
  BellOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  FileTextOutlined,
  InboxOutlined,
  ReloadOutlined,
  ToolOutlined,
  WarningOutlined,
  EnvironmentOutlined,
  HomeOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  DatePicker,
  Empty,
  Select,
  Skeleton,
  Tag,
  Tooltip,
} from 'antd';
import { useQuery } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { useState, type ReactNode } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip as ChartTooltip,
  XAxis,
  YAxis,
} from 'recharts';
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
type PageResult<T> = { content?: T[] };
type PartyOption = { id: number; legalName: string };
type SiteOption = { id: number; siteName: string; partyId?: number };
type CategoryOption = { id: number; name: string };

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
const quantity = (value?: number) => Number(value ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 });
const currency = (value?: number) => `₹${Number(value ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;
const initialRange = [dayjs().subtract(29, 'day'), dayjs()] as [Dayjs, Dayjs];
const optionRows = <T,>(response?: PageResult<T>) => Array.isArray(response?.content) ? response.content : [];

export function DashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [draftRange, setDraftRange] = useState<[Dayjs, Dayjs]>(initialRange);
  const [appliedRange, setAppliedRange] = useState<[Dayjs, Dayjs]>(initialRange);
  const [draftParty, setDraftParty] = useState<number>();
  const [draftSite, setDraftSite] = useState<number>();
  const [draftCategory, setDraftCategory] = useState<number>();
  const [filters, setFilters] = useState<{ partyId?: number; siteId?: number; categoryId?: number }>({});
  const [from, to] = appliedRange;

  const overview = useQuery({
    queryKey: ['dashboard-overview', from.format('YYYY-MM-DD'), to.format('YYYY-MM-DD'), filters.partyId, filters.siteId, filters.categoryId],
    queryFn: async () => (await apiClient.get<DashboardOverview>('/dashboard/overview', {
      params: {
        dateFrom: from.format('YYYY-MM-DD'),
        dateTo: to.format('YYYY-MM-DD'),
        partyId: filters.partyId,
        siteId: filters.siteId,
        categoryId: filters.categoryId,
      },
    })).data,
  });
  const parties = useQuery({
    queryKey: ['dashboard-parties'],
    queryFn: async () => (await apiClient.get<PageResult<PartyOption>>('/parties', { params: { size: 200, active: true } })).data,
  });
  const sites = useQuery({
    queryKey: ['dashboard-sites', draftParty],
    queryFn: async () => (await apiClient.get<PageResult<SiteOption>>('/sites', { params: { size: 200, partyId: draftParty, status: 'ACTIVE' } })).data,
  });
  const categories = useQuery({
    queryKey: ['dashboard-categories'],
    queryFn: async () => (await apiClient.get<PageResult<CategoryOption>>('/categories', { params: { size: 200, active: true } })).data,
  });

  const data = overview.data;
  const isViewer = user?.roles.includes('ROLE_VIEWER');
  const siteRows = optionRows(sites.data).filter((site) => !draftParty || !site.partyId || site.partyId === draftParty);
  const primaryAction = data?.quickActions.find((action) => action.targetPath.includes('challans/issued')) ?? data?.quickActions[0];

  const applyFilters = () => {
    setAppliedRange(draftRange);
    setFilters({ partyId: draftParty, siteId: draftSite, categoryId: draftCategory });
  };
  const resetFilters = () => {
    setDraftRange(initialRange);
    setAppliedRange(initialRange);
    setDraftParty(undefined);
    setDraftSite(undefined);
    setDraftCategory(undefined);
    setFilters({});
  };

  if (overview.isLoading) {
    return <div className="dashboard-control-room"><Skeleton active paragraph={{ rows: 14 }} /></div>;
  }
  if (overview.isError) {
    return <Alert type="error" showIcon message="Dashboard could not be loaded" action={<Button onClick={() => overview.refetch()}>Retry</Button>} />;
  }
  if (!data) return <Empty description="No dashboard data available" />;

  const operationalMetrics = [
    { title: 'Godown available', value: quantity(data.stockSummary.godownAvailable), note: `${quantity(data.stockByStatus.length)} tracked stock groups`, icon: <HomeOutlined />, tone: 'teal' },
    { title: 'At sites', value: quantity(data.stockSummary.materialAtSites), note: `${data.topSites.length} sites with pending stock`, icon: <EnvironmentOutlined />, tone: 'indigo', badge: `${data.agreementSummary.activeAgreements} active` },
    { title: 'Issued in period', value: quantity(data.movementSummary.issuedInPeriod), note: data.movementSummary.issuedInPeriod ? `${quantity(data.movementSummary.issuedToday)} issued today` : 'No active shipments', icon: <ArrowUpOutlined />, tone: 'blue' },
    { title: 'Received in period', value: quantity(data.movementSummary.receivedInPeriod), note: data.movementSummary.receivedInPeriod ? `${quantity(data.movementSummary.receivedToday)} received today` : 'No pending intakes', icon: <InboxOutlined />, tone: 'red' },
  ];
  const financeMetrics = [
    { title: 'Invoiced', value: currency(data.billingSummary.totalInvoicedInPeriod), note: `${data.billingSummary.issuedInvoices} issued invoices`, tone: 'teal' },
    { title: 'Cash received', value: currency(data.paymentSummary.cashReceived), note: 'Net posted collections', tone: 'neutral' },
    { title: 'TDS deducted', value: currency(data.paymentSummary.tds), note: 'Tax compliance', tone: 'neutral' },
    { title: 'Outstanding', value: currency(data.billingSummary.totalOutstanding), note: `${data.exceptionSummary.overdueInvoices} overdue invoices`, tone: 'danger' },
  ];

  return (
    <div className="dashboard-control-room">
      <header className="dashboard-titlebar">
        <div>
          <span className="dashboard-overline">Operations control</span>
          <h1>Operational Dashboard</h1>
          <p>Stock, site work, billing and exceptions</p>
        </div>
        <div className="dashboard-title-actions">
          <div className="dashboard-updated">
            <span>Last updated: {dayjs(data.generatedAt).format('DD MMM YYYY, HH:mm')}</span>
            <strong>{data.roleMode.replaceAll('_', ' ')} view</strong>
          </div>
          <Tooltip title="Refresh dashboard"><Button aria-label="Refresh dashboard" icon={<ReloadOutlined />} onClick={() => overview.refetch()} /></Tooltip>
          <Tooltip title="Attention centre"><Button aria-label="Attention centre" icon={<BellOutlined />} onClick={() => document.getElementById('dashboard-attention')?.scrollIntoView({ behavior: 'smooth' })} /></Tooltip>
          {!isViewer && primaryAction && <Button type="primary" onClick={() => navigate(primaryAction.targetPath)}>{primaryAction.label}</Button>}
        </div>
      </header>

      <section className="dashboard-filter-panel" aria-label="Dashboard filters">
        <label>
          <span>Date range</span>
          <RangePicker value={draftRange} format="DD MMM YYYY" allowClear={false} suffixIcon={<CalendarOutlined />} onChange={(value) => value?.[0] && value?.[1] && setDraftRange([value[0], value[1]])} />
        </label>
        <label>
          <span>Party</span>
          <Select allowClear showSearch optionFilterProp="label" placeholder="All parties" value={draftParty} onChange={(value) => { setDraftParty(value); setDraftSite(undefined); }} options={optionRows(parties.data).map((party) => ({ value: party.id, label: party.legalName }))} />
        </label>
        <label>
          <span>Site</span>
          <Select allowClear showSearch optionFilterProp="label" placeholder="All sites" value={draftSite} onChange={setDraftSite} options={siteRows.map((site) => ({ value: site.id, label: site.siteName }))} />
        </label>
        <label>
          <span>Category</span>
          <Select allowClear showSearch optionFilterProp="label" placeholder="All categories" value={draftCategory} onChange={setDraftCategory} options={optionRows(categories.data).map((category) => ({ value: category.id, label: category.name }))} />
        </label>
        <div className="dashboard-filter-actions">
          <Button type="primary" onClick={applyFilters}>Apply filters</Button>
          <Button onClick={resetFilters}>Reset</Button>
        </div>
      </section>

      <section className="dashboard-kpi-grid">
        {operationalMetrics.map((metric) => (
          <article className="dashboard-kpi-card" key={metric.title}>
            <div className="dashboard-kpi-head">
              <span className={`dashboard-icon dashboard-icon-${metric.tone}`}>{metric.icon}</span>
              {metric.badge && <span className="dashboard-soft-badge">{metric.badge}</span>}
            </div>
            <span className="dashboard-card-label">{metric.title}</span>
            <strong className="dashboard-card-value">{metric.value}</strong>
            <small><i className={`dashboard-dot dashboard-dot-${metric.tone}`} />{metric.note}</small>
          </article>
        ))}
      </section>

      <section className="dashboard-two-column">
        <article className="dashboard-panel dashboard-stock-panel">
          <h2>Stock distribution</h2>
          <StockDistribution data={data} />
        </article>
        <article className="dashboard-panel dashboard-trend-panel">
          <h2>Issued vs received</h2>
          {data.movementTrend.some((point) => point.issued || point.received)
            ? <MovementTrend data={data.movementTrend} />
            : <DashboardEmpty icon={<BarChartOutlined />} title="No movement detected" description="No stock movement during this period. Create an issued or receiving challan to see the trend." action="View activity logs" onAction={() => navigate('/audit-logs')} />}
        </article>
      </section>

      <section className="dashboard-two-column">
        <article id="dashboard-attention" className="dashboard-panel dashboard-list-panel">
          <div className="dashboard-panel-heading">
            <h2>Attention required</h2>
            <Tag color="red">{data.attentionItems.length} alerts</Tag>
          </div>
          {data.attentionItems.length ? data.attentionItems.slice(0, 5).map((item) => (
            <button aria-label={item.title} className="dashboard-attention-row" key={item.key} onClick={() => navigate(item.targetPath)}>
              <span className={`dashboard-alert-icon dashboard-alert-${item.severity}`}><AlertOutlined /></span>
              <span className="dashboard-row-copy"><strong>{item.title}</strong><small>{item.description}</small></span>
              <b>{item.count}</b>
              <span className={`dashboard-severity dashboard-severity-${item.severity}`}>{item.severity}</span>
            </button>
          )) : <DashboardEmpty compact icon={<CheckCircleOutlined />} title="All clear" description="There are no urgent operational alerts." />}
        </article>
        <article className="dashboard-panel">
          <h2>Top sites by pending stock</h2>
          {data.topSites.length ? <TopSites data={data.topSites} /> : <DashboardEmpty icon={<EnvironmentOutlined />} title="No pending site stock" description="No pending stock at sites for this period." />}
        </article>
      </section>

      <section className="dashboard-finance-grid">
        {financeMetrics.map((metric) => (
          <article className={`dashboard-finance-card dashboard-finance-${metric.tone}`} key={metric.title}>
            <span>{metric.title}</span>
            <strong>{metric.value}</strong>
            <small>{metric.note}</small>
          </article>
        ))}
      </section>

      <section className="dashboard-two-column dashboard-bottom-grid">
        <article className="dashboard-panel dashboard-documents-panel">
          <div className="dashboard-panel-heading">
            <h2>Recent documents</h2>
            <Button type="link" onClick={() => navigate('/documents')}>View all</Button>
          </div>
          <div className="dashboard-document-head"><span>Type</span><span>No.</span><span>Party / site</span><span>Date</span><span>Status</span></div>
          {data.recentDocuments.length ? data.recentDocuments.slice(0, 6).map((document) => (
            <button className="dashboard-document-row" key={`${document.type}-${document.id}`} onClick={() => navigate(document.targetPath)}>
              <span>{document.type}</span>
              <code>{document.number}</code>
              <span>StockSync operation</span>
              <span>{dayjs(document.documentDate).format('DD MMM')}</span>
              <StatusPill value={document.status} />
            </button>
          )) : <DashboardEmpty compact icon={<FileTextOutlined />} title="No recent documents" description="Documents created in this period will appear here." />}
        </article>
        <article className="dashboard-panel dashboard-activity-panel">
          <h2>Recent business activity</h2>
          {data.recentActivity.length ? data.recentActivity.slice(0, 6).map((activity, index) => (
            <div className="dashboard-activity-row" key={activity.id}>
              <div className="dashboard-activity-rail">
                <span>{activityIcon(activity.action)}</span>
                {index < Math.min(data.recentActivity.length, 6) - 1 && <i />}
              </div>
              <div><strong>{activity.action.replaceAll('_', ' ')}</strong><p>{activity.description}</p><small>{activity.username} · {dayjs(activity.createdAt).format('DD MMM, HH:mm')}</small></div>
            </div>
          )) : <DashboardEmpty compact icon={<ClockCircleOutlined />} title="No recent activity" description="Posted business events will appear here." />}
        </article>
      </section>
    </div>
  );
}

function StockDistribution({ data }: { data: DashboardOverview }) {
  const godown = Number(data.stockSummary.godownAvailable ?? 0);
  const sites = Number(data.stockSummary.materialAtSites ?? 0);
  const total = Math.max(godown + sites, 1);
  return (
    <div className="dashboard-stock-distribution">
      <ProgressLine label="Godown" value={godown} percent={(godown / total) * 100} tone="teal" />
      <ProgressLine label="At sites" value={sites} percent={(sites / total) * 100} tone="indigo" />
      <div className="dashboard-stock-buckets">
        <div><span>Damaged / repair</span><strong>{quantity(data.stockSummary.damaged + data.stockSummary.underRepair)}</strong></div>
        <div><span>Lost / scrapped</span><strong>{quantity(data.stockSummary.lost + data.stockSummary.scrapped)}</strong></div>
      </div>
    </div>
  );
}

function ProgressLine({ label, value, percent, tone }: { label: string; value: number; percent: number; tone: string }) {
  return <div className="dashboard-progress"><div><span>{label}</span><strong>{quantity(value)}</strong></div><div className="dashboard-progress-track"><i className={`dashboard-progress-${tone}`} style={{ width: `${Math.max(percent, value ? 2 : 0)}%` }} /></div></div>;
}

function MovementTrend({ data }: { data: TrendPoint[] }) {
  return (
    <ResponsiveContainer width="100%" height={230}>
      <BarChart data={data} margin={{ top: 12, right: 6, left: -24, bottom: 0 }}>
        <CartesianGrid stroke="#e6ebe9" vertical={false} />
        <XAxis dataKey="date" tick={{ fill: '#667085', fontSize: 11 }} axisLine={false} tickLine={false} />
        <YAxis tick={{ fill: '#667085', fontSize: 11 }} axisLine={false} tickLine={false} />
        <ChartTooltip />
        <Bar dataKey="issued" name="Issued" fill="#006565" radius={[4, 4, 0, 0]} />
        <Bar dataKey="received" name="Received" fill="#8bceff" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}

function TopSites({ data }: { data: ChartPoint[] }) {
  const maximum = Math.max(...data.map((item) => item.value), 1);
  return <div className="dashboard-top-sites">{data.slice(0, 6).map((site, index) => <div key={site.label}><span className="dashboard-rank">{index + 1}</span><span className="dashboard-site-name">{site.label}</span><span className="dashboard-site-bar"><i style={{ width: `${(site.value / maximum) * 100}%` }} /></span><strong>{quantity(site.value)}</strong></div>)}</div>;
}

function DashboardEmpty({ icon, title, description, action, onAction, compact = false }: { icon: ReactNode; title: string; description: string; action?: string; onAction?: () => void; compact?: boolean }) {
  return <div className={`dashboard-empty ${compact ? 'dashboard-empty-compact' : ''}`}><span>{icon}</span><strong>{title}</strong><p>{description}</p>{action && <Button type="link" onClick={onAction}>{action}</Button>}</div>;
}

function StatusPill({ value }: { value: string }) {
  const normalized = value.toLowerCase();
  const tone = normalized.includes('issue') || normalized.includes('active') || normalized.includes('settled') || normalized.includes('posted') ? 'success' : normalized.includes('draft') || normalized.includes('pending') ? 'pending' : 'neutral';
  return <span className={`dashboard-status dashboard-status-${tone}`}>{value.replaceAll('_', ' ')}</span>;
}

function activityIcon(action: string) {
  const normalized = action.toLowerCase();
  if (normalized.includes('issue') || normalized.includes('post')) return <CheckCircleOutlined />;
  if (normalized.includes('agreement')) return <FileTextOutlined />;
  if (normalized.includes('damage') || normalized.includes('loss') || normalized.includes('exception')) return <WarningOutlined />;
  if (normalized.includes('payment') || normalized.includes('invoice')) return <DollarOutlined />;
  if (normalized.includes('repair')) return <ToolOutlined />;
  return <ClockCircleOutlined />;
}
