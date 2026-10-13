import { Link } from 'react-router-dom'

export default function Brand({ className = '' }) {
  return (
    <Link className={`brand-lockup${className ? ` ${className}` : ''}`} to="/" aria-label="OwnSpace home">
      <span className="brand-mark">O</span>
      <strong>OwnSpace</strong>
    </Link>
  )
}
