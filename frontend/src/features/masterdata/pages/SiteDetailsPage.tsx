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
  Modal,
  Descriptions,
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
  EyeOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import { apiClient } from '../../../api/client';
import { reportsApi } from '../../reports/api';
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
  partyName?: string;
  siteName?: string;
  items?: { itemId: number; itemName?: string; itemCode?: string; unit?: string; quantity: number; fulfilledQuantity?: number }[];
}

interface IssuedChallan {
  id: number;
  challanNumber: string;
  dispatchDate: string;
  vehicleNumber?: string;
  driverName?: string;
  status: string;
  siteOrderNumber?: string;
  partyName?: string;
  siteName?: string;
  notes?: string;
  createdBy?: string;
  createdAt?: string;
  items?: { itemId: number; itemName?: string; itemCode?: string; unit?: string; quantity: number }[];
}

interface ReceivingChallan {
  id: number;
  challanNumber: string;
  receiveDate: string;
  vehicleNumber?: string;
  driverName?: string;
  status: string;
  partyName?: string;
  siteName?: string;
  notes?: string;
  createdBy?: string;
  createdAt?: string;
  items?: { id?: number; itemId?: number; itemName?: string; itemCode?: string; unit?: string; goodReturnedQuantity?: number; damagedReturnedQuantity?: number; lostQuantity?: number }[];
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

export async function exportSiteStockExcel(siteId: number) {
  const result = await reportsApi.export('SITE_PENDING_STOCK', { siteId }, 'EXCEL');
  return reportsApi.downloadUrl(result.id);
}

export function SiteDetailsPage() {
  const { siteId } = useParams<{ siteId: string }>();
  const navigate = useNavigate();
  const numericSiteId = Number(siteId);

  const [stockSearch, setStockSearch] = useState('');
  const [ledgerModalOpen, setLedgerModalOpen] = useState(false);
  const [exporting, setExporting] = useState(false);

  // Selected items for viewing in Modal
  const [selectedIssuedChallan, setSelectedIssuedChallan] = useState<IssuedChallan | null>(null);
  const [selectedReceivingChallan, setSelectedReceivingChallan] = useState<ReceivingChallan | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<SiteOrder | null>(null);

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

  // Download PDF for Issued Challan
  const handleDownloadPdf = async (c: IssuedChallan) => {
    try {
      const res = await apiClient.get(`/challans/issued/${c.id}/pdf`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `challan-${c.challanNumber.replaceAll('/', '-')}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Failed to download challan PDF');
    }
  };

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
          unit: itm.unit || 'pcs',
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
          unit: itm.unit || 'pcs',
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
      const downloadUrl = await exportSiteStockExcel(numericSiteId);
      window.open(downloadUrl, '_blank', 'noopener,noreferrer');
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
    {
      title: 'Action',
      key: 'action',
      width: 100,
      render: (_: any, r: SiteOrder) => (
        <Button size="small" icon={<EyeOutlined />} onClick={() => setSelectedOrder(r)}>
          View
        </Button>
      ),
    },
  ];

  const issuedColumns = [
    { title: 'Challan No.', dataIndex: 'challanNumber', key: 'challanNumber', render: (v: string) => <code>{v}</code> },
    { title: 'Dispatch Date', dataIndex: 'dispatchDate', key: 'dispatchDate' },
    { title: 'Vehicle', dataIndex: 'vehicleNumber', key: 'vehicleNumber', render: (v: string) => v || '-' },
    { title: 'Driver', dataIndex: 'driverName', key: 'driverName', render: (v: string) => v || '-' },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color="processing">{v}</Tag> },
    {
      title: 'Dispatched Materials & Quantities',
      key: 'items',
      render: (_: any, r: IssuedChallan) => {
        if (!r.items || r.items.length === 0) return <span style={{ color: '#8c8c8c' }}>-</span>;
        return (
          <Space wrap size={[4, 6]}>
            {r.items.map((itm, idx) => (
              <Tag key={idx} color="teal" style={{ fontSize: '0.85rem', padding: '2px 8px' }}>
                <strong>{itm.itemName || itm.itemCode || `Item #${itm.itemId}`}</strong>: {Number(itm.quantity || 0).toLocaleString('en-IN')} {itm.unit || 'NOS'}
              </Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: 'Action',
      key: 'action',
      width: 160,
      render: (_: any, r: IssuedChallan) => (
        <Space wrap>
          <Button size="small" type="primary" ghost icon={<EyeOutlined />} onClick={() => setSelectedIssuedChallan(r)}>
            View
          </Button>
          <Button size="small" icon={<DownloadOutlined />} onClick={() => handleDownloadPdf(r)}>
            PDF
          </Button>
        </Space>
      ),
    },
  ];

  const receivingColumns = [
    { title: 'Challan No.', dataIndex: 'challanNumber', key: 'challanNumber', render: (v: string) => <code>{v}</code> },
    { title: 'Return Date', dataIndex: 'receiveDate', key: 'receiveDate' },
    { title: 'Vehicle', dataIndex: 'vehicleNumber', key: 'vehicleNumber', render: (v: string) => v || '-' },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color={v === 'CANCELLED' ? 'error' : 'success'}>{v}</Tag> },
    {
      title: 'Returned Materials',
      key: 'items',
      render: (_: any, r: ReceivingChallan) => {
        if (!r.items || r.items.length === 0) return <span style={{ color: '#8c8c8c' }}>-</span>;
        return (
          <Space wrap size={[4, 6]}>
            {r.items.map((itm, idx) => (
              <Tag key={idx} color="green" style={{ fontSize: '0.85rem', padding: '2px 8px' }}>
                <strong>{itm.itemName || itm.itemCode}</strong>: Good {Number(itm.goodReturnedQuantity || 0).toLocaleString('en-IN')}
                {(itm.damagedReturnedQuantity || 0) > 0 && ` | Dmg ${itm.damagedReturnedQuantity}`}
                {(itm.lostQuantity || 0) > 0 && ` | Lost ${itm.lostQuantity}`}
              </Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: 'Action',
      key: 'action',
      width: 100,
      render: (_: any, r: ReceivingChallan) => (
        <Button size="small" icon={<EyeOutlined />} onClick={() => setSelectedReceivingChallan(r)}>
          View
        </Button>
      ),
    },
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
                  <Table
                    rowKey="id"
                    columns={orderColumns}
                    dataSource={orders}
                    pagination={{ pageSize: 10 }}
                    scroll={{ x: 650 }}
                  />
                </div>
              ),
            },
            {
              key: 'issued_challans',
              label: `Issued Challans (${issuedChallans.length})`,
              children: (
                <div style={{ paddingTop: 12 }}>
                  <Table
                    rowKey="id"
                    columns={issuedColumns}
                    dataSource={issuedChallans}
                    pagination={{ pageSize: 10 }}
                    scroll={{ x: 800 }}
                  />
                </div>
              ),
            },
            {
              key: 'receiving_challans',
              label: `Receiving Challans (${receivingChallans.length})`,
              children: (
                <div style={{ paddingTop: 12 }}>
                  <Table
                    rowKey="id"
                    columns={receivingColumns}
                    dataSource={receivingChallans}
                    pagination={{ pageSize: 10 }}
                    scroll={{ x: 750 }}
                  />
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

      {/* Issued Challan Details Modal */}
      <Modal
        title={`Issued Delivery Challan: ${selectedIssuedChallan?.challanNumber || ''}`}
        open={Boolean(selectedIssuedChallan)}
        width={750}
        footer={null}
        onCancel={() => setSelectedIssuedChallan(null)}
      >
        {selectedIssuedChallan && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Challan Number"><code>{selectedIssuedChallan.challanNumber}</code></Descriptions.Item>
              <Descriptions.Item label="Dispatch Date">{selectedIssuedChallan.dispatchDate}</Descriptions.Item>
              <Descriptions.Item label="Site">{site.siteName}</Descriptions.Item>
              <Descriptions.Item label="Party">{site.partyLegalName || site.partyName || '-'}</Descriptions.Item>
              <Descriptions.Item label="Vehicle">{selectedIssuedChallan.vehicleNumber || '-'}</Descriptions.Item>
              <Descriptions.Item label="Driver">{selectedIssuedChallan.driverName || '-'}</Descriptions.Item>
              <Descriptions.Item label="Status"><Tag color="processing">{selectedIssuedChallan.status}</Tag></Descriptions.Item>
              {selectedIssuedChallan.siteOrderNumber && (
                <Descriptions.Item label="Order No."><code>{selectedIssuedChallan.siteOrderNumber}</code></Descriptions.Item>
              )}
            </Descriptions>

            <div>
              <h4 style={{ margin: '8px 0 12px' }}>Material Items & Quantities</h4>
              <Table
                rowKey={(_r, i) => i || 0}
                pagination={false}
                size="small"
                dataSource={selectedIssuedChallan.items || []}
                columns={[
                  { title: 'Code', dataIndex: 'itemCode', key: 'itemCode', render: (v: string) => <code>{v || '-'}</code> },
                  { title: 'Material Item Name', dataIndex: 'itemName', key: 'itemName', render: (v: string) => <strong>{v}</strong> },
                  { title: 'Unit', dataIndex: 'unit', key: 'unit', render: (v: string) => v || 'NOS' },
                  {
                    title: 'Dispatched Quantity',
                    dataIndex: 'quantity',
                    key: 'quantity',
                    render: (v: number, r: any) => (
                      <strong style={{ color: '#006565' }}>
                        {Number(v || 0).toLocaleString('en-IN')} {r.unit || 'NOS'}
                      </strong>
                    ),
                  },
                ]}
              />
            </div>

            {selectedIssuedChallan.notes && (
              <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                <strong>Remarks / Notes:</strong> {selectedIssuedChallan.notes}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 8 }}>
              <Button icon={<DownloadOutlined />} onClick={() => handleDownloadPdf(selectedIssuedChallan)}>
                Download PDF
              </Button>
              <Button type="primary" onClick={() => setSelectedIssuedChallan(null)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Receiving Challan Details Modal */}
      <Modal
        title={`Receiving Return Challan: ${selectedReceivingChallan?.challanNumber || ''}`}
        open={Boolean(selectedReceivingChallan)}
        width={750}
        footer={null}
        onCancel={() => setSelectedReceivingChallan(null)}
      >
        {selectedReceivingChallan && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Challan Number"><code>{selectedReceivingChallan.challanNumber}</code></Descriptions.Item>
              <Descriptions.Item label="Return Date">{selectedReceivingChallan.receiveDate}</Descriptions.Item>
              <Descriptions.Item label="Vehicle">{selectedReceivingChallan.vehicleNumber || '-'}</Descriptions.Item>
              <Descriptions.Item label="Driver">{selectedReceivingChallan.driverName || '-'}</Descriptions.Item>
              <Descriptions.Item label="Status"><Tag color={selectedReceivingChallan.status === 'CANCELLED' ? 'error' : 'success'}>{selectedReceivingChallan.status}</Tag></Descriptions.Item>
            </Descriptions>

            <div>
              <h4 style={{ margin: '8px 0 12px' }}>Returned Material Items</h4>
              <Table
                rowKey={(_r, i) => i || 0}
                pagination={false}
                size="small"
                dataSource={selectedReceivingChallan.items || []}
                columns={[
                  { title: 'Code', dataIndex: 'itemCode', key: 'itemCode', render: (v: string) => <code>{v || '-'}</code> },
                  { title: 'Material Name', dataIndex: 'itemName', key: 'itemName', render: (v: string) => <strong>{v}</strong> },
                  { title: 'Good Qty', dataIndex: 'goodReturnedQuantity', key: 'goodReturnedQuantity', render: (v: number) => Number(v || 0).toLocaleString('en-IN') },
                  { title: 'Damaged Qty', dataIndex: 'damagedReturnedQuantity', key: 'damagedReturnedQuantity', render: (v: number) => v ? <Tag color="warning">{v}</Tag> : '0' },
                  { title: 'Lost Qty', dataIndex: 'lostQuantity', key: 'lostQuantity', render: (v: number) => v ? <Tag color="error">{v}</Tag> : '0' },
                ]}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 8 }}>
              <Button type="primary" onClick={() => setSelectedReceivingChallan(null)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Site Order Details Modal */}
      <Modal
        title={`Site Order: ${selectedOrder?.orderNumber || ''}`}
        open={Boolean(selectedOrder)}
        width={650}
        footer={null}
        onCancel={() => setSelectedOrder(null)}
      >
        {selectedOrder && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Order Number"><code>{selectedOrder.orderNumber}</code></Descriptions.Item>
              <Descriptions.Item label="Order Date">{selectedOrder.orderDate}</Descriptions.Item>
              <Descriptions.Item label="Requirement">{selectedOrder.requirement || '-'}</Descriptions.Item>
              <Descriptions.Item label="Status"><Tag color={selectedOrder.status === 'CONFIRMED' ? 'success' : 'default'}>{selectedOrder.status}</Tag></Descriptions.Item>
            </Descriptions>

            <div>
              <h4 style={{ margin: '8px 0 12px' }}>Order Items</h4>
              <Table
                rowKey={(_r, i) => i || 0}
                pagination={false}
                size="small"
                dataSource={selectedOrder.items || []}
                columns={[
                  { title: 'Code', dataIndex: 'itemCode', key: 'itemCode', render: (v: string) => <code>{v || '-'}</code> },
                  { title: 'Material Name', dataIndex: 'itemName', key: 'itemName', render: (v: string) => <strong>{v}</strong> },
                  { title: 'Ordered Qty', dataIndex: 'quantity', key: 'quantity', render: (v: number) => Number(v || 0).toLocaleString('en-IN') },
                  { title: 'Fulfilled Qty', dataIndex: 'fulfilledQuantity', key: 'fulfilledQuantity', render: (v: number) => Number(v || 0).toLocaleString('en-IN') },
                ]}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 8 }}>
              <Button type="primary" onClick={() => setSelectedOrder(null)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>

      <LedgerImportModal
        open={ledgerModalOpen}
        siteId={numericSiteId}
        siteName={site.siteName}
        onClose={() => setLedgerModalOpen(false)}
      />
    </div>
  );
}
