export default function PageHeader({
  eyebrow,
  title,
  description,
  action,
  className = '',
}) {
  return (
    <header className={`ui-page-header${className ? ` ${className}` : ''}`}>
      <div className="ui-page-header__copy">
        {eyebrow && <span className="ui-page-header__eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {action && <div className="ui-page-header__action">{action}</div>}
    </header>
  )
}
