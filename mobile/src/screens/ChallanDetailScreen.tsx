import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { encode } from 'base64-arraybuffer';
import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, StatusPill } from '../components/ui';
import type { RootStackParams } from '../navigation/types';
import { colors } from '../theme';
import { dateLabel, quantity } from '../utils/format';
import { shareServerPdf } from '../utils/sharePdf';

type IssuedProps = NativeStackScreenProps<RootStackParams, 'IssuedChallanDetail'>;
type ReceivedProps = NativeStackScreenProps<RootStackParams, 'ReceivingChallanDetail'>;

export function IssuedChallanDetailScreen({ route, navigation }: IssuedProps) {
  const { challan } = route.params;
  const [sharing, setSharing] = useState(false);

  const sharePdf = async () => {
    setSharing(true);
    try {
      const response = await apiClient.get<ArrayBuffer>(`/challans/issued/${challan.id}/pdf`, {
        responseType: 'arraybuffer',
      });
      const name = `challan-${challan.challanNumber.replaceAll('/', '-')}.pdf`;
      const path = `${FileSystem.cacheDirectory}${name}`;
      await FileSystem.writeAsStringAsync(path, encode(response.data), {
        encoding: FileSystem.EncodingType.Base64,
      });
      if (!(await Sharing.isAvailableAsync())) throw new Error('Sharing is not available on this device');
      await Sharing.shareAsync(path, { mimeType: 'application/pdf', dialogTitle: 'Share issued challan' });
    } catch (cause) {
      Alert.alert('PDF unavailable', apiErrorMessage(cause, 'Unable to download and share this challan.'));
    } finally {
      setSharing(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Card>
        <Text style={styles.number}>{challan.challanNumber}</Text>
        <Text style={styles.party}>{challan.partyName}</Text>
        <Text style={styles.site}>{challan.siteName}</Text>
        <View style={styles.details}>
          <Detail label="Dispatch date" value={dateLabel(challan.dispatchDate)} />
          <Detail label="Order" value={challan.siteOrderNumber} />
          <Detail label="Vehicle" value={challan.vehicleNumber ?? 'Not recorded'} />
          <Detail label="Driver" value={challan.driverName ?? 'Not recorded'} />
        </View>
      </Card>
      <Text style={styles.heading}>Dispatched material</Text>
      {challan.items.map((item) => (
        <Card key={item.id} style={styles.item}>
          <View style={styles.itemCopy}>
            <Text style={styles.code}>{item.itemCode}</Text>
            <Text style={styles.itemName}>{item.itemName}</Text>
          </View>
          <View>
            <Text style={styles.qty}>{quantity(item.quantity)}</Text>
            <Text style={styles.unit}>{item.unit}</Text>
          </View>
        </Card>
      ))}
      {challan.notes ? <Card style={styles.notes}><Text style={styles.notesLabel}>Notes</Text><Text style={styles.notesText}>{challan.notes}</Text></Card> : null}
      <AppButton
        title="Share challan PDF"
        onPress={sharePdf}
        loading={sharing}
        icon={<Ionicons name="share-social-outline" size={20} color="#fff" />}
      />
      <AppButton
        title="Record material return"
        variant="secondary"
        onPress={() => navigation.navigate('CreateReceivingChallan', { issuedChallan: challan })}
        icon={<Ionicons name="return-down-back-outline" size={20} color={colors.primary} />}
      />
    </ScrollView>
  );
}

export function ReceivingChallanDetailScreen({ route }: ReceivedProps) {
  const { user } = useAuth();
  const [challan, setChallan] = useState(route.params.challan);
  const [working, setWorking] = useState('');
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  const canWrite = user?.roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_OPERATIONS') ?? false;

  const action = async (name: 'approve-extra' | 'post') => {
    setWorking(name);
    try {
      const response = await apiClient.post<typeof challan>(`/challans/receiving/${challan.id}/${name}`);
      setChallan(response.data);
    } catch (cause) {
      Alert.alert('Action failed', apiErrorMessage(cause, 'Unable to update this receiving challan.'));
    } finally {
      setWorking('');
    }
  };

  const sharePdf = async () => {
    setWorking('share');
    try {
      await shareServerPdf(
        `/challans/receiving/${challan.id}/pdf`,
        `receiving-challan-${challan.receivingChallanNumber}.pdf`,
        'Share receiving challan',
      );
    } catch (cause) {
      Alert.alert('PDF unavailable', apiErrorMessage(cause, 'Unable to download and share this receiving challan.'));
    } finally {
      setWorking('');
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Card>
        <View style={styles.titleRow}>
          <Text style={styles.number}>{challan.receivingChallanNumber}</Text>
          <StatusPill value={challan.status} />
        </View>
        <Text style={styles.party}>{challan.partyName}</Text>
        <Text style={styles.site}>{challan.siteName}</Text>
        <View style={styles.details}>
          <Detail label="Receive date" value={dateLabel(challan.receiveDate)} />
          <Detail label="Vehicle" value={challan.vehicleNumber ?? 'Not recorded'} />
          <Detail label="Source" value={challan.linkedIssuedChallanNumber ?? challan.sourceType.replaceAll('_', ' ')} />
          <Detail label="Agreement" value={challan.agreementNumber ?? 'Not linked'} />
        </View>
      </Card>
      <Text style={styles.heading}>Returned material</Text>
      {challan.items.map((item) => (
        <Card key={item.id} style={styles.receivingItem}>
          <Text style={styles.code}>{item.itemCode}</Text>
          <Text style={styles.itemName}>{item.itemName}</Text>
          <View style={styles.returnGrid}>
            <Detail label="Good" value={quantity(item.goodReturnedQuantity)} />
            <Detail label="Damaged" value={quantity(item.damagedReturnedQuantity)} />
            <Detail label="Lost" value={quantity(item.lostQuantity)} />
            <Detail label="Extra" value={quantity(item.extraReturnedQuantity)} />
          </View>
        </Card>
      ))}
      {isAdmin && challan.status === 'EXTRA_APPROVAL_REQUIRED' ? (
        <AppButton title="Approve extra return" onPress={() => void action('approve-extra')} loading={working === 'approve-extra'} />
      ) : null}
      {canWrite && (challan.status === 'DRAFT' || challan.status === 'APPROVED_FOR_POSTING') ? (
        <AppButton title="Post return and reconcile stock" onPress={() => void action('post')} loading={working === 'post'} />
      ) : null}
      <AppButton
        title="Share receiving PDF"
        variant="secondary"
        onPress={() => void sharePdf()}
        loading={working === 'share'}
        icon={<Ionicons name="share-social-outline" size={20} color={colors.primary} />}
      />
      {challan.status === 'POSTED' ? (
        <Card style={styles.posted}>
          <Ionicons name="checkmark-circle" size={23} color={colors.green} />
          <View style={styles.postedCopy}>
            <Text style={styles.postedTitle}>Stock reconciled</Text>
            <Text style={styles.postedText}>The site balance and rental timeline now include this return.</Text>
          </View>
        </Card>
      ) : null}
    </ScrollView>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return <View style={styles.detail}><Text style={styles.detailLabel}>{label}</Text><Text style={styles.detailValue}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  content: { padding: 18, paddingBottom: 34, backgroundColor: colors.canvas, gap: 10 },
  titleRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 10 },
  number: { color: colors.primary, fontSize: 18, fontWeight: '900', flexShrink: 1 },
  party: { color: colors.ink, fontSize: 22, fontWeight: '900', marginTop: 13 },
  site: { color: colors.muted, marginTop: 4 },
  details: { flexDirection: 'row', flexWrap: 'wrap', borderTopWidth: 1, borderTopColor: '#E9EFEE', marginTop: 16, paddingTop: 14, rowGap: 14 },
  detail: { width: '50%', paddingRight: 8 },
  detailLabel: { color: colors.muted, fontSize: 11, marginBottom: 3 },
  detailValue: { color: colors.ink, fontSize: 14, fontWeight: '800' },
  heading: { color: colors.ink, fontSize: 19, fontWeight: '900', marginTop: 10 },
  item: { flexDirection: 'row', alignItems: 'center' },
  itemCopy: { flex: 1, paddingRight: 12 },
  code: { color: colors.primary, fontSize: 11, fontWeight: '900' },
  itemName: { color: colors.ink, fontSize: 16, fontWeight: '800', marginTop: 3 },
  qty: { color: colors.ink, fontSize: 20, fontWeight: '900', textAlign: 'right' },
  unit: { color: colors.muted, fontSize: 11, textAlign: 'right' },
  notes: { marginVertical: 4 },
  notesLabel: { color: colors.muted, fontSize: 11, fontWeight: '800', marginBottom: 5 },
  notesText: { color: colors.ink, lineHeight: 21 },
  receivingItem: {},
  returnGrid: { flexDirection: 'row', marginTop: 14 },
  posted: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.greenSoft, borderColor: '#CFEAD7' },
  postedCopy: { flex: 1, marginLeft: 10 },
  postedTitle: { color: colors.green, fontWeight: '800' },
  postedText: { color: colors.muted, fontSize: 11, lineHeight: 17, marginTop: 2 },
});
