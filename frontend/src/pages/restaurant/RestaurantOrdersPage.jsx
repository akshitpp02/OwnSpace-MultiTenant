import { useCallback, useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import Loader from '../../components/Loader'
import { useAuth } from '../../context/useAuth'
import { errorText, money, roleLabel } from './restaurantUtils'

const statusOptions = ['PLACED', 'PREPARING', 'READY', 'SERVED', 'BILLED']

export default function RestaurantOrdersPage({ kitchenOnly = false }) {
  const { user } = useAuth()
  const location = useLocation()
  const management = ['OWNER', 'MANAGER'].includes(user?.role)
  const kitchen = ['KITCHEN', 'KITCHEN_STAFF'].includes(user?.role)
  const [orders, setOrders] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState(location.state?.notice || '')
  const [updating, setUpdating] = useState(null)

  const loadOrders = useCallback(async () => {
    try {
      const { data } = await api.get('/orders', {
        params: kitchenOnly ? undefined : { status: statusFilter || undefined },
      })
      const results = data.data || []
      setOrders(kitchenOnly ? results.filter((order) => ['PLACED', 'PREPARING', 'READY'].includes(order.status)) : results)
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Orders could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [kitchenOnly, statusFilter])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadOrders() }, [loadOrders])

  useEffect(() => {
    if (!kitchenOnly) return undefined
    const timer = window.setInterval(loadOrders, 10_000)
    return () => window.clearInterval(timer)
  }, [kitchenOnly, loadOrders])

  function nextAction(order) {
    if (management) {
      if (order.status === 'PLACED') return 'PREPARING'
      if (order.status === 'PREPARING') return 'READY'
      if (order.status === 'READY') return 'SERVED'
    }
    if (kitchen && order.status === 'PLACED') return 'PREPARING'
    if (kitchen && order.status === 'PREPARING') return 'READY'
    if (user?.role === 'WAITER' && order.status === 'READY') return 'SERVED'
    return null
  }

  async function changeStatus(order, nextStatus) {
    setUpdating(order.id)
    setError('')
    try {
      await api.patch(`/orders/${order.id}/status`, { status: nextStatus })
      setNotice(`Order #${order.id} moved to ${roleLabel(nextStatus)}.`)
      await loadOrders()
    } catch (requestError) {
      setError(errorText(requestError))
    } finally {
      setUpdating(null)
    }
  }

  return (
    <div className="content-column restaurant-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> {kitchenOnly ? 'LIVE KITCHEN' : 'SERVICE'}</div><h1>{kitchenOnly ? 'Kitchen screen' : 'Orders'}</h1><p>{kitchenOnly ? 'Incoming tickets refresh automatically every 10 seconds.' : 'Track every table ticket from placement through service.'}</p></div><button className="button button-secondary" type="button" onClick={loadOrders}>Refresh</button></div>
      <ErrorMessage message={error} onRetry={loadOrders} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {!kitchenOnly && <section className="surface-card restaurant-filter-card"><label className="field"><span className="field-label">Filter by status</span><select value={statusFilter} onChange={(event) => { setLoading(true); setStatusFilter(event.target.value) }}><option value="">All orders</option>{statusOptions.map((status) => <option key={status} value={status}>{roleLabel(status)}</option>)}</select></label></section>}
      {loading ? <Loader label="Loading restaurant orders" /> : kitchenOnly ? (
        <div className="kitchen-board" aria-label="Kitchen orders by preparation status">
          {[{ status: 'PLACED', label: 'New', tone: 'new' }, { status: 'PREPARING', label: 'Preparing', tone: 'preparing' }, { status: 'READY', label: 'Ready', tone: 'ready' }].map((column) => {
            const columnOrders = orders.filter((order) => order.status === column.status)
            return (
              <section className={`kitchen-column kitchen-column--${column.tone}`} key={column.status}>
                <header className="kitchen-column__heading"><h2>{column.label}</h2><span>{columnOrders.length}</span></header>
                <div className="kitchen-column__orders">
                  {columnOrders.map((order) => {
                    const next = nextAction(order)
                    const amount = order.items?.reduce((total, item) => total + Number(item.lineTotal), 0) || 0
                    return (
                      <article className="kitchen-ticket" key={order.id}>
                        <div className="kitchen-ticket__heading"><div><span>ORDER #{order.id}</span><h3>{order.tableName}</h3></div><time>{order.createdAt ? new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Now'}</time></div>
                        <div className="kitchen-ticket__items">{order.items?.map((item) => <div key={item.id || `${item.menuItemId}-${item.name}`}><span className="kitchen-ticket__quantity">{item.quantity}×</span><span><strong>{item.name}</strong>{item.notes && <small>{item.notes}</small>}</span></div>)}</div>
                        <footer className="kitchen-ticket__footer"><strong>{money(amount)}</strong>{next ? <button className="kitchen-ticket__action" type="button" disabled={updating === order.id} onClick={() => changeStatus(order, next)}>{updating === order.id ? 'Updating…' : column.status === 'PLACED' ? 'Start preparing' : column.status === 'PREPARING' ? 'Mark ready' : 'Mark served'} <span aria-hidden="true">→</span></button> : <span className="kitchen-ticket__waiting">Waiting for pickup</span>}</footer>
                      </article>
                    )
                  })}
                  {!columnOrders.length && <div className="kitchen-column__empty">No {column.label.toLowerCase()} orders</div>}
                </div>
              </section>
            )
          })}
        </div>
      ) : orders.length ? <div className="restaurant-order-cards">
        {orders.map((order) => {
          const next = nextAction(order)
          const amount = order.items?.reduce((total, item) => total + Number(item.lineTotal), 0) || 0
          return <article className="surface-card restaurant-ticket" key={order.id}>
            <div className="restaurant-ticket-heading"><div><span className="eyebrow">ORDER #{order.id}</span><h2>{order.tableName}</h2></div><span className={`restaurant-order-status status-${order.status?.toLowerCase()}`}>{roleLabel(order.status)}</span></div>
            <p className="restaurant-ticket-meta">Opened {order.createdAt ? new Date(order.createdAt).toLocaleString() : 'just now'} · {order.waiterName}</p>
            <div className="restaurant-ticket-items">{order.items?.map((item) => <div key={item.id || `${item.menuItemId}-${item.name}`}><span><strong>{item.quantity}×</strong> {item.name}{item.notes && <small>{item.notes}</small>}</span><strong>{money(item.lineTotal)}</strong></div>)}</div>
            <div className="restaurant-ticket-footer"><strong>{money(amount)}</strong>{next && <button className="button button-primary button-small" type="button" disabled={updating === order.id} onClick={() => changeStatus(order, next)}>{updating === order.id ? 'Updating…' : `Mark ${roleLabel(next)}`}</button>}</div>
          </article>
        })}
      </div> : <div className="surface-card empty-state">{kitchenOnly ? 'No open kitchen tickets. New orders will appear here.' : 'No orders match this filter.'}</div>}
    </div>
  )
}
