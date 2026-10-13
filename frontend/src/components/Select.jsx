function optionValue(option) {
  return typeof option === 'object' ? option.value : option
}

function optionLabel(option) {
  return typeof option === 'object' ? option.label : option
}

export default function Select({
  id,
  name,
  label,
  hint,
  error,
  options = [],
  className = '',
  wrapperClassName = '',
  required = false,
  children,
  ...props
}) {
  const controlId = id || name
  const describedBy = [hint && `${controlId}-hint`, error && `${controlId}-error`].filter(Boolean).join(' ') || undefined

  return (
    <div className={`field ui-field${wrapperClassName ? ` ${wrapperClassName}` : ''}`}>
      <label className="field-label" htmlFor={controlId}>
        {label}{required && <span aria-hidden="true"> *</span>}
      </label>
      <select
        {...props}
        id={controlId}
        name={name}
        required={required}
        className={`ui-field-control${className ? ` ${className}` : ''}`}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy}
      >
        {children || options.map((option) => (
          <option key={optionValue(option)} value={optionValue(option)}>
            {optionLabel(option)}
          </option>
        ))}
      </select>
      {hint && <span className="field-help ui-field-hint" id={`${controlId}-hint`}>{hint}</span>}
      {error && <span className="ui-field-error" id={`${controlId}-error`}>{error}</span>}
    </div>
  )
}
