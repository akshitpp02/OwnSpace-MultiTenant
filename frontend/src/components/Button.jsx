export default function Button({
  children,
  variant = 'primary',
  size = 'medium',
  loading = false,
  disabled = false,
  className = '',
  type = 'button',
  ...props
}) {
  return (
    <button
      {...props}
      className={`ui-button ui-button--${variant} ui-button--${size}${className ? ` ${className}` : ''}`}
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
    >
      {loading && <span className="ui-button__loader" aria-hidden="true" />}
      {children}
    </button>
  )
}
