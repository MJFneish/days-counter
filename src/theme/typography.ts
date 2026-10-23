/** System fonts on purpose: they ship proper Arabic shaping (Noto Naskh / SF Arabic) for free,
 * which a bundled Latin display face wouldn't. Body text stays >= 16 for readability. Numbers that
 * tick use `tabular-nums`, so a changing digit never shifts its neighbours. */
export const typography = {
  hero: { fontSize: 64, fontWeight: '800' as const, letterSpacing: -1.5, fontVariant: ['tabular-nums' as const] },
  displayLarge: { fontSize: 34, fontWeight: '700' as const, letterSpacing: -0.4 },
  displayMedium: { fontSize: 28, fontWeight: '700' as const },
  digits: { fontSize: 28, fontWeight: '700' as const, fontVariant: ['tabular-nums' as const] },
  title: { fontSize: 20, fontWeight: '700' as const },
  subtitle: { fontSize: 17, fontWeight: '600' as const },
  body: { fontSize: 16, fontWeight: '400' as const, lineHeight: 24 },
  bodyStrong: { fontSize: 16, fontWeight: '600' as const },
  caption: { fontSize: 13, fontWeight: '500' as const, lineHeight: 18 },
  label: { fontSize: 12, fontWeight: '700' as const, letterSpacing: 0.6 },
};
