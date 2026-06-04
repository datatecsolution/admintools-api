import { MoonIcon, SunIcon } from '@heroicons/react/24/outline'
import { useUIStore } from '@/stores/ui-store'

/**
 * Switch claro/oscuro. Lee y escribe ui-store; el store ya aplica la
 * clase 'dark' al <html> y persiste en localStorage. Sprint 4 #52.
 */
export function ThemeToggle() {
  const theme = useUIStore((s) => s.theme)
  const setTheme = useUIStore((s) => s.setTheme)
  const isDark = theme === 'dark'
  return (
    <button
      type="button"
      onClick={() => setTheme(isDark ? 'light' : 'dark')}
      className="inline-flex h-10 w-10 items-center justify-center rounded-md text-(--color-fg) hover:bg-slate-100 dark:hover:bg-slate-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-(--color-primary)"
      aria-label={isDark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
    >
      {isDark ? <SunIcon className="h-5 w-5" /> : <MoonIcon className="h-5 w-5" />}
    </button>
  )
}
