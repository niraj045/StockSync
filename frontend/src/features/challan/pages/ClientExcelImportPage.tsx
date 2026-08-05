
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button, Card, Form, Select, Upload, message, Typography, Space
} from 'antd';
import { CloudUploadOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd';
import { apiClient } from '../../../api/client';

interface SiteOrderOption {
  id: number;
  orderNumber: string;
  party: { legalName: string };
  site: { siteName: string };
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
}

export function ClientExcelImportPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<{ siteOrderId: number; file: UploadFile[] }>();
  
  const siteOrders = useQuery({
    queryKey: ['import-site-order-options'],
    queryFn: async () => (await apiClient.get<PageResponse<SiteOrderOption>>('/orders', {
      params: { page: 0, size: 500, sort: 'createdAt,desc' },
    })).data.content,
  });

  const upload = useMutation({
    mutationFn: async (values: { siteOrderId: number; file: UploadFile[] }) => {
      const source = values.file[0]?.originFileObj;
      if (!source) throw new Error('Select the client XLSX workbook');
      
      const body = new FormData();
      body.append('siteOrderId', values.siteOrderId.toString());
      body.append('file', source);
      
      return (await apiClient.post<{ success: boolean; importedCount: number }>('/challans/issued/import-client-excel', body, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })).data;
    },
    onSuccess: (result) => {
      message.success(`Successfully imported ${result.importedCount} historical challans.`);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['issued-challans'] });
      queryClient.invalidateQueries({ queryKey: ['stock-balances'] });
    },
    onError: (error: any) => {
      message.error(error.response?.data?.message || 'Failed to import Excel file');
    },
  });

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Historical D&R Import</h1>
          <p className="page-description">Import client D&R Excel workbooks to generate historical issued challans automatically.</p>
        </div>
      </div>

      <Card className="premium-card">
        <Typography.Paragraph>
          This tool parses standard SBUT D&R Excel layouts. It scans for the "Challan no." header, maps column labels to system items (fuzzy match), and creates immutable Issued Challan records along with ledger transactions.
        </Typography.Paragraph>

        <Form form={form} layout="vertical" onFinish={(values) => upload.mutate(values)} style={{ maxWidth: 600, marginTop: 24 }}>
          <Form.Item name="siteOrderId" label="Target Site Order" rules={[{ required: true, message: 'Select the site order' }]}>
            <Select 
              showSearch 
              optionFilterProp="label" 
              placeholder="Select an active Site Order to attach challans to"
              loading={siteOrders.isLoading}
              options={(siteOrders.data ?? []).map((order) => ({ 
                value: order.id, 
                label: `${order.orderNumber} · ${order.party.legalName} / ${order.site.siteName}` 
              }))} 
            />
          </Form.Item>

          <Form.Item name="file" label="Client XLSX Workbook" valuePropName="fileList"
            getValueFromEvent={(event) => event?.fileList} rules={[{ required: true, message: 'Select the XLSX workbook' }]}>
            <Upload beforeUpload={() => false} accept=".xlsx" maxCount={1}>
              <Button icon={<CloudUploadOutlined />}>Choose workbook</Button>
            </Upload>
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={upload.isPending} icon={<CloudUploadOutlined />}>
                Process Import
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
