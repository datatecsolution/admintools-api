import { Bars3Icon } from '@heroicons/react/24/outline'
import { useUIStore } from '@/stores/ui-store'
import { ThemeToggle } from '@/components/ui/ThemeToggle'
import { UserMenu } from './UserMenu'

/**
 * Barra superior. Sprint 4 #52.
 *
 * Layout: [toggle sidebar] [titulo placeholder] [theme] [user menu].
 * Breadcrumbs los agregamos cuando haya paginas reales (US-023 cierra
 * con el shell; cada pagina pondra el suyo en US-025+).
 */
export function Header() {
  const toggleSidebar = useUIStore((s) => s.toggleSidebar)
  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-(--color-border) bg-(--color-surface) px-4">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={toggleSidebar}
          className="inline-flex h-10 w-10 items-center justify-center rounded-md text-(--color-fg) hover:bg-slate-100 dark:hover:bg-slate-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-(--color-primary)"
          aria-label="Alternar menú lateral"
        >
          <Bars3Icon className="h-5 w-5" />
        </button>
        <h1 className="text-base font-semibold text-(--color-fg)">
          admintools-pos
        </h1>
      </div>
      <div className="flex items-center gap-2">
        <ThemeToggle />
        <UserMenu />
      </div>
    </header>
  )
}
