import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import ConfirmDialog from '../components/ConfirmDialog'
import DataTable from '../components/DataTable'
import ErrorMessage from '../components/ErrorMessage'
import Loader from '../components/Loader'
import StatCard from '../components/StatCard'
import { useAuth } from '../context/useAuth'

const emptyStats = { totalTenants: 0, tenantsByType: {}, tenantsByStatus: {} }

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(value))
}

export default function PlatformPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [stats, setStats] = useState(emptyStats)
  const [pageData, setPageData] = useState({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })
  const [statusFilter, setStatusFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [selectedTenant, setSelectedTenant] = useState(null)
  const [saving, setSaving] = useState(false)

  const loadPlatform = useCallback(async (page = 0) => {
    setLoading(true)
    setError('')
    try {
      const [tenantResponse, statsResponse] = await Promise.all([
        api.get('/platform/tenants', {
          params: {
            page,
            size: 10,
            status: statusFilter || undefined,
            type: typeFilter || undefined,
          },
        }),
        api.get('/platform/stats'),
      ])
      setPageData(tenantResponse.data.data)
      setStats(statsResponse.data.data)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Platform information could not be loaded.')
    } finally {
      setLoading(false)
    }
  }, [statusFilter, typeFilter])

  // Reload when a filter changes; data updates asynchronously after this effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadPlatform(0) }, [loadPlatform])

  async function updateStatus() {
    if (!selectedTenant) return
    const tenant = selectedTenant
    const nextStatus = tenant.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await api.patch(`/platform/tenants/${tenant.id}/status`, { status: nextStatus })
      setSelectedTenant(null)
      setNotice(`${tenant.name} is now ${nextStatus.toLowerCase()}.`)
      await loadPlatform(pageData.page)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The tenant status could not be changed.')
    } finally {
      setSaving(false)
    }
  }

  function handleLogout() {
    logout()
    navigate('/admin/login', { replace: true })
  }

  const columns = [
    {
      key: 'name',
      label: 'Business',
      render: (tenant) => (
        <span className="table-person">
          <span className="table-avatar platform-tenant-avatar">{tenant.name?.[0]?.toUpperCase() || 'B'}</span>
          <span><strong>{tenant.name}</strong><small>{tenant.code}</small></span>
        </span>
      ),
    },
    { key: 'businessType', label: 'Type', render: (tenant) => <span className="role-chip">{tenant.businessType}</span> },
    {
      key: 'status',
      label: 'Status',
      render: (tenant) => <span className={`status-pill ${tenant.status === 'ACTIVE' ? 'is-active' : tenant.status === 'SUSPENDED' ? 'is-inactive' : 'is-pending'}`}><span />{tenant.status}</span>,
    },
    { key: 'createdAt', label: 'Created', render: (tenant) => formatDate(tenant.createdAt) },
    {
      key: 'actions',
      label: '',
      render: (tenant) => tenant.status === 'ACTIVE' || tenant.status === 'SUSPENDED'
        ? <button className={`text-button ${tenant.status === 'ACTIVE' ? 'suspend-action' : ''}`} type="button" onClick={() => setSelectedTenant(tenant)}>{tenant.status === 'ACTIVE' ? 'Suspend' : 'Reactivate'}</button>
        : <span className="table-muted">Unavailable</span>,
    },
  ]

  return (
    <div className="platform-shell">
      <header className="platform-topbar">
        <div className="platform-brand">
          <span className="brand-mark">O</span>
          <span><strong>OwnSpace</strong><small>PLATFORM CONSOLE</small></span>
        </div>
        <div className="platform-account">
          <span className="platform-admin-badge">PLATFORM ADMIN</span>
          <span className="platform-admin-name">{user?.email}</span>
          <button className="button button-secondary button-small" type="button" onClick={handleLogout}>Log out</button>
        </div>
      </header>
      <main className="platform-content">
        <div className="page-heading">
          <div><div className="eyebrow"><span className="eyebrow-dot" /> THE OWNSPACE NETWORK</div><h1>Platform overview</h1><p>See your workspaces at a glance and keep the platform running smoothly.</p></div>
          <span className="platform-live-chip"><span className="status-dot" /> Platform online</span>
        </div>
        {error && <ErrorMessage message={error} onRetry={() => loadPlatform(pageData.page)} />}
        {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
        <div className="stat-grid platform-stats">
          <StatCard label="Total workspaces" value={stats.totalTenants ?? 0} detail="Across the OwnSpace platform" icon="◈" />
          <StatCard label="Active workspaces" value={stats.tenantsByStatus?.ACTIVE ?? 0} detail="Ready for business" icon="↗" />
          <StatCard label="Suspended" value={stats.tenantsByStatus?.SUSPENDED ?? 0} detail="Currently paused" icon="Ⅱ" />
          <StatCard label="Restaurants" value={stats.tenantsByType?.RESTAURANT ?? 0} detail="Restaurant workspaces" icon="⌂" />
          <StatCard label="Retail businesses" value={stats.tenantsByType?.RETAIL ?? 0} detail="Retail workspaces" icon="▤" />
        </div>
        <section className="surface-card platform-tenants-card">
          <div className="platform-table-heading">
            <div><span className="eyebrow">WORKSPACE DIRECTORY</span><h2>Business workspaces</h2><p>Review tenant status and manage access to the platform.</p></div>
            <div className="platform-filters">
              <label className="filter-field"><span className="sr-only">Filter by status</span><select value={statusFilter} onChange={(event) => { setLoading(true); setStatusFilter(event.target.value) }}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="SUSPENDED">Suspended</option><option value="PROVISIONING">Provisioning</option><option value="FAILED">Failed</option></select></label>
              <label className="filter-field"><span className="sr-only">Filter by business type</span><select value={typeFilter} onChange={(event) => { setLoading(true); setTypeFilter(event.target.value) }}><option value="">All types</option><option value="RESTAURANT">Restaurant</option><option value="RETAIL">Retail</option></select></label>
            </div>
          </div>
          {loading ? <Loader label="Loading platform workspaces" /> : <DataTable
            columns={columns}
            rows={pageData.content || []}
            emptyMessage="No workspaces match these filters."
            page={pageData.page}
            totalPages={pageData.totalPages}
            onPageChange={(page) => { setLoading(true); loadPlatform(page) }}
          />}
          {!loading && <div className="platform-table-foot"><span>Showing {pageData.content?.length || 0} of {pageData.totalElements || 0} workspaces</span><span>Changes take effect immediately.</span></div>}
        </section>
        <footer className="platform-footer"><span>OwnSpace Platform Console</span><span>Workspaces are isolated by tenant database.</span></footer>
      </main>
      <ConfirmDialog
        open={Boolean(selectedTenant)}
        title={selectedTenant?.status === 'ACTIVE' ? 'Suspend this workspace?' : 'Reactivate this workspace?'}
        message={selectedTenant ? selectedTenant.status === 'ACTIVE'
          ? `${selectedTenant.name} will be signed out of tenant APIs and its users will not be able to log in until it is reactivated.`
          : `${selectedTenant.name} and its users will regain access to the workspace.` : ''}
        confirmLabel={saving ? 'Updating…' : selectedTenant?.status === 'ACTIVE' ? 'Suspend workspace' : 'Reactivate workspace'}
        onCancel={() => { if (!saving) setSelectedTenant(null) }}
        onConfirm={updateStatus}
      />
    </div>
  )
}
