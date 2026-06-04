import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/react'
import {
  ArrowRightOnRectangleIcon,
  UserCircleIcon,
} from '@heroicons/react/24/outline'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

/**
 * Menu del avatar (top-right). Sprint 4 #53 / US-024.
 *
 * Usa AuthContext para mostrar el username y para logout. Navega a
 * /login con react-router (sin hard reload) — el QueryClient se
 * limpia dentro del logout() del context.
 */
export function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <Menu as="div" className="relative">
      <MenuButton className="inline-flex items-center gap-2 rounded-md px-2 py-1.5 text-sm font-medium text-(--color-fg) hover:bg-slate-100 dark:hover:bg-slate-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-(--color-primary)">
        <UserCircleIcon className="h-7 w-7" />
        <span className="hidden sm:inline">{user?.username ?? 'invitado'}</span>
      </MenuButton>
      <MenuItems
        anchor="bottom end"
        className="z-50 mt-2 w-48 origin-top-right rounded-md border border-(--color-border) bg-(--color-surface) p-1 shadow-lg focus:outline-none"
      >
        <div className="px-3 py-2 text-xs text-(--color-muted)">
          Sesión: {user?.username}
        </div>
        <div className="my-1 h-px bg-(--color-border)" />
        <MenuItem>
          {({ focus }) => (
            <button
              type="button"
              onClick={handleLogout}
              className={`flex w-full items-center gap-2 rounded px-3 py-2 text-sm ${
                focus
                  ? 'bg-slate-100 dark:bg-slate-700'
                  : 'text-(--color-fg)'
              }`}
            >
              <ArrowRightOnRectangleIcon className="h-4 w-4" />
              Cerrar sesión
            </button>
          )}
        </MenuItem>
      </MenuItems>
    </Menu>
  )
}
