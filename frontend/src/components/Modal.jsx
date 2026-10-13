import { useEffect } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'

export default function Modal({
  open,
  title,
  children,
  onClose,
  className = '',
  closeLabel = 'Close dialog',
}) {
  const reduceMotion = useReducedMotion()

  useEffect(() => {
    if (!open) return undefined
    function handleKeyDown(event) {
      if (event.key === 'Escape') onClose?.()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [open, onClose])

  const duration = reduceMotion ? 0 : 0.18

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="ui-modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) onClose?.()
          }}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1, transition: { duration } }}
          exit={{ opacity: 0, transition: { duration } }}
        >
          <motion.section
            className={`ui-modal${className ? ` ${className}` : ''}`}
            role="dialog"
            aria-modal="true"
            aria-labelledby="ui-modal-title"
            initial={{ opacity: 0, scale: reduceMotion ? 1 : 0.97 }}
            animate={{ opacity: 1, scale: 1, transition: { duration, ease: 'easeOut' } }}
            exit={{ opacity: 0, scale: reduceMotion ? 1 : 0.97, transition: { duration } }}
          >
            <header className="ui-modal__header">
              <h2 id="ui-modal-title">{title}</h2>
              {onClose && <button className="ui-modal__close" type="button" onClick={onClose} aria-label={closeLabel}>×</button>}
            </header>
            {children}
          </motion.section>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
