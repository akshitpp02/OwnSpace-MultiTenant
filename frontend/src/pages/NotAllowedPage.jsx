import { Link } from 'react-router-dom'

export default function NotAllowedPage() {
  return <main className="centered-page"><div className="not-allowed-mark">↗</div><div className="eyebrow">A LITTLE DETOUR</div><h1>This space isn’t yours to access.</h1><p>Your current account doesn’t have permission to view this page. If you think this is a mistake, check in with your workspace owner.</p><Link className="button button-primary" to="/dashboard">Back to your workspace <span aria-hidden="true">→</span></Link></main>
}
