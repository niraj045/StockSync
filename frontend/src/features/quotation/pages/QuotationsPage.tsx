import { useEffect,useMemo,useState } from 'react';
import { Alert,App,Button,Card,Descriptions,Form,Input,InputNumber,Modal,Select,Space,Table,Tag } from 'antd';
import { DownloadOutlined,EyeOutlined,FileDoneOutlined,PlusOutlined,SearchOutlined } from '@ant-design/icons';
import { useMutation,useQuery,useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { useAuth } from '../../auth/context/AuthContext';
import type { Option,Page,Quotation,Template } from '../types';
import {useLocation,useNavigate,useParams} from 'react-router';
import { apiErrorCode,apiFormErrors,quotationPermissions,requiresUnsavedConfirmation,sitesForParty,validateQuotationEditor,type QuotationEditor as Editor } from '../quotationForm';
import {ReportExcelButton} from '../../../components/ReportExcelButton';

const rental=[{value:'PER_PIECE_PER_DAY',label:'Per piece per day'},{value:'PER_PIECE_PER_WEEK',label:'Per piece per week'},{value:'PER_PIECE_PER_MONTH',label:'Per piece per month'}];
type StockRow={itemId:number;availableQuantity:number};
const exactTemplateCode='STEELFAB_EXACT_HIRE_V1';
const exactSlots=[
  {key:'hframe',label:'H Frame'},{key:'bracing',label:'Bracing'},{key:'mspipe',label:'20 Ft / MS Pipe'},
  {key:'platepipe',label:'Plate Pipe'},{key:'basejack',label:'Base Jack'},{key:'platform',label:'Platform'},{key:'coupler',label:'Coupler'},
];
const normalize=(value?:string)=>value?.toLowerCase().replace(/[^a-z0-9]/g,'')??'';
const stateCodes:Record<string,string>={
  andhrapradesh:'37',arunachalpradesh:'12',assam:'18',bihar:'10',chhattisgarh:'22',goa:'30',gujarat:'24',haryana:'06',
  himachalpradesh:'02',jharkhand:'20',karnataka:'29',kerala:'32',madhyapradesh:'23',maharashtra:'27',manipur:'14',
  meghalaya:'17',mizoram:'15',nagaland:'13',odisha:'21',punjab:'03',rajasthan:'08',sikkim:'11',tamilnadu:'33',
  telangana:'36',tripura:'16',uttarpradesh:'09',uttarakhand:'05',westbengal:'19',delhi:'07',jammuandkashmir:'01',ladakh:'38',
};
const stateCode=(gstin?:string,location?:string)=>{const value=gstin?.trim().toUpperCase();if(value&&/^\d{2}[0-9A-Z]{13}$/.test(value))return value.slice(0,2);const normalized=normalize(location);return Object.entries(stateCodes).find(([state])=>normalized.includes(state))?.[1];};
const addDays=(date:string,days:number)=>{const value=new Date(`${date}T00:00:00`);value.setDate(value.getDate()+days);return value.toISOString().slice(0,10);};
const matchesSlot=(item:Option,key:string)=>{const value=normalize(`${item.itemCode} ${item.itemName}`);if(key==='hframe')return value.includes('hframe');if(key==='bracing')return value.includes('bracing')||value.includes('crossbrace');if(key==='mspipe')return value.includes('20ftpipe')||value.includes('mspipe')||value.includes('steelpipe');if(key==='platepipe')return value.includes('platepipe');if(key==='basejack')return value.includes('basejack');if(key==='platform')return value.includes('platform')||value.includes('walkway');return value.includes('coupler')||value.includes('clamp');};
export const exactDefaultItems=(items:Option[])=>exactSlots.flatMap(slot=>{const item=items.find(candidate=>matchesSlot(candidate,slot.key));return item?[{itemId:item.id,quantity:0,requiredQuantity:0,rate:0,hireMonths:6,replacementRate:0,rentalType:'PER_PIECE_PER_MONTH'}]:[];});
const money=(v?:number)=>new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:0}).format(v??0);
export function QuotationsPage(){
  const navigate=useNavigate(),location=useLocation(),{quotationId}=useParams();
  const {modal,message}=App.useApp(),{user}=useAuth(),roles=user?.roles??[],{isAdmin,canWrite}=quotationPermissions(roles),qc=useQueryClient();
  const [search,setSearch]=useState(''),[status,setStatus]=useState<string|undefined>(),[editing,setEditing]=useState<Quotation|null>(null),[selected,setSelected]=useState<Quotation|null>(null);
  const [form]=Form.useForm<Editor>();const partyId=Form.useWatch('partyId',form);const siteId=Form.useWatch('siteId',form);const discountType=Form.useWatch('discountType',form);const templateId=Form.useWatch('quotationTemplateId',form);const watched=Form.useWatch([],form);
  const isEditorRoute=location.pathname==='/quotations/new'||Boolean(quotationId);
  const quotations=useQuery({queryKey:['quotations',search,status],queryFn:async()=>(await apiClient.get<Page<Quotation>>('/quotations',{params:{search,status,size:50,sort:'id,desc'}})).data});
  const templates=useQuery({queryKey:['quotation-templates-active'],queryFn:async()=>(await apiClient.get<Page<Template>>('/quotation-templates',{params:{active:true,size:100}})).data.content});
  const parties=useQuery({queryKey:['parties-options'],queryFn:async()=>(await apiClient.get<Page<Option>>('/parties',{params:{active:true,size:500,sort:'id,desc'}})).data.content});
  const sites=useQuery({queryKey:['sites-options'],queryFn:async()=>(await apiClient.get<Page<Option>>('/sites',{params:{size:500,sort:'id,desc'}})).data.content});
  const items=useQuery({queryKey:['items-options'],queryFn:async()=>(await apiClient.get<Page<Option>>('/items',{params:{active:true,size:500}})).data.content});
  const stock=useQuery({queryKey:['quotation-stock-balances'],queryFn:async()=>(await apiClient.get<Page<StockRow>>('/stock/balances',{params:{size:500}})).data.content});
  const availability=useMemo(()=>new Map((stock.data??[]).map(row=>[row.itemId,Number(row.availableQuantity)||0])),[stock.data]);
  const selectedTemplate=(templates.data??[]).find(template=>template.id===templateId);
  const selectedParty=(parties.data??[]).find(party=>party.id===partyId);
  const selectedSite=(sites.data??[]).find(site=>site.id===siteId);
  const isExact=selectedTemplate?.templateCode===exactTemplateCode;
  const automaticGst=useMemo(()=>{const total=Number(watched?.exactHire?.gstPercentage)||0;const supplier=stateCode(selectedTemplate?.companyGstin,selectedTemplate?.companyAddress);const customer=stateCode(selectedParty?.gstin,selectedParty?.state);return supplier&&supplier===customer?{cgst:total/2,sgst:total/2,igst:0,label:'Same state'}:{cgst:0,sgst:0,igst:total,label:'Interstate / state unavailable'};},[watched?.exactHire?.gstPercentage,selectedTemplate,selectedParty]);
  const oneMonthRent=useMemo(()=>((watched?.items??[]).reduce((sum,item)=>sum+(Number(item.quantity)||0)*(Number(item.rate)||0),0)),[watched?.items]);
  const save=useMutation({mutationFn:async(v:Editor)=>{const payload=isExact?{...v,rentalType:'PER_PIECE_PER_MONTH',items:v.items.map(item=>({...item,rentalType:'PER_PIECE_PER_MONTH'}))}:v;return editing?(await apiClient.put(`/quotations/${editing.id}`,{...payload,version:editing.version})).data:(await apiClient.post('/quotations',payload)).data;},
    onSuccess:(q:Quotation)=>{message.success(`Saved ${q.quotationNumber}`);navigate('/quotations');void qc.invalidateQueries({queryKey:['quotations']});},onError:(error:unknown)=>{const fields=apiFormErrors(error);if(fields.length)form.setFields(fields);message.error(apiErrorCode(error)==='OPTIMISTIC_LOCK_CONFLICT'?'This quotation was changed by another user. Reload and try again.':'Unable to save quotation.');}});
  const action=useMutation({mutationFn:async({q,name,reason}:{q:Quotation;name:string;reason?:string})=>(await apiClient.post(`/quotations/${q.id}/${name}`,reason?{reason}:undefined)).data,
    onSuccess:()=>void qc.invalidateQueries({queryKey:['quotations']})});
  const defaults=()=>({quotationDate:new Date().toISOString().slice(0,10),validUntil:new Date(Date.now()+30*86400000).toISOString().slice(0,10),rentalType:'PER_PIECE_PER_DAY',discountType:'NONE',discountValue:0,cgstRate:9,sgstRate:9,igstRate:0,transportCharge:0,loadingCharge:0,unloadingCharge:0,otherCharge:0,roundOff:0,securityDeposit:0,items:[{quantity:1,rentalType:'PER_PIECE_PER_DAY'}]});
  const edit=(q?:Quotation)=>navigate(q?`/quotations/${q.id}/edit`:'/quotations/new');
  const closeEditor=()=>{const close=()=>{setEditing(null);navigate('/quotations');};if(requiresUnsavedConfirmation(form.isFieldsTouched()))modal.confirm({title:'Discard unsaved changes?',onOk:close});else close();};
  useEffect(()=>{
    if(location.pathname==='/quotations/new'){form.resetFields();setEditing(null);form.setFieldsValue(defaults());}
    if(quotationId&&quotations.data){const quotation=quotations.data.content.find(q=>q.id===Number(quotationId));if(quotation){form.resetFields();setEditing(quotation);form.setFieldsValue({...quotation});}}
  },[location.pathname,quotationId,quotations.data]);
  useEffect(()=>{
    if(!isExact||!items.data||editing?.quotationTemplateCode===exactTemplateCode)return;
    const current=form.getFieldValue('items')??[];if(current.length>0&&current.every((line:{itemId?:number})=>line.itemId))return;
    form.setFieldsValue({rentalType:'PER_PIECE_PER_MONTH',discountType:'NONE',discountValue:0,cgstRate:0,sgstRate:0,igstRate:18,transportCharge:0,loadingCharge:0,unloadingCharge:0,otherCharge:0,roundOff:0,validUntil:addDays(form.getFieldValue('quotationDate')??new Date().toISOString().slice(0,10),7),
      exactHire:{partyAddress:selectedParty?.address??'',subject:`Quotation for Supply of H frame Scaffolding Materials on Hire for ${selectedSite?.siteName??'the selected site'}.`,validityDays:7,minimumHirePeriod:'6 Months (180 days)',minimumHireDays:90,gstPercentage:18,paymentDueDays:3,authorizedPerson:'',authorizedDesignation:'',authorizedPhone:''},
      items:exactDefaultItems(items.data)});
  },[isExact,items.data,editing,form,selectedParty,selectedSite]);
  useEffect(()=>{
    if(!isExact)return;
    form.setFieldsValue({cgstRate:automaticGst.cgst,sgstRate:automaticGst.sgst,igstRate:automaticGst.igst});
  },[isExact,automaticGst.cgst,automaticGst.sgst,automaticGst.igst,form]);
  useEffect(()=>{
    if(!isExact)return;
    if(selectedParty)form.setFieldValue(['exactHire','partyAddress'],selectedParty.address??'');
    if(selectedSite)form.setFieldValue(['exactHire','subject'],`Quotation for Supply of H frame Scaffolding Materials on Hire for ${selectedSite.siteName??'the selected site'}.`);
  },[isExact,selectedParty,selectedSite,form]);
  useEffect(()=>{
    if(!isEditorRoute)return;
    window.scrollTo({top:0,behavior:'instant'});
    document.querySelector('.app-content')?.scrollTo({top:0,behavior:'instant'});
  },[isEditorRoute,location.pathname]);
  const confirm=(q:Quotation,name:string,needsReason=false)=>{let reason='';modal.confirm({title:`${name} ${q.quotationNumber}?`,content:needsReason?<Input.TextArea placeholder="Reason is required" onChange={e=>reason=e.target.value}/>:undefined,onOk:()=>{if(needsReason&&!reason.trim()){message.error('Reason is required');return Promise.reject();}return action.mutateAsync({q,name,reason});}});};
  const download=async(q:Quotation)=>{const r=await apiClient.get(`/quotations/${q.id}/pdf`,{responseType:'blob'});const url=URL.createObjectURL(r.data as Blob);const a=document.createElement('a');a.href=url;a.download=`quotation-${q.quotationNumber.replaceAll('/','-')}.pdf`;a.click();URL.revokeObjectURL(url);};
  const preview=async(q:Quotation)=>{const r=await apiClient.get(`/quotations/${q.id}/pdf/preview`,{params:{template:exactTemplateCode},responseType:'blob'});const url=URL.createObjectURL(r.data as Blob);window.open(url,'_blank','noopener,noreferrer');window.setTimeout(()=>URL.revokeObjectURL(url),60000);};
  const finalizePdf=async(q:Quotation)=>{const r=await apiClient.post(`/quotations/${q.id}/pdf/finalize`,undefined,{responseType:'blob'});const url=URL.createObjectURL(r.data as Blob);const a=document.createElement('a');a.href=url;a.download=`steelfab-hire-${q.quotationNumber.replaceAll('/','-')}.pdf`;a.click();URL.revokeObjectURL(url);message.success('Exact PDF finalized and locked');void qc.invalidateQueries({queryKey:['quotations']});};
  const applyStandardTerms = async () => {
    const current = form.getFieldValue('terms');
    const doFetch = async () => {
      try {
        const r = await apiClient.get<{headerText?:string, partATitle?:string, partBTitle?:string, terms:string}>('/settings/default-terms?documentType=QUOTATION');
        form.setFieldsValue({ headerText: r.data.headerText, partATitle: r.data.partATitle, partBTitle: r.data.partBTitle, terms: r.data.terms });
      } catch (e) {
        message.error('Failed to load standard terms');
      }
    };
    if (current && current.trim()) {
      modal.confirm({title: 'Replace text?', content: 'This will replace the current header text and terms with the standard ones. Continue?', onOk: doFetch});
    } else {
      doFetch();
    }
  };
  const estimate=useMemo(()=>{const v=watched; if(!v)return 0;const sub=(v.items??[]).reduce((s,i)=>s+(Number(i.quantity)||0)*(Number(i.rate)||0)*(isExact?(Number(i.hireMonths)||0):1),0);const discount=v.discountType==='PERCENTAGE'?sub*(Number(v.discountValue)||0)/100:v.discountType==='FIXED'?Number(v.discountValue)||0:0;const taxable=sub-discount+(Number(v.transportCharge)||0)+(Number(v.loadingCharge)||0)+(Number(v.unloadingCharge)||0)+(Number(v.otherCharge)||0);const tax=isExact?Number(v.exactHire?.gstPercentage)||0:(Number(v.cgstRate)||0)+(Number(v.sgstRate)||0)+(Number(v.igstRate)||0);return taxable*(1+tax/100)+(Number(v.roundOff)||0);},[watched,isExact]);
  if(isEditorRoute)return <div className="quotation-editor-page">
    <button type="button" className="quotation-back-link" onClick={closeEditor}>← Back to quotations</button>
    <div className="quotation-editor-heading">
      <h1 className="page-heading">{editing?'Edit quotation':'New quotation'}</h1>
      <p className="page-description">Configure commercial terms, item lines, taxes and totals.</p>
    </div>
    <Form className="quotation-editor-form" form={form} layout="vertical" onFinish={v=>{const sanitized={...v,cgstRate:isExact?automaticGst.cgst:v.cgstRate,sgstRate:isExact?automaticGst.sgst:v.sgstRate,igstRate:isExact?automaticGst.igst:v.igstRate,transportCharge:v.transportCharge??0,loadingCharge:v.loadingCharge??0,unloadingCharge:v.unloadingCharge??0,otherCharge:v.otherCharge??0,roundOff:v.roundOff??0,securityDeposit:v.securityDeposit??0,exactHire:isExact?{...v.exactHire,advanceRent:v.exactHire?.advanceRent??oneMonthRent}:undefined,discountValue:v.discountType==='NONE'||v.discountValue===undefined||v.discountValue===null?0:v.discountValue};const errors=validateQuotationEditor(sanitized,sites.data??[]);if(errors.length){form.setFields(errors);return;}save.mutate(sanitized);}}>
      <section className="quotation-form-section">
        <h2>Quotation details</h2>
        <div className="master-form-grid">
          <Form.Item name="quotationTemplateId" label="Template" rules={[{required:true}]}><Select placeholder="Select quotation template" options={(templates.data??[]).map(t=>({value:t.id,label:`${t.templateCode} — ${t.name}`}))}/></Form.Item>
          <Form.Item name="quotationDate" label="Quotation date" rules={[{required:true}]}><Input type="date" onChange={event=>{if(isExact){const days=Number(form.getFieldValue(['exactHire','validityDays']))||7;form.setFieldValue('validUntil',addDays(event.target.value,days));}}}/></Form.Item>
          <Form.Item name="validUntil" label="Valid until" rules={[{required:true}]}><Input type="date"/></Form.Item>
        </div>
      </section>
      <section className="quotation-form-section">
        <h2>Customer and site</h2>
        <div className="master-form-grid">
          <Form.Item name="partyId" label="Party" rules={[{required:true}]}><Select placeholder="Select party" showSearch optionFilterProp="label" options={(parties.data??[]).map(p=>({value:p.id,label:p.legalName}))} onChange={(value:number)=>{form.setFieldValue('siteId',undefined);const party=(parties.data??[]).find(p=>p.id===value);if(isExact)form.setFieldValue(['exactHire','partyAddress'],party?.address??'');}}/></Form.Item>
          <Form.Item name="siteId" label="Site" rules={[{required:true}]}><Select placeholder={partyId?'Select site':'Select a party first'} disabled={!partyId} showSearch optionFilterProp="label" options={sitesForParty(sites.data??[],partyId).map(s=>({value:s.id,label:`${s.siteCode} — ${s.siteName}`}))}/></Form.Item>
        </div>
      </section>
      {isExact&&<section className="quotation-form-section">
        <h2>Project and document details</h2>
        <div className="master-form-grid">
          <Form.Item className="master-form-wide" name={['exactHire','partyAddress']} label="Customer address" rules={[{required:true}]}><Input.TextArea rows={2}/></Form.Item>
          <Form.Item className="master-form-wide" name={['exactHire','subject']} label="Subject" rules={[{required:true}]}><Input/></Form.Item>
          <Form.Item name={['exactHire','validityDays']} label="Validity (days)" rules={[{required:true}]}><InputNumber min={1} max={365} style={{width:'100%'}} onChange={value=>{const date=form.getFieldValue('quotationDate');if(date&&Number(value)>0)form.setFieldValue('validUntil',addDays(date,Number(value)));}}/></Form.Item>
          <Form.Item name={['exactHire','minimumHirePeriod']} label="Quoted hire period" rules={[{required:true}]}><Input placeholder="6 Months (180 days)"/></Form.Item>
          <Form.Item name={['exactHire','minimumHireDays']} label="Minimum hire days" rules={[{required:true}]}><InputNumber min={1} style={{width:'100%'}}/></Form.Item>
          <Form.Item name={['exactHire','siteLengthRmt']} label="Site length (RMT)" rules={[{required:true}]}><InputNumber min={0} style={{width:'100%'}}/></Form.Item>
          <Form.Item name={['exactHire','siteHeightMtr']} label="Site height (MTR)" rules={[{required:true}]}><InputNumber min={0} style={{width:'100%'}}/></Form.Item>
        </div>
      </section>}
      <section className="quotation-form-section">
        <h2>Rental terms</h2>
        <div className="master-form-grid"><Form.Item name="rentalType" label="Rental type" rules={[{required:true}]}><Select options={rental}/></Form.Item></div>
      </section>
      {!isExact&&<section className="quotation-form-section">
        <h2>Discount and taxes</h2>
        <div className="master-form-grid">
          <Form.Item name="discountType" label="Discount"><Select options={['NONE','PERCENTAGE','FIXED'].map(value=>({value,label:value}))}/></Form.Item>
          {discountType!=='NONE'&&<Money name="discountValue" label="Discount value"/>}
          <Money name="cgstRate" label="CGST %"/><Money name="sgstRate" label="SGST %"/><Money name="igstRate" label="IGST %"/>
        </div>
      </section>}
      {isExact&&<section className="quotation-form-section"><h2>Commercial terms</h2><div className="master-form-grid"><Form.Item name={['exactHire','gstPercentage']} label="GST %" rules={[{required:true}]}><InputNumber min={0} max={100} style={{width:'100%'}}/></Form.Item><Form.Item label="GST allocation"><Input readOnly value={`${automaticGst.label}: CGST ${automaticGst.cgst}% / SGST ${automaticGst.sgst}% / IGST ${automaticGst.igst}%`}/></Form.Item><Money name="securityDeposit" label="Security deposit"/><Form.Item name={['exactHire','advanceRent']} label={`One month advance rent (${money(oneMonthRent)})`}><InputNumber min={0} placeholder="Auto calculated if blank" style={{width:'100%'}}/></Form.Item><Form.Item name={['exactHire','paymentDueDays']} label="Payment due (days)" rules={[{required:true}]}><InputNumber min={0} style={{width:'100%'}}/></Form.Item></div></section>}
      <section className="quotation-form-section">{isExact?<ExactHireItemRows items={items.data??[]} availability={availability}/>:<QuotationItemRows items={items.data??[]} availability={availability}/>}</section>
      {!isExact&&<section className="quotation-form-section">
        <h2>Additional charges</h2>
        <div className="master-form-grid"><Money name="transportCharge" label="Transport"/><Money name="loadingCharge" label="Loading"/><Money name="unloadingCharge" label="Unloading"/><Money name="otherCharge" label="Other charge"/><Money name="roundOff" label="Round off" signed/><Money name="securityDeposit" label="Security deposit"/></div>
      </section>}
      {isExact&&<section className="quotation-form-section"><h2>Authorization and acceptance</h2><div className="master-form-grid">
        <Form.Item name={['exactHire','authorizedPerson']} label="SteelFab authorized person" rules={[{required:true}]}><Input/></Form.Item>
        <Form.Item name={['exactHire','authorizedDesignation']} label="Designation" rules={[{required:true}]}><Input/></Form.Item>
        <Form.Item name={['exactHire','authorizedPhone']} label="Phone" rules={[{required:true}]}><Input/></Form.Item>
        <Form.Item name={['exactHire','acceptedBy']} label="Accepted by"><Input/></Form.Item>
        <Form.Item name={['exactHire','acceptedDesignation']} label="Customer designation"><Input/></Form.Item>
        <Form.Item name={['exactHire','acceptedPhone']} label="Customer phone"><Input/></Form.Item>
        <Form.Item name={['exactHire','acceptedDate']} label="Acceptance date"><Input type="date"/></Form.Item>
      </div></section>}
      <section className="quotation-form-section">
        <h2>Terms and notes</h2>
        <div className="master-form-grid"><Form.Item className="master-form-wide" name="headerText" label={<Space>Header Intro <Button size="small" type="link" onClick={applyStandardTerms}>Use Standard Text</Button></Space>} extra="Introductory paragraph."><Input.TextArea rows={3}/></Form.Item><Form.Item name="partATitle" label="Part A Title" extra="Leave blank to use default."><Input/></Form.Item><Form.Item name="partBTitle" label="Part B Title" extra="Leave blank to use default."><Input/></Form.Item><Form.Item className="master-form-wide" name="terms" label="Terms" extra="Use the standard terms, edit them manually, or leave the field blank to generate the document without terms."><Input.TextArea rows={3}/></Form.Item><Form.Item className="master-form-wide" name="notes" label="Notes"><Input.TextArea rows={3}/></Form.Item></div>
      </section>
      <section className="quotation-form-section quotation-summary"><h2>Quotation summary</h2><Alert type="info" showIcon message={`Estimated grand total: ${money(estimate)}`} description="The backend recalculates and stores the authoritative total."/></section>
      <footer className="quotation-action-footer"><Space>{editing&&isExact&&<Button icon={<EyeOutlined/>} onClick={()=>void preview(editing)}>Preview exact PDF</Button>}<Button onClick={closeEditor}>Cancel</Button><Button type="primary" loading={save.isPending} onClick={()=>form.submit()}>{editing?'Save changes':'Create quotation'}</Button></Space></footer>
    </Form>
  </div>;
  return <div className="page-stack"><div className="page-header-container"><div><h1 className="page-heading">Quotations</h1><p className="page-description">Prepare, issue and approve commercial offers without reserving stock.</p></div><Space wrap><ReportExcelButton reportType="QUOTATION_REGISTER" filters={{status,documentNumber:search||undefined}}/>{canWrite&&<Button type="primary" icon={<PlusOutlined/>} onClick={()=>edit()}>Add quotation</Button>}</Space></div>
    <Card className="premium-card"><Space className="filters-bar" wrap><Input prefix={<SearchOutlined/>} allowClear placeholder="Search quotations" value={search} onChange={e=>setSearch(e.target.value)}/><Select allowClear placeholder="All statuses" style={{width:170}} onChange={setStatus} options={['DRAFT','SENT','APPROVED','REJECTED','EXPIRED','CANCELLED'].map(v=>({value:v,label:v}))}/></Space>
      <Table rowKey="id" loading={quotations.isLoading} dataSource={quotations.data?.content} scroll={{x:1100}} columns={[
        {title:'Quotation',dataIndex:'quotationNumber'},{title:'Party',dataIndex:'partyName'},{title:'Site',dataIndex:'siteName'},{title:'Date',dataIndex:'quotationDate'},{title:'Total',dataIndex:'grandTotal',render:money},{title:'Status',dataIndex:'status',render:(v:string)=><Tag color={v==='APPROVED'?'success':v==='REJECTED'||v==='CANCELLED'?'error':'processing'}>{v}</Tag>},
        {title:'Actions',render:(_:unknown,q:Quotation)=><QuotationActionButtons q={q} isAdmin={isAdmin} canWrite={canWrite} onView={()=>setSelected(q)} onEdit={()=>edit(q)} onAction={(name,reason)=>confirm(q,name,reason)} onPdf={()=>void download(q)} onPreview={()=>void preview(q)} onFinalize={()=>void finalizePdf(q)}/>},
      ]}/></Card>
    <Modal open={!!selected} title={selected?.quotationNumber} footer={null} onCancel={()=>setSelected(null)} width={850}>{selected&&<><Descriptions bordered column={2} items={[{key:'party',label:'Party',children:selected.partyName},{key:'site',label:'Site',children:selected.siteName},{key:'template',label:'Template',children:selected.quotationTemplateName},{key:'total',label:'Grand total',children:money(selected.grandTotal)},{key:'tax',label:'Total GST',children:money(selected.totalTax)},{key:'deposit',label:'Security deposit',children:money(selected.securityDeposit)}]}/><Table style={{marginTop:16}} pagination={false} rowKey={(i)=>i.id??i.itemId} dataSource={selected.items} columns={[{title:'Code',dataIndex:'itemCodeSnapshot'},{title:'Item',dataIndex:'itemNameSnapshot'},{title:'Qty',dataIndex:'quantity'},{title:'Rate',dataIndex:'rate',render:money},{title:'Amount',dataIndex:'amount',render:money}]}/></>}</Modal>
  </div>;
}
export function QuotationActionButtons({q,isAdmin,canWrite,onView,onEdit,onAction,onPdf,onPreview=()=>undefined,onFinalize=()=>undefined}:{q:Quotation;isAdmin:boolean;canWrite:boolean;onView:()=>void;onEdit:()=>void;onAction:(name:string,needsReason?:boolean)=>void;onPdf:()=>void;onPreview?:()=>void;onFinalize?:()=>void}){
  const exact=q.quotationTemplateCode===exactTemplateCode;
  return <Space wrap><Button onClick={onView}>View</Button>{canWrite&&q.status==='DRAFT'&&<Button onClick={onEdit}>Edit</Button>}{canWrite&&q.status==='DRAFT'&&<Button onClick={()=>onAction('send')}>Send</Button>}{isAdmin&&q.status==='SENT'&&<Button onClick={()=>onAction('approve')}>Approve</Button>}{isAdmin&&q.status==='SENT'&&<Button danger onClick={()=>onAction('reject',true)}>Reject</Button>}{isAdmin&&['DRAFT','SENT'].includes(q.status)&&<Button danger onClick={()=>onAction('cancel',true)}>Cancel</Button>}{canWrite&&<Button onClick={()=>onAction('clone')}>Clone</Button>}{exact&&<Button icon={<EyeOutlined/>} onClick={onPreview}>Preview</Button>}{exact&&isAdmin&&['APPROVED','CONVERTED'].includes(q.status)&&!q.exactPdfAttachmentId&&<Button icon={<FileDoneOutlined/>} onClick={onFinalize}>Finalize</Button>}<Button icon={<DownloadOutlined/>} onClick={onPdf}>PDF</Button><ReportExcelButton reportType="QUOTATION_REGISTER" filters={{documentNumber:q.quotationNumber}}/></Space>;
}
function ExactStockQuantityFields({name,availability}:{name:number;availability:Map<number,number>}){
  const line=Form.useWatch(['items',name])??{};
  const available=availability.get(line.itemId)??0;
  const demandShortage=Math.max(0,(Number(line.requiredQuantity)||0)-available);
  const offeredShortage=Math.max(0,(Number(line.quantity)||0)-available);
  return <>
    <Form.Item name={[name,'requiredQuantity']} label="Required qty" extra={line.itemId?`Available: ${available.toLocaleString('en-IN')}`:undefined} validateStatus={demandShortage>0?'error':undefined} help={demandShortage>0?`Demand shortage: ${demandShortage.toLocaleString('en-IN')}`:undefined} rules={[{required:true,type:'number',min:0}]}><InputNumber min={0} status={demandShortage>0?'error':undefined}/></Form.Item>
    <Form.Item name={[name,'quantity']} label="Offered qty" validateStatus={offeredShortage>0?'error':undefined} help={offeredShortage>0?`Only ${available.toLocaleString('en-IN')} available`:undefined} rules={[{required:true,type:'number',min:0.0001}]}><InputNumber min={0.0001} status={offeredShortage>0?'error':undefined}/></Form.Item>
  </>;
}
export function QuotationItemRows({items,availability=new Map()}:{items:Option[];availability?:Map<number,number>}){
  void availability;
  return <Form.List name="items">{(fields,{add,remove})=><div className="commercial-lines"><Space><strong>Items</strong><Button onClick={()=>add({quantity:1,rentalType:'PER_PIECE_PER_DAY'})}>Add line</Button></Space>{fields.map(({key,name})=><Space key={key} align="start" wrap><Form.Item name={[name,'itemId']} label="Item" rules={[{required:true}]}><Select style={{width:260}} showSearch optionFilterProp="label" options={items.map(i=>({value:i.id,label:`${i.itemCode} — ${i.itemName}`}))}/></Form.Item><Form.Item name={[name,'quantity']} label="Quantity" rules={[{required:true,type:'number',min:0.0001}]}><InputNumber min={0.0001}/></Form.Item><Form.Item name={[name,'rate']} label="Rate" rules={[{required:true,type:'number',min:0.0001,message:'Rate must be greater than 0'}]}><InputNumber min={0.0001}/></Form.Item><Form.Item name={[name,'rentalType']} label="Rental type"><Select style={{width:190}} options={rental}/></Form.Item><Form.Item name={[name,'area']} label="Area"><InputNumber min={0}/></Form.Item><Form.Item name={[name,'weight']} label="Weight"><InputNumber min={0}/></Form.Item><Button danger type="text" onClick={()=>remove(name)} disabled={fields.length===1}>Remove</Button></Space>)}</div>}</Form.List>;
}
export function ExactHireItemRows({items,availability=new Map()}:{items:Option[];availability?:Map<number,number>}){
  const selected=Form.useWatch('items')??[];
  return <Form.List name="items">{(fields,{add,remove})=><div className="commercial-lines"><Space wrap><strong>SteelFab material schedule</strong><Button icon={<PlusOutlined/>} onClick={()=>add({quantity:0,requiredQuantity:0,rate:0,hireMonths:6,replacementRate:0,rentalType:'PER_PIECE_PER_DAY'})}>Add item</Button></Space>{!fields.length&&<Alert style={{marginTop:16}} type="info" showIcon message="No material added" description="Add items from the inventory master and enter the agreed quantities and rates."/>}{fields.map(({key,name},index)=><div key={key} style={{marginTop:16}}><Space align="start" wrap>
    <div style={{width:48,paddingTop:30,fontWeight:600}}>{index+1}.</div>
    <Form.Item name={[name,'itemId']} label="Stock item" rules={[{required:true,message:'Select a stock item'}]}><Select style={{width:280}} showSearch optionFilterProp="label" options={items.filter(item=>!selected.some((line:{itemId?:number},lineIndex:number)=>line.itemId===item.id&&lineIndex!==name)).map(item=>({value:item.id,label:`${item.itemCode} - ${item.itemName}`}))}/></Form.Item>
    <ExactStockQuantityFields name={name} availability={availability}/>
    <Form.Item name={[name,'rate']} label="Monthly rate" rules={[{required:true,type:'number',min:0.0001}]}><InputNumber min={0.0001}/></Form.Item>
    <Form.Item name={[name,'hireMonths']} label="Months" rules={[{required:true,type:'number',min:0.01}]}><InputNumber min={0.01}/></Form.Item>
    <Form.Item name={[name,'replacementRate']} label="Replacement rate" rules={[{required:true,type:'number',min:0}]}><InputNumber min={0}/></Form.Item>
    <Form.Item name={[name,'area']} label="Total area (SFT)"><InputNumber min={0}/></Form.Item>
    <Form.Item name={[name,'rentalType']} hidden><Input/></Form.Item>
    <Button danger type="text" onClick={()=>remove(name)}>Remove</Button>
  </Space></div>)}</div>}</Form.List>;
}
function Money({name,label,signed=false}:{name:keyof Editor;label:string;signed?:boolean}){return <Form.Item name={name} label={label} rules={[{required:true}]}><InputNumber min={signed?undefined:0} style={{width:'100%'}}/></Form.Item>;}
