export default function Loader({ label = 'Loading' }) {
  return (
    <div className="loader-wrap" role="status">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}
