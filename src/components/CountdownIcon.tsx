import {
  Baby,
  Briefcase,
  Cake,
  CalendarDays,
  Flag,
  Gift,
  GraduationCap,
  Heart,
  House,
  Plane,
  Star,
  Trophy,
  type LucideIcon,
} from 'lucide-react-native';

import type { CountdownIcon } from '../core/types';

/** The icon a countdown can carry (editor grid, hero card, list rows). */
export const COUNTDOWN_ICON_COMPONENTS: Record<CountdownIcon, LucideIcon> = {
  calendar: CalendarDays,
  star: Star,
  heart: Heart,
  cake: Cake,
  plane: Plane,
  graduation: GraduationCap,
  briefcase: Briefcase,
  home: House,
  baby: Baby,
  trophy: Trophy,
  gift: Gift,
  flag: Flag,
};
