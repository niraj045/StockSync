import { useState } from 'react';
import { App, Button, Card, Descriptions, Empty, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Checkbox, Divider, Drawer } from 'antd';
import { PlusOutlined, CalculatorOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import dayjs from 'dayjs';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import type { BillingRun } from '../types';

type Page<T> = { content: T[]; totalElements: number };
type EligibleAgreement = {
  id: number;
  agreementNumber: string;
  partyName: string;
  siteName: string;
  effectiveDate: string;
  expiryDate?: string;
};
const money = (v?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(v ?? 0);
const errorMessage = (error: unknown, fallback: string) => {
  const err = error as { response?: { data?: { message?: string } } };
  return err.response?.data?.message ?? fallback;
};

export function BillingRunsPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const accounts = roles.includes('ROLE_ACCOUNTS');
  const canWrite = admin || accounts;

  const navigate = useNavigate();
  const { modal, message } = App.useApp();
  const qc = useQueryClient();

  const [agreementId] = useState<number | undefined>();
  const [status, setStatus] = useState<string | undefined>();
  
  const [createOpen, setCreateOpen] = useState(false);
  const [createAgreementId, setCreateAgreementId] = useState<number | undefined>();
  const [periodStart, setPeriodStart] = useState<string>('');
  const [periodEnd, setPeriodEnd] = useState<string>('');

  const [selectedRun, setSelectedRun] = useState<BillingRun | undefined>();
  const [adjustmentVal, setAdjustmentVal] = useState<number>(0);
  const [discountType, setDiscountType] = useState<'NONE' | 'PERCENTAGE' | 'FIXED'>('NONE');
  const [discountValue, setDiscountValue] = useState<number>(0);
  const [selectedCharges, setSelectedCharges] = useState<number[]>([]);

  // Queries
  const billingRunsQuery = useQuery({
    queryKey: ['billing-runs', agreementId, status],
    queryFn: async () => (await apiClient.get<Page<BillingRun>>('/billing-runs', {
      params: { agreementId, status, size: 50, sort: 'id,desc' }
    })).data
  });

  const eligibleAgreementsQuery = useQuery({
    queryKey: ['eligible-agreements'],
    queryFn: async () => (await apiClient.get<EligibleAgreement[]>('/billing-runs/eligible-agreements')).data,
    enabled: createOpen
  });

  // Fetch suggested period when agreement changes
  useQuery({
    queryKey: ['suggested-period', createAgreementId],
    queryFn: async () => {
      const res = await apiClient.get<Record<string, string>>(`/billing-runs/agreement/${createAgreementId}/suggested-period`);
      setPeriodStart(res.data.periodStart || '');
      setPeriodEnd(res.data.periodEnd || '');
      return res.data;
    },
    enabled: !!createAgreementId
  });

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['billing-runs'] });
  };

  // Mutations
  const createMutation = useMutation({
    mutationFn: async (v: { agreementId: number; periodStart: string; periodEnd: string }) => 
      (await apiClient.post<BillingRun>('/billing-runs', v)).data,
    onSuccess: (b) => {
      message.success(`Created billing run: ${b.billingRunNumber}`);
      setCreateOpen(false);
      setCreateAgreementId(undefined);
      refresh();
      // Open the newly created run details
      loadDetailedRun(b.id);
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to create billing run'))
  });

  const updateMutation = useMutation({
    mutationFn: async (v: { id: number; manualAdjustmentTotal: number; discountType: string; discountValue: number; selectedChargeIds: number[] }) =>
      (await apiClient.put<BillingRun>(`/billing-runs/${v.id}`, v)).data,
    onSuccess: (b) => {
      message.success('Draft values saved');
      setSelectedRun(b);
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to save values'))
  });

  const calculateMutation = useMutation({
    mutationFn: async (id: number) => (await apiClient.post<BillingRun>(`/billing-runs/${id}/calculate`)).data,
    onSuccess: (b) => {
      message.success('Recalculation complete');
      setSelectedRun(b);
      // Synchronize states
      setAdjustmentVal(b.manualAdjustmentTotal);
      setDiscountType(b.discountType);
      setDiscountValue(b.discountValue);
      setSelectedCharges(b.charges.filter(c => c.selected).map(c => c.id));
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Recalculation failed'))
  });

  const finalizeMutation = useMutation({
    mutationFn: async (id: number) => (await apiClient.post<BillingRun>(`/billing-runs/${id}/finalize`)).data,
    onSuccess: (b) => {
      message.success(`Run finalized: ${b.billingRunNumber}`);
      setSelectedRun(b);
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Finalization failed'))
  });

  const cancelMutation = useMutation({
    mutationFn: async (v: { id: number; reason: string }) => 
      (await apiClient.post<BillingRun>(`/billing-runs/${v.id}/cancel`, null, { params: { reason: v.reason } })).data,
    onSuccess: (b) => {
      message.success('Billing run cancelled');
      setSelectedRun(b);
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to cancel'))
  });

  const generateInvoiceMutation = useMutation({
    mutationFn: async (runId: number) => 
      (await apiClient.post<{ id: number }>('/invoices/from-billing-run/' + runId)).data,
    onSuccess: (inv) => {
      message.success('Generated invoice draft');
      setSelectedRun(undefined);
      navigate(`/invoices/${inv.id}`);
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to generate invoice'))
  });

  const loadDetailedRun = async (id: number) => {
    try {
      const res = (await apiClient.get<BillingRun>(`/billing-runs/${id}`)).data;
      setSelectedRun(res);
      setAdjustmentVal(res.manualAdjustmentTotal);
      setDiscountType(res.discountType);
      setDiscountValue(res.discountValue);
      setSelectedCharges(res.charges.filter(c => c.selected).map(c => c.id));
    } catch {
      message.error('Failed to load detailed run');
    }
  };

  const handleCreate = () => {
    if (!createAgreementId || !periodStart || !periodEnd) {
      message.error('All fields are required');
      return;
    }
    if (dayjs(periodEnd).isAfter(dayjs(), 'day')) {
      message.error('Billing period cannot end in the future');
      return;
    }
    createMutation.mutate({
      agreementId: createAgreementId,
      periodStart,
      periodEnd
    });
  };

  const handleUpdate = () => {
    if (!selectedRun) return;
    updateMutation.mutate({
      id: selectedRun.id,
      manualAdjustmentTotal: adjustmentVal,
      discountType,
      discountValue,
      selectedChargeIds: selectedCharges
    });
  };

  const handleFinalize = () => {
    if (!selectedRun) return;
    modal.confirm({
      title: 'Finalize Billing Run?',
      content: 'Finalizing locks this rental timeline and allocations. This action is immutable.',
      onOk: () => finalizeMutation.mutateAsync(selectedRun.id)
    });
  };

  const handleCancelRun = () => {
    if (!selectedRun) return;
    let reasonText = '';
    modal.confirm({
      title: 'Cancel Billing Run?',
      content: (
        <Input.TextArea
          placeholder="Reason for cancellation is required"
          style={{ marginTop: 10 }}
          onChange={e => reasonText = e.target.value}
        />
      ),
      onOk: () => {
        if (!reasonText.trim()) {
          message.error('Reason is required');
          return Promise.reject();
        }
        return cancelMutation.mutateAsync({ id: selectedRun.id, reason: reasonText });
      }
    });
  };

  const handleChargeToggle = (id: number, checked: boolean) => {
    if (checked) {
      setSelectedCharges([...selectedCharges, id]);
    } else {
      setSelectedCharges(selectedCharges.filter(x => x !== id));
    }
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Rental Billing Runs</h1>
          <p className="page-description">Manage and calculate site rental timelines and operational charges.</p>
        </div>
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            New Billing Run
          </Button>
        )}
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
                return <Tag color={color}>{s}</Tag>;
              }
            },
            {
              title: 'Actions',
              width: 100,
              render: (_, row) => (
                <Button onClick={() => loadDetailedRun(row.id)}>Open</Button>
              )
            }
          ]}
        />
      </Card>

      {/* Create Run Modal */}
      <Modal
        title="Create New Billing Run"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={handleCreate}
        confirmLoading={createMutation.isPending}
      >
        <Form layout="vertical" style={{ marginTop: 15 }}>
          <Form.Item label="Select Active Agreement" required>
            <Select
              placeholder="Select an agreement"
              onChange={(v) => setCreateAgreementId(v)}
              options={(eligibleAgreementsQuery.data || []).map(ag => ({
                value: ag.id,
                label: `${ag.agreementNumber} — ${ag.partyName} (${ag.siteName})`
              }))}
            />
          </Form.Item>
          {createAgreementId && (
            <>
              <Form.Item label="Period Start" required>
                <Input type="date" value={periodStart} onChange={e => setPeriodStart(e.target.value)} />
              </Form.Item>
              <Form.Item label="Period End" required>
                <Input type="date" value={periodEnd} onChange={e => setPeriodEnd(e.target.value)} />
              </Form.Item>
            </>
          )}
        </Form>
      </Modal>

      {/* Run Detail Drawer */}
      <Drawer
        title={`Billing Run Detail — ${selectedRun?.billingRunNumber || ''}`}
        open={!!selectedRun}
        width={1000}
        onClose={() => setSelectedRun(undefined)}
        footer={
          selectedRun && (
            <div style={{ textAlign: 'right' }}>
              <Space>
                {canWrite && (selectedRun.status === 'DRAFT' || selectedRun.status === 'CALCULATED') && (
                  <>
                    <Button icon={<CalculatorOutlined />} onClick={() => calculateMutation.mutate(selectedRun.id)} loading={calculateMutation.isPending}>
                      Recalculate
                    </Button>
                    <Button type="primary" icon={<CheckCircleOutlined />} onClick={handleFinalize} loading={finalizeMutation.isPending}>
                      Finalize Run
                    </Button>
                  </>
                )}
                {canWrite && selectedRun.status === 'FINALIZED' && (
                  <Button type="primary" onClick={() => generateInvoiceMutation.mutate(selectedRun.id)} loading={generateInvoiceMutation.isPending}>
                    Generate Invoice
                  </Button>
                )}
                {canWrite && selectedRun.status !== 'CANCELLED' && (
                  <Button danger icon={<CloseCircleOutlined />} onClick={handleCancelRun} loading={cancelMutation.isPending}>
                    Cancel Run
                  </Button>
                )}
              </Space>
            </div>
          )
        }
      >
        {selectedRun && (
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Descriptions bordered column={2}>
              <Descriptions.Item label="Agreement">{selectedRun.agreementNumber}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={selectedRun.status === 'FINALIZED' ? 'green' : selectedRun.status === 'CANCELLED' ? 'red' : 'orange'}>
                  {selectedRun.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Party">{selectedRun.partyName}</Descriptions.Item>
              <Descriptions.Item label="Site">{selectedRun.siteName}</Descriptions.Item>
              <Descriptions.Item label="Start Date">{selectedRun.periodStart}</Descriptions.Item>
              <Descriptions.Item label="End Date">{selectedRun.periodEnd}</Descriptions.Item>
            </Descriptions>

            <Divider orientation="left">Rental Timeline Segments</Divider>
            <Table
              rowKey="id"
              dataSource={selectedRun.segments}
              pagination={false}
              columns={[
                { title: '#', dataIndex: 'sequenceNumber', width: 50 },
                { title: 'Code', dataIndex: 'itemCode' },
                { title: 'Item', dataIndex: 'itemName' },
                { title: 'Qty', dataIndex: 'quantity', width: 80 },
                { title: 'Days', dataIndex: 'billableDays', width: 70 },
                { title: 'Start', dataIndex: 'segmentStart', width: 110 },
                { title: 'End', dataIndex: 'segmentEnd', width: 110 },
                { title: 'Explanation', dataIndex: 'calculationExplanation' },
                { title: 'Amount', dataIndex: 'amount', render: money, width: 110 }
              ]}
            />

            <Divider orientation="left">Operational &amp; Recovery Charges</Divider>
            {selectedRun.charges.length === 0 ? (
              <Empty description="No operational charges occurred in this period" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Table
                rowKey="id"
                dataSource={selectedRun.charges}
                pagination={false}
                columns={[
                  {
                    title: 'Select',
                    width: 70,
                    render: (_, row) => (
                      <Checkbox
                        checked={selectedCharges.includes(row.id)}
                        disabled={selectedRun.status !== 'DRAFT' && selectedRun.status !== 'CALCULATED'}
                        onChange={(e) => handleChargeToggle(row.id, e.target.checked)}
                      />
                    )
                  },
                  { title: 'Doc Ref', dataIndex: 'sourceDocumentNumber', width: 150 },
                  { title: 'Charge Type', dataIndex: 'chargeType', width: 150 },
                  { title: 'Description', dataIndex: 'description' },
                  { title: 'Amount', dataIndex: 'amount', render: money, width: 120 }
                ]}
              />
            )}

            {(selectedRun.status === 'DRAFT' || selectedRun.status === 'CALCULATED') && (
              <>
                <Divider orientation="left">Adjustments &amp; Discounts</Divider>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 20 }}>
                  <Form.Item label="Manual Adjustment Total">
                    <InputNumber
                      style={{ width: '100%' }}
                      value={adjustmentVal}
                      onChange={v => setAdjustmentVal(v || 0)}
                    />
                  </Form.Item>
                  <Form.Item label="Discount Type">
                    <Select
                      value={discountType}
                      onChange={v => setDiscountType(v)}
                      options={['NONE', 'PERCENTAGE', 'FIXED'].map(v => ({ value: v, label: v }))}
                    />
                  </Form.Item>
                  {discountType !== 'NONE' && (
                    <Form.Item label="Discount Value">
                      <InputNumber
                        style={{ width: '100%' }}
                        value={discountValue}
                        onChange={v => setDiscountValue(v || 0)}
                      />
                    </Form.Item>
                  )}
                </div>
                <Button type="primary" onClick={handleUpdate} loading={updateMutation.isPending}>
                  Save Adjustment &amp; Charges selection
                </Button>
              </>
            )}

            <Divider orientation="left">Cost Breakup &amp; Grand Total</Divider>
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <div style={{ width: 350, border: '1px solid #f0f0f0', borderRadius: 4, padding: 15, background: '#fafafa' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>Rental Subtotal:</span>
                  <span>{money(selectedRun.rentalSubtotal)}</span>
                </div>
                {selectedRun.discountAmount > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, color: 'red' }}>
                    <span>Discount:</span>
                    <span>-{money(selectedRun.discountAmount)}</span>
                  </div>
                )}
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>Operational Charges:</span>
                  <span>{money(selectedRun.operationalChargeTotal)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>Losses Recovery:</span>
                  <span>{money(selectedRun.lossChargeTotal)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>Damages Recovery:</span>
                  <span>{money(selectedRun.damageChargeTotal)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>Manual Adjustment:</span>
                  <span>{money(selectedRun.manualAdjustmentTotal)}</span>
                </div>
                <Divider style={{ margin: '8px 0' }} />
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontWeight: 'bold' }}>
                  <span>Taxable Value:</span>
                  <span>{money(selectedRun.taxableAmount)}</span>
                </div>
                {selectedRun.cgstAmount > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span>CGST ({selectedRun.cgstRate}%):</span>
                    <span>{money(selectedRun.cgstAmount)}</span>
                  </div>
                )}
                {selectedRun.sgstAmount > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span>SGST ({selectedRun.sgstRate}%):</span>
                    <span>{money(selectedRun.sgstAmount)}</span>
                  </div>
                )}
                {selectedRun.igstAmount > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span>IGST ({selectedRun.igstRate}%):</span>
                    <span>{money(selectedRun.igstAmount)}</span>
                  </div>
                )}
                {selectedRun.roundOff !== 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span>Round Off:</span>
                    <span>{money(selectedRun.roundOff)}</span>
                  </div>
                )}
                <Divider style={{ margin: '8px 0' }} />
                <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', fontSize: 16, color: '#087f72' }}>
                  <span>Grand Total:</span>
                  <span>{money(selectedRun.grandTotal)}</span>
                </div>
              </div>
            </div>
          </Space>
        )}
      </Drawer>
    </div>
  );
}
