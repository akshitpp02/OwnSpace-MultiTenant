import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import Loader from '../../components/Loader'
import StatCard from '../../components/StatCard'
import DashboardGreeting from '../../components/DashboardGreeting'
import NeedsAttention from '../../components/NeedsAttention'
import { useAuth } from '../../context/useAuth'

export default function DashboardPage() {
  const { user } = useAuth()
  const canManageUsers = ['OWNER', 'MANAGER'].includes(user?.role)
  const [dashboard, setDashboard] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  async function loadDashboard() {
    try {
      const { data } = await api.get('/dashboard/core')
      setDashboard(data.data)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Dashboard information could not be loaded.')
    } finally {
      setLoading(false)
    }
  }

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadDashboard() }, [])

  if (loading) return <Loader label="Getting your workspace ready" />
  const inactiveUsers = Math.max(0, (dashboard?.totalUsers ?? 0) - (dashboard?.activeUsers ?? 0))
  const attentionItems = canManageUsers && inactiveUsers > 0
    ? [{ icon: '♙', title: `${inactiveUsers} inactive ${inactiveUsers === 1 ? 'team account' : 'team accounts'}`, description: 'Review account access for your team.', to: '/users' }]
    : []
  return (
    <div className="content-column dashboard-v2">
      <DashboardGreeting eyebrow="YOUR BUSINESS, AT A GLANCE" description={`Here’s how things are looking at ${dashboard?.businessName || user?.businessName || 'your workspace'}.`} />
      <ErrorMessage message={error} onRetry={() => { setLoading(true); setError(''); loadDashboard() }} />
      {dashboard && <>
        <div className="stat-grid dashboard-v2__stats">
          <StatCard label="Team members" value={dashboard.totalUsers ?? 0} detail="Across your workspace" icon="♙" />
          <StatCard label="Active accounts" value={dashboard.activeUsers ?? 0} detail="Ready to get things done" icon="↗" />
          <StatCard label="Workspace" value={dashboard.businessName || user?.businessName || '—'} detail={`${user?.businessType || 'Business'} workspace`} icon="◈" />
          <StatCard label="Your access" value={user?.role?.replaceAll('_', ' ') || 'Team member'} detail="Your workspace role" icon="✓" />
        </div>
        <NeedsAttention items={attentionItems} />
        <section className="welcome-card">
          <div className="welcome-copy"><div className="eyebrow">A GOOD PLACE TO BEGIN</div><h2>Your workspace is yours to shape.</h2><p>Invite the people you trust and keep the essentials up to date. The best tools are the ones that quietly make your day easier.</p><div className="welcome-actions">{canManageUsers && <Link className="button button-primary" to="/users">Meet your team <span aria-hidden="true">→</span></Link>}<span className="welcome-footnote"><span className="ready-dot" /> Your workspace is active</span></div></div>
          <div className="welcome-art" aria-hidden="true"><div className="welcome-ring ring-a" /><div className="welcome-ring ring-b" /><div className="welcome-diamond">✦</div><span className="welcome-star star-a">✳</span><span className="welcome-star star-b">✧</span><span className="welcome-small-dot dot-a" /><span className="welcome-small-dot dot-b" /></div>
        </section>
        <section className="surface-card next-steps-card"><div className="section-heading"><div><span className="eyebrow">YOUR NEXT STEPS</span><h2>Make this space work for you</h2></div><span className="section-mark">✦</span></div><div className="next-step-list">
          {canManageUsers && <Link className="next-step" to="/users"><span className="step-icon">01</span><span><strong>Bring your team together</strong><small>Add people and give them the access they need.</small></span><span className="step-arrow">→</span></Link>}
          {user?.role === 'OWNER' && <Link className="next-step" to="/settings"><span className="step-icon">02</span><span><strong>Set your business details</strong><small>Keep your workspace information current.</small></span><span className="step-arrow">→</span></Link>}
          <div className="next-step"><span className="step-icon">✳</span><span><strong>Everything is in one place</strong><small>Your business data stays in your own workspace.</small></span><span className="step-complete">✓</span></div>
        </div></section>
      </>}
    </div>
  )
}
