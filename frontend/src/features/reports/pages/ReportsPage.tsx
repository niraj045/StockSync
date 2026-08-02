import { DownloadOutlined, FileExcelOutlined, FilePdfOutlined, FileTextOutlined, SaveOutlined } from '@ant-design/icons';
import { App, Button, Card, DatePicker, Empty, Form, Input, InputNumber, Modal, Select, Space, Statistic, Table, Tabs, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import { reportsApi } from '../api';
import type { ExportFormat, ExportHistory, ReportFilter, ReportRow, SavedReportFilter } from '../types';

const { RangePicker } = DatePicker;
const categories = ['Inventory', 'Sites and Parties', 'Operations', 'Commercial', 'Financial', 'GST', 'Audit'];

const cellText = (value: unknown) => {
  if (value === null || value === undefined || value === '') return '-';
  return String(value);
};

const moneyKeys = ['total', 'amount', 'tax', 'cash', 'tds', 'outstanding', 'deposit', 'charge', 'value', 'grand'];

const reportRowKey = (row: ReportRow) => {
  const keyParts = [
    row.id,
    row.document_number,
    row.invoice_number,
    row.receipt_number,
    row.agreement_number,
    row.item_code,
    row.metric,
  ].filter((part) => part !== null && part !== undefined && part !== '');
  return keyParts.length > 0 ? keyParts.join('-') : JSON.stringify(row);
};

export function ReportsPage() {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [saveForm] = Form.useForm<{ name: string }>();
  const [category, setCategory] = useState('Inventory');
  const [reportType, setReportType] = useState<string>();
  const [filters, setFilters] = useState<ReportFilter>({ page: 0, size: 25 });
  const [saveOpen, setSaveOpen] = useState(false);

  const catalogQuery = useQuery({ queryKey: ['reports', 'catalog'], queryFn: reportsApi.catalog });
  const historyQuery = useQuery({ queryKey: ['reports', 'history'], queryFn: reportsApi.history });
  const savedQuery = useQuery({ queryKey: ['reports', 'saved'], queryFn: reportsApi.savedFilters });

  const reports = catalogQuery.data ?? [];
  const selectedReport = useMemo(() => {
    const chosen = reports.find((report) => report.reportType === reportType);
    return chosen ?? reports.find((report) => report.category === category) ?? reports[0];
  }, [category, reportType, reports]);

  const previewQuery = useQuery({
    queryKey: ['reports', 'preview', selectedReport?.reportType, filters],
    enabled: Boolean(selectedReport),
    queryFn: () => reportsApi.preview(selectedReport?.reportType ?? '', filters),
  });

  const exportMutation = useMutation({
    mutationFn: ({ format }: { format: ExportFormat }) => reportsApi.export(selectedReport?.reportType ?? '', filters, format),
    onSuccess: (result) => {
      message.success(`${result.format} export generated`);
      queryClient.invalidateQueries({ queryKey: ['reports', 'history'] });
      window.open(reportsApi.downloadUrl(result.id), '_blank');
    },
    onError: () => message.error('Unable to export report'),
  });

  const saveMutation = useMutation({
    mutationFn: ({ name }: { name: string }) => reportsApi.createSavedFilter({ name: name.trim(), reportType: selectedReport?.reportType ?? '', filters, shared: false }),
    onSuccess: () => {
      message.success('Saved filter preset');
      setSaveOpen(false);
      saveForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['reports', 'saved'] });
    },
    onError: () => message.error('Unable to save filter preset'),
  });

  const deleteMutation = useMutation({
    mutationFn: reportsApi.deleteSavedFilter,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reports', 'saved'] }),
  });

  const preview = previewQuery.data;
  const filteredReports = reports.filter((report) => report.category === category);
  const columns: ColumnsType<ReportRow> = (preview?.columns ?? []).map((key) => ({
    title: key.replaceAll('_', ' '),
    dataIndex: key,
    key,
    ellipsis: true,
    align: moneyKeys.some((word) => key.includes(word)) ? 'right' : 'left',
    render: (value: ReportRow[string]) => cellText(value),
  }));

  const updateFilter = (patch: ReportFilter) => setFilters((current) => ({ ...current, ...patch, page: 0 }));

  const applyPreset = (presetId: number) => {
    const preset = savedQuery.data?.find((item) => item.id === presetId);
    if (!preset) return;
    setCategory(reports.find((report) => report.reportType === preset.reportType)?.category ?? category);
    setReportType(preset.reportType);
    setFilters({ ...preset.filters, page: 0, size: preset.filters.size ?? 25 });
  };

  const chooseReport = (value: string) => {
    setReportType(value);
    setFilters({ page: 0, size: filters.size ?? 25 });
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Report Centre</h1>
          <p className="page-description">Preview operational, commercial, financial and GST preparation reports without changing source records.</p>
        </div>
      </div>

      <Card className="premium-card reports-filter-card">
        <Tabs
          className="reports-tabs"
          activeKey={category}
          items={categories.map((name) => ({ key: name, label: name, disabled: !reports.some((report) => report.category === name) }))}
          onChange={(key) => {
            setCategory(key);
            const next = reports.find((report) => report.category === key);
            if (next) chooseReport(next.reportType);
          }}
        />
        <div className="reports-filter-grid">
          <Select
            className="reports-report-select"
            value={selectedReport?.reportType}
            options={filteredReports.map((report) => ({ value: report.reportType, label: report.name }))}
            onChange={chooseReport}
            loading={catalogQuery.isLoading}
          />
          <RangePicker
            onChange={(dates) => updateFilter({
              startDate: dates?.[0]?.format('YYYY-MM-DD'),
              endDate: dates?.[1]?.format('YYYY-MM-DD'),
            })}
          />
          <DatePicker picker="month" onChange={(date) => updateFilter({ month: date?.format('YYYY-MM') })} />
          <InputNumber placeholder="Party id" min={1} onChange={(value) => updateFilter({ partyId: typeof value === 'number' ? value : undefined })} />
          <InputNumber placeholder="Site id" min={1} onChange={(value) => updateFilter({ siteId: typeof value === 'number' ? value : undefined })} />
          <InputNumber placeholder="Agreement id" min={1} onChange={(value) => updateFilter({ agreementId: typeof value === 'number' ? value : undefined })} />
          <InputNumber placeholder="Item id" min={1} onChange={(value) => updateFilter({ itemId: typeof value === 'number' ? value : undefined })} />
          <Input placeholder="Status" onChange={(event) => updateFilter({ status: event.target.value || undefined })} />
          <Input className="reports-document-filter" placeholder="Document number" onChange={(event) => updateFilter({ documentNumber: event.target.value || undefined })} />
        </div>
      </Card>

      <Card className="premium-card reports-results-card">
        <div className="section-header-row">
          <div>
            <Typography.Title level={4}>{selectedReport?.name ?? 'Reports'}</Typography.Title>
            <Typography.Text type="secondary">{selectedReport?.description}</Typography.Text>
          </div>
          <Space wrap>
            {selectedReport?.gstPreparation && <Tag color="gold">Preparation only</Tag>}
            <Button
              icon={<SaveOutlined />}
              onClick={() => {
                saveForm.resetFields();
                setSaveOpen(true);
              }}
              disabled={!selectedReport}
            >
              Save preset
            </Button>
            {selectedReport?.formats.includes('PDF') && <Button icon={<FilePdfOutlined />} onClick={() => exportMutation.mutate({ format: 'PDF' })}>PDF</Button>}
            {selectedReport?.formats.includes('EXCEL') && <Button icon={<FileExcelOutlined />} onClick={() => exportMutation.mutate({ format: 'EXCEL' })}>Excel</Button>}
            {selectedReport?.formats.includes('CSV') && <Button icon={<FileTextOutlined />} onClick={() => exportMutation.mutate({ format: 'CSV' })}>CSV</Button>}
          </Space>
        </div>
        {preview?.warning && <Typography.Paragraph type="warning">{preview.warning}</Typography.Paragraph>}
        {Object.keys(preview?.totals ?? {}).length > 0 && (
          <div className="reports-total-grid">
            {Object.entries(preview?.totals ?? {}).slice(0, 8).map(([key, value]) => (
              <div className="reports-total-tile" key={key}>
                <Statistic title={key.replaceAll('_', ' ')} value={value} precision={2} />
              </div>
            ))}
          </div>
        )}
        {preview && preview.rows.length === 0 ? (
          <Empty description="No report rows match the selected filters" />
        ) : (
          <Table
            rowKey={reportRowKey}
            size="small"
            loading={previewQuery.isLoading}
            columns={columns}
            dataSource={preview?.rows ?? []}
            scroll={{ x: true }}
            pagination={{
              current: (filters.page ?? 0) + 1,
              pageSize: filters.size ?? 25,
              total: preview?.totalElements ?? 0,
              onChange: (page, size) => setFilters((current) => ({ ...current, page: page - 1, size })),
            }}
          />
        )}
      </Card>

      <div className="responsive-grid two-columns">
        <SavedFilters
          data={savedQuery.data ?? []}
          onApply={applyPreset}
          onDelete={(id) => deleteMutation.mutate(id)}
        />
        <ExportHistory data={historyQuery.data ?? []} />
      </div>

      <Modal
        title="Save Filter Preset"
        open={saveOpen}
        onCancel={() => {
          setSaveOpen(false);
          saveForm.resetFields();
        }}
        okButtonProps={{ htmlType: 'submit', form: 'save-report-preset-form', loading: saveMutation.isPending }}
      >
        <Form
          id="save-report-preset-form"
          form={saveForm}
          layout="vertical"
          onFinish={(values) => saveMutation.mutate(values)}
        >
          <Form.Item
            name="name"
            label="Preset name"
            rules={[
              { required: true, whitespace: true, message: 'Enter a preset name' },
              { max: 120, message: 'Preset name must be 120 characters or less' },
            ]}
          >
            <Input autoFocus />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

function SavedFilters({ data, onApply, onDelete }: { data: SavedReportFilter[]; onApply: (id: number) => void; onDelete: (id: number) => void }) {
  return (
    <Card className="premium-card" title="Saved Presets">
      {data.length === 0 ? <Empty description="No saved presets" /> : (
        <Table
          size="small"
          rowKey="id"
          dataSource={data}
          pagination={false}
          columns={[
            { title: 'Name', dataIndex: 'name' },
            { title: 'Report', dataIndex: 'reportType' },
            { title: 'Shared', dataIndex: 'shared', render: (value: boolean) => value ? 'Yes' : 'No' },
            {
              title: 'Action',
              render: (_, row) => (
                <Space>
                  <Button size="small" onClick={() => onApply(row.id)}>Apply</Button>
                  <Button size="small" danger onClick={() => onDelete(row.id)}>Delete</Button>
                </Space>
              ),
            },
          ]}
        />
      )}
    </Card>
  );
}

function ExportHistory({ data }: { data: ExportHistory[] }) {
  return (
    <Card className="premium-card" title="Recent Exports">
      {data.length === 0 ? <Empty description="No exports yet" /> : (
        <Table
          size="small"
          rowKey="id"
          dataSource={data}
          pagination={false}
          columns={[
            { title: 'Report', dataIndex: 'reportType' },
            { title: 'Format', dataIndex: 'format' },
            { title: 'Status', dataIndex: 'status' },
            { title: 'Generated', dataIndex: 'generatedAt', render: (value: string) => dayjs(value).format('DD MMM YYYY HH:mm') },
            {
              title: 'Download',
              render: (_, row) => row.status === 'SUCCESS' ? (
                <Button size="small" icon={<DownloadOutlined />} href={reportsApi.downloadUrl(row.id)} />
              ) : cellText(row.errorMessage),
            },
          ]}
        />
      )}
    </Card>
  );
}
