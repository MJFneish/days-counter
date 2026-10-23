/**
 * Days Counter — countdowns to the dates that matter, with a home-screen widget.
 * Scaffold placeholder; the real app shell lands in the next version.
 * @format
 */
import React from 'react';
import { StatusBar, StyleSheet, Text, View } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';

export default function App() {
  return (
    <GestureHandlerRootView style={styles.root}>
      <SafeAreaProvider>
        <StatusBar barStyle="light-content" />
        <View style={styles.center}>
          <Text style={styles.title}>Days Counter</Text>
        </View>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#0E1530' },
  title: { color: '#FFFFFF', fontSize: 28, fontWeight: '700' },
});
