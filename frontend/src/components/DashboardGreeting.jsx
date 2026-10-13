import { useAuth } from '../context/useAuth'
import Button from './Button'

function timeGreeting() {
  const hour = new Date().getHours()
  return hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening'
}

export default function DashboardGreeting({ eyebrow, description, onRefresh, loading = false }) {
  const { user } = useAuth()
  const firstName = user?.name?.trim().split(/\s+/)[0]

  return (
    <div className="page-heading dashboard-v2__heading">
      <div>
        {eyebrow && <div className="eyebrow"><span className="eyebrow-dot" /> {eyebrow}</div>}
        <h1>{timeGreeting()}{firstName ? `, ${firstName}` : ''}.</h1>
        <p>{description}</p>
      </div>
      {onRefresh && <Button variant="secondary" onClick={onRefresh} loading={loading}>Refresh</Button>}
    </div>
  )
}
