import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import { errorText, money, responseData, responseList, saleItems } from './retailUtils'

export default function RetailSalesPage() {
  const [filters, setFilters] = useState({ from: '', to: '' })
  const [sales, setSales] = useState([])
  const [selected, setSelected] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadingBill, setLoadingBill] = useState(false)
  const [error, setError] = useState('')
  const [pageInfo, setPageInfo] = useState(null)
  const [page, setPage] = useState(0)

  const loadSales = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await api.get('/sales', { params: { fromDate: filters.from || undefined, toDate: filters.to || undefined, page } })
      const payload = responseData({ data })
      setSales(responseList(payload))
      setPageInfo(payload && !Array.isArray(payload) ? payload : null)
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Sales history could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [filters, page])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadSales() }, [loadSales])

  async function openBill(sale) {
    setLoadingBill(true)
    setError('')
    try {
      const { data } = await api.get(`/sales/${sale.id}`)
      setSelected(responseData({ data }) || sale)
    } catch (requestError) {
      setError(errorText(requestError, 'The sale bill could not be opened.'))
    } finally {
      setLoadingBill(false)
    }
  }

  const columns = [
    { key: 'id', label: 'Bill', render: (sale) => `#${sale.id}` },
    { key: 'createdAt', label: 'Date', render: (sale) => sale.createdAt ? new Date(sale.createdAt).toLocaleString() : '—' },
    { key: 'customerName', label: 'Customer', render: (sale) => sale.customerName || sale.customer?.name || 'Walk-in' },
    { key: 'paymentMode', label: 'Payment', render: (sale) => sale.paymentMode || '—' },
    { key: 'total', label: 'Total', render: (sale) => money(sale.total) },
    { key: 'view', label: '', render: (sale) => <button className="text-button" type="button" onClick={() => openBill(sale)}>View bill</button> },
  ]
  const items = saleItems(selected)

  return (
    <div className="content-column retail-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> TRANSACTIONS</div><h1>Sales history</h1><p>Filter completed sales and open a printable copy of any bill.</p></div><button className="button button-secondary" type="button" onClick={loadSales}>Refresh</button></div>
      <ErrorMessage message={error} onRetry={loadSales} />
      <section className="surface-card retail-table-card">
        <form className="retail-filter-form" onSubmit={(event) => { event.preventDefault(); setPage(0); loadSales() }}>
          <FormField label="From" name="salesFrom" type="date" value={filters.from} onChange={(event) => setFilters((current) => ({ ...current, from: event.target.value }))} />
          <FormField label="To" name="salesTo" type="date" value={filters.to} onChange={(event) => setFilters((current) => ({ ...current, to: event.target.value }))} />
          <button className="button button-secondary button-small" type="submit">Apply dates</button>
        </form>
        {loading ? <Loader label="Loading sales history" /> : <>
          <DataTable columns={columns} rows={sales} emptyMessage="No sales found for this date range." />
          {pageInfo && Number(pageInfo.totalPages) > 1 && <div className="pagination"><span>Page {page + 1} of {pageInfo.totalPages}</span><div><button className="button button-secondary button-small" type="button" disabled={page <= 0} onClick={() => setPage((current) => current - 1)}>Previous</button><button className="button button-secondary button-small" type="button" disabled={page + 1 >= Number(pageInfo.totalPages)} onClick={() => setPage((current) => current + 1)}>Next</button></div></div>}
        </>}
      </section>
      {selected && <section className="surface-card retail-receipt" aria-label="Selected sale bill">
        <div className="retail-receipt-header"><div><span className="eyebrow">SALE DETAILS</span><h2>Bill #{selected.id}</h2><p>{selected.createdAt ? new Date(selected.createdAt).toLocaleString() : '—'} · {selected.customerName || selected.customer?.name || 'Walk-in'} · {selected.paymentMode}</p></div><div className="retail-receipt-actions"><button className="button button-secondary button-small" type="button" onClick={() => setSelected(null)}>Close</button><button className="button button-primary button-small print-hide" type="button" onClick={() => window.print()}>Print bill</button></div></div>
        {loadingBill ? <Loader label="Loading bill details" /> : <>
          <div className="retail-receipt-items">{items.map((item, index) => <div key={item.id || index}><span>{item.quantity}× {item.productName || item.name || `Product #${item.productId}`}</span><strong>{money(item.lineTotal ?? Number(item.unitPrice || item.unit_price || 0) * Number(item.quantity || 0))}</strong></div>)}</div>
          <div className="retail-bill-totals"><div><span>Subtotal</span><strong>{money(selected.subtotal)}</strong></div><div><span>Discount</span><strong>−{money(selected.discount)}</strong></div><div><span>Tax</span><strong>{money(selected.tax)}</strong></div><div className="bill-grand-total"><span>Total</span><strong>{money(selected.total)}</strong></div></div>
        </>}
      </section>}
    </div>
  )
}
