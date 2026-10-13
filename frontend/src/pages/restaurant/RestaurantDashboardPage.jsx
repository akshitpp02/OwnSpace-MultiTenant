import { useCallback, useEffect, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import Loader from '../../components/Loader'
import StatCard from '../../components/StatCard'
import DashboardGreeting from '../../components/DashboardGreeting'
import NeedsAttention from '../../components/NeedsAttention'
import { money, errorText } from './restaurantUtils'
import { useAuth } from '../../context/useAuth'

export default function RestaurantDashboardPage() {
  const { user } = useAuth()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadDashboard = useCallback(async () => {
    try {
      const { data } = await api.get('/dashboard/restaurant')
      setDashboard(data.data)
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Restaurant dashboard could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadDashboard() }, [loadDashboard])

  if (loading) return <Loader label="Loading restaurant dashboard" />
  const topDish = dashboard?.topDishes?.[0]
  const attentionItems = dashboard?.activeOrders > 0
    ? [{ icon: '◷', title: `${dashboard.activeOrders} active ${dashboard.activeOrders === 1 ? 'order' : 'orders'}`, description: 'Check order progress and keep service moving.', to: '/restaurant/orders' }]
    : []

  return (
    <div className="content-column restaurant-content dashboard-v2">
      <DashboardGreeting eyebrow="RESTAURANT OVERVIEW" description={`Here’s how service is going${user?.businessName ? ` at ${user.businessName}` : ''}.`} onRefresh={() => { setLoading(true); loadDashboard() }} />
      <ErrorMessage message={error} onRetry={() => { setLoading(true); loadDashboard() }} />
      {dashboard && <>
        <div className="stat-grid restaurant-stats">
          <StatCard label="Today's sales" value={money(dashboard.todaysSales)} detail="Paid bills today" icon="₹" />
          <StatCard label="Orders today" value={dashboard.todaysOrderCount ?? 0} detail="All orders created today" icon="◷" />
          <StatCard label="Active orders" value={dashboard.activeOrders ?? 0} detail="Placed, preparing or ready" icon="↗" />
          <StatCard label="Best-selling dish" value={topDish?.name || '—'} detail={topDish ? `${topDish.quantitySold} sold in the last 7 days` : 'No dish sales yet'} icon="☆" />
        </div>
        <div className="dashboard-v2__body">
          <div className="dashboard-v2__charts">
            <section className="surface-card restaurant-chart-card dashboard-v2__chart-card">
              <div className="section-heading"><div><span className="eyebrow">LAST 7 DAYS</span><h2>Top dishes</h2></div></div>
              {dashboard.topDishes?.length
                ? <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={dashboard.topDishes} layout="vertical" margin={{ left: 8, right: 24 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis type="number" allowDecimals={false} tick={{ fontSize: 14 }} />
                    <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 14 }} />
                    <Tooltip />
                    <Bar dataKey="quantitySold" name="Items sold" fill="#2563EB" radius={[0, 5, 5, 0]} />
                  </BarChart>
                </ResponsiveContainer>
                : <div className="empty-state">Dish activity will appear here.</div>}
            </section>
            <section className="surface-card restaurant-chart-card dashboard-v2__chart-card">
              <div className="section-heading"><div><span className="eyebrow">TODAY</span><h2>Orders by hour</h2></div></div>
              <ResponsiveContainer width="100%" height={280}>
                <LineChart data={dashboard.ordersPerHour || []} margin={{ left: 0, right: 12 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="hour" tickFormatter={(hour) => `${String(hour).padStart(2, '0')}:00`} interval={3} tick={{ fontSize: 14 }} />
                  <YAxis allowDecimals={false} width={40} tick={{ fontSize: 14 }} />
                  <Tooltip labelFormatter={(hour) => `${String(hour).padStart(2, '0')}:00`} />
                  <Line type="monotone" dataKey="orderCount" name="Orders" stroke="#2563EB" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </section>
          </div>
          <NeedsAttention items={attentionItems} />
        </div>
      </>}
    </div>
  )
}
