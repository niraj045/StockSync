import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { Text, TextInput } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider } from './src/auth/AuthContext';
import { AppNavigator } from './src/navigation/AppNavigator';

const NativeText = Text as typeof Text & { defaultProps?: { style?: object } };
const NativeTextInput = TextInput as typeof TextInput & { defaultProps?: { style?: object } };

export default function App() {
  const [fontsLoaded] = useFonts({
    Inter: require('@expo-google-fonts/inter/400Regular/Inter_400Regular.ttf'),
    InterSemiBold: require('@expo-google-fonts/inter/600SemiBold/Inter_600SemiBold.ttf'),
    InterBold: require('@expo-google-fonts/inter/700Bold/Inter_700Bold.ttf'),
    InterExtraBold: require('@expo-google-fonts/inter/800ExtraBold/Inter_800ExtraBold.ttf'),
    InterBlack: require('@expo-google-fonts/inter/900Black/Inter_900Black.ttf'),
  });

  NativeText.defaultProps = { ...NativeText.defaultProps, style: [{ fontFamily: 'Inter' }, NativeText.defaultProps?.style] };
  NativeTextInput.defaultProps = { ...NativeTextInput.defaultProps, style: [{ fontFamily: 'Inter' }, NativeTextInput.defaultProps?.style] };

  if (!fontsLoaded) return null;

  return (
    <SafeAreaProvider>
      <AuthProvider>
        <StatusBar style="dark" />
        <AppNavigator />
      </AuthProvider>
    </SafeAreaProvider>
  );
}
