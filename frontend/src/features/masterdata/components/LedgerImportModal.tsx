import { useState } from 'react';
import { Modal, Upload, Button, message, Table, Typography } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { apiClient } from '../../../api/client';

const { Dragger } = Upload;
const { Text } = Typography;

interface LedgerImportModalProps {
  open: boolean;
  siteId: number | null;
  siteName: string;
  onClose: () => void;
}

interface LedgerItemTotal {
  itemName: string;
  totalDelivered: number;
}

export function LedgerImportModal({ open, siteId, siteName, onClose }: LedgerImportModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<LedgerItemTotal[] | null>(null);

  const handleUpload = async () => {
    if (!file || !siteId) return;
    
    setLoading(true);
    const formData = new FormData();
    formData.append('file', file);
    
    try {
      const response = await apiClient.post<LedgerItemTotal[]>(`/sites/${siteId}/import-ledger`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setResults(response.data);
      message.success('Ledger imported successfully. Opening stock balances created.');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Failed to import ledger');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFile(null);
    setResults(null);
    onClose();
  };

  return (
    <Modal
      title={`Import D&R Ledger - ${siteName}`}
      open={open}
      onCancel={handleClose}
      footer={
        results ? [
          <Button key="close" type="primary" onClick={handleClose}>Done</Button>
        ] : [
          <Button key="cancel" onClick={handleClose}>Cancel</Button>,
          <Button key="upload" type="primary" onClick={handleUpload} loading={loading} disabled={!file}>
            Upload and Process
          </Button>
        ]
      }
      width={600}
    >
      {!results ? (
        <div style={{ marginTop: 24, marginBottom: 24 }}>
          <p>
            Upload a Statement of Material Delivered & Returned (.xlsx) to automatically parse the totals and post them as the opening balance for this site.
          </p>
          <Dragger
            accept=".xlsx"
            maxCount={1}
            beforeUpload={(f) => {
              setFile(f);
              return false;
            }}
            onRemove={() => setFile(null)}
            fileList={file ? [file as any] : []}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">Click or drag file to this area to upload</p>
            <p className="ant-upload-hint">Support for a single D&R Excel file.</p>
          </Dragger>
        </div>
      ) : (
        <div style={{ marginTop: 24, marginBottom: 24 }}>
          <Text type="success" strong style={{ display: 'block', marginBottom: 16 }}>
            Successfully mapped the following items and created opening stock balances:
          </Text>
          <Table
            dataSource={results}
            rowKey="itemName"
            pagination={false}
            size="small"
            columns={[
              { title: 'Item Name', dataIndex: 'itemName' },
              { title: 'Total Delivered', dataIndex: 'totalDelivered', align: 'right' }
            ]}
          />
        </div>
      )}
    </Modal>
  );
}
