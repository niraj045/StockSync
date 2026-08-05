import { useMemo, useState } from 'react';
import { App, Button, Card, DatePicker, Divider, Form, Input, InputNumber, Select, Space, Statistic, Typography } from 'antd';
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useNavigate } from 'react-router';
import { apiClient } from '../../../api/client';
import { paymentsApi, type AllocationInput, type PaymentInput } from '../../payment/api';
import type { EligibleInvoice } from '../../payment/types';

type PartyOption = { id: number; legalName: string };
type PaymentFormValues = Omit<PaymentInput, 'paymentDate' | 'chequeDate' | 'allocations'> & {
  paymentDate: dayjs.Dayjs;
  chequeDate?: dayjs.Dayjs;
  allocations?: AllocationInput[];
};

const money = (value?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value ?? 0);

export function CreatePaymentPage() {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [form] = Form.useForm<PaymentFormValues>();
  const [partyId, setPartyId] = useState<number>();

  const parties = useQuery({
    queryKey: ['party-options'],
    queryFn: async () => (await apiClient.get<{ content: PartyOption[] }>('/parties', { params: { size: 100, sort: 'legalName,asc' } })).data.content,
  });

  const eligible = useQuery({
    queryKey: ['eligible-invoices', partyId],
    enabled: Boolean(partyId),
    queryFn: () => paymentsApi.eligibleInvoices(partyId as number),
  });

  const advance = useQuery({
    queryKey: ['payment-advance', partyId],
    enabled: Boolean(partyId),
    queryFn: () => paymentsApi.advance(partyId as number),
  });

  const createPayment = useMutation({
    mutationFn: (input: PaymentInput) => paymentsApi.create(input),
    onSuccess: async () => {
      message.success('Payment draft saved');
      await queryClient.invalidateQueries({ queryKey: ['payments'] });
      await queryClient.invalidateQueries({ queryKey: ['eligible-invoices'] });
      await queryClient.invalidateQueries({ queryKey: ['payment-advance'] });
      navigate('/payments');
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Failed to create payment';
      message.error(msg);
    }
  });

  const allocationRows = Form.useWatch('allocations', form) ?? [];
  const totals = useMemo(() => allocationRows.reduce((sum, row) => ({
    cash: sum.cash + Number(row?.cashAllocated ?? 0),
    tds: sum.tds + Number(row?.tdsAllocated ?? 0),
  }), { cash: 0, tds: 0 }), [allocationRows]);
  
  const cashAmount = Number(Form.useWatch('cashAmount', form) ?? 0);
  const tdsAmount = Number(Form.useWatch('tdsAmount', form) ?? 0);
  const existingAdvance = advance.data?.availableAdvance ?? 0;
  
  const remainingAdvance = (cashAmount + tdsAmount + existingAdvance) - totals.cash - totals.tds;
  
  // Validation Logic to prevent 400 Bad Request
  const isOverAllocatedCash = totals.cash > (cashAmount + existingAdvance);
  const isOverAllocatedTDS = totals.tds > tdsAmount;
  const hasValidationError = isOverAllocatedCash || isOverAllocatedTDS;

  const submit = async () => {
    if (hasValidationError) {
      message.error('Cannot save: Allocation exceeds available payment amount.');
      return;
    }
    const values = await form.validateFields();
    await createPayment.mutateAsync({
      ...values,
      paymentDate: values.paymentDate.format('YYYY-MM-DD'),
      chequeDate: values.chequeDate?.format('YYYY-MM-DD'),
      allocations: values.allocations ?? [],
    });
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate('/payments')} style={{ paddingLeft: 0, marginBottom: 8 }}>
            Back to Payments
          </Button>
          <h1 className="page-heading">Create Payment</h1>
          <p className="page-description">Record a new receipt and allocate it to outstanding invoices.</p>
        </div>
        <Space>
          <Button 
            type="primary" 
            size="large" 
            icon={<SaveOutlined />} 
            onClick={submit} 
            loading={createPayment.isPending}
            disabled={hasValidationError}
          >
            Save Draft
          </Button>
        </Space>
      </div>

      <Card className="premium-card">
        <Form 
          form={form} 
          layout="vertical" 
          initialValues={{ paymentDate: dayjs(), paymentMode: 'CASH', cashAmount: 0, tdsAmount: 0, allocations: [] }}
          size="large"
        >
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '20px', marginBottom: 20 }}>
            <Form.Item name="partyId" label="Party" rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={(parties.data ?? []).map((party: any) => ({ value: party.id, label: party.legalName }))}
                onChange={setPartyId}
                placeholder="Select Party"
              />
            </Form.Item>
            <Form.Item name="paymentDate" label="Date" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="paymentMode" label="Mode" rules={[{ required: true }]}>
              <Select options={['CASH', 'BANK_TRANSFER', 'CHEQUE', 'UPI', 'NEFT', 'RTGS', 'IMPS', 'OTHER'].map((value) => ({ value, label: value }))} />
            </Form.Item>
            <Form.Item name="referenceNumber" label="Reference (Cheque/Txn No)"><Input /></Form.Item>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '20px' }}>
            <Form.Item 
              name="cashAmount" 
              label="Received Cash Amount" 
              rules={[{ required: true }]}
              help="The actual amount received in bank/hand"
            >
              <InputNumber min={0} precision={2} style={{ width: '100%' }} prefix="₹" />
            </Form.Item>
            <Form.Item 
              name="tdsAmount" 
              label="TDS Deducted" 
              rules={[{ required: true }]}
              help="TDS deducted by the client (if any)"
            >
              <InputNumber min={0} precision={2} style={{ width: '100%' }} prefix="₹" />
            </Form.Item>
            <Form.Item name="notes" label="Internal Notes"><Input.TextArea rows={2} /></Form.Item>
          </div>

          <Divider />
          
          <div style={{ background: '#f8fafc', padding: '24px', borderRadius: '8px', border: '1px solid #e2e8f0', marginBottom: 24 }}>
            <Space size="large" wrap style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
              <Statistic title="Existing Advance" value={existingAdvance} prefix="₹" precision={2} valueStyle={{ color: '#0891b2' }} />
              <Statistic title="Allocated Cash" value={totals.cash} prefix="₹" precision={2} valueStyle={{ color: isOverAllocatedCash ? '#ef4444' : '#10b981' }} />
              <Statistic title="Allocated TDS" value={totals.tds} prefix="₹" precision={2} valueStyle={{ color: isOverAllocatedTDS ? '#ef4444' : '#10b981' }} />
              <Statistic title="Remaining Balance" value={remainingAdvance} prefix="₹" precision={2} valueStyle={{ color: remainingAdvance < 0 ? '#ef4444' : '#333' }} />
            </Space>
            {hasValidationError && (
              <div style={{ marginTop: 16, color: '#ef4444', fontWeight: 500 }}>
                ⚠️ Allocation cannot exceed available payment + existing advance amounts. Please adjust your allocations or increase the received amount.
              </div>
            )}
          </div>

          <Typography.Title level={4}>Invoice Allocations</Typography.Title>
          <Form.List name="allocations">
            {(fields, { add, remove }) => (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {fields.map((field, index) => (
                  <div key={field.key} style={{ display: 'flex', gap: 16, alignItems: 'flex-start', background: '#fafafa', padding: 16, borderRadius: 8, border: '1px solid #f0f0f0' }}>
                    <div style={{ paddingTop: 8, fontWeight: 500, color: '#888' }}>{index + 1}.</div>
                    <Form.Item name={[field.name, 'invoiceId']} rules={[{ required: true }]} style={{ margin: 0, flex: 2 }}>
                      <Select 
                        placeholder="Select unpaid invoice" 
                        options={(eligible.data ?? []).map((invoice: EligibleInvoice) => ({ 
                          value: invoice.id, 
                          label: `${invoice.invoiceNumber} (Outstanding: ${money(invoice.outstandingAmount)})` 
                        }))} 
                      />
                    </Form.Item>
                    <Form.Item name={[field.name, 'cashAllocated']} style={{ margin: 0, flex: 1 }} label={index === 0 ? "Cash Alloc." : ""}>
                      <InputNumber min={0} precision={2} placeholder="0.00" style={{ width: '100%' }} prefix="₹" />
                    </Form.Item>
                    <Form.Item name={[field.name, 'tdsAllocated']} style={{ margin: 0, flex: 1 }} label={index === 0 ? "TDS Alloc." : ""}>
                      <InputNumber min={0} precision={2} placeholder="0.00" style={{ width: '100%' }} prefix="₹" />
                    </Form.Item>
                    <Button danger type="text" onClick={() => remove(field.name)} style={{ marginTop: index === 0 ? 30 : 0 }}>Remove</Button>
                  </div>
                ))}
                <Button type="dashed" onClick={() => add({ cashAllocated: 0, tdsAllocated: 0 })} block style={{ height: 48, marginTop: 8 }}>
                  + Add Invoice Allocation
                </Button>
              </div>
            )}
          </Form.List>
        </Form>
      </Card>
    </div>
  );
}
