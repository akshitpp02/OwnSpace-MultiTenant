import { Link } from 'react-router-dom'

export default function NeedsAttention({ items = [] }) {
  return (
    <section className="dashboard-v2__attention ui-card" aria-labelledby="dashboard-attention-heading">
      <div className="dashboard-v2__panel-heading">
        <div>
          <span className="dashboard-v2__eyebrow">QUICK CHECK</span>
          <h2 id="dashboard-attention-heading">Needs attention</h2>
        </div>
        <span className="dashboard-v2__attention-count" aria-label={`${items.length} items need attention`}>{items.length}</span>
      </div>
      {items.length ? (
        <ul className="dashboard-v2__attention-list">
          {items.map((item) => {
            const content = (
              <>
                <span className={`dashboard-v2__attention-icon dashboard-v2__attention-icon--${item.tone || 'warning'}`} aria-hidden="true">{item.icon || '!'}</span>
                <span className="dashboard-v2__attention-copy"><strong>{item.title}</strong><small>{item.description}</small></span>
                {item.to && <span className="dashboard-v2__attention-arrow" aria-hidden="true">→</span>}
              </>
            )
            return (
              <li key={item.title}>
                {item.to ? <Link to={item.to} className="dashboard-v2__attention-item">{content}</Link> : <div className="dashboard-v2__attention-item">{content}</div>}
              </li>
            )
          })}
        </ul>
      ) : (
        <div className="dashboard-v2__attention-empty">
          <span aria-hidden="true">✓</span>
          <p>You’re all caught up. Nothing needs attention right now.</p>
        </div>
      )}
    </section>
  )
}
