import { useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import {
  Card,
  Table,
  Tag,
  Tabs,
  Button,
  Statistic,
  Row,
  Col,
  Input,
  Space,
  Spin,
  Alert,
  Breadcrumb,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  EnvironmentOutlined,
  UploadOutlined,
  FileExcelOutlined,
  SearchOutlined,
  ShoppingOutlined,
  CheckCircleOutlined,
  TruckOutlined,
  FileTextOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { apiClient } from '../../../api/client';
import { LedgerImportModal } from '../components/LedgerImportModal';

interface SiteProfile {
  id: number;
  siteName: string;
  siteCode: string;
  partyId: number;
  partyName?: string;
  partyLegalName?: string;
  address?: string;
  contactPerson?: string;
  phone?: string;
  status: string;
  defaulter?: boolean;
}

interface StockBalance {
  itemId: number;
  itemCode: string;
  itemName: string;
  unit: string;
  availableQuantity?: number;
  hiredQuantity?: number;
  pendingQuantity?: number;
  issuedQuantity?: number;
}

interface SiteOrder {
  id: number;
  orderNumber: string;
  orderDate: string;
  status: string;
  fulfillmentStatus?: string;
  requirement?: string;
  items?: { itemId: number; itemName?: string; itemCode?: string; quantity: number }[];
}

interface IssuedChallan {
  id: number;
  challanNumber: string;
  dispatchDate: string;
  vehicleNumber?: string;
  driverName?: string;
  status: string;
  items?: { itemId: number; itemName?: string; itemCode?: string; quantity: number }[];
}

interface ReceivingChallan {
  id: number;
  challanNumber: string;
  receiveDate: string;
  vehicleNumber?: string;
  driverName?: string;
  status: string;
  items?: { id: number; itemName?: string; itemCode?: string; goodReturnedQuantity?: number; damagedReturnedQuantity?: number; lostQuantity?: number }[];
}

interface SiteOperation {
  id: number;
  operationNumber: string;
  operationDate: string;
  operationType: string;
  direction: string;
  providerName?: string;
  transporterName?: string;
  vehicleNumber?: string;
  quantity?: number;
  rate: number;
  amount: number;
  status: string;
}

interface PageResult<T> {
  content?: T[];
}

export function SiteDetailsPage() {
  const { siteId } = useParams<{ siteId: string }>();
  const navigate = useNavigate();
  const numericSiteId = Number(siteId);

  const [stockSearch, setStockSearch] = useState('');
  const [ledgerModalOpen, setLedgerModalOpen] = useState(false);
  const [exporting, setExporting] = useState(false);

  // 1. Fetch site profile
  const siteQuery = useQuery({
    queryKey: ['site-detail', numericSiteId],
    queryFn: async () => (await apiClient.get<SiteProfile>(`/sites/${numericSiteId}`)).data,
    enabled: Boolean(numericSiteId),
  });

  // 2. Fetch stock balances
  const stockQuery = useQuery({
    queryKey: ['site-stock-balances', numericSiteId],
    queryFn: async () => (await apiClient.get<PageResult<StockBalance>>(`/stock/balances/site/${numericSiteId}`, { params: { size: 500 } })).data,
    enabled: Boolean(numericSiteId),
  });

  // 3. Fetch site orders
  const ordersQuery = useQuery({
    queryKey: ['site-orders', numericSiteId],
    queryFn: async () => (await apiClient.get<PageResult<SiteOrder>>('/orders', { params: { siteId: numericSiteId, size: 200, sort: 'id,desc' } })).data,
    enabled: Boolean(numericSiteId),
  });

  // 4. Fetch issued challans
  const issuedChallansQuery = useQuery({
    queryKey: ['site-issued-challans', numericSiteId],
    queryFn: async () => (await apiClient.get<PageResult<IssuedChallan>>('/challans/issued', { params: { siteId: numericSiteId, size: 200, sort: 'id,desc' } })).data,
    enabled: Boolean(numericSiteId),
  });

  // 5. Fetch receiving challans
  const receivingChallansQuery = useQuery({
    queryKey: ['site-receiving-challans', numericSiteId],
    queryFn: async () => (await apiClient.get<PageResult<ReceivingChallan>>('/challans/receiving', { params: { siteId: numericSiteId, size: 200, sort: 'id,desc' } })).data,
    enabled: Boolean(numericSiteId),
  });

  // 6. Fetch operations
  const operationsQuery = useQuery({
    queryKey: ['site-operations', numericSiteId],
    queryFn: async () => (await apiClient.get<SiteOperation[]>('/client-workflow/operations', { params: { siteId: numericSiteId } })).data,
    enabled: Boolean(numericSiteId),
  });

  const site = siteQuery.data;
  const stockRows = stockQuery.data?.content || [];
  const orders = ordersQuery.data?.content || [];
  const issuedChallans = issuedChallansQuery.data?.content || [];
  const receivingChallans = receivingChallansQuery.data?.content || [];
  const operations = operationsQuery.data || [];

  // Aggregate Material Quantities across Stock, Issued, and Receiving Challans
  const aggregatedMaterials = () => {
    const map = new Map<string, {
      key: string;
      itemId?: number;
      itemCode: string;
      itemName: string;
      unit: string;
      totalDispatched: number;
      totalReturned: number;
      totalLostOrDamaged: number;
      currentSiteQuantity: number;
    }>();

    // 1. Stock balances
    for (const stk of stockRows) {
      const code = stk.itemCode || `ITEM-${stk.itemId}`;
      const key = code.toLowerCase();
      const currentQty = Number(stk.availableQuantity || stk.hiredQuantity || stk.pendingQuantity || stk.issuedQuantity || 0);
      map.set(key, {
        key,
        itemId: stk.itemId,
        itemCode: code,
        itemName: stk.itemName || code,
        unit: stk.unit || 'pcs',
        totalDispatched: 0,
        totalReturned: 0,
        totalLostOrDamaged: 0,
        currentSiteQuantity: currentQty,
      });
    }

    // 2. Issued Challans
    for (const ch of issuedChallans) {
      if (!ch.items) continue;
      for (const itm of ch.items) {
        const code = itm.itemCode || `ITEM-${itm.itemId}`;
        const key = code.toLowerCase();
        const qty = Number(itm.quantity || 0);
        const existing = map.get(key) || {
          key,
          itemId: itm.itemId,
          itemCode: code,
          itemName: itm.itemName || code,
          unit: 'pcs',
          totalDispatched: 0,
          totalReturned: 0,
          totalLostOrDamaged: 0,
          currentSiteQuantity: 0,
        };
        existing.totalDispatched += qty;
        map.set(key, existing);
      }
    }

    // 3. Receiving Challans
    for (const rch of receivingChallans) {
      if (rch.status === 'CANCELLED' || !rch.items) continue;
      for (const itm of rch.items) {
        const code = itm.itemCode || `ITEM-${itm.id}`;
        const key = code.toLowerCase();
        const retQty = Number(itm.goodReturnedQuantity || 0);
        const lossDmg = Number(itm.damagedReturnedQuantity || 0) + Number(itm.lostQuantity || 0);
        const existing = map.get(key) || {
          key,
          itemCode: code,
          itemName: itm.itemName || code,
          unit: 'pcs',
          totalDispatched: 0,
          totalReturned: 0,
          totalLostOrDamaged: 0,
          currentSiteQuantity: 0,
        };
        existing.totalReturned += retQty;
        existing.totalLostOrDamaged += lossDmg;
        map.set(key, existing);
      }
    }

    return Array.from(map.values());
  };

  const materials = aggregatedMaterials();

  const filteredMaterials = materials.filter((m) => {
    if (!stockSearch.trim()) return true;
    const term = stockSearch.trim().toLowerCase();
    return m.itemCode.toLowerCase().includes(term) || m.itemName.toLowerCase().includes(term);
  });

  const totalDispatchedAll = materials.reduce((acc, m) => acc + m.totalDispatched, 0);
  const totalReturnedAll = materials.reduce((acc, m) => acc + m.totalReturned, 0);
  const totalSiteBalanceAll = materials.reduce((acc, m) => acc + m.currentSiteQuantity, 0);

  const handleExportExcel = async () => {
    setExporting(true);
    try {
      const res = await apiClient.post<{ id: number; filename: string }>('/reports/site-inventory/export', {
        filters: { siteId: numericSiteId },
        format: 'EXCEL',
      });
      window.open(`/api/v1/reports/exports/${res.data.id}/download`, '_blank');
      message.success('Excel export downloaded');
    } catch {
      message.error('Unable to export Excel report');
    } finally {
      setExporting(false);
    }
  };

  const refreshAll = () => {
    siteQuery.refetch();
    stockQuery.refetch();
    ordersQuery.refetch();
    issuedChallansQuery.refetch();
    receivingChallansQuery.refetch();
    operationsQuery.refetch();
  };

  if (siteQuery.isLoading) {
    return <div className="page-stack" style={{ padding: 24 }}><Spin size="large" /></div>;
  }

  if (siteQuery.isError || !site) {
    return (
      <div className="page-stack" style={{ padding: 24 }}>
        <Alert type="error" message="Site not found" description="The requested site could not be loaded." />
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/sites')} style={{ marginTop: 16 }}>Back to Sites</Button>
      </div>
    );
  }

  const stockColumns = [
    { title: 'Code', dataIndex: 'itemCode', key: 'itemCode', render: (v: string) => <code>{v}</code> },
    { title: 'Material / Item Name', dataIndex: 'itemName', key: 'itemName', render: (v: string) => <strong>{v}</strong> },
    { title: 'Dispatched', dataIndex: 'totalDispatched', key: 'totalDispatched', render: (v: number) => v.toLocaleString('en-IN') },
    { title: 'Returned', dataIndex: 'totalReturned', key: 'totalReturned', render: (v: number) => v.toLocaleString('en-IN') },
    { title: 'Lost / Damaged', dataIndex: 'totalLostOrDamaged', key: 'totalLostOrDamaged', render: (v: number) => v ? <Tag color="error">{v.toLocaleString('en-IN')}</Tag> : '-' },
    {
      title: 'Site Balance',
      dataIndex: 'currentSiteQuantity',
      key: 'currentSiteQuantity',
      render: (v: number, row: any) => (
        <strong style={{ color: v > 0 ? '#006565' : '#667085', fontSize: '1.05rem' }}>
          {v.toLocaleString('en-IN')} {row.unit}
        </strong>
      ),
    },
  ];

  const orderColumns = [
    { title: 'Order No.', dataIndex: 'orderNumber', key: 'orderNumber', render: (v: string) => <code>{v}</code> },
    { title: 'Date', dataIndex: 'orderDate', key: 'orderDate' },
    { title: 'Requirement', dataIndex: 'requirement', key: 'requirement', render: (v: string) => v || '-' },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color={v === 'CONFIRMED' ? 'success' : 'default'}>{v}</Tag> },
  ];

  const issuedColumns = [
    { title: 'Challan No.', dataIndex: 'challanNumber', key: 'challanNumber', render: (v: string) => <code>{v}</code> },
    { title: 'Dispatch Date', dataIndex: 'dispatchDate', key: 'dispatchDate' },
    { title: 'Vehicle', dataIndex: 'vehicleNumber', key: 'vehicleNumber', render: (v: string) => v || '-' },
    { title: 'Driver', dataIndex: 'driverName', key: 'driverName', render: (v: string) => v || '-' },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color="processing">{v}</Tag> },
    { title: 'Items', key: 'items', render: (_: any, r: IssuedChallan) => r.items ? r.items.length + ' item types' : '-' },
  ];

  const receivingColumns = [
    { title: 'Challan No.', dataIndex: 'challanNumber', key: 'challanNumber', render: (v: string) => <code>{v}</code> },
    { title: 'Return Date', dataIndex: 'receiveDate', key: 'receiveDate' },
    { title: 'Vehicle', dataIndex: 'vehicleNumber', key: 'vehicleNumber', render: (v: string) => v || '-' },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color={v === 'CANCELLED' ? 'error' : 'success'}>{v}</Tag> },
  ];

  const operationColumns = [
    { title: 'Op No.', dataIndex: 'operationNumber', key: 'operationNumber', render: (v: string) => <code>{v}</code> },
    { title: 'Date', dataIndex: 'operationDate', key: 'operationDate' },
    { title: 'Type', dataIndex: 'operationType', key: 'operationType', render: (v: string) => <Tag color="blue">{v.replace('_', ' ')}</Tag> },
    { title: 'Direction', dataIndex: 'direction', key: 'direction', render: (v: string) => <Tag>{v}</Tag> },
    { title: 'Vehicle / Provider', key: 'provider', render: (_: any, r: SiteOperation) => r.vehicleNumber || r.providerName || r.transporterName || '-' },
    { title: 'Amount', dataIndex: 'amount', key: 'amount', render: (v: number) => `₹${v.toLocaleString('en-IN')}` },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color="success">{v}</Tag> },
  ];

  return (
    <div className="page-stack" style={{ padding: '16px 24px', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <Breadcrumb items={[
        { title: <a onClick={() => navigate('/sites')}>Sites</a> },
        { title: site.siteName },
      ]} />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/sites')} />
            <h1 className="page-heading" style={{ margin: 0 }}>{site.siteName}</h1>
            <Tag color={site.status === 'ACTIVE' ? 'success' : 'default'}>{site.status}</Tag>
            {site.defaulter && <Tag color="error">Defaulter</Tag>}
          </div>
          <p className="page-description" style={{ marginTop: 4 }}>
            <EnvironmentOutlined /> {site.partyLegalName || site.partyName || 'Party Site'} &bull; Code: <code>{site.siteCode}</code>
          </p>
        </div>

        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={refreshAll}>Refresh</Button>
          <Button icon={<UploadOutlined />} onClick={() => setLedgerModalOpen(true)}>Import D&R Ledger</Button>
          <Button type="primary" icon={<FileExcelOutlined />} loading={exporting} onClick={handleExportExcel}>
            Export Excel
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card className="premium-card">
            <Statistic title="Total Tracked Items" value={materials.length} prefix={<ShoppingOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="premium-card">
            <Statistic title="Current Site Stock" value={totalSiteBalanceAll} valueStyle={{ color: '#006565' }} prefix={<CheckCircleOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="premium-card">
            <Statistic title="Total Dispatched" value={totalDispatchedAll} prefix={<TruckOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="premium-card">
            <Statistic title="Total Returned" value={totalReturnedAll} prefix={<FileTextOutlined />} />
          </Card>
        </Col>
      </Row>

      <Card className="premium-card">
        <Tabs
          defaultActiveKey="stock"
          items={[
            {
              key: 'stock',
              label: `Material Stock (${materials.length})`,
              children: (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16, paddingTop: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Input
                      placeholder="Search material stock by code or name..."
                      prefix={<SearchOutlined />}
                      value={stockSearch}
                      onChange={(e) => setStockSearch(e.target.value)}
                      style={{ maxWidth: 360 }}
                      allowClear
                    />
                  </div>
                  <Table
                    rowKey="key"
                    columns={stockColumns}
                    dataSource={filteredMaterials}
                    pagination={{ pageSize: 15 }}
                    scroll={{ x: 700 }}
                  />
                </div>
              ),
            },
            {
              key: 'orders',
              label: `Site Orders (${orders.length})`,
              children: (
                <div style={{ paddingTop: 12 }}>
                  <Table rowKey="id" columns={orderColumns} dataSource={orders} pagination={{ pageSize: 10 }} scroll={{ x: 600 }} />
                </div>
              ),
            },
            {
              key: 'issued_challans',
              label: `Issued Challans (${issuedChallans.length})`,
              children: (
                <div style={{ paddingTop: 12 }}>
                  <Table rowKey="id" columns={issuedColumns} dataSource={issuedChallans} pagination={{ pageSize: 10 }} scroll={{ x: 700 }} />
                </div>
              ),
            },
            {
              key: 'receiving_challans',
              label: `Receiving Challans (${receivingChallans.length})`,
              children: (
                <div style={{ paddingTop: 12 }}>
                  <Table rowKey="id" columns={receivingColumns} dataSource={receivingChallans} pagination={{ pageSize: 10 }} scroll={{ x: 600 }} />
                </div>
              ),
            },
            {
              key: 'operations',
              label: `Transport & Expenses (${operations.length})`,
              children: (
                <div style={{ paddingTop: 12 }}>
                  <Table rowKey="id" columns={operationColumns} dataSource={operations} pagination={{ pageSize: 10 }} scroll={{ x: 750 }} />
                </div>
              ),
            },
          ]}
        />
      </Card>

      <LedgerImportModal
        open={ledgerModalOpen}
        siteId={numericSiteId}
        siteName={site.siteName}
        onClose={() => setLedgerModalOpen(false)}
      />
    </div>
  );
}
