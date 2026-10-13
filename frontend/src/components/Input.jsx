function FieldMessages({ id, hint, error }) {
  const ids = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ') || undefined

  return {
    describedBy: ids,
    messages: (
      <>
        {hint && <span className="field-help ui-field-hint" id={`${id}-hint`}>{hint}</span>}
        {error && <span className="ui-field-error" id={`${id}-error`}>{error}</span>}
      </>
    ),
  }
}

export default function Input({
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
  const field = FieldMessages({ id: controlId, hint, error })

  return (
    <div className={`field ui-field${wrapperClassName ? ` ${wrapperClassName}` : ''}`}>
      <label className="field-label" htmlFor={controlId}>
        {label}{required && <span aria-hidden="true"> *</span>}
      </label>
      <input
        {...props}
        id={controlId}
        name={name}
        required={required}
        className={`ui-field-control${className ? ` ${className}` : ''}`}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={field.describedBy}
      />
      {field.messages}
    </div>
  )
}
