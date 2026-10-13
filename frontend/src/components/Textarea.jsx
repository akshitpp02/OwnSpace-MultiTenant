export default function Textarea({
  id,
  name,
  label,
  hint,
  error,
  className = '',
  wrapperClassName = '',
  required = false,
  ...props
}) {
  const controlId = id || name
  const describedBy = [hint && `${controlId}-hint`, error && `${controlId}-error`].filter(Boolean).join(' ') || undefined

  return (
    <div className={`field ui-field${wrapperClassName ? ` ${wrapperClassName}` : ''}`}>
      <label className="field-label" htmlFor={controlId}>
        {label}{required && <span aria-hidden="true"> *</span>}
      </label>
      <textarea
        {...props}
        id={controlId}
        name={name}
        required={required}
        className={`ui-field-control${className ? ` ${className}` : ''}`}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy}
      />
      {hint && <span className="field-help ui-field-hint" id={`${controlId}-hint`}>{hint}</span>}
      {error && <span className="ui-field-error" id={`${controlId}-error`}>{error}</span>}
    </div>
  )
}
