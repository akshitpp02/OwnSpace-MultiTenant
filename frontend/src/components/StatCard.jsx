import Card from './Card'

export default function StatCard({ label, value, detail, icon }) {
  return (
    <Card as="article" className="stat-card">
      <div className="stat-card-top">
        <span className="stat-label">{label}</span>
        <span className="stat-icon" aria-hidden="true">{icon}</span>
      </div>
      <strong className="stat-value">{value}</strong>
      {detail && <span className="stat-detail">{detail}</span>}
    </Card>
  )
}
