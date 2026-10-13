export default function ErrorMessage({ message, onRetry }) {
  if (!message) return null
  return (
    <div className="alert alert-error" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button className="text-button" type="button" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}
