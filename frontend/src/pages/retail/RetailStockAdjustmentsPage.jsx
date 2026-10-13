import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import { errorText, dateTime, responseData, responseList } from './retailUtils'

export default function RetailStockAdjustmentsPage() {
  const [products, setProducts] = useState([])
  const [movements, setMovements] = useState([])
  const [productId, setProductId] = useState('')
  const [filterProductId, setFilterProductId] = useState('')
  const [quantityChange, setQuantityChange] = useState('')
  const [note, setNote] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [page, setPage] = useState(0)
  const [pageInfo, setPageInfo] = useState(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [productResponse, movementResponse] = await Promise.all([
        api.get('/products'),
        api.get('/stock/movements', { params: { productId: filterProductId || undefined, page } }),
      ])
      const movementPayload = responseData(movementResponse)
      setProducts(responseList(responseData(productResponse)))
      setMovements(responseList(movementPayload))
      setPageInfo(movementPayload && !Array.isArray(movementPayload) ? movementPayload : null)
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Stock and movement data could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [filterProductId, page])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadData() }, [loadData])

  async function adjustStock(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await api.post('/stock/adjust', { productId: Number(productId), changeQty: Number(quantityChange), note: note.trim() })
      setNotice('Stock adjustment saved and movement recorded.')
      setProductId('')
      setQuantityChange('')
      setNote('')
      await loadData()
    } catch (requestError) {
      setError(errorText(requestError, 'Stock adjustment could not be saved.'))
    } finally {
      setSaving(false)
    }
  }

  const columns = [
    { key: 'createdAt', label: 'Date', render: (movement) => dateTime(movement.createdAt) },
    { key: 'productName', label: 'Product', render: (movement) => movement.productName || movement.product?.name || `Product #${movement.productId}` },
    { key: 'reason', label: 'Reason', render: (movement) => <span className="retail-status-pill">{movement.reason}</span> },
    { key: 'changeQty', label: 'Change', render: (movement) => <strong className={Number(movement.changeQty) < 0 ? 'retail-negative-change' : 'retail-positive-change'}>{Number(movement.changeQty) > 0 ? '+' : ''}{movement.changeQty}</strong> },
    { key: 'note', label: 'Note', render: (movement) => movement.note || '—' },
    { key: 'createdByName', label: 'By', render: (movement) => movement.createdByName || movement.createdBy || '—' },
  ]

  return (
    <div className="content-column retail-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> INVENTORY CONTROL</div><h1>Stock adjustments</h1><p>Record damaged, lost or counted stock and review the movement ledger.</p></div><button className="button button-secondary" type="button" onClick={loadData}>Refresh</button></div>
      <ErrorMessage message={error} onRetry={loadData} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      <div className="retail-stock-layout">
        <section className="surface-card retail-form-card">
          <div className="section-heading"><div><span className="eyebrow">MANUAL CHANGE</span><h2>Adjust quantity</h2></div></div>
          <form className="form-stack" onSubmit={adjustStock}>
            <label className="field"><span className="field-label">Product</span><select value={productId} onChange={(event) => setProductId(event.target.value)} required><option value="">Choose a product</option>{products.map((product) => <option key={product.id} value={product.id}>{product.name} · {product.sku} ({product.stockQty} in stock)</option>)}</select></label>
            <FormField label="Quantity change" name="quantityChange" type="number" step="1" value={quantityChange} onChange={(event) => setQuantityChange(event.target.value)} required placeholder="Use a minus sign to remove units" help="Positive adds stock; negative removes stock. Inventory cannot go below zero." />
            <FormField label="Reason note" name="adjustmentNote" value={note} onChange={(event) => setNote(event.target.value)} required placeholder="e.g. Damaged in storage or cycle count" rows={3} />
            <button className="button button-primary" type="submit" disabled={saving || !quantityChange || Number(quantityChange) === 0 || !note.trim()}>{saving ? 'Saving adjustment…' : 'Save adjustment'}</button>
          </form>
        </section>
        <section className="surface-card retail-table-card retail-movements-card">
          <div className="retail-table-toolbar"><div className="section-heading"><div><span className="eyebrow">AUDIT TRAIL</span><h2>Stock movements</h2></div></div><label className="field retail-inline-filter"><span className="field-label">Product</span><select value={filterProductId} onChange={(event) => { setPage(0); setFilterProductId(event.target.value) }}><option value="">All products</option>{products.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}</select></label></div>
          {loading ? <Loader label="Loading stock movements" /> : <DataTable columns={columns} rows={movements} emptyMessage="No stock movements match this filter." />}
          {pageInfo && Number(pageInfo.totalPages) > 1 && <div className="pagination"><span>Page {page + 1} of {pageInfo.totalPages}</span><div><button className="button button-secondary button-small" type="button" disabled={page <= 0} onClick={() => setPage((current) => current - 1)}>Previous</button><button className="button button-secondary button-small" type="button" disabled={page + 1 >= Number(pageInfo.totalPages)} onClick={() => setPage((current) => current + 1)}>Next</button></div></div>}
        </section>
      </div>
    </div>
  )
}
