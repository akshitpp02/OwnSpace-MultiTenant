import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import Loader from '../../components/Loader'
import { errorText, money, responseData, responseList } from './retailUtils'

export default function RetailLowStockPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadLowStock = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await api.get('/products/low-stock')
      setProducts(responseList(responseData({ data })))
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Low-stock products could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadLowStock() }, [loadLowStock])

  const columns = [
    { key: 'name', label: 'Product', render: (product) => <div className="retail-product-name"><strong>{product.name}</strong><small>{product.sku}</small></div> },
    { key: 'categoryName', label: 'Category', render: (product) => product.categoryName || product.category?.name || 'Uncategorized' },
    { key: 'stockQty', label: 'Current stock', render: (product) => <span className="retail-stock-count is-low">{product.stockQty}</span> },
    { key: 'lowStockThreshold', label: 'Alert level' },
    { key: 'price', label: 'Selling price', render: (product) => money(product.price) },
  ]

  return (
    <div className="content-column retail-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot retail-alert-dot" /> INVENTORY ALERTS</div><h1>Low stock</h1><p>Products at or below their configured replenishment threshold.</p></div><button className="button button-secondary" type="button" onClick={loadLowStock}>Refresh</button></div>
      <ErrorMessage message={error} onRetry={loadLowStock} />
      <section className="surface-card retail-table-card">
        <div className="retail-alert-summary"><strong>{products.length}</strong><span>{products.length === 1 ? 'product needs' : 'products need'} attention</span></div>
        {loading ? <Loader label="Loading low-stock products" /> : <DataTable columns={columns} rows={products} emptyMessage="All clear — no products are below their stock threshold." />}
      </section>
    </div>
  )
}
