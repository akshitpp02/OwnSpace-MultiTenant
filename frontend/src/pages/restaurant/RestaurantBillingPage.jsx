import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import Loader from '../../components/Loader'
import { errorText, money } from './restaurantUtils'

export default function RestaurantBillingPage() {
  const [orders, setOrders] = useState([])
  const [orderId, setOrderId] = useState('')
  const [paymentMode, setPaymentMode] = useState('CASH')
  const [bill, setBill] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadOrders = useCallback(async () => {
    try {
      const { data } = await api.get('/orders', { params: { status: 'SERVED' } })
      setOrders(data.data || [])
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Served orders could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadOrders() }, [loadOrders])

  const selected = orders.find((order) => String(order.id) === orderId)
  const subtotal = selected?.items?.reduce((sum, item) => sum + Number(item.lineTotal), 0) || 0

  async function createBill(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const { data } = await api.post(`/orders/${orderId}/bill`, { paymentMode })
      setBill(data.data)
      setNotice(`Bill #${data.data.id} created. The table is free again.`)
      setOrderId('')
      await loadOrders()
    } catch (requestError) {
      setError(errorText(requestError, 'The bill could not be created.'))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Loader label="Loading served orders" />

  return (
    <div className="content-column restaurant-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> CHECKOUT</div><h1>Billing</h1><p>Choose a served order, take payment and print the customer’s bill.</p></div><button className="button button-secondary" type="button" onClick={loadOrders}>Refresh orders</button></div>
      <ErrorMessage message={error} onRetry={loadOrders} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      <div className="restaurant-billing-layout">
        <section className="surface-card restaurant-form-card restaurant-checkout">
          <div className="section-heading"><div><span className="eyebrow">PAYMENT</span><h2>Review and pay</h2></div></div>
          <form className="form-stack" onSubmit={createBill}>
            <label className="field"><span className="field-label">Served order</span><select value={orderId} onChange={(event) => { setOrderId(event.target.value); setBill(null) }} required><option value="">Choose an order</option>{orders.map((order) => <option key={order.id} value={order.id}>#{order.id} · {order.tableName} · {money(order.items?.reduce((sum, item) => sum + Number(item.lineTotal), 0))}</option>)}</select></label>
            {selected && <div className="restaurant-checkout-summary">
              <div className="restaurant-checkout-summary__heading"><strong>Order #{selected.id} · {selected.tableName}</strong><span>{selected.items?.length || 0} {selected.items?.length === 1 ? 'dish' : 'dishes'}</span></div>
              <div className="restaurant-checkout-summary__items">{selected.items?.map((item) => <div key={item.id || `${item.menuItemId}-${item.name}`}><span><strong>{item.quantity}×</strong> {item.name}</span><strong>{money(item.lineTotal)}</strong></div>)}</div>
              <div className="restaurant-checkout-summary__subtotal"><span>Subtotal before tax</span><strong>{money(subtotal)}</strong></div>
              <p>Applicable tax is added using your business tax settings.</p>
            </div>}
            <fieldset className="restaurant-payment-picker">
              <legend>How did the customer pay?</legend>
              <div className="restaurant-payment-options">
                {[{ value: 'CASH', label: 'Cash', icon: '₹' }, { value: 'CARD', label: 'Card', icon: '▤' }, { value: 'UPI', label: 'UPI', icon: '⌁' }].map((method) => (
                  <label className={`restaurant-payment-option${paymentMode === method.value ? ' is-selected' : ''}`} key={method.value}>
                    <input type="radio" name="paymentMode" value={method.value} checked={paymentMode === method.value} onChange={() => setPaymentMode(method.value)} />
                    <span aria-hidden="true">{method.icon}</span>{method.label}
                  </label>
                ))}
              </div>
            </fieldset>
            {selected && <div className="restaurant-checkout-total"><span>Order total before tax</span><strong>{money(subtotal)}</strong></div>}
            <button className="button button-primary restaurant-checkout-submit" type="submit" disabled={!selected || saving}>{saving ? 'Creating bill…' : 'Confirm payment and create bill'}</button>
          </form>
          {!orders.length && <div className="empty-state">There are no served orders waiting for payment.</div>}
        </section>
        {bill && <section className="surface-card restaurant-bill" aria-label="Printable bill">
          <div className="restaurant-bill-header"><span className="eyebrow">PAYMENT RECEIPT</span><h2>Bill #{bill.id}</h2><p>{bill.tableName} · {new Date(bill.paidAt).toLocaleString()}</p></div>
          <div className="restaurant-ticket-items">{bill.items.map((item) => <div key={item.id || `${item.menuItemId}-${item.name}`}><span><strong>{item.quantity}×</strong> {item.name}</span><strong>{money(item.lineTotal)}</strong></div>)}</div>
          <div className="restaurant-bill-totals"><div><span>Subtotal</span><strong>{money(bill.subtotal)}</strong></div><div><span>Tax</span><strong>{money(bill.tax)}</strong></div><div className="bill-grand-total"><span>Total · {bill.paymentMode}</span><strong>{money(bill.total)}</strong></div></div>
          <button className="button button-primary print-hide" type="button" onClick={() => window.print()}>Print bill</button>
        </section>}
      </div>
    </div>
  )
}
