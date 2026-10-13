const variants = new Set(['neutral', 'accent', 'success', 'warning', 'error'])

export default function Badge({ children, variant = 'neutral', className = '', ...props }) {
  const tone = variants.has(variant) ? variant : 'neutral'
  return (
    <span {...props} className={`ui-badge ui-badge--${tone}${className ? ` ${className}` : ''}`}>
      {children}
    </span>
  )
}
