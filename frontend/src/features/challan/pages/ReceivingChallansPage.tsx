import { useState } from 'react';
import { App, Button, Card, Descriptions, Empty, Form, Input, InputNumber, Modal, Select, Space, Table, DatePicker, Radio } from 'antd';
import { DownloadOutlined, SearchOutlined, PlusOutlined, UndoOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { FormDrawer } from '../../../components/FormDrawer';
import { ReportExcelButton } from '../../../components/ReportExcelButton';
import type { ReceivingChallan, ReceivingChallanItem, SiteStockBalance } from '../types';

type Page<T> = { content: T[]; totalElements: number };

type ChallanFormValue = {
  partyId: number;
  siteId: number;
  agreementId?: number;
  linkedIssuedChallanId?: number;
  receiveDate: dayjs.Dayjs;
  vehicleNumber?: string;
  driverName?: string;
  driverPhone?: string;
  notes?: string;
  sourceType: string;
  items: {
    itemId: number;
    itemCode: string;
    itemName: string;
    unit: string;
    pendingQuantitySnapshot: number;
    goodReturnedQuantity: number;
    damagedReturnedQuantity: number;
    lostQuantity: number;
    extraReturnedQuantity: number;
    exchangedToItemId?: number;
    exchangedQuantity?: number;
    notes?: string;
  }[];
};

export function ReceivingChallansPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canWrite = admin || operations;

  const { message } = App.useApp();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<ReceivingChallan>();
  const [createOpen, setCreateOpen] = useState(false);
  const [cancelReasonOpen, setCancelReasonOpen] = useState(false);
  const [cancellationReason, setCancellationReason] = useState('');

  const [form] = Form.useForm<ChallanFormValue>();
  const selectedPartyId = Form.useWatch('partyId', form);
  const selectedSiteId = Form.useWatch('siteId', form);
  const selectedSourceType = Form.useWatch('sourceType', form);

  // Queries
  const challans = useQuery({
    queryKey: ['receiving-challans', search],
    queryFn: async () =>
      (
        await apiClient.get<Page<ReceivingChallan>>('/challans/receiving', {
          params: { search, size: 50, sort: 'id,desc' },
        })
      ).data,
  });

  const parties = useQuery({
    queryKey: ['parties-lookup'],
    queryFn: async () => (await apiClient.get<any>('/parties', { params: { size: 200 } })).data.content,
  });

  const sites = useQuery({
    queryKey: ['sites-lookup'],
    queryFn: async () => (await apiClient.get<any>('/sites', { params: { size: 200 } })).data.content,
  });

  const itemsList = useQuery({
    queryKey: ['items-lookup'],
    queryFn: async () => (await apiClient.get<any>('/items', { params: { size: 500 } })).data.content,
  });

  const sitePendingBalances = useQuery({
    queryKey: ['site-pending-balances', selectedSiteId],
    enabled: !!selectedSiteId,
    queryFn: async () =>
      (
        await apiClient.get<SiteStockBalance[]>('/challans/receiving/site-pending-balances', {
          params: { siteId: selectedSiteId },
        })
      ).data,
  });

  const siteIssuedChallans = useQuery({
    queryKey: ['site-issued-lookup', selectedSiteId],
    enabled: !!selectedSiteId && selectedSourceType === 'ISSUED_CHALLAN',
    queryFn: async () =>
      (
        await apiClient.get<any[]>('/challans/receiving/issued-lookup', {
          params: { siteId: selectedSiteId },
        })
      ).data,
  });

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['receiving-challans'] });
    void qc.invalidateQueries({ queryKey: ['site-pending-balances'] });
    void qc.invalidateQueries({ queryKey: ['site-issued-lookup'] });
  };

  // Mutations
  const createChallan = useMutation({
    mutationFn: async (v: ChallanFormValue) => {
      const payload = {
        partyId: v.partyId,
        siteId: v.siteId,
        linkedIssuedChallanId: v.linkedIssuedChallanId,
        receiveDate: v.receiveDate.format('YYYY-MM-DD'),
        vehicleNumber: v.vehicleNumber,
        driverName: v.driverName,
        driverPhone: v.driverPhone,
        notes: v.notes,
        sourceType: v.sourceType,
        items: v.items.map((i) => ({
          itemId: i.itemId,
          goodReturnedQuantity: i.goodReturnedQuantity || 0,
          damagedReturnedQuantity: i.damagedReturnedQuantity || 0,
          lostQuantity: i.lostQuantity || 0,
          extraReturnedQuantity: i.extraReturnedQuantity || 0,
          exchangedFromItemId: (i.exchangedQuantity || 0) > 0 ? i.itemId : null,
          exchangedToItemId: (i.exchangedQuantity || 0) > 0 ? i.exchangedToItemId : null,
          exchangedQuantity: i.exchangedQuantity || 0,
          notes: i.notes,
        })),
      };
      return (await apiClient.post<ReceivingChallan>('/challans/receiving', payload)).data;
    },
    onSuccess: (c) => {
      message.success(`Receiving Challan ${c.receivingChallanNumber} created`);
      setCreateOpen(false);
      form.resetFields();
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Unable to create challan');
    },
  });

  const postChallan = useMutation({
    mutationFn: async (id: number) => {
      return (await apiClient.post<ReceivingChallan>(`/challans/receiving/${id}/post`)).data;
    },
    onSuccess: (c) => {
      message.success(`Challan ${c.receivingChallanNumber} posted successfully`);
      setSelected(c);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Unable to post challan');
    },
  });

  const approveExtra = useMutation({
    mutationFn: async (id: number) => {
      return (await apiClient.post<ReceivingChallan>(`/challans/receiving/${id}/approve-extra`)).data;
    },
    onSuccess: (c) => {
      message.success(`Extra returns for ${c.receivingChallanNumber} approved`);
      setSelected(c);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Unable to approve extra returns');
    },
  });

  const cancelChallan = useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) => {
      return (
        await apiClient.post<ReceivingChallan>(`/challans/receiving/${id}/cancel`, {
          cancellationReason: reason,
        })
      ).data;
    },
    onSuccess: (c) => {
      message.success(`Challan ${c.receivingChallanNumber} cancelled and stock reversed`);
      setCancelReasonOpen(false);
      setCancellationReason('');
      setSelected(c);
      refresh();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message ?? 'Unable to cancel challan');
    },
  });

  const handleDownloadPdf = async (c: ReceivingChallan) => {
    try {
      const res = await apiClient.get(`/challans/receiving/${c.id}/pdf`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `receiving_challan_${c.receivingChallanNumber}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Unable to download PDF');
    }
  };

  const handleSiteChange = () => {
    form.setFieldsValue({ items: [] });
  };

  const handleLoadPendingBalances = () => {
    if (!sitePendingBalances.data) return;
    const itemsData = sitePendingBalances.data.map((sb) => ({
      itemId: sb.itemId,
      itemCode: sb.itemCode,
      itemName: sb.itemName,
      unit: sb.unit,
      pendingQuantitySnapshot: sb.pendingQuantity,
      goodReturnedQuantity: 0,
      damagedReturnedQuantity: 0,
      lostQuantity: 0,
      extraReturnedQuantity: 0,
    }));
    form.setFieldsValue({ items: itemsData });
  };

  const filteredSites = (sites.data ?? []).filter((s: any) => s.partyId === selectedPartyId);
  const itemsOptions = (itemsList.data ?? []).map((i: any) => ({ value: i.id, label: `${i.itemCode} - ${i.itemName}` }));

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Receiving Challans</h1>
          <p className="page-description">Record returns and reconcile deployed site materials.</p>
        </div>
        <Space wrap>
          <ReportExcelButton reportType="RECEIVING_CHALLANS_REGISTER" filters={{ documentNumber: search || undefined }} />
          {canWrite && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields();
              form.setFieldsValue({ receiveDate: dayjs(), sourceType: 'SITE_PENDING_BALANCE', items: [] });
              setCreateOpen(true);
            }}
          >
            New Return
          </Button>
          )}
        </Space>
      </div>

      <div className="master-detail-container">
        <Card className="premium-card master-pane">
          <div className="filters-bar">
            <Input
              allowClear
              placeholder="Search challan, party or site"
              prefix={<SearchOutlined />}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ width: '100%' }}
            />
          </div>
          <Table
            rowKey="id"
            dataSource={challans.data?.content}
            loading={challans.isLoading}
            pagination={{ total: challans.data?.totalElements, pageSize: 50 }}
            onRow={(record) => ({
              onClick: () => setSelected(record),
            })}
            rowClassName={(record) => (selected?.id === record.id ? 'premium-row-selected clickable-row' : 'clickable-row')}
            columns={[
              { title: 'Challan Number', dataIndex: 'receivingChallanNumber' },
              { title: 'Site', dataIndex: 'siteName' },
              { title: 'Date', dataIndex: 'receiveDate' },
              { title: 'Status', dataIndex: 'status' },
            ]}
          />
        </Card>

        <Card className="premium-card detail-pane">
          {selected ? (
            <div className="detail-pane-content">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                <h2>Challan Details: {selected.receivingChallanNumber}</h2>
                <Space>
                  <Button icon={<DownloadOutlined />} onClick={() => handleDownloadPdf(selected)}>
                    PDF
                  </Button>
                  <ReportExcelButton reportType="RECEIVING_CHALLANS_REGISTER" filters={{ documentNumber: selected.receivingChallanNumber }} />
                  {selected.status === 'EXTRA_APPROVAL_REQUIRED' && admin && (
                    <Button type="primary" icon={<CheckCircleOutlined />} onClick={() => approveExtra.mutate(selected.id)}>
                      Approve Extra
                    </Button>
                  )}
                  {(selected.status === 'DRAFT' || selected.status === 'APPROVED_FOR_POSTING') && canWrite && (
                    <Button type="primary" onClick={() => postChallan.mutate(selected.id)}>
                      Post Return
                    </Button>
                  )}
                  {selected.status === 'POSTED' && admin && (
                    <Button danger icon={<UndoOutlined />} onClick={() => setCancelReasonOpen(true)}>
                      Cancel/Reverse
                    </Button>
                  )}
                </Space>
              </div>

              <Descriptions bordered column={2} size="small">
                <Descriptions.Item label="Party">{selected.partyName}</Descriptions.Item>
                <Descriptions.Item label="Site">{selected.siteName}</Descriptions.Item>
                <Descriptions.Item label="Source Type">{selected.sourceType}</Descriptions.Item>
                <Descriptions.Item label="Date">{selected.receiveDate}</Descriptions.Item>
                <Descriptions.Item label="Vehicle">{selected.vehicleNumber || '-'}</Descriptions.Item>
                <Descriptions.Item label="Driver">{selected.driverName || '-'}</Descriptions.Item>
                <Descriptions.Item label="Notes" span={2}>{selected.notes || '-'}</Descriptions.Item>
                {selected.status === 'CANCELLED' && (
                  <Descriptions.Item label="Cancellation Reason" span={2} labelStyle={{ color: 'red' }}>
                    {selected.cancellationReason || '-'}
                  </Descriptions.Item>
                )}
              </Descriptions>

              <h3 style={{ marginTop: 20 }}>Returned Items</h3>
              <Table
                rowKey="id"
                dataSource={selected.items}
                pagination={false}
                size="small"
                columns={[
                  { title: 'Item Code', dataIndex: 'itemCode' },
                  { title: 'Item Name', dataIndex: 'itemName' },
                  { title: 'Pending Before', dataIndex: 'pendingQuantitySnapshot' },
                  { title: 'Good', dataIndex: 'goodReturnedQuantity' },
                  { title: 'Damaged', dataIndex: 'damagedReturnedQuantity' },
                  { title: 'Lost', dataIndex: 'lostQuantity' },
                  { title: 'Extra', dataIndex: 'extraReturnedQuantity' },
                  {
                    title: 'Exchange',
                    render: (_, row: ReceivingChallanItem) =>
                      row.exchangedQuantity ? (
                        <span>
                          {row.exchangedQuantity} {row.exchangedFromItemCode} &rarr; {row.exchangedToItemCode}
                        </span>
                      ) : (
                        '-'
                      ),
                  },
                ]}
              />
            </div>
          ) : (
            <Empty description="Select a receiving challan to view details" style={{ marginTop: 100 }} />
          )}
        </Card>
      </div>

      <FormDrawer
        open={createOpen}
        title="Record Receiving Challan"
        subtitle="Specify material returns and stock adjustments."
        onClose={() => setCreateOpen(false)}
        onSubmit={() => form.submit()}
        loading={createChallan.isPending}
        width={960}
      >
        <Form form={form} layout="vertical" onFinish={(v) => createChallan.mutate(v)} style={{ marginTop: 20 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '15px' }}>
            <Form.Item name="partyId" label="Party" rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={(parties.data ?? []).map((p: any) => ({ value: p.id, label: p.legalName }))}
              />
            </Form.Item>
            <Form.Item name="siteId" label="Site" rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                onChange={handleSiteChange}
                disabled={!selectedPartyId}
                options={filteredSites.map((s: any) => ({ value: s.id, label: s.siteName }))}
              />
            </Form.Item>
            <Form.Item name="sourceType" label="Source Type" rules={[{ required: true }]}>
              <Radio.Group optionType="button" buttonStyle="solid">
                <Radio.Button value="SITE_PENDING_BALANCE">Pending Balance</Radio.Button>
                <Radio.Button value="ISSUED_CHALLAN">Issued Challan</Radio.Button>
                <Radio.Button value="OPENING_SITE_BALANCE">Legacy Opening Stock</Radio.Button>
              </Radio.Group>
            </Form.Item>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: '15px' }}>
            <Form.Item name="receiveDate" label="Receive Date" rules={[{ required: true }]}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="vehicleNumber" label="Vehicle Number">
              <Input />
            </Form.Item>
            <Form.Item name="driverName" label="Driver Name">
              <Input />
            </Form.Item>
            <Form.Item name="driverPhone" label="Driver Phone">
              <Input />
            </Form.Item>
          </div>

          {selectedSourceType === 'ISSUED_CHALLAN' && (
            <Form.Item name="linkedIssuedChallanId" label="Linked Issued Challan">
              <Select
                showSearch
                optionFilterProp="label"
                disabled={!selectedSiteId}
                options={(siteIssuedChallans.data ?? []).map((c) => ({
                  value: c.id,
                  label: `${c.challanNumber} (${c.dispatchDate})`,
                }))}
              />
            </Form.Item>
          )}

          <div style={{ marginBottom: 15 }}>
            <Button type="dashed" onClick={handleLoadPendingBalances} disabled={!selectedSiteId}>
              Pull Current Site Pending Balances
            </Button>
          </div>

          <Form.List name="items">
            {(fields, { add, remove }) => (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                  <strong>Item Lines</strong>
                  <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({})} />
                </div>

                {fields.map((field) => (
                  <div
                    key={field.key}
                    style={{
                      border: '1px solid #f0f0f0',
                      padding: 10,
                      marginBottom: 10,
                      borderRadius: 4,
                      background: '#fafafa',
                    }}
                  >
                    <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr', gap: '10px', marginBottom: 10 }}>
                      <Form.Item {...field} name={[field.name, 'itemId']} label="Item" rules={[{ required: true }]}>
                        <Select showSearch optionFilterProp="label" options={itemsOptions} />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'pendingQuantitySnapshot']} label="Site Pending">
                        <InputNumber disabled style={{ width: '100%' }} />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'goodReturnedQuantity']} label="Good Returned">
                        <InputNumber min={0} style={{ width: '100%' }} />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'damagedReturnedQuantity']} label="Damaged">
                        <InputNumber min={0} style={{ width: '100%' }} />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'lostQuantity']} label="Lost">
                        <InputNumber min={0} style={{ width: '100%' }} />
                      </Form.Item>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr 1fr 1fr', gap: '10px' }}>
                      <Form.Item {...field} name={[field.name, 'extraReturnedQuantity']} label="Extra Quantity">
                        <InputNumber min={0} style={{ width: '100%' }} />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'exchangedToItemId']} label="Exchanged Target Item">
                        <Select showSearch optionFilterProp="label" options={itemsOptions} />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'exchangedQuantity']} label="Exchanged Qty">
                        <InputNumber min={0} style={{ width: '100%' }} />
                      </Form.Item>
                      <div style={{ alignSelf: 'center', textAlign: 'right', paddingTop: 20 }}>
                        <Button danger type="link" onClick={() => remove(field.name)}>
                          Remove
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </>
            )}
          </Form.List>

          <Form.Item name="notes" label="Notes">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </FormDrawer>

      <Modal
        title="Cancel & Reverse Receiving Challan"
        open={cancelReasonOpen}
        onOk={() => {
          if (!cancellationReason.trim()) {
            message.warning('Provide a reason for cancellation');
            return;
          }
          if (selected) {
            cancelChallan.mutate({ id: selected.id, reason: cancellationReason });
          }
        }}
        onCancel={() => {
          setCancelReasonOpen(false);
          setCancellationReason('');
        }}
        okText="Confirm Cancellation"
        okButtonProps={{ danger: true }}
      >
        <p>This action will reverse all ledger entries and restore site pending quantities. Never deleted.</p>
        <Input.TextArea
          rows={3}
          placeholder="Reason for cancellation"
          value={cancellationReason}
          onChange={(e) => setCancellationReason(e.target.value)}
        />
      </Modal>
    </div>
  );
}
