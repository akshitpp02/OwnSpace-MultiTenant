import { Children } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'

const enterTransition = {
  duration: 0.2,
  ease: 'easeOut',
}

const fadeVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: enterTransition },
}

const slideUpVariants = {
  hidden: { opacity: 0, y: 8 },
  visible: { opacity: 1, y: 0, transition: enterTransition },
}

const staggerContainerVariants = {
  hidden: {},
  visible: {
    transition: {
      staggerChildren: 0.04,
    },
  },
}

const staggerItemVariants = {
  hidden: { opacity: 0, y: 8 },
  visible: { opacity: 1, y: 0, transition: enterTransition },
}

function useMotionVariants(variants) {
  const shouldReduceMotion = useReducedMotion()
  if (!shouldReduceMotion) return variants

  return Object.fromEntries(
    Object.entries(variants).map(([key, value]) => [
      key,
      { ...value, y: 0, transition: { duration: 0 } },
    ]),
  )
}

function useEntranceProps(variants, inView) {
  const motionVariants = useMotionVariants(variants)
  return {
    variants: motionVariants,
    initial: 'hidden',
    animate: inView ? undefined : 'visible',
    whileInView: inView ? 'visible' : undefined,
    viewport: inView ? { once: true, amount: 0.15 } : undefined,
  }
}

export function FadeIn({ children, as = 'div', inView = false, ...props }) {
  const entranceProps = useEntranceProps(fadeVariants, inView)
  const Element = motion[as]
  return <Element {...props} {...entranceProps}>{children}</Element>
}

export function SlideUp({ children, as = 'div', inView = false, ...props }) {
  const entranceProps = useEntranceProps(slideUpVariants, inView)
  const Element = motion[as]
  return <Element {...props} {...entranceProps}>{children}</Element>
}

export function Stagger({
  children,
  as = 'div',
  itemAs = 'div',
  inView = false,
  ...props
}) {
  const entranceProps = useEntranceProps(staggerContainerVariants, inView)
  const itemVariants = useMotionVariants(staggerItemVariants)
  const Container = motion[as]
  const Item = motion[itemAs]
  const items = Children.toArray(children)

  return (
    <Container {...props} {...entranceProps}>
      {items.map((child, index) => index < 10
        ? <Item key={child.key ?? index} variants={itemVariants}>{child}</Item>
        : child)}
    </Container>
  )
}

export function PageTransition({ children, pageKey, ...props }) {
  const variants = useMotionVariants(slideUpVariants)

  return (
    <AnimatePresence initial={false} mode="wait">
      <motion.div
        key={pageKey}
        {...props}
        variants={variants}
        initial="hidden"
        animate="visible"
        exit="hidden"
      >
        {children}
      </motion.div>
    </AnimatePresence>
  )
}
