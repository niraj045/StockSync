import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Pressable, StyleSheet, Text, View } from 'react-native';
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
    <View style={[styles.content, { paddingTop: insets.top + 18 }]}>
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

      {canExportGst ? <Pressable style={styles.menuRow} onPress={() => navigation.navigate('GstExport')}>
        <View style={styles.menuIcon}><Ionicons name="calendar-outline" size={21} color={colors.primary} /></View>
        <View style={styles.menuCopy}>
          <Text style={styles.menuTitle}>Monthly GST export</Text>
          <Text style={styles.menuText}>Export one consolidated GSTR-1 preparation file</Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
      </Pressable> : null}

      <View style={styles.logout}>
        <AppButton title="Sign out" variant="danger" onPress={() => void logout()} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  content: { flex: 1, backgroundColor: colors.canvas, padding: 18 },
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
