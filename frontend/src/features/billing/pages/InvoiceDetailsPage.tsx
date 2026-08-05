import { useState } from 'react';
import { App, Button, Card, Descriptions, Divider, Form, Input, Modal, Space, Table, Tag } from 'antd';
import { DownloadOutlined, EditOutlined, FilePdfOutlined, CheckCircleOutlined, CloseCircleOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import type { Invoice } from '../types';

const money = (v?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(v ?? 0);
const errorMessage = (error: unknown, fallback: string) => {
  const err = error as { response?: { data?: { message?: string } } };
  return err.response?.data?.message ?? fallback;
};

export function InvoiceDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes('ROLE_ADMIN');
  const accounts = roles.includes('ROLE_ACCOUNTS');
  const canWrite = admin || accounts;

  const { modal, message } = App.useApp();
  const qc = useQueryClient();

  const [editOpen, setEditOpen] = useState(false);
  const [form] = Form.useForm();

  // Query detailed invoice
  const { data: invoice, isLoading, refetch } = useQuery({
    queryKey: ['invoice-details', id],
    queryFn: async () => (await apiClient.get<Invoice>(`/invoices/${id}`)).data,
    enabled: !!id
  });

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['invoices'] });
    void refetch();
  };

  // Mutations
  const updateMutation = useMutation({
    mutationFn: async (v: { dueDate: string; terms: string; notes: string }) => 
      (await apiClient.put<Invoice>(`/invoices/${id}`, { ...v, version: invoice?.version })).data,
    onSuccess: () => {
      message.success('Invoice metadata updated');
      setEditOpen(false);
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to update metadata'))
  });

  const generatePdfMutation = useMutation({
    mutationFn: async () => (await apiClient.post<Invoice>(`/invoices/${id}/generate-pdf`)).data,
    onSuccess: () => {
      message.success('PDF document generated successfully');
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to generate PDF'))
  });

  const issueMutation = useMutation({
    mutationFn: async () => (await apiClient.post<Invoice>(`/invoices/${id}/issue`)).data,
    onSuccess: () => {
      message.success('Invoice has been issued');
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to issue invoice'))
  });

  const cancelMutation = useMutation({
    mutationFn: async (reason: string) => 
      (await apiClient.post<Invoice>(`/invoices/${id}/cancel`, { reason })).data,
    onSuccess: () => {
      message.success('Invoice has been cancelled');
      refresh();
    },
    onError: (e) => message.error(errorMessage(e, 'Failed to cancel invoice'))
  });

  const downloadPdf = async () => {
    if (!invoice) return;
    try {
      const res = await apiClient.get(`/invoices/${invoice.id}/pdf`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${invoice.invoiceNumber.replaceAll('/', '-')}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Failed to download invoice PDF');
    }
  };

  const handleEdit = () => {
    if (!invoice) return;
    form.setFieldsValue({
      dueDate: invoice.dueDate,
      terms: invoice.terms,
      notes: invoice.notes
    });
    setEditOpen(true);
  };

  const handleSaveMetadata = () => {
    form.validateFields().then(values => {
      updateMutation.mutate(values);
    });
  };

  const handleCancelInvoice = () => {
    let reasonText = '';
    modal.confirm({
      title: 'Cancel Tax Invoice?',
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
        return cancelMutation.mutateAsync(reasonText);
      }
    });
  };

  if (isLoading) {
    return <Card loading />;
  }

  if (!invoice) {
    return <Card>Invoice not found</Card>;
  }

  return (
    <div className="page-stack">
      <div style={{ marginBottom: 15 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/invoices')}>
          Back to Invoices
        </Button>
      </div>

      <div className="page-header-container">
        <div>
          <h1 className="page-heading">{invoice.invoiceNumber}</h1>
          <p className="page-description">
            Billing Period: {invoice.periodStart} to {invoice.periodEnd}
          </p>
        </div>
        <Space>
          {canWrite && invoice.status === 'DRAFT' && (
            <>
              <Button icon={<EditOutlined />} onClick={handleEdit}>
                Edit
              </Button>
              <Button icon={<FilePdfOutlined />} onClick={() => generatePdfMutation.mutate()} loading={generatePdfMutation.isPending}>
                {invoice.generatedPdfAttachmentId ? 'Regenerate PDF' : 'Generate PDF'}
              </Button>
              <Button type="primary" icon={<CheckCircleOutlined />} onClick={() => issueMutation.mutate()} loading={issueMutation.isPending}>
                Issue Invoice
              </Button>
            </>
          )}
          {invoice.generatedPdfAttachmentId && (
            <Button icon={<DownloadOutlined />} onClick={downloadPdf}>
              Download PDF
            </Button>
          )}
          {canWrite && invoice.status !== 'CANCELLED' && (
            <Button danger icon={<CloseCircleOutlined />} onClick={handleCancelInvoice} loading={cancelMutation.isPending}>
              Cancel Invoice
            </Button>
          )}
        </Space>
      </div>

      <Card className="premium-card">
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Descriptions bordered column={2}>
            <Descriptions.Item label="Invoice Number">{invoice.invoiceNumber}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={invoice.status === 'ISSUED' ? 'green' : invoice.status === 'CANCELLED' ? 'red' : 'orange'}>
                {invoice.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Invoice Date">{invoice.invoiceDate}</Descriptions.Item>
            <Descriptions.Item label="Due Date">{invoice.dueDate}</Descriptions.Item>
            <Descriptions.Item label="Agreement Reference">{invoice.agreementNumberSnapshot}</Descriptions.Item>
            <Descriptions.Item label="Billing Run Reference">{invoice.billingRunNumber}</Descriptions.Item>

            <Descriptions.Item label="Billed To (Client)" span={2}>
              <strong>{invoice.partyNameSnapshot}</strong><br />
              {invoice.partyAddressSnapshot}<br />
              <strong>State:</strong> {invoice.partyStateSnapshot} | <strong>GSTIN:</strong> {invoice.partyGstinSnapshot || '-'}
            </Descriptions.Item>

            <Descriptions.Item label="Site Delivery Info" span={2}>
              <strong>Site Name:</strong> {invoice.siteNameSnapshot} (Code: {invoice.siteCodeSnapshot})<br />
              {invoice.siteAddressSnapshot}
            </Descriptions.Item>
          </Descriptions>

          <Divider orientation="left">Invoice Line Items</Divider>
          <Table
            rowKey="id"
            dataSource={invoice.items}
            pagination={false}
            columns={[
              { title: '#', dataIndex: 'sequenceNumber', width: 50 },
              { title: 'Type', dataIndex: 'lineType', width: 120 },
              { title: 'Description', dataIndex: 'description' },
              { title: 'Qty', dataIndex: 'quantity', width: 100 },
              { title: 'Days', dataIndex: 'billableDays', render: v => v ?? '-', width: 80 },
              { title: 'Rate', dataIndex: 'rate', render: money, width: 120 },
              { title: 'Amount', dataIndex: 'amount', render: money, width: 120 }
            ]}
          />

          <Divider orientation="left">Summary &amp; Taxes</Divider>
          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <div style={{ width: 350, border: '1px solid #f0f0f0', borderRadius: 4, padding: 15, background: '#fafafa' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span>Subtotal:</span>
                <span>{money(invoice.subtotal)}</span>
              </div>
              {invoice.discountAmount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, color: 'red' }}>
                  <span>Discount:</span>
                  <span>-{money(invoice.discountAmount)}</span>
                </div>
              )}
              <Divider style={{ margin: '8px 0' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontWeight: 'bold' }}>
                <span>Taxable Value:</span>
                <span>{money(invoice.taxableAmount)}</span>
              </div>
              {invoice.cgstAmount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>CGST ({invoice.cgstRate}%):</span>
                  <span>{money(invoice.cgstAmount)}</span>
                </div>
              )}
              {invoice.sgstAmount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>SGST ({invoice.sgstRate}%):</span>
                  <span>{money(invoice.sgstAmount)}</span>
                </div>
              )}
              {invoice.igstAmount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>IGST ({invoice.igstRate}%):</span>
                  <span>{money(invoice.igstAmount)}</span>
                </div>
              )}
              {invoice.roundOff !== 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>Round Off:</span>
                  <span>{money(invoice.roundOff)}</span>
                </div>
              )}
              <Divider style={{ margin: '8px 0' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', fontSize: 16, color: '#087f72' }}>
                <span>Grand Total:</span>
                <span>{money(invoice.grandTotal)}</span>
              </div>
            </div>
          </div>

          <Divider />
          <div>
            <strong>Terms &amp; Conditions:</strong>
            <p style={{ whiteSpace: 'pre-wrap', color: '#595959', marginTop: 5 }}>{invoice.terms || '-'}</p>
          </div>
          <div>
            <strong>Internal Notes:</strong>
            <p style={{ whiteSpace: 'pre-wrap', color: '#595959', marginTop: 5 }}>{invoice.notes || '-'}</p>
          </div>
        </Space>
      </Card>

      {/* Edit Metadata Modal */}
      <Modal
        title="Edit Invoice Metadata"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={handleSaveMetadata}
        confirmLoading={updateMutation.isPending}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 15 }}>
          <Form.Item name="dueDate" label="Due Date" rules={[{ required: true, message: 'Please select a due date' }]}>
            <Input type="date" />
          </Form.Item>
          <Form.Item name="terms" label="Terms &amp; Conditions">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="notes" label="Internal Notes">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
