import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import Loader from '../../components/Loader'
import { errorText, money } from './restaurantUtils'

export default function RestaurantNewOrderPage() {
  const navigate = useNavigate()
  const [tables, setTables] = useState([])
  const [items, setItems] = useState([])
  const [tableId, setTableId] = useState('')
  const [cart, setCart] = useState({})
  const [notes, setNotes] = useState({})
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('All')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const loadData = useCallback(async () => {
    try {
      const [tableResponse, menuResponse] = await Promise.all([
        api.get('/tables'),
        api.get('/menu-items', { params: { size: 100 } }),
      ])
      setTables((tableResponse.data.data || []).filter((table) => table.status === 'FREE'))
      setItems((menuResponse.data.data.content || []).filter((item) => item.available))
    } catch (requestError) {
      setError(errorText(requestError, 'Tables and menu could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadData() }, [loadData])

  function setQuantity(id, quantity) {
    setCart((current) => {
      const next = { ...current }
      const count = Number(quantity)
      if (count > 0) next[id] = count
      else delete next[id]
      return next
    })
  }

  function adjustQuantity(id, difference) {
    setQuantity(id, (cart[id] || 0) + difference)
  }

  const selected = items.filter((item) => cart[item.id])
  const categories = ['All', ...new Set(items.map((item) => item.categoryName).filter(Boolean))]
  const visibleItems = items.filter((item) => (
    (categoryFilter === 'All' || item.categoryName === categoryFilter)
    && item.name.toLowerCase().includes(search.trim().toLowerCase())
  ))
  const total = selected.reduce((sum, item) => sum + Number(item.price) * cart[item.id], 0)

  async function submitOrder(event) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      await api.post('/orders', {
        tableId: Number(tableId),
        items: selected.map((item) => ({
          menuItemId: item.id,
          quantity: cart[item.id],
          notes: notes[item.id]?.trim() || null,
        })),
      })
      navigate('/restaurant/orders', { state: { notice: 'Order sent to the kitchen.' } })
    } catch (requestError) {
      setError(errorText(requestError, 'The order could not be placed.'))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Loader label="Preparing a new order" />

  return (
    <div className="content-column restaurant-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> WAITER WORKSPACE</div><h1>Start an order</h1><p>Choose a free table, then add dishes to the order.</p></div></div>
      <ErrorMessage message={error} onRetry={() => { setLoading(true); loadData() }} />
      <form className="restaurant-order-workspace" onSubmit={submitOrder}>
        <section className="surface-card restaurant-form-card restaurant-order-table-picker">
          <label className="field"><span className="field-label">Table for this order</span><select value={tableId} onChange={(event) => setTableId(event.target.value)} required><option value="">Choose a free table</option>{tables.map((table) => <option key={table.id} value={table.id}>{table.name} · {table.capacity} seats</option>)}</select></label>
          {!tables.length && <p className="restaurant-order-help">No free tables are available right now.</p>}
        </section>
        <div className="restaurant-order-layout">
          <section className="surface-card restaurant-form-card restaurant-order-menu-panel">
            <div className="section-heading"><div><span className="eyebrow">AVAILABLE MENU</span><h2>Choose dishes</h2></div></div>
            <label className="restaurant-order-search"><span aria-hidden="true">⌕</span><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search dishes" aria-label="Search available dishes" /></label>
            <div className="restaurant-order-categories" role="tablist" aria-label="Dish categories">
              {categories.map((category) => <button className={`restaurant-order-category${categoryFilter === category ? ' is-active' : ''}`} type="button" role="tab" aria-selected={categoryFilter === category} key={category} onClick={() => setCategoryFilter(category)}>{category}</button>)}
            </div>
            <div className="restaurant-order-menu">
              {visibleItems.map((item) => <article className="restaurant-order-menu-row" key={item.id}>
                <div className="restaurant-order-item-copy"><strong>{item.name}</strong><span>{item.categoryName} · {money(item.price)}</span></div>
                <div className="restaurant-quantity-stepper" aria-label={`Quantity for ${item.name}`}>
                  <button type="button" onClick={() => adjustQuantity(item.id, -1)} disabled={!cart[item.id]} aria-label={`Remove one ${item.name}`}>−</button>
                  <span aria-live="polite">{cart[item.id] || 0}</span>
                  <button type="button" onClick={() => adjustQuantity(item.id, 1)} aria-label={`Add one ${item.name}`}>+</button>
                </div>
              </article>)}
              {!items.length && <div className="empty-state">No available menu items. Ask a manager to update the menu.</div>}
              {items.length > 0 && !visibleItems.length && <div className="empty-state">No dishes match your search.</div>}
            </div>
          </section>
          <aside className="surface-card restaurant-form-card restaurant-order-cart">
            <div className="section-heading"><div><span className="eyebrow">CURRENT ORDER</span><h2>{tables.find((table) => String(table.id) === tableId)?.name || 'Select a table'}</h2></div><span className="restaurant-order-cart-count">{selected.reduce((count, item) => count + cart[item.id], 0)}</span></div>
            <div className="restaurant-order-cart-items">
              {selected.map((item) => <div className="restaurant-order-cart-row" key={item.id}>
                <div className="restaurant-order-cart-copy"><strong>{item.name}</strong><span>{money(item.price)} each</span></div>
                <div className="restaurant-quantity-stepper restaurant-quantity-stepper--small" aria-label={`Quantity for ${item.name}`}>
                  <button type="button" onClick={() => adjustQuantity(item.id, -1)} aria-label={`Remove one ${item.name}`}>−</button>
                  <span>{cart[item.id]}</span>
                  <button type="button" onClick={() => adjustQuantity(item.id, 1)} aria-label={`Add one ${item.name}`}>+</button>
                </div>
                <strong className="restaurant-order-line-total">{money(Number(item.price) * cart[item.id])}</strong>
                <input className="restaurant-note-input" aria-label={`Notes for ${item.name}`} value={notes[item.id] || ''} onChange={(event) => setNotes((current) => ({ ...current, [item.id]: event.target.value }))} placeholder="Add preparation note (optional)" maxLength="200" />
              </div>)}
              {!selected.length && <div className="restaurant-order-cart-empty"><span aria-hidden="true">＋</span><strong>Your order is empty</strong><p>Add dishes from the menu to get started.</p></div>}
            </div>
            <div className="restaurant-order-summary"><span>{selected.reduce((count, item) => count + cart[item.id], 0)} items</span><strong>{money(total)}</strong></div>
            <button className="button button-primary restaurant-order-place" type="submit" disabled={saving || !tableId || !selected.length}>{saving ? 'Sending order…' : 'Place order'}</button>
          </aside>
        </div>
      </form>
    </div>
  )
}
