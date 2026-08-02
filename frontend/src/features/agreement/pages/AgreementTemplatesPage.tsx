import {useState} from 'react';
import {Alert,App,Button,Card,Descriptions,Empty,Form,Input,Modal,Space,Table,Tag,Upload} from 'antd';
import {CheckOutlined,DownloadOutlined,EyeOutlined,UploadOutlined} from '@ant-design/icons';
import {useMutation,useQuery,useQueryClient} from '@tanstack/react-query';
import {apiClient} from '../../../api/client';
import {useAuth} from '../../auth/context/AuthContext';
import type {AgreementTemplate,AgreementTemplateAnalysis} from '../types';

export function AgreementTemplatesPage(){
 const {message}=App.useApp(),qc=useQueryClient(),{user}=useAuth(),admin=user?.roles.includes('ROLE_ADMIN')??false;
 const [uploadOpen,setUploadOpen]=useState(false),[analysisId,setAnalysisId]=useState<number>();const[form]=Form.useForm();
 const templates=useQuery({queryKey:['agreement-templates'],queryFn:async()=>(await apiClient.get<AgreementTemplate[]>('/agreement-templates')).data});
 const analysis=useQuery({queryKey:['agreement-template-analysis',analysisId],queryFn:async()=>(await apiClient.get<AgreementTemplateAnalysis>(`/agreement-templates/${analysisId}/analysis`)).data,enabled:Boolean(analysisId)});
 const upload=useMutation({mutationFn:async(v:{name:string;description?:string;file:any[]})=>{const data=new FormData();data.append('name',v.name);if(v.description)data.append('description',v.description);data.append('file',v.file[0].originFileObj);return(await apiClient.post<AgreementTemplate>('/agreement-templates',data,{headers:{'Content-Type':'multipart/form-data'}})).data;},onSuccess:t=>{message.success('PDF analysed and template draft created');setUploadOpen(false);form.resetFields();void qc.invalidateQueries({queryKey:['agreement-templates']});setAnalysisId(t.id);},onError:(e:unknown)=>message.error(apiMessage(e))});
 const validate=useMutation({mutationFn:async(id:number)=>(await apiClient.post(`/agreement-templates/${id}/validate-analysis`)).data,onSuccess:()=>{message.success('Analysis marked as reviewed');void qc.invalidateQueries({queryKey:['agreement-templates']});void qc.invalidateQueries({queryKey:['agreement-template-analysis']});},onError:(e:unknown)=>message.error(apiMessage(e))});
 return <div className="page-stack"><div className="page-header-container"><div><h1 className="page-heading">Agreement templates</h1><p className="page-description">Import client PDFs, review locally extracted fields and manage generating designs.</p></div>{admin&&<Button type="primary" icon={<UploadOutlined/>} onClick={()=>setUploadOpen(true)}>Import client PDF</Button>}</div>
 <Alert type="info" showIcon message="Imported PDFs become review drafts" description="SteelFab extracts data locally without a paid AI API. A reviewed reference PDF does not generate agreements until a native layout or field mapping is completed."/>
 <Card className="premium-card"><Table rowKey="id" loading={templates.isLoading} dataSource={templates.data??[]} locale={{emptyText:<Empty description="No agreement templates"/>}} columns={[
  {title:'Template',render:(_:unknown,t:AgreementTemplate)=><><strong>{t.name}</strong><br/><span>{t.description}</span></>},
  {title:'Type',render:(_:unknown,t:AgreementTemplate)=><Tag color={t.renderingMode==='NATIVE'?'green':'blue'}>{t.renderingMode==='NATIVE'?'GENERATING DESIGN':'IMPORTED REFERENCE'}</Tag>},
  {title:'Analysis',render:(_:unknown,t:AgreementTemplate)=><Tag color={statusColor(t.analysisStatus)}>{t.analysisStatus.replaceAll('_',' ')}</Tag>},
  {title:'Pages',dataIndex:'pageCount',render:(v?:number)=>v??'—'},
  {title:'Version',dataIndex:'templateVersion'},
  {title:'Actions',render:(_:unknown,t:AgreementTemplate)=><Space wrap>{!t.builtIn&&<Button icon={<EyeOutlined/>} onClick={()=>setAnalysisId(t.id)}>Review</Button>}{t.originalFilename&&<Button icon={<DownloadOutlined/>} href={`/api/v1/agreement-templates/${t.id}/download`}>Source</Button>}</Space>}
 ]}/></Card>
 <Modal title="Import client agreement PDF" open={uploadOpen} onCancel={()=>setUploadOpen(false)} onOk={()=>form.submit()} confirmLoading={upload.isPending} okText="Upload and analyse">
  <Form form={form} layout="vertical" onFinish={v=>upload.mutate(v)}><Form.Item name="name" label="Template name" rules={[{required:true}]}><Input placeholder="Example: Rocks & Logs Agreement 2026"/></Form.Item><Form.Item name="description" label="Description"><Input.TextArea rows={2}/></Form.Item><Form.Item name="file" label="Client PDF" valuePropName="fileList" getValueFromEvent={e=>e?.fileList} rules={[{required:true}]}><Upload accept=".pdf" maxCount={1} beforeUpload={()=>false}><Button icon={<UploadOutlined/>}>Choose PDF</Button></Upload></Form.Item></Form>
 </Modal>
 <Modal title={analysis.data?.templateName??'Template analysis'} open={Boolean(analysisId)} onCancel={()=>setAnalysisId(undefined)} width={900} footer={analysis.data?.analysisStatus==='REVIEW_REQUIRED'&&admin?<Button type="primary" icon={<CheckOutlined/>} loading={validate.isPending} onClick={()=>validate.mutate(analysis.data!.templateId)}>Mark analysis reviewed</Button>:null}>
  {analysis.isLoading?<p>Analysing…</p>:analysis.data&&<div className="page-stack"><Alert type="warning" showIcon message="Human verification required" description="Detected values are suggestions. Review the source PDF before creating a native generating design or coordinate mapping."/><Descriptions bordered size="small" column={2} items={[{key:'status',label:'Status',children:analysis.data.analysisStatus.replaceAll('_',' ')},{key:'pages',label:'Pages',children:analysis.data.pageCount},{key:'checksum',label:'SHA-256',span:2,children:<code>{analysis.data.checksumSha256}</code>}]}/><Card size="small" title="Detected fields"><Descriptions bordered size="small" column={1} items={Object.entries(analysis.data.detectedFields).map(([key,value])=>({key,label:fieldLabel(key),children:value}))}/></Card>{analysis.data.warnings.map(w=><Alert key={w} type="warning" showIcon message={w}/>) }<Card size="small" title="Extracted document text"><Input.TextArea readOnly rows={14} value={analysis.data.extractedText}/></Card></div>}
 </Modal>
 </div>;
}
const statusColor=(s:string)=>s==='VALIDATED'?'green':s==='REVIEW_REQUIRED'?'orange':s==='FAILED'?'red':'default';
const fieldLabel=(v:string)=>v.replace(/([A-Z])/g,' $1').replace(/^./,c=>c.toUpperCase());
const apiMessage=(e:unknown)=>(e as {response?:{data?:{message?:string}}}).response?.data?.message??'Template operation failed';
