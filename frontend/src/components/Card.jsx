export default function Card({
  as: Element = 'section',
  interactive = false,
  className = '',
  children,
  ...props
}) {
  return (
    <Element
      {...props}
      className={`ui-card${interactive ? ' ui-card--interactive' : ''}${className ? ` ${className}` : ''}`}
    >
      {children}
    </Element>
  )
}
