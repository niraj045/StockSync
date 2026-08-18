import { FileExcelOutlined } from '@ant-design/icons';
import { App, Button } from 'antd';
import { useState } from 'react';
import { reportsApi } from '../features/reports/api';
import type { ReportFilter } from '../features/reports/types';

export function ReportExcelButton({
  reportType,
  filters = {},
  label = 'Excel',
  size,
  disabled = false,
  title,
}: {
  reportType: string;
  filters?: ReportFilter;
  label?: string;
  size?: 'small' | 'middle' | 'large';
  disabled?: boolean;
  title?: string;
}) {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const exportReport = async () => {
    setLoading(true);
    try {
      const result = await reportsApi.export(reportType, filters, 'EXCEL');
      message.success('Excel report generated');
      window.open(reportsApi.downloadUrl(result.id), '_blank', 'noopener,noreferrer');
    } catch {
      message.error('Unable to generate Excel report');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Button
      icon={<FileExcelOutlined />}
      loading={loading}
      disabled={disabled}
      onClick={() => void exportReport()}
      size={size}
      title={title}
    >
      {label}
    </Button>
  );
}
