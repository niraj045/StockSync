import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../auth/AuthContext';
import { LoadingBlock } from '../components/ui';
import { ChallansScreen } from '../screens/ChallansScreen';
import { DashboardScreen } from '../screens/DashboardScreen';
import { IssuedChallanDetailScreen, ReceivingChallanDetailScreen } from '../screens/ChallanDetailScreen';
import { CreateIssuedChallanScreen } from '../screens/CreateIssuedChallanScreen';
import { CreateReceivingChallanScreen } from '../screens/CreateReceivingChallanScreen';
import { LoginScreen } from '../screens/LoginScreen';
import { MoreScreen } from '../screens/MoreScreen';
import { SalesScreen } from '../screens/SalesScreen';
import { CreatePartyScreen } from '../screens/CreatePartyScreen';
import { CreateSiteScreen } from '../screens/CreateSiteScreen';
import { CreateQuotationScreen } from '../screens/CreateQuotationScreen';
import { AgreementFlowScreen } from '../screens/AgreementFlowScreen';
import { OrdersScreen } from '../screens/OrdersScreen';
import { CreateOrderScreen } from '../screens/CreateOrderScreen';
import { StockScreen } from '../screens/StockScreen';
import { GstExportScreen } from '../screens/GstExportScreen';
import { ExcelReportsScreen } from '../screens/ExcelReportsScreen';
import { OpeningStockImportScreen } from '../screens/OpeningStockImportScreen';
import { SiteLedgerImportScreen } from '../screens/SiteLedgerImportScreen';
import { ClientExcelImportScreen } from '../screens/ClientExcelImportScreen';
import { BillingScreen } from '../screens/BillingScreen';
import { BillingRunDetailScreen } from '../screens/BillingRunDetailScreen';
import { InquiriesScreen } from '../screens/InquiriesScreen';
import { CreateInquiryScreen } from '../screens/CreateInquiryScreen';
import { SiteOperationsScreen } from '../screens/SiteOperationsScreen';
import { CreateSiteOperationScreen } from '../screens/CreateSiteOperationScreen';
import { colors, fonts } from '../theme';
import type { MainTabParams, RootStackParams } from './types';

const Stack = createNativeStackNavigator<RootStackParams>();
const Tab = createBottomTabNavigator<MainTabParams>();

const navigationTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    primary: colors.primary,
    background: colors.canvas,
    card: colors.surface,
    text: colors.ink,
    border: colors.line,
  },
};

const tabIcons: Record<keyof MainTabParams, keyof typeof Ionicons.glyphMap> = {
  Dashboard: 'speedometer-outline',
  Sales: 'briefcase-outline',
  Orders: 'clipboard-outline',
  Challans: 'document-text-outline',
  More: 'menu-outline',
};

function MainTabs() {
  const insets = useSafeAreaInsets();
  const bottomInset = Math.max(0, insets.bottom);

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: '#FFFFFF',
        tabBarInactiveTintColor: colors.muted,
        tabBarActiveBackgroundColor: colors.primary,
        tabBarHideOnKeyboard: true,
        tabBarLabelStyle: { fontSize: 10, fontFamily: fonts.bold, paddingBottom: 4 },
        tabBarItemStyle: { height: 51, marginHorizontal: 3, marginVertical: 8, borderRadius: 8 },
        tabBarStyle: {
          height: 72 + bottomInset,
          paddingBottom: bottomInset,
          paddingHorizontal: 7,
          borderTopColor: colors.line,
          backgroundColor: colors.surface,
        },
        tabBarIcon: ({ color, size }) => <Ionicons name={tabIcons[route.name]} color={color} size={size} />,
      })}
    >
      <Tab.Screen name="Dashboard" component={DashboardScreen} />
      <Tab.Screen name="Sales" component={SalesScreen} />
      <Tab.Screen name="Orders" component={OrdersScreen} />
      <Tab.Screen name="Challans" component={ChallansScreen} />
      <Tab.Screen name="More" component={MoreScreen} />
    </Tab.Navigator>
  );
}

export function AppNavigator() {
  const { user, loading } = useAuth();
  if (loading) return <LoadingBlock />;

  return (
    <NavigationContainer theme={navigationTheme}>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: colors.surface },
          headerTintColor: colors.ink,
          headerTitleStyle: { fontFamily: fonts.bold },
          headerShadowVisible: false,
          contentStyle: { backgroundColor: colors.canvas },
        }}
      >
        {user ? (
          <>
            <Stack.Screen name="Main" component={MainTabs} options={{ headerShown: false }} />
            <Stack.Screen name="Stock" component={StockScreen} options={{ title: 'Current stock' }} />
            <Stack.Screen name="GstExport" component={GstExportScreen} options={{ title: 'Monthly GST export' }} />
            <Stack.Screen name="ExcelReports" component={ExcelReportsScreen} options={{ title: 'Excel reports' }} />
            <Stack.Screen name="OpeningStockImport" component={OpeningStockImportScreen} options={{ title: 'Import Stock' }} />
            <Stack.Screen name="SiteLedgerImport" component={SiteLedgerImportScreen} options={{ title: 'Import Site Ledger' }} />
            <Stack.Screen name="ClientExcelImport" component={ClientExcelImportScreen} options={{ title: 'Historical D&R Import' }} />
            <Stack.Screen name="Billing" component={BillingScreen} options={{ title: 'Monthly rental billing' }} />
            <Stack.Screen name="BillingRunDetail" component={BillingRunDetailScreen} options={{ title: 'Billing details' }} />
            <Stack.Screen name="CreateParty" component={CreatePartyScreen} options={{ title: 'New customer' }} />
            <Stack.Screen name="CreateSite" component={CreateSiteScreen} options={{ title: 'New site' }} />
            <Stack.Screen name="CreateQuotation" component={CreateQuotationScreen} options={{ title: 'New quotation' }} />
            <Stack.Screen name="AgreementFlow" component={AgreementFlowScreen} options={{ title: 'Agreement setup' }} />
            <Stack.Screen name="CreateOrder" component={CreateOrderScreen} options={{ title: 'New site order' }} />
            <Stack.Screen name="CreateIssuedChallan" component={CreateIssuedChallanScreen} options={{ title: 'New issued challan' }} />
            <Stack.Screen name="CreateReceivingChallan" component={CreateReceivingChallanScreen} options={{ title: 'Record material return' }} />
            <Stack.Screen name="IssuedChallanDetail" component={IssuedChallanDetailScreen} options={{ title: 'Issued challan' }} />
            <Stack.Screen name="ReceivingChallanDetail" component={ReceivingChallanDetailScreen} options={{ title: 'Receiving challan' }} />
            <Stack.Screen name="Inquiries" component={InquiriesScreen} options={{ title: 'Inquiries' }} />
            <Stack.Screen name="CreateInquiry" component={CreateInquiryScreen} options={{ title: 'Inquiry details' }} />
            <Stack.Screen name="SiteOperations" component={SiteOperationsScreen} options={{ title: 'Site operations' }} />
            <Stack.Screen name="CreateSiteOperation" component={CreateSiteOperationScreen} options={{ title: 'Operation details' }} />
          </>
        ) : (
          <>
            <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
