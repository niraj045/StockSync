import { useState } from 'react';
import { Button,Card,Form,Input,Select,Space,Switch,Table,Tag,message } from 'antd';
import { PlusOutlined,SearchOutlined } from '@ant-design/icons';
import { useMutation,useQuery,useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { FormDrawer } from '../../../components/FormDrawer';
import { useAuth } from '../../auth/context/AuthContext';
import type { Page,Template } from '../types';

export function QuotationTemplatesPage(){
  const {user}=useAuth(),isAdmin=user?.roles.includes('ROLE_ADMIN')??false,qc=useQueryClient();
  const [search,setSearch]=useState(''),[active,setActive]=useState<boolean|undefined>(undefined),[editing,setEditing]=useState<Template|null>(null),[open,setOpen]=useState(false);
  const [form]=Form.useForm<Partial<Template>>();
  const query=useQuery({queryKey:['quotation-templates',search,active],queryFn:async()=>(await apiClient.get<Page<Template>>('/quotation-templates',{params:{search,active,size:100,sort:'name,asc'}})).data});
  const save=useMutation({mutationFn:async(v:Partial<Template>)=>editing?(await apiClient.put(`/quotation-templates/${editing.id}`,{...v,version:editing.version})).data:(await apiClient.post('/quotation-templates',v)).data,
    onSuccess:()=>{message.success('Template saved');setOpen(false);void qc.invalidateQueries({queryKey:['quotation-templates']});},onError:()=>message.error('Unable to save template')});
  const toggle=useMutation({mutationFn:async(t:Template)=>(await apiClient.post(`/quotation-templates/${t.id}/${t.active?'deactivate':'activate'}`)).data,
    onSuccess:()=>void qc.invalidateQueries({queryKey:['quotation-templates']})});
  const edit=(t?:Template)=>{setEditing(t??null);form.resetFields();form.setFieldsValue(t??{});setOpen(true);};
  return <div className="page-stack"><div className="page-header-container"><div><h1 className="page-heading">Quotation templates</h1><p className="page-description">Control company identity, defaults and PDF presentation.</p></div>{isAdmin&&<Button type="primary" icon={<PlusOutlined/>} onClick={()=>edit()}>Add template</Button>}</div>
    <Card className="premium-card"><Space className="filters-bar" wrap><Input prefix={<SearchOutlined/>} allowClear placeholder="Search templates" value={search} onChange={e=>setSearch(e.target.value)}/><Select allowClear placeholder="All statuses" style={{width:160}} onChange={setActive} options={[{value:true,label:'Active'},{value:false,label:'Inactive'}]}/></Space>
      <Table rowKey="id" loading={query.isLoading} dataSource={query.data?.content} pagination={false} columns={[
        {title:'Code',dataIndex:'templateCode'},{title:'Name',dataIndex:'name'},{title:'Company',dataIndex:'companyName'},
        {title:'Status',dataIndex:'active',render:(v:boolean)=><Tag color={v?'success':'default'}>{v?'ACTIVE':'INACTIVE'}</Tag>},
        {title:'Actions',render:(_:unknown,t:Template)=>isAdmin?<Space><Button onClick={()=>edit(t)}>Edit</Button><Switch checked={t.active} onChange={()=>toggle.mutate(t)}/></Space>:null},
      ]}/></Card>
    <FormDrawer open={open} title={editing?'Edit quotation template':'New quotation template'} width={760} onClose={()=>setOpen(false)} onSubmit={()=>form.submit()} loading={save.isPending} okText={editing?'Save changes':'Create template'}>
      <Form form={form} layout="vertical" onFinish={v=>save.mutate(v)}><div className="master-form-grid">
        <Form.Item name="templateCode" label="Template code" rules={[{required:true},{max:50}]}><Input/></Form.Item>
        <Form.Item name="name" label="Name" rules={[{required:true},{max:150}]}><Input/></Form.Item>
        <Form.Item name="companyName" label="Company name"><Input/></Form.Item><Form.Item name="companyGstin" label="Company GSTIN"><Input maxLength={15}/></Form.Item>
        <Form.Item className="master-form-wide" name="companyAddress" label="Company address"><Input.TextArea/></Form.Item>
        <Form.Item className="master-form-wide" name="headerText" label="Header text"><Input.TextArea/></Form.Item>
        <Form.Item className="master-form-wide" name="defaultTerms" label="Default terms"><Input.TextArea rows={4}/></Form.Item>
        <Form.Item className="master-form-wide" name="defaultNotes" label="Default notes"><Input.TextArea/></Form.Item>
        <Form.Item className="master-form-wide" name="footerText" label="Footer text"><Input.TextArea/></Form.Item>
      </div></Form>
    </FormDrawer></div>;
}
