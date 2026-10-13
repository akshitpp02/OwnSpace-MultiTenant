import { useCallback, useEffect, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import Loader from '../../components/Loader'
import StatCard from '../../components/StatCard'
import DashboardGreeting from '../../components/DashboardGreeting'
import NeedsAttention from '../../components/NeedsAttention'
import { useAuth } from '../../context/useAuth'
import { errorText, money, responseData, responseList } from './retailUtils'

function dateLabel(value) {
  return value ? new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : ''
}

export default function RetailDashboardPage() {
  const { user } = useAuth()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    try {
      const response = await api.get('/dashboard/retail')
      setDashboard(responseData(response))
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Retail dashboard could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadDashboard() }, [loadDashboard])

  if (loading) return <Loader label="Loading retail dashboard" />

  const topProducts = responseList(dashboard?.topProducts || dashboard?.topSellingProducts).map((product) => ({
    ...product,
    name: product.name || product.productName,
    quantitySold: product.quantitySold ?? product.quantity ?? 0,
  }))
  const dailySales = responseList(dashboard?.salesPerDay || dashboard?.dailySales).map((day) => ({
    ...day,
    date: day.date || day.day,
    totalSales: day.totalSales ?? day.sales ?? day.total ?? 0,
  }))
  const lowStockCount = dashboard?.lowStockCount ?? dashboard?.lowStockProductCount ?? 0
  const canViewLowStock = ['OWNER', 'MANAGER', 'STOCK_KEEPER', 'INVENTORY_MANAGER'].includes(user?.role)
  const attentionItems = lowStockCount > 0
    ? [{
      icon: '!',
      title: `${lowStockCount} low-stock ${lowStockCount === 1 ? 'product' : 'products'}`,
      description: 'Review items at or below their restock threshold.',
      to: canViewLowStock ? '/retail/low-stock' : undefined,
    }]
    : []

  return (
    <div className="content-column retail-content dashboard-v2">
      <DashboardGreeting eyebrow="RETAIL OVERVIEW" description={`Here’s how your store is doing${user?.businessName ? ` at ${user.businessName}` : ''}.`} onRefresh={loadDashboard} loading={loading} />
      <ErrorMessage message={error} onRetry={loadDashboard} />
      {dashboard && <>
        <div className="stat-grid retail-stats">
          <StatCard label="Today's sales" value={money(dashboard.todaysSales ?? dashboard.todaySales)} detail="Completed transactions" icon="₹" />
          <StatCard label="Today's profit" value={money(dashboard.todaysProfit ?? dashboard.todayProfit)} detail="After discounts" icon="↗" />
          <StatCard label="Bills today" value={dashboard.todayBillCount ?? dashboard.todaysBillCount ?? dashboard.billCount ?? 0} detail="Completed sales" icon="▤" />
          <StatCard label="Low-stock items" value={lowStockCount} detail="At or below threshold" icon="!" />
        </div>
        <div className="dashboard-v2__body">
          <div className="dashboard-v2__charts">
            <section className="surface-card retail-chart-card dashboard-v2__chart-card">
              <div className="section-heading"><div><span className="eyebrow">LAST 7 DAYS</span><h2>Top products</h2></div></div>
              {topProducts.length ? <ResponsiveContainer width="100%" height={285}>
                <BarChart data={topProducts} layout="vertical" margin={{ left: 12, right: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                  <XAxis type="number" allowDecimals={false} tick={{ fontSize: 14 }} />
                  <YAxis type="category" dataKey="name" width={116} tick={{ fontSize: 14 }} />
                  <Tooltip />
                  <Bar dataKey="quantitySold" name="Units sold" fill="#2563EB" radius={[0, 5, 5, 0]} />
                </BarChart>
              </ResponsiveContainer> : <div className="empty-state">Product sales will appear here.</div>}
            </section>
            <section className="surface-card retail-chart-card dashboard-v2__chart-card">
              <div className="section-heading"><div><span className="eyebrow">RECENT PERFORMANCE</span><h2>Sales per day</h2></div></div>
              {dailySales.length ? <ResponsiveContainer width="100%" height={285}>
                <LineChart data={dailySales} margin={{ left: 8, right: 14 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" tickFormatter={dateLabel} tick={{ fontSize: 14 }} />
                  <YAxis tickFormatter={(value) => money(value)} width={100} tick={{ fontSize: 14 }} />
                  <Tooltip labelFormatter={(date) => dateLabel(date)} formatter={(value) => money(value)} contentStyle={{ fontSize: 14 }} />
                  <Line type="monotone" dataKey="totalSales" name="Sales" stroke="#2563EB" strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer> : <div className="empty-state">Daily sales will appear here as bills are completed.</div>}
            </section>
          </div>
          <NeedsAttention items={attentionItems} />
        </div>
      </>}
    </div>
  )
}
