import { useState } from 'react';
import { App, Button, Card, Select, Space, Table, Tag } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { apiClient } from '../../../api/client';
import type { Invoice } from '../types';
import { ReportExcelButton } from '../../../components/ReportExcelButton';

type Page<T> = { content: T[]; totalElements: number };
const money = (v?: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(v ?? 0);

export function InvoicesPage() {
  const navigate = useNavigate();
  const { message } = App.useApp();

  const [agreementId] = useState<number | undefined>();
  const [status, setStatus] = useState<string | undefined>();

  // Fetch Invoices
  const invoicesQuery = useQuery({
    queryKey: ['invoices', agreementId, status],
    queryFn: async () => (await apiClient.get<Page<Invoice>>('/invoices', {
      params: { agreementId, status, size: 50, sort: 'id,desc' }
    })).data
  });

  const downloadPdf = async (id: number, number: string) => {
    try {
      const res = await apiClient.get(`/invoices/${id}/pdf`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${number.replaceAll('/', '-')}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Failed to download invoice PDF');
    }
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Rental Invoices</h1>
          <p className="page-description">Track rent and recovery charges billed to customers.</p>
        </div>
        <ReportExcelButton reportType="INVOICE_REGISTER" filters={{ agreementId, status }} />
      </div>

      <Card className="premium-card">
        <Space className="filters-bar" wrap>
          <Select
            allowClear
            placeholder="Filter by Status"
            style={{ width: 160 }}
            onChange={setStatus}
            options={['DRAFT', 'ISSUED', 'CANCELLED'].map(v => ({ value: v, label: v }))}
          />
        </Space>

        <Table
          rowKey="id"
          loading={invoicesQuery.isLoading}
          dataSource={invoicesQuery.data?.content}
          columns={[
            { title: 'Invoice Number', dataIndex: 'invoiceNumber', width: 150 },
            { title: 'Agreement', dataIndex: 'agreementNumberSnapshot', width: 150 },
            { title: 'Party', dataIndex: 'partyNameSnapshot' },
            { title: 'Site', dataIndex: 'siteNameSnapshot' },
            { title: 'Invoice Date', dataIndex: 'invoiceDate', width: 120 },
            { title: 'Due Date', dataIndex: 'dueDate', width: 120 },
            { title: 'Grand Total', dataIndex: 'grandTotal', render: money, width: 120 },
            { 
              title: 'Status', 
              dataIndex: 'status', 
              width: 120,
              render: (s: string) => {
                const color = s === 'ISSUED' ? 'green' : s === 'CANCELLED' ? 'red' : 'orange';
                return <Tag color={color}>{s}</Tag>;
              }
            },
            {
              title: 'Actions',
              width: 180,
              render: (_, row) => (
                <Space>
                  <Button onClick={() => navigate(`/invoices/${row.id}`)}>Open</Button>
                  {row.generatedPdfAttachmentId && (
                    <Button icon={<DownloadOutlined />} onClick={() => downloadPdf(row.id, row.invoiceNumber)}>
                      PDF
                    </Button>
                  )}
                </Space>
              )
            }
          ]}
        />
      </Card>
    </div>
  );
}
