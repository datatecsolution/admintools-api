import { NavLink } from 'react-router-dom'
import {
  HomeIcon,
  CubeIcon,
  TagIcon,
  CurrencyDollarIcon,
  UsersIcon,
  UserCircleIcon,
} from '@heroicons/react/24/outline'
import { useUIStore } from '@/stores/ui-store'

/**
 * Sidebar de navegacion lateral. Sprint 4 #52.
 *
 * Colapsable (estado en ui-store). En mobile <md se renderiza encima
 * del contenido como overlay; en desktop, side-by-side. La presencia
 * de NavLink hace que React Router marque la pagina activa
 * automaticamente con `aria-current`.
 *
 * Estado de roles para mostrar/ocultar items se hara en US-024 cuando
 * el AuthContext este disponible. Por ahora todos los items se ven.
 */

interface NavItem {
  to: string
  label: string
  Icon: typeof HomeIcon
}

const items: NavItem[] = [
  { to: '/', label: 'Inicio', Icon: HomeIcon },
  { to: '/products', label: 'Productos', Icon: CubeIcon },
  { to: '/categories', label: 'Categorías', Icon: TagIcon },
  { to: '/price-types', label: 'Tipos de precio', Icon: CurrencyDollarIcon },
  { to: '/customers', label: 'Clientes', Icon: UsersIcon },
  { to: '/users', label: 'Usuarios', Icon: UserCircleIcon },
]

export function Sidebar() {
  const sidebarOpen = useUIStore((s) => s.sidebarOpen)
  return (
    <aside
      className={`flex h-full shrink-0 flex-col border-r border-(--color-border) bg-(--color-surface) transition-[width] duration-200 ${
        sidebarOpen ? 'w-56' : 'w-16'
      }`}
      aria-label="Navegación principal"
    >
      {/* Brand */}
      <div className="flex h-14 items-center border-b border-(--color-border) px-3">
        <span
          className={`text-lg font-bold text-(--color-primary) transition-opacity ${
            sidebarOpen ? 'opacity-100' : 'opacity-0'
          }`}
        >
          POS
        </span>
      </div>

      {/* Nav items */}
      <nav className="flex-1 overflow-y-auto p-2">
        <ul className="space-y-1">
          {items.map(({ to, label, Icon }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  `group flex h-10 items-center gap-3 rounded-md px-3 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-(--color-primary) text-white'
                      : 'text-(--color-fg) hover:bg-slate-100 dark:hover:bg-slate-700'
                  }`
                }
                title={!sidebarOpen ? label : undefined}
              >
                <Icon className="h-5 w-5 shrink-0" />
                <span
                  className={`truncate transition-opacity ${
                    sidebarOpen ? 'opacity-100' : 'hidden opacity-0'
                  }`}
                >
                  {label}
                </span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  )
}
