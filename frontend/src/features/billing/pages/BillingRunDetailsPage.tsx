import { useEffect, useState } from 'react';
import { Alert, App, Button, Card, Checkbox, Descriptions, Divider, Empty, Form, Input, InputNumber, Select, Space, Table, Tag, Typography } from 'antd';
import { ArrowLeftOutlined, CalculatorOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import type { BillingRun } from '../types';

const money = (v?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(v ?? 0);
const errorMessage = (error: unknown, fallback: string) => {
  const err = error as { response?: { data?: { message?: string } } };
  return err.response?.data?.message ?? fallback;
};

export function BillingRunDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const runId = parseInt(id || '0', 10);
  const navigate = useNavigate();
  const { message, modal } = App.useApp();
  const qc = useQueryClient();

  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const accounts = roles.includes('ROLE_ACCOUNTS');
  const operations = roles.includes('ROLE_OPERATIONS');
  const canPrepare = admin || accounts || operations;
  const canFinalize = admin || accounts;

  const [adjustmentVal, setAdjustmentVal] = useState<number>(0);
  const [discountType, setDiscountType] = useState<'NONE' | 'PERCENTAGE' | 'FIXED'>('NONE');
  const [discountValue, setDiscountValue] = useState<number>(0);
  const [selectedCharges, setSelectedCharges] = useState<number[]>([]);

  const { data: selectedRun, isLoading, isError } = useQuery({
    queryKey: ['billing-run', runId],
    queryFn: async () => (await apiClient.get<BillingRun>(`/billing-runs/${runId}`)).data,
    enabled: runId > 0,
  });

  useEffect(() => {
    if (selectedRun) {
      setAdjustmentVal(selectedRun.manualAdjustmentTotal);
      setDiscountType(selectedRun.discountType);
      setDiscountValue(selectedRun.discountValue);
      setSelectedCharges(selectedRun.charges.filter((c: any) => c.selected).map((c: any) => c.id));
    }
  }, [selectedRun]);

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['billing-run', runId] });
    void qc.invalidateQueries({ queryKey: ['billing-runs'] });
  };

  const updateMutation = useMutation({
    mutationFn: async (v: { id: number; manualAdjustmentTotal: number; discountType: string; discountValue: number; selectedChargeIds: number[] }) =>
      (await apiClient.put<BillingRun>(`/billing-runs/${v.id}`, v)).data,
    onSuccess: () => {
      message.success('Draft values saved');
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to save values'))
  });

  const calculateMutation = useMutation({
    mutationFn: async () => (await apiClient.post<BillingRun>(`/billing-runs/${runId}/calculate`)).data,
    onSuccess: () => {
      message.success('Recalculation complete');
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Recalculation failed'))
  });

  const finalizeMutation = useMutation({
    mutationFn: async () => (await apiClient.post<BillingRun>(`/billing-runs/${runId}/finalize`)).data,
    onSuccess: (b) => {
      message.success(`Run finalized: ${b.billingRunNumber}`);
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Finalization failed'))
  });

  const cancelMutation = useMutation({
    mutationFn: async (v: { reason: string }) => 
      (await apiClient.post<BillingRun>(`/billing-runs/${runId}/cancel`, null, { params: { reason: v.reason } })).data,
    onSuccess: () => {
      message.success('Billing run cancelled');
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to cancel'))
  });

  const generateInvoiceMutation = useMutation({
    mutationFn: async () => 
      (await apiClient.post<{ id: number }>('/invoices/from-billing-run/' + runId)).data,
    onSuccess: (inv) => {
      message.success('Generated invoice draft');
      navigate(`/invoices/${inv.id}`);
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to generate invoice'))
  });

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
      onOk: () => finalizeMutation.mutateAsync()
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
        return cancelMutation.mutateAsync({ reason: reasonText });
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

  if (isError) {
    return <div className="page-stack"><Card>Error loading billing run. It may have been deleted.</Card></div>;
  }

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate('/billing-runs')} style={{ paddingLeft: 0, marginBottom: 8 }}>
            Back to Billing Runs
          </Button>
          <h1 className="page-heading">Billing Run: {selectedRun?.billingRunNumber || '...'}</h1>
          <p className="page-description">Review the calculated rental segments, apply manual adjustments, and finalize to generate an invoice.</p>
        </div>
        <Space>
          {selectedRun && canPrepare && (selectedRun.status === 'DRAFT' || selectedRun.status === 'CALCULATED') && (
            <>
              <Button icon={<CalculatorOutlined />} onClick={() => calculateMutation.mutate()} loading={calculateMutation.isPending}>
                Recalculate Totals
              </Button>
              {canFinalize && (
                <Button type="primary" icon={<CheckCircleOutlined />} onClick={handleFinalize} loading={finalizeMutation.isPending} disabled={selectedRun.grandTotal <= 0}>
                  Finalize Run
                </Button>
              )}
            </>
          )}
          {selectedRun && canFinalize && selectedRun.status === 'FINALIZED' && (
            <Button type="primary" onClick={() => generateInvoiceMutation.mutate()} loading={generateInvoiceMutation.isPending}>
              Generate Invoice
            </Button>
          )}
          {selectedRun && canFinalize && selectedRun.status !== 'CANCELLED' && (
            <Button danger icon={<CloseCircleOutlined />} onClick={handleCancelRun} loading={cancelMutation.isPending}>
              Cancel Run
            </Button>
          )}
        </Space>
      </div>

      <Card className="premium-card" loading={isLoading}>
        {selectedRun && (
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Descriptions bordered column={{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }}>
              <Descriptions.Item label="Agreement"><b>{selectedRun.agreementNumber}</b></Descriptions.Item>
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

            <Divider orientation="left">1. Rental Timeline Segments</Divider>
            {selectedRun.segments.length === 0 && <Alert type="warning" showIcon message="No billable material in this period" description="Rent begins from a posted issued challan. Choose a period containing deployed material; a zero-value run cannot be finalized." />}
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

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))', gap: '24px', marginTop: 24 }}>
              
              <div>
                <Typography.Title level={5}>2. Operational & Recovery Charges</Typography.Title>
                <div style={{ border: '1px solid #f0f0f0', borderRadius: 8, overflow: 'hidden' }}>
                  {selectedRun.charges.length === 0 ? (
                    <Empty description="No operational charges occurred in this period" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ margin: '24px 0' }} />
                  ) : (
                    <Table
                      rowKey="id"
                      dataSource={selectedRun.charges}
                      pagination={false}
                      size="small"
                      columns={[
                        {
                          title: '',
                          width: 40,
                          render: (_, row) => (
                            <Checkbox
                              checked={selectedCharges.includes(row.id as any)}
                              disabled={selectedRun.status !== 'DRAFT' && selectedRun.status !== 'CALCULATED'}
                              onChange={(e) => handleChargeToggle(row.id as any, e.target.checked)}
                            />
                          )
                        },
                        { title: 'Type', dataIndex: 'chargeType', width: 120 },
                        { title: 'Description', dataIndex: 'description' },
                        { title: 'Amount', dataIndex: 'amount', render: money, width: 110 }
                      ]}
                    />
                  )}
                </div>

                {(selectedRun.status === 'DRAFT' || selectedRun.status === 'CALCULATED') && (
                  <div style={{ background: '#fafafa', padding: 24, borderRadius: 8, border: '1px solid #f0f0f0', marginTop: 24 }}>
                    <Typography.Title level={5}>3. Adjustments & Discounts</Typography.Title>
                    <Form layout="vertical">
                      <Form.Item label="Manual Adjustment Total">
                        <InputNumber
                          style={{ width: '100%' }}
                          value={adjustmentVal}
                          onChange={v => setAdjustmentVal(v || 0)}
                          prefix="₹"
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
                      <Button type="primary" onClick={handleUpdate} loading={updateMutation.isPending} block>
                        Save Adjustments & Charge Selections
                      </Button>
                    </Form>
                  </div>
                )}
              </div>

              <div>
                <Typography.Title level={5}>4. Final Cost Breakup & Grand Total</Typography.Title>
                <div style={{ border: '1px solid #f0f0f0', borderRadius: 8, padding: 24, background: '#fafafa', fontSize: 15 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                    <span style={{ color: '#555' }}>Rental Subtotal:</span>
                    <span>{money(selectedRun.rentalSubtotal)}</span>
                  </div>
                  {selectedRun.discountAmount > 0 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12, color: 'red' }}>
                      <span>Discount:</span>
                      <span>-{money(selectedRun.discountAmount)}</span>
                    </div>
                  )}
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                    <span style={{ color: '#555' }}>Operational Charges:</span>
                    <span>{money(selectedRun.operationalChargeTotal)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                    <span style={{ color: '#555' }}>Losses Recovery:</span>
                    <span>{money(selectedRun.lossChargeTotal)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                    <span style={{ color: '#555' }}>Damages Recovery:</span>
                    <span>{money(selectedRun.damageChargeTotal)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                    <span style={{ color: '#555' }}>Manual Adjustment:</span>
                    <span>{money(selectedRun.manualAdjustmentTotal)}</span>
                  </div>
                  
                  <Divider style={{ margin: '16px 0' }} />
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12, fontWeight: 600 }}>
                    <span>Taxable Value:</span>
                    <span>{money(selectedRun.taxableAmount)}</span>
                  </div>
                  {selectedRun.cgstAmount > 0 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, color: '#555' }}>
                      <span>CGST ({selectedRun.cgstRate}%):</span>
                      <span>{money(selectedRun.cgstAmount)}</span>
                    </div>
                  )}
                  {selectedRun.sgstAmount > 0 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, color: '#555' }}>
                      <span>SGST ({selectedRun.sgstRate}%):</span>
                      <span>{money(selectedRun.sgstAmount)}</span>
                    </div>
                  )}
                  {selectedRun.igstAmount > 0 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, color: '#555' }}>
                      <span>IGST ({selectedRun.igstRate}%):</span>
                      <span>{money(selectedRun.igstAmount)}</span>
                    </div>
                  )}
                  {selectedRun.roundOff !== 0 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, color: '#555' }}>
                      <span>Round Off:</span>
                      <span>{money(selectedRun.roundOff)}</span>
                    </div>
                  )}
                  
                  <Divider style={{ margin: '16px 0' }} />
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', fontSize: 22, color: '#0f172a' }}>
                    <span>Grand Total:</span>
                    <span>{money(selectedRun.grandTotal)}</span>
                  </div>
                </div>
              </div>
            </div>
          </Space>
        )}
      </Card>
    </div>
  );
}
