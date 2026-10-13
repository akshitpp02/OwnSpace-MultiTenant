import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import { errorText, money, responseData, responseList } from './retailUtils'

export default function RetailStockInPage() {
  const [suppliers, setSuppliers] = useState([])
  const [products, setProducts] = useState([])
  const [purchases, setPurchases] = useState([])
  const [supplierId, setSupplierId] = useState('')
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [unitCost, setUnitCost] = useState('')
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [supplierResponse, productResponse, purchaseResponse] = await Promise.all([
        api.get('/suppliers'),
        api.get('/products', { params: { active: true } }),
        api.get('/purchases'),
      ])
      setSuppliers(responseList(responseData(supplierResponse)))
      setProducts(responseList(responseData(productResponse)))
      setPurchases(responseList(responseData(purchaseResponse)))
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Stock-in data could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadData() }, [loadData])

  function addItem(event) {
    event.preventDefault()
    const product = products.find((item) => String(item.id) === productId)
    if (!product) return
    const amount = Number(quantity)
    const cost = Number(unitCost === '' ? product.costPrice ?? product.cost_price ?? 0 : unitCost)
    setItems((current) => {
      const existing = current.find((item) => item.productId === product.id)
      if (existing) return current.map((item) => item.productId === product.id ? { ...item, quantity: item.quantity + amount, unitCost: cost } : item)
      return [...current, { productId: product.id, name: product.name, sku: product.sku, quantity: amount, unitCost: cost }]
    })
    setProductId('')
    setQuantity('1')
    setUnitCost('')
  }

  async function receiveStock(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const { data } = await api.post('/purchases', {
        supplierId: Number(supplierId),
        items: items.map(({ productId: id, quantity: amount, unitCost: cost }) => ({ productId: id, quantity: amount, unitCost: cost })),
      })
      const purchase = responseData({ data })
      setNotice(`Stock received${purchase?.id ? ` in purchase #${purchase.id}` : ''}.`)
      setItems([])
      setSupplierId('')
      await loadData()
    } catch (requestError) {
      setError(errorText(requestError, 'Stock could not be received.'))
    } finally {
      setSaving(false)
    }
  }

  const historyColumns = [
    { key: 'id', label: 'Purchase' , render: (purchase) => `#${purchase.id}` },
    { key: 'supplierName', label: 'Supplier', render: (purchase) => purchase.supplierName || purchase.supplier?.name || `Supplier #${purchase.supplierId}` },
    { key: 'totalCost', label: 'Total cost', render: (purchase) => money(purchase.totalCost) },
    { key: 'createdAt', label: 'Received', render: (purchase) => purchase.createdAt ? new Date(purchase.createdAt).toLocaleString() : '—' },
  ]

  const selectedProduct = products.find((product) => String(product.id) === productId)

  return (
    <div className="content-column retail-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> INVENTORY</div><h1>Stock in</h1><p>Record supplier deliveries and update inventory in one transaction.</p></div><button className="button button-secondary" type="button" onClick={loadData}>Refresh</button></div>
      <ErrorMessage message={error} onRetry={loadData} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {loading ? <Loader label="Loading stock-in options" /> : <div className="retail-stock-layout">
        <section className="surface-card retail-form-card">
          <div className="section-heading"><div><span className="eyebrow">NEW DELIVERY</span><h2>Receive products</h2></div></div>
          <form className="form-stack" onSubmit={receiveStock}>
            <label className="field"><span className="field-label">Supplier</span><select value={supplierId} onChange={(event) => setSupplierId(event.target.value)} required><option value="">Choose a supplier</option>{suppliers.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}</select></label>
            {!suppliers.length && <p className="retail-hint">Add a supplier before recording a delivery.</p>}
            <div className="retail-add-stock-row">
              <label className="field"><span className="field-label">Product</span><select value={productId} onChange={(event) => { setProductId(event.target.value); const product = products.find((item) => String(item.id) === event.target.value); setUnitCost(String(product?.costPrice ?? product?.cost_price ?? '')) }}><option value="">Choose a product</option>{products.map((product) => <option key={product.id} value={product.id}>{product.name} · {product.sku}</option>)}</select></label>
              <FormField label="Quantity" name="stockInQuantity" type="number" min="1" step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} required />
              <FormField label="Unit cost" name="stockInCost" type="number" min="0" step="0.01" value={unitCost} onChange={(event) => setUnitCost(event.target.value)} required />
              <button className="button button-secondary button-small" type="button" disabled={!selectedProduct || Number(quantity) < 1 || Number(unitCost) < 0} onClick={addItem}>Add item</button>
            </div>
            <div className="retail-cart-list">{items.map((item) => <div className="retail-cart-line" key={item.productId}><span><strong>{item.name}</strong><small>{item.quantity} units × {money(item.unitCost)}</small></span><strong>{money(item.quantity * item.unitCost)}</strong><button className="text-button retail-danger-text" type="button" onClick={() => setItems((current) => current.filter((line) => line.productId !== item.productId))}>Remove</button></div>)}{!items.length && <div className="empty-state">Add one or more products to this delivery.</div>}</div>
            <div className="retail-total-preview"><span>Delivery total</span><strong>{money(items.reduce((sum, item) => sum + item.quantity * item.unitCost, 0))}</strong></div>
            <button className="button button-primary" type="submit" disabled={saving || !supplierId || !items.length}>{saving ? 'Receiving stock…' : 'Receive stock'}</button>
          </form>
        </section>
        <section className="surface-card retail-table-card retail-purchase-history"><div className="section-heading"><div><span className="eyebrow">RECENT ACTIVITY</span><h2>Recent deliveries</h2></div></div><DataTable columns={historyColumns} rows={purchases.slice(0, 8)} emptyMessage="No stock deliveries recorded yet." /></section>
      </div>}
    </div>
  )
}
