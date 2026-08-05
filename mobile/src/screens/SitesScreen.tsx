import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps, useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useCallback, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiClient, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppButton, Card, EmptyBlock, PageHeader, StatusPill } from '../components/ui';
import { ExcelExportButton } from '../components/ExcelExportButton';
import type { MainTabParams, RootStackParams } from '../navigation/types';
import { colors, fonts } from '../theme';

type Props = CompositeScreenProps<
  BottomTabScreenProps<MainTabParams, 'Sites'>,
  NativeStackScreenProps<RootStackParams>
>;

interface SiteRow {
  id: number;
  siteName: string;
  siteCode: string;
  partyId: number;
  partyLegalName?: string;
  partyName?: string;
  addressLine1?: string;
  city?: string;
  state?: string;
  pincode?: string;
  status: string;
  defaulter?: boolean;
}

interface Page<T> {
  content: T[];
}

export function SitesScreen({ navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const canWrite = user?.roles.some((role) => role === 'ROLE_ADMIN' || role === 'ROLE_OPERATIONS');

  const [sites, setSites] = useState<SiteRow[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<Page<SiteRow>>('/sites', {
        params: { size: 500, sort: 'id,desc' },
      });
      setSites(response.data.content || []);
    } catch (cause) {
      setError(apiErrorMessage(cause, 'Unable to load sites.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => void load(), [load]));

  const filtered = sites.filter((s) => {
    const q = search.trim().toLowerCase();
    if (!q) return true;
    const name = (s.siteName || '').toLowerCase();
    const party = (s.partyLegalName || s.partyName || '').toLowerCase();
    const code = (s.siteCode || '').toLowerCase();
    return name.includes(q) || party.includes(q) || code.includes(q);
  });

  return (
    <FlatList
      data={filtered}
      keyExtractor={(item) => String(item.id)}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 18 }]}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} colors={[colors.primary]} />}
      ListHeaderComponent={
        <>
          <PageHeader
            eyebrow="Site operations"
            title="Sites"
            action={
              <View style={styles.headerActions}>
                <ExcelExportButton reportType="SITE_PENDING_STOCK" />
                {canWrite ? (
                  <Pressable
                    accessibilityLabel="Create site"
                    onPress={() => navigation.navigate('CreateSite')}
                    style={styles.add}
                  >
                    <Ionicons name="add" color="#fff" size={26} />
                  </Pressable>
                ) : null}
              </View>
            }
          />
          <Text style={styles.intro}>
            View active customer sites, track pending materials, challans and site operations.
          </Text>

          <View style={styles.searchBox}>
            <Ionicons name="search" size={20} color={colors.muted} style={styles.searchIcon} />
            <TextInput
              style={styles.searchInput}
              placeholder="Search site, client or code..."
              placeholderTextColor={colors.muted}
              value={search}
              onChangeText={setSearch}
            />
            {search ? (
              <Pressable onPress={() => setSearch('')}>
                <Ionicons name="close-circle" size={18} color={colors.muted} />
              </Pressable>
            ) : null}
          </View>

          {error ? <Text style={styles.error}>{error}</Text> : null}
        </>
      }
      ListEmptyComponent={!loading ? (
        <Card>
          <EmptyBlock title="No sites found" message="Create a new site or adjust your search filter." />
          {canWrite ? <AppButton title="Create site" onPress={() => navigation.navigate('CreateSite')} /> : null}
        </Card>
      ) : null}
      renderItem={({ item }) => {
        const partyName = item.partyLegalName || item.partyName || 'Customer';
        return (
          <Pressable onPress={() => navigation.navigate('SiteDetail', { siteId: item.id })}>
            <Card style={styles.card}>
              <View style={styles.top}>
                <Text style={styles.code}>{item.siteCode || `SITE-${item.id}`}</Text>
                <View style={styles.topRight}>
                  {item.defaulter ? <Text style={styles.defaulterBadge}>DEFAULTER</Text> : null}
                  <StatusPill value={item.status} />
                  <ExcelExportButton reportType="SITE_PENDING_STOCK" filters={{ siteId: String(item.id) }} />
                </View>
              </View>
              <Text style={styles.name}>{item.siteName}</Text>
              <Text style={styles.party}>{partyName}</Text>
              {item.city || item.state ? (
                <Text style={styles.location}>
                  <Ionicons name="location-outline" size={13} color={colors.muted} /> {[item.addressLine1, item.city, item.state].filter(Boolean).join(', ')}
                </Text>
              ) : null}
              <View style={styles.footer}>
                <Text style={styles.actionText}>Tap for site details & challans</Text>
                <Ionicons name="chevron-forward" size={18} color={colors.primary} />
              </View>
            </Card>
          </Pressable>
        );
      }}
    />
  );
}

const styles = StyleSheet.create({
  content: { padding: 18, paddingBottom: 36, backgroundColor: colors.canvas, flexGrow: 1 },
  headerActions: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  add: { width: 46, height: 46, borderRadius: 8, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  intro: { color: colors.muted, lineHeight: 21, marginTop: -8, marginBottom: 12 },
  searchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    paddingHorizontal: 12,
    height: 46,
    marginBottom: 14,
  },
  searchIcon: { marginRight: 8 },
  searchInput: { flex: 1, color: colors.ink, fontFamily: fonts.semiBold, fontSize: 14 },
  error: { color: colors.red, backgroundColor: colors.redSoft, borderRadius: 8, padding: 12, marginBottom: 12 },
  card: { marginBottom: 10, gap: 8 },
  top: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  topRight: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  code: { color: colors.primary, fontFamily: fonts.bold, fontSize: 12 },
  defaulterBadge: { color: '#fff', backgroundColor: colors.red, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4, fontSize: 10, fontFamily: fonts.bold },
  name: { color: colors.ink, fontSize: 18, fontFamily: fonts.extraBold },
  party: { color: colors.muted, fontSize: 13, fontFamily: fonts.semiBold, marginTop: -3 },
  location: { color: colors.muted, fontSize: 12, marginTop: 2 },
  footer: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 10, marginTop: 4 },
  actionText: { color: colors.primary, fontSize: 12, fontFamily: fonts.bold },
});
