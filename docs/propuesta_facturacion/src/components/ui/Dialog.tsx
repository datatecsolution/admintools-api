import {
  Dialog as HeadlessDialog,
  DialogPanel,
  DialogTitle,
  Transition,
  TransitionChild,
} from '@headlessui/react'
import { XMarkIcon } from '@heroicons/react/24/outline'
import { Fragment, type ReactNode } from 'react'

/**
 * Modal Dialog reutilizable basado en Headless UI. Sprint 4 #56.
 *
 * Uso:
 *   <Dialog open={open} onClose={close} title="Nueva categoria">
 *     <form>...</form>
 *   </Dialog>
 *
 * Props:
 *   - open / onClose: controlado por el padre.
 *   - title: opcional; si va, se renderiza como header con boton X.
 *   - size: 'sm' | 'md' | 'lg'; afecta max-width.
 *   - children: contenido del body (formulario, info, etc.).
 */

interface DialogProps {
  open: boolean
  onClose: () => void
  title?: string
  size?: 'sm' | 'md' | 'lg'
  children: ReactNode
}

const sizeClass: Record<NonNullable<DialogProps['size']>, string> = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-2xl',
}

export function Dialog({
  open,
  onClose,
  title,
  size = 'md',
  children,
}: DialogProps) {
  return (
    <Transition show={open} as={Fragment}>
      <HeadlessDialog as="div" className="relative z-50" onClose={onClose}>
        {/* Backdrop */}
        <TransitionChild
          as={Fragment}
          enter="ease-out duration-150"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in duration-100"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-black/40" aria-hidden="true" />
        </TransitionChild>

        {/* Panel */}
        <div className="fixed inset-0 flex items-start justify-center overflow-y-auto p-4 sm:items-center">
          <TransitionChild
            as={Fragment}
            enter="ease-out duration-150"
            enterFrom="opacity-0 scale-95"
            enterTo="opacity-100 scale-100"
            leave="ease-in duration-100"
            leaveFrom="opacity-100 scale-100"
            leaveTo="opacity-0 scale-95"
          >
            <DialogPanel
              className={`relative w-full ${sizeClass[size]} rounded-lg border border-(--color-border) bg-(--color-surface) shadow-xl`}
            >
              {title && (
                <div className="flex items-center justify-between border-b border-(--color-border) px-5 py-3">
                  <DialogTitle className="text-base font-semibold text-(--color-fg)">
                    {title}
                  </DialogTitle>
                  <button
                    type="button"
                    onClick={onClose}
                    className="inline-flex h-8 w-8 items-center justify-center rounded-md text-(--color-muted) hover:bg-slate-100 dark:hover:bg-slate-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-(--color-primary)"
                    aria-label="Cerrar"
                  >
                    <XMarkIcon className="h-4 w-4" />
                  </button>
                </div>
              )}
              <div className="px-5 py-4">{children}</div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </HeadlessDialog>
    </Transition>
  )
}
