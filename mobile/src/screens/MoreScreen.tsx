import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, PageHeader } from '../components/ui';
import type { MainTabParams, RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';

type Props = CompositeScreenProps<
  BottomTabScreenProps<MainTabParams, 'More'>,
  NativeStackScreenProps<RootStackParams>
>;

export function MoreScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { user, logout } = useAuth();
  const canExportGst = user?.roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_ACCOUNTS') ?? false;
  return (
    <ScrollView contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]} bounces={false}>
      <PageHeader eyebrow="SteelFab" title="Account" />
      <Card>
        <View style={styles.profile}>
          <View style={styles.avatar}>
            <Text style={styles.initial}>{user?.fullName?.charAt(0).toUpperCase() ?? 'U'}</Text>
          </View>
          <View style={styles.profileCopy}>
            <Text style={styles.name}>{user?.fullName}</Text>
            <Text style={styles.username}>@{user?.username}</Text>
          </View>
        </View>
        <View style={styles.roles}>
          {user?.roles.map((role) => <Text key={role} style={styles.role}>{role.replace('ROLE_', '')}</Text>)}
        </View>
      </Card>

      <Pressable style={styles.menuRow} onPress={() => navigation.navigate('Stock')}>
        <View style={styles.menuIcon}><Ionicons name="grid-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Current stock</Text>
          <Text style={styles.menuText}>View godown and site material balances</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable>

      <Pressable style={styles.menuRow} onPress={() => navigation.navigate('Inquiries')}>
        <View style={styles.menuIcon}><Ionicons name="mail-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Inquiries</Text>
          <Text style={styles.menuText}>Track leads, sources and quotation follow-ups</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable>

      <Pressable style={styles.menuRow} onPress={() => navigation.navigate('SiteOperations')}>
        <View style={styles.menuIcon}><Ionicons name="construct-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Site operations</Text>
          <Text style={styles.menuText}>Transport, labour, Mathadi, TPI and site costs</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable>

      <Pressable style={styles.menuRow} onPress={() => navigation.navigate('ExcelReports')}>
        <View style={styles.menuIcon}><Ionicons name="document-text-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Excel reports</Text>
          <Text style={styles.menuText}>Export SteelFab inventory, operations and accounts records</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable>

      <Pressable style={styles.menuRow} onPress={() => navigation.navigate('Billing')}>
        <View style={styles.menuIcon}><Ionicons name="receipt-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Monthly rental billing</Text>
          <Text style={styles.menuText}>Prepare rental periods, review challan usage and create invoices</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable>

      {canExportGst ? <Pressable style={styles.menuRow} onPress={() => navigation.navigate('GstExport')}>
        <View style={styles.menuIcon}><Ionicons name="calendar-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Monthly GST export</Text>
          <Text style={styles.menuText}>Export one consolidated GSTR-1 preparation file</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable> : null}

      <Pressable style={styles.menuRow} onPress={() => navigation.navigate('OpeningStockImport')}>
        <View style={styles.menuIcon}><Ionicons name="cloud-upload-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Import opening stock</Text>
          <Text style={styles.menuText}>Upload Excel files to import stock records</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable>

      <Pressable style={styles.menuRow} onPress={() => navigation.navigate('SiteLedgerImport')}>
        <View style={styles.menuIcon}><Ionicons name="documents-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Import site ledger</Text>
          <Text style={styles.menuText}>Upload a customer D&R statement to auto-generate opening site balances</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable>

      <View style={styles.logout}>
        <AppButton title="Sign out" variant="danger" onPress={() => void logout()} />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, backgroundColor: colors.canvas, padding: 18 },
  profile: { flexDirection: 'row', alignItems: 'center' },
  avatar: { width: 58, height: 58, borderRadius: 29, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  initial: { color: '#fff', fontSize: 25, fontFamily: fonts.black },
  profileCopy: { flex: 1, paddingLeft: 14 },
  name: { color: colors.ink, fontSize: 20, fontFamily: fonts.extraBold },
  username: { color: colors.muted, marginTop: 3 },
  roles: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 16, paddingTop: 14, borderTopWidth: 1, borderTopColor: colors.line },
  role: { color: colors.primaryDark, backgroundColor: colors.primarySoft, paddingHorizontal: 9, paddingVertical: 5, borderRadius: 8, fontSize: 11, fontFamily: fonts.bold },
  menuRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 8, padding: 14, marginTop: 14 },
  menuIcon: { width: 42, height: 42, borderRadius: 8, backgroundColor: colors.primarySoft, alignItems: 'center', justifyContent: 'center' },
  menuCopy: { flex: 1, paddingHorizontal: 12 },
  menuTitle: { color: colors.ink, fontFamily: fonts.bold },
  menuText: { color: colors.muted, fontSize: 11, marginTop: 3 },
  logout: { marginTop: 'auto', paddingTop: 18 },
});
