import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/hooks/useAuth'
import { ApiError } from '@/lib/api'

/**
 * Login page. Sprint 4 #53 / US-024.
 *
 * Form con react-hook-form + zod. Errores 401 muestran mensaje al
 * usuario sin redirigir. Si ya hay sesion activa al entrar, redirige
 * al dashboard (o a location.state.from si vino de un guard).
 */

const schema = z.object({
  username: z.string().min(1, 'Usuario requerido'),
  password: z.string().min(1, 'Contraseña requerida'),
})

type FormValues = z.infer<typeof schema>

interface LocationState {
  from?: { pathname: string }
}

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as LocationState | null)?.from?.pathname ?? '/'
  const [submitError, setSubmitError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', password: '' },
  })

  if (isAuthenticated) {
    return <Navigate to={from} replace />
  }

  const onSubmit = async (values: FormValues) => {
    setSubmitError(null)
    try {
      await login(values.username, values.password)
      navigate(from, { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setSubmitError('Usuario o contraseña incorrectos')
      } else if (err instanceof Error) {
        setSubmitError(err.message)
      } else {
        setSubmitError('Error inesperado')
      }
    }
  }

  return (
    <main className="flex min-h-full items-center justify-center bg-(--color-bg) p-6">
      <div className="w-full max-w-sm rounded-lg border border-(--color-border) bg-(--color-surface) p-6 shadow-sm">
        <h1 className="mb-1 text-2xl font-bold text-(--color-fg)">
          Iniciar sesión
        </h1>
        <p className="mb-6 text-sm text-(--color-muted)">
          Panel administrativo del POS
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label
              htmlFor="username"
              className="mb-1 block text-sm font-medium text-(--color-fg)"
            >
              Usuario
            </label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              autoFocus
              {...register('username')}
              className="h-10 w-full rounded-md border border-(--color-border) bg-(--color-bg) px-3 text-sm text-(--color-fg) focus:outline-none focus:ring-2 focus:ring-(--color-primary)"
            />
            {errors.username && (
              <p className="mt-1 text-xs text-red-500">{errors.username.message}</p>
            )}
          </div>

          <div>
            <label
              htmlFor="password"
              className="mb-1 block text-sm font-medium text-(--color-fg)"
            >
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              {...register('password')}
              className="h-10 w-full rounded-md border border-(--color-border) bg-(--color-bg) px-3 text-sm text-(--color-fg) focus:outline-none focus:ring-2 focus:ring-(--color-primary)"
            />
            {errors.password && (
              <p className="mt-1 text-xs text-red-500">{errors.password.message}</p>
            )}
          </div>

          {submitError && (
            <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
              {submitError}
            </div>
          )}

          <Button type="submit" disabled={isSubmitting} className="w-full">
            {isSubmitting ? 'Ingresando...' : 'Ingresar'}
          </Button>
        </form>
      </div>
    </main>
  )
}
