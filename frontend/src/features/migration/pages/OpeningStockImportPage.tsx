import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Button, Card, Checkbox, Col, Descriptions, Empty, Form, Input, Modal, Popconfirm, Radio, Row,
  Select, Space, Statistic, Table, Tabs, Tag, Typography, Upload, message,
} from 'antd';
import { FormDrawer } from '../../../components/FormDrawer';
import {
  CheckCircleOutlined, CloudUploadOutlined, FileSearchOutlined, LinkOutlined, ReloadOutlined,
  RollbackOutlined, SafetyCertificateOutlined, ThunderboltOutlined,
} from '@ant-design/icons';
import type { UploadFile } from 'antd';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
}

type BatchStatus =
  | 'UPLOADED' | 'PARSED' | 'MAPPING_REQUIRED' | 'VALIDATED' | 'POSTED'
  | 'PARTIALLY_POSTED' | 'FAILED' | 'REVERSED';
type ValidationStatus = 'READY' | 'MAPPING_REQUIRED' | 'WARNING' | 'ERROR' | 'POSTED' | 'REVERSED';
type LocationType = 'GODOWN' | 'PARTY_OR_SITE';

interface ImportBatch {
  id: number;
  batchCode: string;
  importType: string;
  originalFilename: string;
  fileChecksum: string;
  sourceFormat: string;
  partySnapshotDate: string;
  godownSnapshotDate: string;
  status: BatchStatus;
  totalSourceRows: number;
  totalBalanceRows: number;
  validRows: number;
  warningRows: number;
  errorRows: number;
  expectedPartyTotal: number;
  expectedGodownTotal: number;
  expectedCombinedTotal: number;
  importedAt?: string;
  importedBy?: string;
  reversedAt?: string;
  reversedBy?: string;
  reversalReason?: string;
  notes?: string;
  version: number;
  createdAt: string;
  createdBy: string;
}

interface ImportRow {
  id: number;
  sourceExcelRow: number;
  sourceExcelColumn: string;
  sourceSrNumber?: string;
  sourceItemName: string;
  normalizedItemSuggestion?: string;
  mappedItemId?: number;
  mappedItemCode?: string;
  mappedItemName?: string;
  sourceLocationName: string;
  mappedPartyId?: number;
  mappedPartyName?: string;
  mappedSiteId?: number;
  mappedSiteName?: string;
  mappedGodownCode?: string;
  locationType: LocationType;
  quantity: number;
  snapshotDate: string;
  targetStockBucket: 'AVAILABLE' | 'ISSUED';
  openingTransactionType: string;
  validationStatus: ValidationStatus;
  validationMessage?: string;
  duplicateConfirmed: boolean;
  excluded: boolean;
  exclusionReason?: string;
  postedStockTransactionId?: number;
  version: number;
}

interface LocationMapping {
  sourceExcelColumn: string;
  sourceLocationName: string;
  mappedPartyId?: number;
  mappedPartyName?: string;
  mappedSiteId?: number;
  mappedSiteName?: string;
  mapped: boolean;
}

interface Preview {
  batchId: number;
  batchCode: string;
  status: BatchStatus;
  partySnapshotDate: string;
  godownSnapshotDate: string;
  sourceRows: number;
  balanceRows: number;
  mappedRows: number;
  warningRows: number;
  errorRows: number;
  excludedRows: number;
  postedRows: number;
  expectedPartyTotal: number;
  expectedGodownTotal: number;
  expectedCombinedTotal: number;
  mappedPartyTotal: number;
  mappedGodownTotal: number;
  mappedCombinedTotal: number;
  excludedPartyTotal: number;
  excludedGodownTotal: number;
  excludedTotal: number;
  errorPartyTotal: number;
  errorGodownTotal: number;
  errorTotal: number;
  postedPartyTotal: number;
  postedGodownTotal: number;
  postedCombinedTotal: number;
  postable: boolean;
  warnings: string[];
}

interface ImportReport {
  batch: ImportBatch;
  preview: Preview;
  locationMappings: LocationMapping[];
  findings: string[];
  unresolvedQuestions: string[];
}

interface ItemOption {
  id: number;
  itemCode: string;
  itemName: string;
  active: boolean;
}

interface PartyOption {
  id: number;
  legalName: string;
}

interface SiteOption {
  id: number;
  partyId: number;
  siteCode: string;
  siteName: string;
  status: string;
}

interface UploadValues {
  file: UploadFile[];
  notes?: string;
}

interface ItemMappingValues {
  mode: 'existing' | 'create' | 'exclude';
  itemId?: number;
  itemCode?: string;
  itemName?: string;
  saveAlias: boolean;
  confirmDuplicate: boolean;
  exclusionReason?: string;
}

interface LocationMappingValues {
  partyMode: 'existing' | 'create';
  partyId?: number;
  partyName?: string;
  siteMode: 'existing' | 'create';
  siteId?: number;
  siteName?: string;
  siteCode?: string;
}

const quantity = (value: number | undefined) =>
  Number(value ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 4 });

const statusColor: Record<string, string> = {
  UPLOADED: 'default', PARSED: 'processing', MAPPING_REQUIRED: 'warning', VALIDATED: 'cyan',
  POSTED: 'success', PARTIALLY_POSTED: 'orange', FAILED: 'error', REVERSED: 'purple',
  READY: 'success', WARNING: 'warning', ERROR: 'error',
};

const apiError = (error: unknown) =>
  (error as { response?: { data?: { message?: string } } }).response?.data?.message
  ?? 'The request could not be completed';

export function openingStockImportPermissions(roles: string[]) {
  return {
    canPrepare: roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_OPERATIONS'),
    canPostOrReverse: roles.includes('ROLE_ADMIN'),
  };
}

export function OpeningStockImportPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const permissions = openingStockImportPermissions(user?.roles ?? []);
  const canPrepare = permissions.canPrepare;
  const isAdmin = permissions.canPostOrReverse;
  const [uploadForm] = Form.useForm<UploadValues>();
  const [itemForm] = Form.useForm<ItemMappingValues>();
  const [locationForm] = Form.useForm<LocationMappingValues>();
  const [postForm] = Form.useForm<{ expectedChecksum: string; confirmed: boolean }>();
  const [reverseForm] = Form.useForm<{ reason: string }>();
  const [page, setPage] = useState(1);
  const [rowPage, setRowPage] = useState(1);
  const [search, setSearch] = useState('');
  const [rowSearch, setRowSearch] = useState('');
  const [rowStatus, setRowStatus] = useState<ValidationStatus>();
  const [selectedId, setSelectedId] = useState<number>();
  const [uploadOpen, setUploadOpen] = useState(false);
  const [mappingRow, setMappingRow] = useState<ImportRow>();
  const [mappingLocation, setMappingLocation] = useState<LocationMapping>();
  const [postOpen, setPostOpen] = useState(false);
  const [reverseOpen, setReverseOpen] = useState(false);

  const batches = useQuery({
    queryKey: ['stock-imports', page, search],
    queryFn: async () => (await apiClient.get<PageResponse<ImportBatch>>('/stock-imports', {
      params: { page: page - 1, size: 10, search, sort: 'createdAt,desc' },
    })).data,
  });
  const batch = useQuery({
    queryKey: ['stock-import', selectedId],
    queryFn: async () => (await apiClient.get<ImportBatch>(`/stock-imports/${selectedId}`)).data,
    enabled: selectedId !== undefined,
  });
  const rows = useQuery({
    queryKey: ['stock-import-rows', selectedId, rowPage, rowSearch, rowStatus],
    queryFn: async () => (await apiClient.get<PageResponse<ImportRow>>(`/stock-imports/${selectedId}/rows`, {
      params: { page: rowPage - 1, size: 25, search: rowSearch, status: rowStatus, sort: 'sourceExcelRow,asc' },
    })).data,
    enabled: selectedId !== undefined,
  });
  const locations = useQuery({
    queryKey: ['stock-import-locations', selectedId],
    queryFn: async () => (await apiClient.get<LocationMapping[]>(`/stock-imports/${selectedId}/location-mappings`)).data,
    enabled: selectedId !== undefined,
  });
  const preview = useQuery({
    queryKey: ['stock-import-preview', selectedId],
    queryFn: async () => (await apiClient.get<Preview>(`/stock-imports/${selectedId}/preview`)).data,
    enabled: selectedId !== undefined,
  });
  const report = useQuery({
    queryKey: ['stock-import-report', selectedId],
    queryFn: async () => (await apiClient.get<ImportReport>(`/stock-imports/${selectedId}/report`)).data,
    enabled: selectedId !== undefined,
  });
  const items = useQuery({
    queryKey: ['stock-import-item-options'],
    queryFn: async () => (await apiClient.get<PageResponse<ItemOption>>('/items', {
      params: { page: 0, size: 500, active: true, sort: 'itemCode,asc' },
    })).data.content,
    enabled: canPrepare,
  });
  const parties = useQuery({
    queryKey: ['stock-import-party-options'],
    queryFn: async () => (await apiClient.get<PageResponse<PartyOption>>('/parties', {
      params: { page: 0, size: 500, active: true, sort: 'legalName,asc' },
    })).data.content,
    enabled: canPrepare,
  });
  const sites = useQuery({
    queryKey: ['stock-import-site-options'],
    queryFn: async () => (await apiClient.get<PageResponse<SiteOption>>('/sites', {
      params: { page: 0, size: 500, sort: 'siteName,asc' },
    })).data.content,
    enabled: canPrepare,
  });

  const refreshSelected = () => {
    queryClient.invalidateQueries({ queryKey: ['stock-imports'] });
    queryClient.invalidateQueries({ queryKey: ['stock-import', selectedId] });
    queryClient.invalidateQueries({ queryKey: ['stock-import-rows', selectedId] });
    queryClient.invalidateQueries({ queryKey: ['stock-import-locations', selectedId] });
    queryClient.invalidateQueries({ queryKey: ['stock-import-preview', selectedId] });
    queryClient.invalidateQueries({ queryKey: ['stock-import-report', selectedId] });
  };

  const upload = useMutation({
    mutationFn: async (values: UploadValues) => {
      const source = values.file[0]?.originFileObj;
      if (!source) throw new Error('Select the client XLSX workbook');
      const body = new FormData();
      body.append('file', source);
      if (values.notes) body.append('notes', values.notes);
      return (await apiClient.post<ImportBatch>('/stock-imports/upload', body, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })).data;
    },
    onSuccess: (created) => {
      message.success(`Parsed ${created.totalBalanceRows} opening balance rows`);
      setUploadOpen(false);
      uploadForm.resetFields();
      setSelectedId(created.id);
      queryClient.invalidateQueries({ queryKey: ['stock-imports'] });
    },
    onError: (error) => message.error(apiError(error)),
  });

  const mapItem = useMutation({
    mutationFn: async (values: ItemMappingValues) => {
      if (!selectedId || !mappingRow) throw new Error('No import row selected');
      return (await apiClient.put(`/stock-imports/${selectedId}/rows/${mappingRow.id}/item-mapping`, {
        itemId: values.mode === 'existing' ? values.itemId : null,
        createNew: values.mode === 'create',
        itemCode: values.itemCode,
        itemName: values.itemName,
        saveAlias: values.mode !== 'exclude' && values.saveAlias,
        confirmDuplicate: values.confirmDuplicate,
        exclude: values.mode === 'exclude',
        exclusionReason: values.exclusionReason,
        version: mappingRow.version,
      })).data;
    },
    onSuccess: () => {
      message.success('Source item mapping saved for every balance in this Excel row');
      setMappingRow(undefined);
      itemForm.resetFields();
      refreshSelected();
      queryClient.invalidateQueries({ queryKey: ['stock-import-item-options'] });
    },
    onError: (error) => message.error(apiError(error)),
  });

  const mapLocation = useMutation({
    mutationFn: async (values: LocationMappingValues) => {
      if (!selectedId || !mappingLocation) throw new Error('No source location selected');
      return (await apiClient.put(`/stock-imports/${selectedId}/location-mappings`, {
        sourceExcelColumn: mappingLocation.sourceExcelColumn,
        partyId: values.partyMode === 'existing' ? values.partyId : null,
        siteId: values.siteMode === 'existing' ? values.siteId : null,
        createParty: values.partyMode === 'create',
        partyName: values.partyName,
        createSite: values.siteMode === 'create',
        siteName: values.siteName,
        siteCode: values.siteCode,
      })).data;
    },
    onSuccess: () => {
      message.success('Party and site mapping saved for the entire source column');
      setMappingLocation(undefined);
      locationForm.resetFields();
      refreshSelected();
      queryClient.invalidateQueries({ queryKey: ['stock-import-party-options'] });
      queryClient.invalidateQueries({ queryKey: ['stock-import-site-options'] });
    },
    onError: (error) => message.error(apiError(error)),
  });

  const validate = useMutation({
    mutationFn: async () => (await apiClient.post<Preview>(`/stock-imports/${selectedId}/validate`)).data,
    onSuccess: (result) => {
      result.postable
        ? message.success('Import validated and ready for administrator posting')
        : message.warning(`${result.errorRows} row(s) still require mapping or clarification`);
      refreshSelected();
    },
    onError: (error) => message.error(apiError(error)),
  });

  const autoMap = useMutation({
    mutationFn: async () => (await apiClient.post<Preview>(`/stock-imports/${selectedId}/auto-map`)).data,
    onSuccess: (result) => {
      result.postable
        ? message.success('Auto-mapped and validated. Admin can post opening stock now.')
        : message.warning(`${result.errorRows} row(s) still need review after auto-map`);
      refreshSelected();
      queryClient.invalidateQueries({ queryKey: ['stock-import-item-options'] });
      queryClient.invalidateQueries({ queryKey: ['stock-import-party-options'] });
      queryClient.invalidateQueries({ queryKey: ['stock-import-site-options'] });
    },
    onError: (error) => message.error(apiError(error)),
  });

  const post = useMutation({
    mutationFn: async (values: { expectedChecksum: string; confirmed: boolean }) =>
      (await apiClient.post<Preview>(`/stock-imports/${selectedId}/post`, values)).data,
    onSuccess: (result) => {
      message.success(`Posted ${result.postedRows} immutable opening-stock transactions`);
      setPostOpen(false);
      postForm.resetFields();
      refreshSelected();
      queryClient.invalidateQueries({ queryKey: ['stock-balances'] });
      queryClient.invalidateQueries({ queryKey: ['stock-history'] });
      queryClient.invalidateQueries({ queryKey: ['stock-summary'] });
    },
    onError: (error) => message.error(apiError(error)),
  });

  const reverse = useMutation({
    mutationFn: async (values: { reason: string }) =>
      (await apiClient.post<Preview>(`/stock-imports/${selectedId}/reverse`, values)).data,
    onSuccess: () => {
      message.success('Opening-stock import reversed with immutable ledger transactions');
      setReverseOpen(false);
      reverseForm.resetFields();
      refreshSelected();
      queryClient.invalidateQueries({ queryKey: ['stock-balances'] });
      queryClient.invalidateQueries({ queryKey: ['stock-history'] });
      queryClient.invalidateQueries({ queryKey: ['stock-summary'] });
    },
    onError: (error) => message.error(apiError(error)),
  });

  const selectedPartyId = Form.useWatch('partyId', locationForm);
  const currentBatch = batch.data;
  const currentPreview = preview.data;
  const editable = currentBatch ? !['POSTED', 'REVERSED'].includes(currentBatch.status) : false;
  const eligibleSites = useMemo(
    () => (sites.data ?? []).filter((site) => site.partyId === selectedPartyId && site.status !== 'CLOSED'),
    [selectedPartyId, sites.data],
  );

  const openItemMapping = (row: ImportRow) => {
    setMappingRow(row);
    itemForm.setFieldsValue({
      mode: row.excluded ? 'exclude' : row.mappedItemId ? 'existing' : 'existing',
      itemId: row.mappedItemId,
      itemCode: `MAT-${String(row.sourceSrNumber ?? row.sourceExcelRow).padStart(3, '0')}`,
      itemName: row.normalizedItemSuggestion ?? row.sourceItemName,
      saveAlias: false,
      confirmDuplicate: row.duplicateConfirmed,
      exclusionReason: row.exclusionReason,
    });
  };

  const openLocationMapping = (location: LocationMapping) => {
    setMappingLocation(location);
    locationForm.setFieldsValue({
      partyMode: 'existing',
      partyId: location.mappedPartyId,
      siteMode: 'existing',
      siteId: location.mappedSiteId,
    });
  };

  return (
    <div className="page-stack">
      <div className="page-header-container">
        <div>
          <h1 className="page-heading">Opening Stock Import</h1>
          <p className="page-description">Stage, reconcile, and post the client’s legacy stock snapshot without creating purchases or challans.</p>
        </div>
        {canPrepare && <Button type="primary" icon={<CloudUploadOutlined />} onClick={() => setUploadOpen(true)}>Upload workbook</Button>}
      </div>

      <Alert type="warning" showIcon
        message="The source represents two different snapshot dates"
        description="Party/site issued stock is dated 25-07-2026. Godown available stock is dated 16-07-2026. The combined quantity must not be treated as a same-date physical count." />

      <Card className="premium-card" title="Import batches">
        <div className="filters-bar">
          <Input.Search allowClear placeholder="Search batch, filename, or checksum" value={search}
            onChange={(event) => { setSearch(event.target.value); setPage(1); }} style={{ width: 380 }} />
          <Button icon={<ReloadOutlined />} onClick={() => batches.refetch()}>Refresh</Button>
        </div>
        <Table rowKey="id" loading={batches.isLoading} dataSource={batches.data?.content} scroll={{ x: 1100 }}
          locale={{ emptyText: <Empty description="No opening-stock imports yet" /> }}
          columns={[
            { title: 'Batch', dataIndex: 'batchCode' },
            { title: 'Source file', dataIndex: 'originalFilename' },
            { title: 'Party date', dataIndex: 'partySnapshotDate' },
            { title: 'Godown date', dataIndex: 'godownSnapshotDate' },
            { title: 'Rows', dataIndex: 'totalBalanceRows' },
            { title: 'Expected total', dataIndex: 'expectedCombinedTotal', render: quantity },
            { title: 'Status', dataIndex: 'status', render: (value: string) => <Tag color={statusColor[value]}>{value.replaceAll('_', ' ')}</Tag> },
            { title: 'Action', render: (_value: unknown, row: ImportBatch) =>
              <Button type={selectedId === row.id ? 'primary' : 'default'} icon={<FileSearchOutlined />} onClick={() => {
                setSelectedId(row.id);
                setRowPage(1);
              }}>Open</Button> },
          ]}
          pagination={{ current: page, pageSize: 10, total: batches.data?.totalElements, onChange: setPage, showSizeChanger: false }} />
      </Card>

      {selectedId && currentBatch && currentPreview && <Card className="premium-card"
        title={<Space><span>{currentBatch.batchCode}</span><Tag color={statusColor[currentBatch.status]}>{currentBatch.status.replaceAll('_', ' ')}</Tag></Space>}
        extra={<Space wrap>
          {canPrepare && editable && <Popconfirm
            title="Auto-map this opening stock import?"
            description="Creates legacy items and one party/site per source column, confirms known ambiguous rows, and validates. Posting still needs admin confirmation."
            okText="Auto-map"
            cancelText="Cancel"
            onConfirm={() => autoMap.mutate()}
          >
            <Button icon={<ThunderboltOutlined />} loading={autoMap.isPending}>Auto-map from Excel</Button>
          </Popconfirm>}
          {canPrepare && editable && <Button icon={<SafetyCertificateOutlined />} loading={validate.isPending} onClick={() => validate.mutate()}>Validate</Button>}
          {isAdmin && currentBatch.status === 'VALIDATED' &&
            <Button type="primary" icon={<CheckCircleOutlined />} disabled={!currentPreview.postable}
              onClick={() => { postForm.setFieldsValue({ expectedChecksum: currentBatch.fileChecksum, confirmed: false }); setPostOpen(true); }}>Post opening balances</Button>}
          {isAdmin && currentBatch.status === 'POSTED' &&
            <Button danger icon={<RollbackOutlined />} onClick={() => setReverseOpen(true)}>Reverse import</Button>}
        </Space>}>
        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, lg: 3 }} style={{ marginBottom: 18 }}>
          <Descriptions.Item label="Source filename">{currentBatch.originalFilename}</Descriptions.Item>
          <Descriptions.Item label="Source format">{currentBatch.sourceFormat}</Descriptions.Item>
          <Descriptions.Item label="Uploaded by">{currentBatch.createdBy}</Descriptions.Item>
          <Descriptions.Item label="Checksum" span={3}><Typography.Text copyable code>{currentBatch.fileChecksum}</Typography.Text></Descriptions.Item>
          {currentBatch.importedAt && <Descriptions.Item label="Posted">{currentBatch.importedAt} by {currentBatch.importedBy}</Descriptions.Item>}
          {currentBatch.reversedAt && <Descriptions.Item label="Reversed">{currentBatch.reversedAt} by {currentBatch.reversedBy}</Descriptions.Item>}
          {currentBatch.reversalReason && <Descriptions.Item label="Reversal reason">{currentBatch.reversalReason}</Descriptions.Item>}
        </Descriptions>

        <Row gutter={[12, 12]} style={{ marginBottom: 18 }}>
          <Summary title="Source item rows" value={currentPreview.sourceRows} />
          <Summary title="Balance rows" value={currentPreview.balanceRows} />
          <Summary title="Mapped rows" value={currentPreview.mappedRows} />
          <Summary title="Warning rows" value={currentPreview.warningRows} />
          <Summary title="Error rows" value={currentPreview.errorRows} />
          <Summary title="Posted rows" value={currentPreview.postedRows} />
          <Summary title="Expected party/site" value={currentPreview.expectedPartyTotal} />
          <Summary title="Expected godown" value={currentPreview.expectedGodownTotal} />
          <Summary title="Expected combined" value={currentPreview.expectedCombinedTotal} />
          <Summary title="Posted combined" value={currentPreview.postedCombinedTotal} />
        </Row>

        <Tabs items={[
          {
            key: 'rows',
            label: 'Item and balance rows',
            children: <>
              <div className="filters-bar">
                <Input.Search allowClear placeholder="Search source item, location, or column" value={rowSearch}
                  onChange={(event) => { setRowSearch(event.target.value); setRowPage(1); }} style={{ width: 360 }} />
                <Select allowClear placeholder="All validation statuses" value={rowStatus}
                  onChange={(value) => { setRowStatus(value); setRowPage(1); }} style={{ width: 210 }}
                  options={['READY', 'MAPPING_REQUIRED', 'WARNING', 'ERROR', 'POSTED', 'REVERSED']
                    .map((value) => ({ value, label: value.replaceAll('_', ' ') }))} />
              </div>
              <Table rowKey="id" loading={rows.isLoading} dataSource={rows.data?.content} scroll={{ x: 1800 }}
                columns={[
                  { title: 'Source row', dataIndex: 'sourceExcelRow', fixed: 'left', width: 100 },
                  { title: 'Source column', dataIndex: 'sourceExcelColumn', width: 115 },
                  { title: 'Sr.', dataIndex: 'sourceSrNumber', width: 60 },
                  { title: 'Source item', dataIndex: 'sourceItemName', width: 210 },
                  { title: 'Suggested name', dataIndex: 'normalizedItemSuggestion', width: 190 },
                  { title: 'Mapped item', width: 210, render: (_value: unknown, row: ImportRow) =>
                    row.mappedItemId ? `${row.mappedItemCode} · ${row.mappedItemName}` : '—' },
                  { title: 'Source location', dataIndex: 'sourceLocationName', width: 170 },
                  { title: 'Mapped party/site', width: 220, render: (_value: unknown, row: ImportRow) =>
                    row.locationType === 'GODOWN' ? row.mappedGodownCode : row.mappedSiteId ? `${row.mappedPartyName} / ${row.mappedSiteName}` : '—' },
                  { title: 'Quantity', dataIndex: 'quantity', width: 100, render: quantity },
                  { title: 'Snapshot', dataIndex: 'snapshotDate', width: 115 },
                  { title: 'Bucket', dataIndex: 'targetStockBucket', width: 105 },
                  { title: 'Status', dataIndex: 'validationStatus', width: 145, render: (value: string) =>
                    <Tag color={statusColor[value]}>{value.replaceAll('_', ' ')}</Tag> },
                  { title: 'Validation message', dataIndex: 'validationMessage', width: 310 },
                  { title: 'Action', fixed: 'right', width: 100, render: (_value: unknown, row: ImportRow) =>
                    canPrepare && editable ? <Button size="small" icon={<LinkOutlined />} onClick={() => openItemMapping(row)}>Map item</Button> : null },
                ]}
                pagination={{ current: rowPage, pageSize: 25, total: rows.data?.totalElements, onChange: setRowPage, showSizeChanger: false }} />
            </>,
          },
          {
            key: 'locations',
            label: `Party/site mapping (${locations.data?.filter((location) => location.mapped).length ?? 0}/16)`,
            children: <Table rowKey="sourceExcelColumn" loading={locations.isLoading} dataSource={locations.data}
              columns={[
                { title: 'Source column', dataIndex: 'sourceExcelColumn' },
                { title: 'Source location', dataIndex: 'sourceLocationName' },
                { title: 'Mapped party', dataIndex: 'mappedPartyName', render: (value?: string) => value ?? '—' },
                { title: 'Mapped site', dataIndex: 'mappedSiteName', render: (value?: string) => value ?? '—' },
                { title: 'Status', dataIndex: 'mapped', render: (value: boolean) =>
                  <Tag color={value ? 'success' : 'warning'}>{value ? 'MAPPED' : 'REQUIRED'}</Tag> },
                { title: 'Action', render: (_value: unknown, location: LocationMapping) =>
                  canPrepare && editable ? <Button onClick={() => openLocationMapping(location)}>{location.mapped ? 'Change mapping' : 'Map column'}</Button> : null },
              ]} pagination={false} />,
          },
          {
            key: 'preview',
            label: 'Dry-run preview',
            children: <PreviewPanel preview={currentPreview} />,
          },
          {
            key: 'report',
            label: 'Import audit report',
            children: <ReportPanel report={report.data} loading={report.isLoading} />,
          },
        ]} />
      </Card>}

      <FormDrawer open={uploadOpen} title="Upload client opening-stock workbook" okText="Upload and parse"
        onClose={() => { setUploadOpen(false); uploadForm.resetFields(); }}
        onSubmit={() => uploadForm.submit()} loading={upload.isPending} width={620}>
        <Alert type="info" showIcon style={{ marginBottom: 16 }}
          message="Controlled format: STEELFAB_STOCK_SNAPSHOT_V1"
          description="SteelFab reads Sheet1, item names from B, party/site balances from C:R, and godown balances from U. Source totals T and V are ignored." />
        <Form form={uploadForm} layout="vertical" onFinish={(values) => upload.mutate(values)}>
          <Form.Item name="file" label="XLSX workbook" valuePropName="fileList"
            getValueFromEvent={(event) => event?.fileList} rules={[{ required: true, message: 'Select the XLSX workbook' }]}>
            <Upload beforeUpload={() => false} accept=".xlsx" maxCount={1}>
              <Button icon={<CloudUploadOutlined />}>Choose workbook</Button>
            </Upload>
          </Form.Item>
          <Form.Item name="notes" label="Import notes"><Input.TextArea rows={3} maxLength={1000} /></Form.Item>
        </Form>
      </FormDrawer>

      <FormDrawer open={Boolean(mappingRow)} title="Map source item" width={720} okText="Save item mapping"
        onClose={() => { setMappingRow(undefined); itemForm.resetFields(); }}
        onSubmit={() => itemForm.submit()} loading={mapItem.isPending}>
        {mappingRow && <Descriptions size="small" bordered column={2} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Source Sr.">{mappingRow.sourceSrNumber}</Descriptions.Item>
          <Descriptions.Item label="Excel row">{mappingRow.sourceExcelRow}</Descriptions.Item>
          <Descriptions.Item label="Exact source value" span={2}>{mappingRow.sourceItemName}</Descriptions.Item>
          <Descriptions.Item label="Suggested name" span={2}>{mappingRow.normalizedItemSuggestion}</Descriptions.Item>
          {mappingRow.validationMessage && <Descriptions.Item label="Warning" span={2}>{mappingRow.validationMessage}</Descriptions.Item>}
        </Descriptions>}
        <Form form={itemForm} layout="vertical" onFinish={(values) => mapItem.mutate(values)}
          initialValues={{ mode: 'existing', saveAlias: false, confirmDuplicate: false }}>
          <Form.Item name="mode" label="Decision">
            <Radio.Group>
              <Radio.Button value="existing">Map existing item</Radio.Button>
              <Radio.Button value="create">Create separate item</Radio.Button>
              <Radio.Button value="exclude">Exclude with reason</Radio.Button>
            </Radio.Group>
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(previous, current) => previous.mode !== current.mode}>
            {({ getFieldValue }) => {
              const mode = getFieldValue('mode') as ItemMappingValues['mode'];
              if (mode === 'existing') return <Form.Item name="itemId" label="Existing active item" rules={[{ required: true }]}>
                <Select showSearch optionFilterProp="label" placeholder="Select item"
                  options={(items.data ?? []).map((item) => ({ value: item.id, label: `${item.itemCode} · ${item.itemName}` }))} />
              </Form.Item>;
              if (mode === 'create') return <Row gutter={12}>
                <Col span={8}><Form.Item name="itemCode" label="New item code" rules={[{ required: true }]}><Input maxLength={50} /></Form.Item></Col>
                <Col span={16}><Form.Item name="itemName" label="Confirmed item name" rules={[{ required: true }]}><Input maxLength={150} /></Form.Item></Col>
              </Row>;
              return <Form.Item name="exclusionReason" label="Documented exclusion reason" rules={[{ required: true }]}>
                <Input.TextArea rows={3} maxLength={500} />
              </Form.Item>;
            }}
          </Form.Item>
          <Space direction="vertical">
            <Form.Item name="saveAlias" valuePropName="checked" noStyle><Checkbox>Save the exact source name as an explicit alias</Checkbox></Form.Item>
            <Form.Item name="confirmDuplicate" valuePropName="checked" noStyle>
              <Checkbox>I explicitly confirm this duplicate/ambiguous source-item decision</Checkbox>
            </Form.Item>
          </Space>
        </Form>
      </FormDrawer>

      <FormDrawer open={Boolean(mappingLocation)} title="Map source party/site column" width={720} okText="Save entire column mapping"
        onClose={() => { setMappingLocation(undefined); locationForm.resetFields(); }}
        onSubmit={() => locationForm.submit()} loading={mapLocation.isPending}>
        {mappingLocation && <Alert type="info" showIcon style={{ marginBottom: 16 }}
          message={`${mappingLocation.sourceExcelColumn}: ${mappingLocation.sourceLocationName}`}
          description="This source label is preserved. Map it to a legal party and an open site; SteelFab does not assume the label represents both." />}
        <Form form={locationForm} layout="vertical" onFinish={(values) => mapLocation.mutate(values)}
          initialValues={{ partyMode: 'existing', siteMode: 'existing' }}>
          <Form.Item name="partyMode" label="Party decision"><Radio.Group>
            <Radio.Button value="existing">Select party</Radio.Button><Radio.Button value="create">Create party</Radio.Button>
          </Radio.Group></Form.Item>
          <Form.Item noStyle shouldUpdate={(previous, current) => previous.partyMode !== current.partyMode}>
            {({ getFieldValue }) => getFieldValue('partyMode') === 'create'
              ? <Form.Item name="partyName" label="Legal party name" rules={[{ required: true }]}><Input maxLength={150} /></Form.Item>
              : <Form.Item name="partyId" label="Existing active party" rules={[{ required: true }]}>
                <Select showSearch optionFilterProp="label" onChange={() => locationForm.setFieldValue('siteId', undefined)}
                  options={(parties.data ?? []).map((party) => ({ value: party.id, label: party.legalName }))} />
              </Form.Item>}
          </Form.Item>
          <Form.Item name="siteMode" label="Site decision"><Radio.Group>
            <Radio.Button value="existing">Select site under party</Radio.Button><Radio.Button value="create">Create site</Radio.Button>
          </Radio.Group></Form.Item>
          <Form.Item noStyle shouldUpdate={(previous, current) => previous.siteMode !== current.siteMode}>
            {({ getFieldValue }) => getFieldValue('siteMode') === 'create'
              ? <Row gutter={12}>
                <Col span={10}><Form.Item name="siteCode" label="New site code" rules={[{ required: true }]}><Input maxLength={50} /></Form.Item></Col>
                <Col span={14}><Form.Item name="siteName" label="New site name" rules={[{ required: true }]}><Input maxLength={150} /></Form.Item></Col>
              </Row>
              : <Form.Item name="siteId" label="Existing open site under selected party" rules={[{ required: true }]}>
                <Select showSearch optionFilterProp="label" disabled={!selectedPartyId}
                  options={eligibleSites.map((site) => ({ value: site.id, label: `${site.siteCode} · ${site.siteName}` }))} />
              </Form.Item>}
          </Form.Item>
        </Form>
      </FormDrawer>

      <Modal open={postOpen} title="Post opening balances" okText="Post immutable opening balances"
        okButtonProps={{ danger: true }} onCancel={() => setPostOpen(false)}
        onOk={() => postForm.submit()} confirmLoading={post.isPending}>
        <Alert type="warning" showIcon style={{ marginBottom: 16 }}
          message="This creates immutable ledger records"
          description="It will update AVAILABLE and ISSUED stock atomically. It will not create purchases, vendors, orders, challans, or rental charges." />
        <Form form={postForm} layout="vertical" onFinish={(values) => post.mutate(values)}>
          <Form.Item name="expectedChecksum" label="Confirm source file checksum"
            rules={[{ required: true }, { pattern: /^[0-9a-fA-F]{64}$/, message: 'Enter the complete 64-character checksum' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="confirmed" valuePropName="checked"
            rules={[{ validator: (_rule, value) => value ? Promise.resolve() : Promise.reject(new Error('Explicit confirmation is required')) }]}>
            <Checkbox>I reviewed all mappings, exclusions, warnings, and dry-run totals.</Checkbox>
          </Form.Item>
        </Form>
      </Modal>

      <Modal open={reverseOpen} title="Reverse posted opening-stock import" okText="Create reversal transactions"
        okButtonProps={{ danger: true }} onCancel={() => setReverseOpen(false)}
        onOk={() => reverseForm.submit()} confirmLoading={reverse.isPending}>
        <Alert type="error" showIcon style={{ marginBottom: 16 }}
          message="Original ledger entries will be preserved"
          description="Reversal is blocked if later stock movements make it unsafe. A documented reason is mandatory." />
        <Form form={reverseForm} layout="vertical" onFinish={(values) => reverse.mutate(values)}>
          <Form.Item name="reason" label="Reversal reason" rules={[{ required: true }]}><Input.TextArea rows={4} maxLength={500} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

function Summary({ title, value }: { title: string; value: number }) {
  return <Col xs={12} sm={8} lg={6} xl={4}><Card size="small"><Statistic title={title} value={value} formatter={() => quantity(value)} /></Card></Col>;
}

function PreviewPanel({ preview }: { preview: Preview }) {
  const data = [
    { key: 'party', label: 'Party/site issued', expected: preview.expectedPartyTotal, mapped: preview.mappedPartyTotal,
      excluded: preview.excludedPartyTotal, errors: preview.errorPartyTotal, posted: preview.postedPartyTotal },
    { key: 'godown', label: 'Godown available', expected: preview.expectedGodownTotal, mapped: preview.mappedGodownTotal,
      excluded: preview.excludedGodownTotal, errors: preview.errorGodownTotal, posted: preview.postedGodownTotal },
    { key: 'combined', label: 'Combined (mixed dates)', expected: preview.expectedCombinedTotal, mapped: preview.mappedCombinedTotal,
      excluded: preview.excludedTotal, errors: preview.errorTotal, posted: preview.postedCombinedTotal },
  ];
  return <>
    {preview.warnings.map((warning) => <Alert key={warning} type="warning" showIcon message={warning} style={{ marginBottom: 10 }} />)}
    <Table rowKey="key" dataSource={data} pagination={false}
      columns={[
        { title: 'Stock scope', dataIndex: 'label' },
        { title: 'Expected', dataIndex: 'expected', render: quantity },
        { title: 'Mapped', dataIndex: 'mapped', render: quantity },
        { title: 'Excluded', dataIndex: 'excluded', render: quantity },
        { title: 'Errors/unresolved', dataIndex: 'errors', render: quantity },
        { title: 'Posted', dataIndex: 'posted', render: quantity },
      ]} />
    <Alert style={{ marginTop: 16 }} type={preview.postable ? 'success' : 'info'} showIcon
      message={preview.postable ? 'Dry run reconciles and is ready for ADMIN posting' : 'Posting remains blocked'}
      description={preview.postable
        ? 'Every source quantity is mapped or explicitly excluded with a documented reason.'
        : 'Resolve mapping errors, confirm ambiguous items, and run validation again.'} />
  </>;
}

function ReportPanel({ report, loading }: { report?: ImportReport; loading: boolean }) {
  if (loading) return <Card loading />;
  if (!report) return <Empty description="Import report unavailable" />;
  return <Row gutter={16}>
    <Col xs={24} lg={12}>
      <Card size="small" title="Verified source findings">
        <ul>{report.findings.map((finding) => <li key={finding}>{finding}</li>)}</ul>
      </Card>
    </Col>
    <Col xs={24} lg={12}>
      <Card size="small" title="Client decisions to preserve">
        <ul>{report.unresolvedQuestions.map((question) => <li key={question}>{question}</li>)}</ul>
      </Card>
    </Col>
  </Row>;
}
