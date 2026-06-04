import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Header } from './Header'

/**
 * Shell del panel: sidebar + header + Outlet para las paginas hijas.
 * Sprint 4 #52.
 *
 * Se usa como Route element padre en App.tsx con rutas protegidas
 * adentro. Ejemplo de uso:
 *
 *   <Route element={<ProtectedRoutes />}>
 *     <Route element={<Layout />}>
 *       <Route path="/" element={<Dashboard />} />
 *       ...
 *     </Route>
 *   </Route>
 */
export function Layout() {
  return (
    <div className="flex h-full">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header />
        <main className="flex-1 overflow-y-auto bg-(--color-bg) p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
