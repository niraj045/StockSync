import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, DateField, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';

type EligibleAgreement={id:number;agreementNumber:string;partyName:string;siteName:string;effectiveDate:string;expiryDate?:string};
type Run={id:number;billingRunNumber:string;agreementNumber:string;partyName:string;siteName:string;periodStart:string;periodEnd:string;status:'DRAFT'|'CALCULATED'|'FINALIZED'|'CANCELLED';grandTotal:number;};
type Page<T>={content:T[]};
const money=(value:number)=>new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:0}).format(value||0);

export function BillingScreen({ navigation }: { navigation: NativeStackNavigationProp<RootStackParams, 'Billing'> }) {
  const insets=useSafeAreaInsets();
  const {user}=useAuth();
  const canPrepare=user?.roles.some(role=>['ROLE_ADMIN','ROLE_ACCOUNTS','ROLE_OPERATIONS'].includes(role))??false;
  
  const [agreements,setAgreements]=useState<EligibleAgreement[]>([]);
  const [runs,setRuns]=useState<Run[]>([]);
  const [agreement,setAgreement]=useState<EligibleAgreement>();
  const [start,setStart]=useState('');
  const [end,setEnd]=useState('');
  const [loading,setLoading]=useState(false);
  const [error,setError]=useState('');

  const load=useCallback(async()=>{
    setLoading(true);
    setError('');
    try{
      const [a,r]=await Promise.all([
        apiClient.get<EligibleAgreement[]>('/billing-runs/eligible-agreements'),
        apiClient.get<Page<Run>>('/billing-runs',{params:{size:50,sort:'id,desc'}})
      ]);
      setAgreements(a.data);
      setRuns(r.data.content);
    } catch(cause) {
      setError(apiErrorMessage(cause,'Unable to load monthly billing.'));
    } finally {
      setLoading(false);
    }
  },[]);

  useEffect(()=>{
    const unsubscribe = navigation.addListener('focus', () => {
      void load();
    });
    return unsubscribe;
  },[navigation, load]);

  const choose=async(value:EligibleAgreement)=>{
    setAgreement(value);
    try{
      const response=await apiClient.get<{periodStart:string;periodEnd:string}>(`/billing-runs/agreement/${value.id}/suggested-period`);
      setStart(response.data.periodStart);
      setEnd(response.data.periodEnd);
    }catch(cause){
      setError(apiErrorMessage(cause,'Unable to suggest the next billing period.'));
    }
  };

  const create=async()=>{
    if(!agreement||!start||!end)return;
    setLoading(true);
    try{
      const run=(await apiClient.post<Run>('/billing-runs',{agreementId:agreement.id,periodStart:start,periodEnd:end})).data;
      setAgreement(undefined);
      await load();
      navigation.navigate('BillingRunDetail', { runId: run.id });
    }catch(cause){
      setError(apiErrorMessage(cause,'Unable to prepare this billing period.'));
    }finally{
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.page} contentContainerStyle={{paddingTop:insets.top+18,paddingBottom:36}} refreshControl={<RefreshControl refreshing={loading} onRefresh={load}/>}>
      <PageHeader eyebrow="Accounts" title="Monthly rental billing"/>
      <Text style={styles.lead}>Rent starts from posted issued challans and stops when material is returned.</Text>
      
      {error?<Text style={styles.error}>{error}</Text>:null}
      
      {canPrepare ? (
        <Card style={styles.section}>
          <Text style={styles.title}>1. Create new billing run</Text>
          {agreements.length ? agreements.map(row=>(
            <Pressable key={row.id} style={[styles.choice,agreement?.id===row.id&&styles.choiceActive]} onPress={()=>void choose(row)}>
              <Text style={styles.choiceTitle}>{row.agreementNumber}</Text>
              <Text style={styles.meta}>{row.partyName} | {row.siteName}</Text>
            </Pressable>
          )) : (
            <EmptyBlock title="No agreement ready" message="Activate an agreement before preparing monthly rent."/>
          )}
          {agreement ? (
            <View style={styles.dates}>
              <DateField label="Period start" value={start} onChange={setStart}/>
              <DateField label="Period end" value={end} onChange={setEnd} maximumDate={new Date()}/>
              <AppButton title="Calculate rental" onPress={()=>void create()} loading={loading} disabled={!start||!end}/>
            </View>
          ) : null}
        </Card>
      ) : null}
      
      <Text style={styles.listTitle}>Billing history</Text>
      {!runs.length ? (
        <EmptyBlock title="No billing runs" message="The first prepared rental period will appear here."/>
      ) : runs.map(run=>(
        <Pressable key={run.id} onPress={() => navigation.navigate('BillingRunDetail', { runId: run.id })}>
          <Card style={styles.run}>
            <View style={styles.row}>
              <View style={styles.flex}>
                <Text style={styles.choiceTitle}>{run.billingRunNumber}</Text>
                <Text style={styles.meta}>{run.partyName} | {run.siteName}</Text>
                <Text style={styles.meta}>{run.periodStart} to {run.periodEnd}</Text>
              </View>
              <StatusPill value={run.status}/>
            </View>
            <Text style={styles.amount}>{money(run.grandTotal)}</Text>
          </Card>
        </Pressable>
      ))}
    </ScrollView>
  );
}

const styles=StyleSheet.create({
  page:{flex:1,backgroundColor:colors.canvas,paddingHorizontal:18},
  lead:{color:colors.muted,lineHeight:20,marginBottom:14},
  error:{color:colors.red,marginBottom:12,fontFamily:fonts.semiBold},
  section:{marginBottom:20},
  title:{color:colors.ink,fontSize:17,fontFamily:fonts.bold,marginBottom:10},
  choice:{borderWidth:1,borderColor:colors.line,borderRadius:8,padding:12,marginTop:8},
  choiceActive:{borderColor:colors.primary,backgroundColor:colors.primarySoft},
  choiceTitle:{color:colors.ink,fontFamily:fonts.bold},
  meta:{color:colors.muted,fontSize:12,lineHeight:18,marginTop:3},
  dates:{gap:14,marginTop:16},
  listTitle:{color:colors.ink,fontSize:20,fontFamily:fonts.extraBold,marginBottom:10},
  run:{marginBottom:10},
  row:{flexDirection:'row',alignItems:'flex-start',justifyContent:'space-between',gap:10},
  flex:{flex:1},
  amount:{color:colors.ink,fontSize:22,fontFamily:fonts.extraBold,marginTop:12}
});
