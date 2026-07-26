import {useEffect,type ReactNode} from 'react';
import {Button,Drawer,Space} from 'antd';

type FormDrawerProps={
  open:boolean;
  title:ReactNode;
  subtitle?:ReactNode;
  children:ReactNode;
  onClose:()=>void;
  onSubmit?:()=>void;
  loading?:boolean;
  okText?:string;
  width?:number|string;
  danger?:boolean;
  footer?:ReactNode|null;
  mode?:'drawer'|'page';
};

export function FormDrawer({open,title,subtitle,children,onClose,onSubmit,loading=false,okText='Save',width=720,danger=false,footer,mode='drawer'}:FormDrawerProps){
  useEffect(()=>{
    if(mode!=='page'||!open)return;
    const previous=document.body.style.overflow;
    document.body.style.overflow='hidden';
    return()=>{document.body.style.overflow=previous;};
  },[mode,open]);
  const actions=footer===null?null:footer??(
    <Space>
      <Button onClick={onClose}>Cancel</Button>
      {onSubmit&&<Button type="primary" danger={danger} loading={loading} onClick={onSubmit}>{okText}</Button>}
    </Space>
  );
  const heading=<div><div>{title}</div>{subtitle&&<div className="form-drawer-subtitle">{subtitle}</div>}</div>;
  if(mode==='page'){
    if(!open)return null;
    return <section className="workflow-editor-page">
      <header className="workflow-editor-header"><button type="button" className="workflow-back" onClick={onClose}>← Back</button>{heading}</header>
      <div className="workflow-editor-body">{children}</div>
      {actions&&<footer className="workflow-editor-footer">{actions}</footer>}
    </section>;
  }
  return <Drawer className="form-drawer" title={heading} placement="right" width={width} open={open}
    onClose={onClose} destroyOnHidden footer={actions}>{children}</Drawer>;
}
