export default function Skeleton({
  variant = 'text',
  rows = 3,
  className = '',
  ...props
}) {
  if (variant === 'table') {
    return (
      <div className={`ui-skeleton-table${className ? ` ${className}` : ''}`} role="status" aria-label="Loading" {...props}>
        {Array.from({ length: Math.min(Math.max(rows, 1), 10) }, (_, index) => (
          <span className="ui-skeleton ui-skeleton--row" key={index} />
        ))}
      </div>
    )
  }

  return (
    <span
      {...props}
      className={`ui-skeleton ui-skeleton--${variant}${className ? ` ${className}` : ''}`}
      role="status"
      aria-label="Loading"
    />
  )
}
