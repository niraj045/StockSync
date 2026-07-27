import { useState } from 'react';
import { App, Button, Card, Descriptions, Empty, Form, Input, InputNumber, Modal, Select, Space, Table, Tag } from 'antd';
import { DownloadOutlined, SearchOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { FormDrawer } from '../../../components/FormDrawer';
import type { Agreement } from '../../agreement/types';

type Page<T> = { content: T[]; totalElements: number };

export interface SiteTransferItem {
  id: number;
  sourceAgreementItemId: number;
  destinationAgreementItemId: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  description?: string;
  size?: string;
  unit: string;
  weightPerPiece: number;
  quantity: number;
  totalWeight: number;
  sourcePendingBefore: number;
  sourcePendingAfter: number;
  destinationPendingBefore: number;
  destinationPendingAfter: number;
  sequence: number;
}

export interface SiteTransfer {
  id: number;
  transferNumber: string;
  sourceAgreementId: number;
  sourceAgreementNumber: string;
  destinationAgreementId: number;
  destinationAgreementNumber: string;
  sourcePartyId: number;
  sourcePartyName: string;
  sourceSiteId: number;
  sourceSiteName: string;
  destinationPartyId: number;
  destinationPartyName: string;
  destinationSiteId: number;
  destinationSiteName: string;
  transferDate: string;
  status: string;
  vehicleNumber?: string;
  driverName?: string;
  driverPhone?: string;
  transporterId?: number;
  notes?: string;
  postedAt?: string;
  postedBy?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  cancellationReason?: string;
  items: SiteTransferItem[];
  createdAt: string;
  createdBy: string;
  version: number;
}

type TransferFormValue = {
  sourceAgreementId: number;
  destinationAgreementId: number;
  transferDate: string;
  vehicleNumber?: string;
  driverName?: string;
  driverPhone?: string;
  transporterId?: number;
  notes?: string;
  items: {
    itemId: number;
    itemCode: string;
    itemName: string;
    unit: string;
    sourceAgreementItemId: number;
    destinationAgreementItemId: number;
    quantity: number;
    totalWeight: number;
  }[];
};

export function SiteTransfersPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canWrite = admin || operations;

  const { message, modal } = App.useApp();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<SiteTransfer>();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<SiteTransfer | null>(null);

  const [form] = Form.useForm<TransferFormValue>();
  const selectedSourceAgreementId = Form.useWatch('sourceAgreementId', form);
  const selectedDestinationAgreementId = Form.useWatch('destinationAgreementId', form);

  // Queries
  const transfers = useQuery({
    queryKey: ['site-transfers', search],
    queryFn: async () =>
      (
        await apiClient.get<Page<SiteTransfer>>('/site-transfers', {
          params: { search, size: 50, sort: 'id,desc' },
        })
      ).data,
  });

  const activeAgreements = useQuery({
    queryKey: ['agreements-active-transfers'],
    queryFn: async () =>
      (
        await apiClient.get<Page<Agreement>>('/agreements', {
          params: { status: 'ACTIVE', size: 100 },
        })
      ).data.content,
  });

  const currentSourceAgreement = activeAgreements.data?.find((a) => a.id === selectedSourceAgreementId);
  const currentDestinationAgreement = activeAgreements.data?.find((a) => a.id === selectedDestinationAgreementId);

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['site-transfers'] });
  };

  const handleSourceAgreementChange = (_sourceId: number) => {
    form.setFieldsValue({ destinationAgreementId: undefined as any, items: [] });
  };

  const handleDestinationAgreementChange = (destId: number) => {
    const dest = activeAgreements.data?.find((a) => a.id === destId);
    if (destId === selectedSourceAgreementId) {
      message.error('Source and destination agreements/sites must be different');
      form.setFieldsValue({ destinationAgreementId: undefined as any, items: [] });
      return;
    }
    if (currentSourceAgreement && dest) {
      const matched = currentSourceAgreement.items
        .filter((srcItem) => dest.items.some((destItem) => destItem.itemId === srcItem.itemId))
        .map((srcItem) => {
          const destItem = dest.items.find((di) => di.itemId === srcItem.itemId)!;
          return {
            itemId: srcItem.itemId,
            itemCode: srcItem.itemCode,
            itemName: srcItem.itemName,
            unit: srcItem.unit,
            sourceAgreementItemId: srcItem.id,
            destinationAgreementItemId: destItem.id,
            quantity: 0,
            totalWeight: 0,
          };
        });
      form.setFieldsValue({ items: matched });
    }
  };

  // Mutations
  const saveTransfer = useMutation({
    mutationFn: async (v: TransferFormValue) => {
      const payload = {
        ...v,
        sourcePartyId: currentSourceAgreement?.partyId,
        sourceSiteId: currentSourceAgreement?.siteId,
        destinationPartyId: currentDestinationAgreement?.partyId,
        destinationSiteId: currentDestinationAgreement?.siteId,
        items: v.items
          .filter((i) => i.quantity > 0)
          .map((i) => ({
            itemId: i.itemId,
            sourceAgreementItemId: i.sourceAgreementItemId,
            destinationAgreementItemId: i.destinationAgreementItemId,
            quantity: i.quantity,
            totalWeight: i.totalWeight || 0,
          })),
      };

      if (editing) {
        return (await apiClient.put<SiteTransfer>(`/site-transfers/${editing.id}`, payload)).data;
      } else {
        return (await apiClient.post<SiteTransfer>('/site-transfers', payload)).data;
      }
    },
    onSuccess: (res) => {
      message.success(`Site transfer ${res.transferNumber} saved successfully`);
      setCreateOpen(false);
      setEditing(null);
      form.resetFields();
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Unable to save site transfer');
    },
  });

  const postTransfer = useMutation({
    mutationFn: async (id: number) => {
      return (await apiClient.post<SiteTransfer>(`/site-transfers/${id}/post`)).data;
    },
    onSuccess: (res) => {
      message.success(`Site transfer ${res.transferNumber} posted to stock ledger`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Posting failed');
    },
  });

  const cancelTransfer = useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) => {
      return (await apiClient.post<SiteTransfer>(`/site-transfers/${id}/cancel`, { cancellationReason: reason })).data;
    },
    onSuccess: (res) => {
      message.success(`Site transfer ${res.transferNumber} cancelled`);
      setSelected(undefined);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Cancellation failed');
    },
  });

  const handlePost = (id: number) => {
    modal.confirm({
      title: 'Post Site Transfer',
      content: 'This will deduct materials from the source site and add them to the destination site pending balance. Proceed?',
      okText: 'Post Transfer',
      onOk: () => postTransfer.mutate(id),
    });
  };

  const handleCancel = (id: number) => {
    let reason = '';
    modal.confirm({
      title: 'Cancel Site Transfer',
      content: (
        <div style={{ marginTop: 8 }}>
          <p>Please enter the cancellation reason:</p>
          <Input
            onChange={(e) => {
              reason = e.target.value;
            }}
            placeholder="Cancellation reason"
          />
        </div>
      ),
      okText: 'Cancel Transfer',
      okType: 'danger',
      onOk: () => {
        if (!reason.trim()) {
          message.error('Cancellation reason is required');
          return Promise.reject();
        }
        cancelTransfer.mutate({ id, reason });
      },
    });
  };

  const handleDownloadPdf = async (t: SiteTransfer) => {
    try {
      const res = await apiClient.get(`/site-transfers/${t.id}/pdf`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `transfer-${t.transferNumber.replaceAll('/', '-')}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Failed to download transfer PDF');
    }
  };

  const handleEdit = (record: SiteTransfer) => {
    setEditing(record);

    const items = record.items.map((line) => ({
      itemId: line.itemId,
      itemCode: line.itemCode,
      itemName: line.itemName,
      unit: line.unit,
      sourceAgreementItemId: line.sourceAgreementItemId,
      destinationAgreementItemId: line.destinationAgreementItemId,
      quantity: line.quantity,
      totalWeight: line.totalWeight,
    }));

    form.setFieldsValue({
      sourceAgreementId: record.sourceAgreementId,
      destinationAgreementId: record.destinationAgreementId,
      transferDate: record.transferDate,
      vehicleNumber: record.vehicleNumber,
      driverName: record.driverName,
      driverPhone: record.driverPhone,
      transporterId: record.transporterId,
      notes: record.notes,
      items,
    });
    setCreateOpen(true);
  };

  const columns = [
    { title: 'Transfer Number', dataIndex: 'transferNumber', key: 'transferNumber' },
    { title: 'Date', dataIndex: 'transferDate', key: 'transferDate' },
    { title: 'Source Site', dataIndex: 'sourceSiteName', key: 'sourceSiteName' },
    { title: 'Destination Site', dataIndex: 'destinationSiteName', key: 'destinationSiteName' },
    { title: 'Vehicle', dataIndex: 'vehicleNumber', key: 'vehicleNumber', render: (v?: string) => v ?? '-' },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (s: string) => {
        let color = 'gold';
        if (s === 'POSTED') color = 'green';
        if (s === 'CANCELLED') color = 'red';
        return <Tag color={color}>{s}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 180,
      render: (_: any, r: SiteTransfer) => (
        <Space wrap>
          <Button size="small" onClick={() => setSelected(r)}>
            View
          </Button>
          {r.status === 'DRAFT' && canWrite && (
            <Button size="small" type="primary" ghost onClick={() => handleEdit(r)}>
              Edit
            </Button>
          )}
          {r.status === 'POSTED' && (
            <Button size="small" icon={<DownloadOutlined />} onClick={() => handleDownloadPdf(r)}>
              PDF
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="page-stack" data-testid="site-transfers-page">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Site Transfers</h1>
          <p className="page-description">
            Track and authorize direct shuttering material shipments between projects and sites.
          </p>
        </div>
        {canWrite && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditing(null);
              form.resetFields();
              form.setFieldsValue({
                transferDate: new Date().toISOString().slice(0, 10),
                items: [],
              });
              setCreateOpen(true);
            }}
          >
            Create Site Transfer
          </Button>
        )}
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search transfers"
            allowClear
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 250 }}
          />
        </Space>

        <Table
          rowKey="id"
          loading={transfers.isLoading}
          dataSource={transfers.data?.content}
          columns={columns}
          pagination={{ hideOnSinglePage: true }}
        />
      </Card>

      {/* Form Drawer */}
      <FormDrawer
        title={editing ? `Edit Transfer Draft: ${editing.transferNumber}` : 'Record Site-to-Site Transfer'}
        open={createOpen}
        width={750}
        onClose={() => {
          setCreateOpen(false);
          form.resetFields();
          setEditing(null);
        }}
        onSubmit={() => form.submit()}
        loading={saveTransfer.isPending}
        okText="Save Draft"
      >
        <Form form={form} layout="vertical" onFinish={(v) => saveTransfer.mutate(v)}>
          <Space size="large" style={{ width: '100%' }}>
            <Form.Item
              name="sourceAgreementId"
              label="Source Project / Agreement"
              rules={[{ required: true, message: 'Select source agreement' }]}
              style={{ width: 330 }}
            >
              <Select
                placeholder="Select source agreement"
                onChange={handleSourceAgreementChange}
                disabled={!!editing}
                options={activeAgreements.data?.map((a) => ({
                  value: a.id,
                  label: `${a.agreementNumber} — ${a.partyName} (${a.siteName})`,
                }))}
              />
            </Form.Item>

            <Form.Item
              name="destinationAgreementId"
              label="Destination Project / Agreement"
              rules={[{ required: true, message: 'Select destination agreement' }]}
              style={{ width: 330 }}
            >
              <Select
                placeholder="Select destination agreement"
                onChange={handleDestinationAgreementChange}
                disabled={!selectedSourceAgreementId || !!editing}
                options={activeAgreements.data?.map((a) => ({
                  value: a.id,
                  label: `${a.agreementNumber} — ${a.partyName} (${a.siteName})`,
                }))}
              />
            </Form.Item>
          </Space>

          {currentSourceAgreement && currentDestinationAgreement && (
            <Card size="small" style={{ marginBottom: 16, background: '#fafafa' }}>
              <div>
                <strong>From Site: </strong> {currentSourceAgreement.siteName} ({currentSourceAgreement.partyName})
              </div>
              <div style={{ marginTop: 4 }}>
                <strong>To Site: </strong> {currentDestinationAgreement.siteName} ({currentDestinationAgreement.partyName})
              </div>
            </Card>
          )}

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item
              name="transferDate"
              label="Transfer Date"
              rules={[{ required: true, message: 'Date is required' }]}
              style={{ width: 210 }}
            >
              <Input type="date" />
            </Form.Item>
            <Form.Item name="vehicleNumber" label="Vehicle Number" style={{ width: 220 }}>
              <Input placeholder="e.g. MH-12-AB-5678" />
            </Form.Item>
            <Form.Item name="driverName" label="Driver Name" style={{ width: 220 }}>
              <Input placeholder="Driver name" />
            </Form.Item>
          </Space>

          <Space size="large" style={{ width: '100%' }}>
            <Form.Item name="driverPhone" label="Driver Phone" style={{ width: 220 }}>
              <Input placeholder="Mobile number" />
            </Form.Item>
            <Form.Item name="transporterId" label="Transporter ID" style={{ width: 220 }}>
              <InputNumber placeholder="Optional ID" style={{ width: '100%' }} />
            </Form.Item>
          </Space>

          <Form.Item name="notes" label="Shipping / Dispatch Notes">
            <Input.TextArea rows={2} placeholder="E-way bill references, transit details, etc." />
          </Form.Item>

          <div style={{ marginBottom: 8, fontWeight: 'bold' }}>Common Matchable Items & Quantities</div>
          <Form.List name="items">
            {(fields) => {
              if (!fields.length) return <Empty description="Select Source & Destination sites to display matchable components" />;
              return (
                <Table
                  dataSource={fields}
                  pagination={false}
                  rowKey="key"
                  size="small"
                  columns={[
                    {
                      title: 'Item',
                      render: (_, field) => {
                        const item = form.getFieldValue(['items', field.name]);
                        return `${item?.itemCode} — ${item?.itemName}`;
                      },
                    },
                    {
                      title: 'Unit',
                      render: (_, field) => form.getFieldValue(['items', field.name])?.unit,
                    },
                    {
                      title: 'Transfer Qty',
                      width: 140,
                      render: (_, field) => (
                        <Form.Item
                          name={[field.name, 'quantity']}
                          rules={[
                            { required: true, message: 'Required' },
                            { type: 'number', min: 0, message: 'Min 0' },
                          ]}
                          noStyle
                        >
                          <InputNumber min={0} style={{ width: '100%' }} />
                        </Form.Item>
                      ),
                    },
                  ]}
                />
              );
            }}
          </Form.List>
        </Form>
      </FormDrawer>

      {/* Detail Modal */}
      <Modal
        title={`Site Transfer Details: ${selected?.transferNumber ?? ''}`}
        open={!!selected}
        width={750}
        footer={null}
        onCancel={() => setSelected(undefined)}
      >
        {selected && (
          <>
            <Descriptions bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Source Agreement">{selected.sourceAgreementNumber}</Descriptions.Item>
              <Descriptions.Item label="Source Site">
                {selected.sourceSiteName} ({selected.sourcePartyName})
              </Descriptions.Item>
              <Descriptions.Item label="Destination Agreement">{selected.destinationAgreementNumber}</Descriptions.Item>
              <Descriptions.Item label="Destination Site">
                {selected.destinationSiteName} ({selected.destinationPartyName})
              </Descriptions.Item>
              <Descriptions.Item label="Transfer Date">{selected.transferDate}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={selected.status === 'POSTED' ? 'green' : selected.status === 'CANCELLED' ? 'red' : 'gold'}>
                  {selected.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Vehicle">{selected.vehicleNumber ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="Driver">
                {selected.driverName ?? '-'} ({selected.driverPhone ?? '-'})
              </Descriptions.Item>
              <Descriptions.Item label="Author">{selected.createdBy}</Descriptions.Item>
              <Descriptions.Item label="Created At">
                {new Date(selected.createdAt).toLocaleString()}
              </Descriptions.Item>
              {selected.postedAt && (
                <>
                  <Descriptions.Item label="Posted By">{selected.postedBy}</Descriptions.Item>
                  <Descriptions.Item label="Posted At">
                    {new Date(selected.postedAt).toLocaleString()}
                  </Descriptions.Item>
                </>
              )}
              {selected.cancelledAt && (
                <>
                  <Descriptions.Item label="Cancelled By">{selected.cancelledBy}</Descriptions.Item>
                  <Descriptions.Item label="Cancelled At">
                    {new Date(selected.cancelledAt).toLocaleString()}
                  </Descriptions.Item>
                  <Descriptions.Item label="Cancellation Reason" span={2}>
                    {selected.cancellationReason}
                  </Descriptions.Item>
                </>
              )}
            </Descriptions>

            <Table
              rowKey="id"
              dataSource={selected.items}
              pagination={false}
              size="small"
              columns={[
                { title: 'Code', dataIndex: 'itemCode' },
                { title: 'Item', dataIndex: 'itemName' },
                { title: 'Size', dataIndex: 'size', render: (v) => v ?? '-' },
                { title: 'Unit', dataIndex: 'unit' },
                { title: 'Qty', dataIndex: 'quantity' },
                { title: 'Weight', dataIndex: 'totalWeight', render: (v) => `${v} Kg` },
              ]}
            />

            {selected.notes && (
              <div style={{ marginTop: 16, marginBottom: 16 }}>
                <strong>Remarks:</strong>
                <p>{selected.notes}</p>
              </div>
            )}

            <Space style={{ marginTop: 16 }}>
              {selected.status === 'DRAFT' && canWrite && (
                <Button type="primary" onClick={() => handlePost(selected.id)}>
                  Post Transfer
                </Button>
              )}
              {selected.status === 'POSTED' && admin && (
                <Button danger type="primary" onClick={() => handleCancel(selected.id)}>
                  Cancel / Reverse Transfer
                </Button>
              )}
              {selected.status === 'POSTED' && (
                <Button icon={<DownloadOutlined />} onClick={() => handleDownloadPdf(selected)}>
                  Download Shipping PDF
                </Button>
              )}
            </Space>
          </>
        )}
      </Modal>
    </div>
  );
}
