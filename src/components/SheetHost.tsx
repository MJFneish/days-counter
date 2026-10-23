import React from 'react';

import { CountdownEditor } from './editor/CountdownEditor';
import { CountdownPickerSheet } from './settings/CountdownPickerSheet';

/** Every bottom sheet, mounted once in App.tsx above the navigator so they cover the tab pager
 * and the tab bar. Screens open them through `useSheetStore`. */
export function SheetHost() {
  return (
    <>
      <CountdownEditor />
      <CountdownPickerSheet />
    </>
  );
}
