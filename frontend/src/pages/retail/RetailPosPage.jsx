import { useCallback, useEffect, useMemo, useState } from 'react'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import { errorText, money, responseData, responseList, saleItems } from './retailUtils'

export default function RetailPosPage() {
  const [query, setQuery] = useState('')
  const [products, setProducts] = useState([])
  const [cart, setCart] = useState([])
  const [customers, setCustomers] = useState([])
  const [customerPhone, setCustomerPhone] = useState('')
  const [customer, setCustomer] = useState(null)
  const [discount, setDiscount] = useState('0')
  const [paymentMode, setPaymentMode] = useState('CASH')
  const [taxPercent, setTaxPercent] = useState(null)
  const [bill, setBill] = useState(null)
  const [loading, setLoading] = useState(true)
  const [searchingCustomer, setSearchingCustomer] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [taxError, setTaxError] = useState('')
  const [notice, setNotice] = useState('')

  const loadProducts = useCallback(async (search = '') => {
    setLoading(true)
    try {
      const { data } = await api.get('/products', { params: { search: search || undefined, active: true } })
      setProducts(responseList(responseData({ data })).filter((product) => product.active !== false))
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Products could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  const loadTax = useCallback(async () => {
    try {
      const { data } = await api.get('/settings/tax')
      setTaxPercent(Number(data.data?.taxPercent ?? 0))
      setTaxError('')
    } catch (requestError) {
      setTaxPercent(null)
      setTaxError(errorText(requestError, 'Tax settings could not be loaded.'))
    }
  }, [])

  useEffect(() => {
    // Fetch completion updates the page asynchronously after the effect returns.
    // oxlint-disable-next-line react/set-state-in-effect
    loadProducts()
    loadTax()
  }, [loadProducts, loadTax])

  const subtotal = useMemo(() => cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0), [cart])
  const safeDiscount = Math.min(subtotal, Math.max(0, Number(discount) || 0))
  const tax = Math.max(0, (subtotal - safeDiscount) * Number(taxPercent || 0) / 100)
  const total = subtotal - safeDiscount + tax

  function addToCart(product) {
    setBill(null)
    setCart((current) => {
      const existing = current.find((item) => item.id === product.id)
      if (existing) {
        if (existing.quantity >= Number(product.stockQty)) return current
        return current.map((item) => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item)
      }
      if (Number(product.stockQty) <= 0) return current
      return [...current, { id: product.id, name: product.name, sku: product.sku, price: Number(product.price), stockQty: Number(product.stockQty), quantity: 1 }]
    })
    setNotice(`${product.name} added to the sale.`)
  }

  function setQuantity(productId, value) {
    const product = cart.find((item) => item.id === productId)
    const next = Number(value)
    if (!product || next <= 0) {
      setCart((current) => current.filter((item) => item.id !== productId))
      return
    }
    setCart((current) => current.map((item) => item.id === productId ? { ...item, quantity: Math.min(next, item.stockQty) } : item))
  }

  function adjustQuantity(item, difference) {
    setQuantity(item.id, item.quantity + difference)
  }

  async function findCustomer(event) {
    event.preventDefault()
    if (!customerPhone.trim()) return
    setSearchingCustomer(true)
    setError('')
    setNotice('')
    try {
      const { data } = await api.get('/customers', { params: { search: customerPhone.trim() } })
      const matches = responseList(responseData({ data }))
      setCustomers(matches)
      const exact = matches.find((entry) => entry.phone === customerPhone.trim())
      if (exact) {
        setCustomer(exact)
        setNotice(`${exact.name} linked to this sale.`)
      } else {
        setCustomer(null)
        setNotice(matches.length ? 'Choose the matching customer below.' : 'No customer found for that phone number. This sale can continue without one.')
      }
    } catch (requestError) {
      setError(errorText(requestError, 'Customer lookup failed.'))
    } finally {
      setSearchingCustomer(false)
    }
  }

  async function completeSale(event) {
    event?.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const { data } = await api.post('/sales', {
        customerId: customer?.id ?? null,
        discount: safeDiscount,
        paymentMode,
        items: cart.map((item) => ({ productId: item.id, quantity: item.quantity })),
      })
      const created = responseData({ data })
      const completedBill = created
      setBill(completedBill)
      setNotice(`Sale ${completedBill?.id ? `#${completedBill.id} ` : ''}completed. Inventory has been updated.`)
      setCart([])
      setCustomer(null)
      setCustomerPhone('')
      setCustomers([])
      setDiscount('0')
      await loadProducts(query)
    } catch (requestError) {
      setError(errorText(requestError, 'The sale could not be completed. No inventory was changed.'))
    } finally {
      setSaving(false)
    }
  }

  const receiptItems = saleItems(bill)

  return (
    <div className="content-column retail-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> POINT OF SALE</div><h1>Billing</h1><p>Choose products, take payment and print a customer receipt.</p></div><button className="button button-secondary" type="button" onClick={() => { loadProducts(query); loadTax() }}>Refresh products</button></div>
      <ErrorMessage message={error || taxError} onRetry={() => { loadProducts(query); loadTax() }} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      <div className="retail-pos-layout">
        <section className="surface-card retail-product-picker">
          <div className="section-heading"><div><span className="eyebrow">SELLABLE ITEMS</span><h2>Find products</h2></div></div>
          <form className="retail-filter-form" onSubmit={(event) => { event.preventDefault(); loadProducts(query) }}><FormField label="Product name or SKU" name="posSearch" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Scan or search…" /><button className="button button-secondary button-small" type="submit">Search</button></form>
          {loading ? <Loader label="Loading products" /> : products.length ? <div className="retail-pos-products">{products.map((product) => <button className="retail-pos-product" key={product.id} type="button" disabled={Number(product.stockQty) <= 0} onClick={() => addToCart(product)}><span className="retail-pos-product-icon">＋</span><span className="retail-pos-product-copy"><strong>{product.name}</strong><small>{product.sku} · {product.stockQty} in stock</small></span><strong>{money(product.price)}</strong></button>)}</div> : <div className="empty-state">No active products match that search.</div>}
        </section>
        <section className="surface-card retail-cart-card">
          <div className="section-heading"><div><span className="eyebrow">CURRENT SALE</span><h2>Cart <span className="retail-cart-count">{cart.reduce((sum, item) => sum + item.quantity, 0)}</span></h2></div><button className="text-button retail-danger-text" type="button" disabled={!cart.length} onClick={() => { setCart([]); setBill(null) }}>Clear</button></div>
          <div className="retail-cart-list">{cart.map((item) => <div className="retail-cart-line retail-pos-cart-line" key={item.id}><span><strong>{item.name}</strong><small>{item.sku} · {money(item.price)} each</small></span><div className="retail-pos-quantity"><button type="button" onClick={() => adjustQuantity(item, -1)} aria-label={item.quantity === 1 ? `Remove ${item.name}` : `Remove one ${item.name}`}>−</button><span aria-live="polite">{item.quantity}</span><button type="button" onClick={() => adjustQuantity(item, 1)} disabled={item.quantity >= item.stockQty} aria-label={`Add one ${item.name}`}>+</button></div><strong>{money(item.price * item.quantity)}</strong><button className="text-button retail-danger-text" type="button" onClick={() => setQuantity(item.id, '0')} aria-label={`Remove ${item.name}`}>×</button></div>)}{!cart.length && <div className="empty-state">Choose a product to start a sale.</div>}</div>
          <div className="form-stack retail-checkout-form">
            <div className="retail-customer-lookup"><form className="retail-customer-search" onSubmit={findCustomer}><FormField label="Customer phone (optional)" name="customerPhone" type="tel" value={customerPhone} onChange={(event) => setCustomerPhone(event.target.value)} placeholder="Look up by phone" /><button className="button button-secondary button-small" type="submit" disabled={!customerPhone.trim() || searchingCustomer}>{searchingCustomer ? 'Searching…' : 'Find'}</button></form>
              {customer && <div className="retail-selected-customer"><span>Customer: <strong>{customer.name}</strong> · {customer.phone}</span><button className="text-button" type="button" onClick={() => setCustomer(null)}>Remove</button></div>}
              {!customer && customers.length > 0 && <label className="field"><span className="field-label">Select customer</span><select value="" onChange={(event) => setCustomer(customers.find((entry) => String(entry.id) === event.target.value) || null)}><option value="">Continue without a customer</option>{customers.map((entry) => <option key={entry.id} value={entry.id}>{entry.name} · {entry.phone}</option>)}</select></label>}
            </div>
            <FormField label="Discount" name="saleDiscount" type="number" min="0" max={subtotal} step="0.01" value={discount} onChange={(event) => setDiscount(event.target.value)} />
            <fieldset className="retail-pos-payment">
              <legend>Payment method</legend>
              <div className="retail-pos-payment__options">
                {[{ value: 'CASH', label: 'Cash', icon: '₹' }, { value: 'CARD', label: 'Card', icon: '▤' }, { value: 'UPI', label: 'UPI', icon: '⌁' }].map((method) => (
                  <label className={`retail-pos-payment__option${paymentMode === method.value ? ' is-selected' : ''}`} key={method.value}>
                    <input type="radio" name="posPaymentMode" value={method.value} checked={paymentMode === method.value} onChange={() => setPaymentMode(method.value)} />
                    <span aria-hidden="true">{method.icon}</span>{method.label}
                  </label>
                ))}
              </div>
            </fieldset>
            <div className="retail-bill-totals"><div><span>Subtotal</span><strong>{money(subtotal)}</strong></div><div><span>Discount</span><strong>−{money(safeDiscount)}</strong></div><div><span>Tax ({taxPercent ?? '…'}%)</span><strong>{money(tax)}</strong></div><div className="bill-grand-total"><span>Total due</span><strong>{money(total)}</strong></div></div>
            <button className="button button-primary retail-complete-sale" type="button" onClick={() => completeSale()} disabled={saving || taxPercent === null || !cart.length || Number(discount) > subtotal}>{saving ? 'Completing sale…' : taxPercent === null ? 'Loading tax settings…' : `Complete sale · ${money(total)}`}</button>
          </div>
        </section>
      </div>
      {bill && <section className="surface-card retail-receipt" aria-label="Printable sale bill">
        <div className="retail-receipt-header"><div><span className="eyebrow">PAYMENT RECEIPT</span><h2>Sale {bill.id ? `#${bill.id}` : 'complete'}</h2><p>{bill.createdAt ? new Date(bill.createdAt).toLocaleString() : 'Just now'} · {bill.paymentMode || paymentMode}</p></div><button className="button button-primary print-hide" type="button" onClick={() => window.print()}>Print bill</button></div>
        {receiptItems.length > 0 && <div className="retail-receipt-items">{receiptItems.map((item, index) => <div key={item.id || item.productId || index}><span>{item.quantity}× {item.productName || item.name || `Product #${item.productId}`}</span><strong>{money(item.lineTotal ?? Number(item.unitPrice || item.unit_price || 0) * Number(item.quantity || 0))}</strong></div>)}</div>}
        <div className="retail-bill-totals"><div><span>Subtotal</span><strong>{money(bill.subtotal ?? subtotal)}</strong></div><div><span>Discount</span><strong>−{money(bill.discount ?? safeDiscount)}</strong></div><div><span>Tax</span><strong>{money(bill.tax ?? tax)}</strong></div><div className="bill-grand-total"><span>Total paid</span><strong>{money(bill.total ?? total)}</strong></div></div>
      </section>}
    </div>
  )
}
