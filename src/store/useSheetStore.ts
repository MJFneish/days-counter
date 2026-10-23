import { create } from 'zustand';

/**
 * Which bottom sheet is open. The sheets render in `SheetHost` (App.tsx), above the tab pager and
 * the tab bar, instead of inside RN `Modal`s — see BottomSheet.tsx for why.
 */
interface SheetState {
  /** `null` id = a new countdown. */
  editor: { open: false } | { open: true; countdownId: string | null };
  pickerOpen: boolean;
  openEditor: (countdownId: string | null) => void;
  closeEditor: () => void;
  openPicker: () => void;
  closePicker: () => void;
}

export const useSheetStore = create<SheetState>(set => ({
  editor: { open: false },
  pickerOpen: false,
  openEditor: countdownId => set({ editor: { open: true, countdownId } }),
  closeEditor: () => set({ editor: { open: false } }),
  openPicker: () => set({ pickerOpen: true }),
  closePicker: () => set({ pickerOpen: false }),
}));
