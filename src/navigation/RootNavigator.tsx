import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import MainTabs from './MainTabs';
import { useTheme } from '../theme/useTheme';
import type { RootStackParamList } from './types';

const Stack = createNativeStackNavigator<RootStackParamList>();

/** Native headers are off everywhere (custom headers + JS-driven RTL, same as the sibling apps).
 * One route today (the two tabs live in MainTabs' pager); the stack is where a future full-screen
 * page would go. */
export function RootNavigator() {
  const { colors } = useTheme();
  return (
    <Stack.Navigator screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.background }, animation: 'fade' }}>
      <Stack.Screen name="Main" component={MainTabs} />
    </Stack.Navigator>
  );
}
