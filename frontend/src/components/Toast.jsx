import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'

const tones = new Set(['success', 'warning', 'error', 'neutral'])

export default function Toast({
  message,
  open = Boolean(message),
  variant = 'neutral',
  onClose,
}) {
  const reduceMotion = useReducedMotion()
  const tone = tones.has(variant) ? variant : 'neutral'
  const duration = reduceMotion ? 0 : 0.2

  return (
    <AnimatePresence>
      {open && message && (
        <motion.div
          className={`ui-toast ui-toast--${tone}`}
          role={tone === 'error' ? 'alert' : 'status'}
          initial={{ opacity: 0, y: reduceMotion ? 0 : -8 }}
          animate={{ opacity: 1, y: 0, transition: { duration, ease: 'easeOut' } }}
          exit={{ opacity: 0, y: reduceMotion ? 0 : -8, transition: { duration } }}
        >
          <span>{message}</span>
          {onClose && <button type="button" onClick={onClose} aria-label="Dismiss notification">×</button>}
        </motion.div>
      )}
    </AnimatePresence>
  )
}
