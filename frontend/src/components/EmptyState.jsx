import Button from './Button'

export default function EmptyState({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  className = '',
}) {
  return (
    <div className={`ui-empty-state${className ? ` ${className}` : ''}`}>
      {icon && <span className="ui-empty-state__icon" aria-hidden="true">{icon}</span>}
      {title && <h2>{title}</h2>}
      <p>{description || title || 'Nothing to show yet.'}</p>
      {actionLabel && onAction && (
        <Button onClick={onAction}>{actionLabel}</Button>
      )}
    </div>
  )
}
