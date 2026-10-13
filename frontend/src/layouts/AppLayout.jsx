import { useEffect, useMemo, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import api from '../api/axios'
import Button from '../components/Button'
import ErrorMessage from '../components/ErrorMessage'
import FormField from '../components/FormField'
import Modal from '../components/Modal'
import { useAuth } from '../context/useAuth'

function initials(name = '') {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'U'
}

function readableRole(role = '') {
  return role.toLowerCase().split('_').map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(' ')
}

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '' })
  const [passwordSaving, setPasswordSaving] = useState(false)
  const [passwordError, setPasswordError] = useState('')
  const [passwordNotice, setPasswordNotice] = useState('')

  const isOwnerOrManager = ['OWNER', 'MANAGER'].includes(user?.role)
  const isRestaurant = user?.businessType === 'RESTAURANT'
  const isRetail = user?.businessType === 'RETAIL'
  const canTakeOrders = isOwnerOrManager || user?.role === 'WAITER'
  const canWorkKitchen = isOwnerOrManager || ['KITCHEN', 'KITCHEN_STAFF'].includes(user?.role)
  const canBill = isOwnerOrManager || user?.role === 'CASHIER'
  const canUseRetailPos = isOwnerOrManager || user?.role === 'CASHIER'
  const canManageRetailStock = isOwnerOrManager || user?.role === 'STOCK_KEEPER'

  const groups = useMemo(() => [
    {
      label: 'Daily work',
      links: [
        { to: '/dashboard', label: 'Overview', icon: '◫' },
        ...(isRestaurant && canTakeOrders ? [{ to: '/restaurant/new-order', label: 'New order', icon: '＋' }] : []),
        ...(isRestaurant ? [{ to: '/restaurant/orders', label: 'Orders', icon: '◷' }] : []),
        ...(isRestaurant && canWorkKitchen ? [{ to: '/restaurant/kitchen', label: 'Kitchen', icon: '♨' }] : []),
        ...(isRestaurant && canBill ? [{ to: '/restaurant/billing', label: 'Billing', icon: '₹' }] : []),
        ...(isRetail && canUseRetailPos ? [{ to: '/retail/pos', label: 'Point of sale', icon: '₹' }] : []),
        ...(isRetail && canUseRetailPos ? [{ to: '/retail/sales', label: 'Sales history', icon: '◷' }] : []),
      ],
    },
    {
      label: 'Manage',
      links: [
        ...(isRestaurant && isOwnerOrManager ? [{ to: '/restaurant/menu', label: 'Menu', icon: '☷' }] : []),
        ...(isRestaurant ? [{ to: '/restaurant/tables', label: 'Tables', icon: '▦' }] : []),
        ...(isRetail ? [{ to: '/retail/products', label: 'Products', icon: '▦' }] : []),
        ...(isRetail && canManageRetailStock ? [{ to: '/retail/low-stock', label: 'Low stock', icon: '!' }] : []),
        ...(isRetail && canUseRetailPos ? [{ to: '/retail/customers', label: 'Customers', icon: '♙' }] : []),
        ...(isRetail && canManageRetailStock ? [{ to: '/retail/suppliers', label: 'Suppliers', icon: '⇄' }] : []),
        ...(isRetail && canManageRetailStock ? [{ to: '/retail/stock-in', label: 'Stock in', icon: '＋' }] : []),
        ...(isRetail && canManageRetailStock ? [{ to: '/retail/stock-adjustments', label: 'Stock movements', icon: '↕' }] : []),
        ...(isOwnerOrManager ? [{ to: '/users', label: 'Team members', icon: '♙' }] : []),
      ],
    },
    {
      label: 'Settings',
      links: user?.role === 'OWNER' ? [{ to: '/settings', label: 'Business settings', icon: '⚙' }] : [],
    },
  ], [canBill, canManageRetailStock, canTakeOrders, canUseRetailPos, canWorkKitchen, isOwnerOrManager, isRestaurant, isRetail, user?.role])

  const pageTitle = groups.flatMap((group) => group.links).find((item) => item.to === location.pathname)?.label || 'Workspace'

  useEffect(() => {
    // Router location changes are external navigation events; close any transient shell menus.
    // oxlint-disable-next-line react/set-state-in-effect
    setMobileMenuOpen(false)
    setUserMenuOpen(false)
  }, [location.pathname])

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  async function changePassword(event) {
    event.preventDefault()
    setPasswordError('')
    setPasswordNotice('')
    setPasswordSaving(true)
    try {
      const { data } = await api.post('/auth/change-password', passwordForm)
      setPasswordNotice(data.message || 'Password changed successfully.')
      setPasswordForm({ currentPassword: '', newPassword: '' })
      setPasswordModalOpen(false)
    } catch (requestError) {
      setPasswordError(requestError.response?.data?.message || 'Password could not be changed. Please try again.')
    } finally {
      setPasswordSaving(false)
    }
  }

  function openPasswordModal() {
    setUserMenuOpen(false)
    setPasswordError('')
    setPasswordNotice('')
    setPasswordModalOpen(true)
  }

  return (
    <div className={`workspace-shell${isRetail ? ' retail-workspace' : ''}${sidebarCollapsed ? ' workspace-shell--collapsed' : ''}${mobileMenuOpen ? ' workspace-shell--menu-open' : ''}`}>
      {mobileMenuOpen && (
        <button
          className="workspace-scrim"
          type="button"
          aria-label="Close navigation menu"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}
      <aside className={`sidebar workspace-sidebar${sidebarCollapsed ? ' workspace-sidebar--collapsed' : ''}${mobileMenuOpen ? ' workspace-sidebar--open' : ''}`}>
        <div className="workspace-sidebar__brand-row">
          <NavLink className="brand-lockup" to="/dashboard" onClick={() => setMobileMenuOpen(false)}>
            <span className="brand-mark">M</span>
            {!sidebarCollapsed && <span><strong>OwnSpace</strong><small>BUSINESS SUITE</small></span>}
          </NavLink>
          <button
            className="workspace-sidebar__collapse"
            type="button"
            aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-expanded={!sidebarCollapsed}
            onClick={() => setSidebarCollapsed((collapsed) => !collapsed)}
          >
            {sidebarCollapsed ? '›' : '‹'}
          </button>
        </div>
        {!sidebarCollapsed && (
          <>
            <div className="workspace-sidebar__business">
              <span className="business-avatar">{initials(user?.businessName)}</span>
              <span className="business-meta"><strong>{user?.businessName || 'Your workspace'}</strong><small>{user?.businessType || 'Platform account'}</small></span>
            </div>
            <nav className="side-nav workspace-nav" aria-label="Main navigation">
              {groups.filter((group) => group.links.length > 0).map((group) => (
                <section className="workspace-nav__group" key={group.label} aria-label={group.label}>
                  <h2 className="workspace-nav__heading">{group.label}</h2>
                  {group.links.map((link) => (
                    <NavLink
                      className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
                      key={link.to}
                      to={link.to}
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      <span className="nav-icon" aria-hidden="true">{link.icon}</span>
                      <span>{link.label}</span>
                    </NavLink>
                  ))}
                </section>
              ))}
            </nav>
            <div className="workspace-sidebar__footer">
              <span className="workspace-sidebar__footer-mark" aria-hidden="true">✓</span>
              <span><strong>Ready for business</strong><small>Your workspace is up to date.</small></span>
            </div>
          </>
        )}
        {sidebarCollapsed && (
          <nav className="workspace-nav workspace-nav--icons" aria-label="Main navigation">
            {groups.flatMap((group) => group.links).map((link) => (
              <NavLink
                className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
                key={link.to}
                to={link.to}
                title={link.label}
                aria-label={link.label}
              >
                <span className="nav-icon" aria-hidden="true">{link.icon}</span>
                <span className="workspace-nav__collapsed-label">{link.label}</span>
              </NavLink>
            ))}
          </nav>
        )}
      </aside>

      <main className="main-panel">
        <header className="topbar workspace-topbar">
          <div className="workspace-topbar__leading">
            <button
              className="workspace-menu-toggle"
              type="button"
              aria-label={mobileMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
              aria-expanded={mobileMenuOpen}
              onClick={() => setMobileMenuOpen((open) => !open)}
            >
              <span /><span /><span />
            </button>
            <div className="workspace-topbar__business">
              <span className="workspace-topbar__business-label">Workspace</span>
              <strong>{user?.businessName || 'Your workspace'}</strong>
            </div>
          </div>
          <div className="workspace-topbar__trailing">
            <span className="workspace-topbar__page">{pageTitle}</span>
            <div className="workspace-user">
              <button
                className="workspace-user__trigger"
                type="button"
                aria-expanded={userMenuOpen}
                aria-haspopup="menu"
                onClick={() => setUserMenuOpen((open) => !open)}
              >
                <span className="avatar-small">{initials(user?.name)}</span>
                <span className="workspace-user__identity">
                  <strong>{user?.name}</strong>
                  <small>{readableRole(user?.role)}</small>
                </span>
                <span aria-hidden="true">⌄</span>
              </button>
              {userMenuOpen && (
                <>
                  <button className="workspace-user__dismiss" type="button" aria-label="Close user menu" onClick={() => setUserMenuOpen(false)} />
                  <div className="workspace-user__menu" role="menu">
                    <div className="workspace-user__menu-identity">
                      <strong>{user?.name}</strong>
                      <span>{readableRole(user?.role)}</span>
                    </div>
                    {user?.role !== 'PLATFORM_ADMIN' && (
                      <button type="button" role="menuitem" onClick={openPasswordModal}>Change password</button>
                    )}
                    <button className="workspace-user__logout" type="button" role="menuitem" onClick={handleLogout}>Sign out</button>
                  </div>
                </>
              )}
            </div>
          </div>
        </header>
        {passwordNotice && (
          <div className="workspace-password-notice" role="status">
            {passwordNotice}
            <button type="button" aria-label="Dismiss notification" onClick={() => setPasswordNotice('')}>×</button>
          </div>
        )}
        <div className="page-content"><Outlet /></div>
      </main>

      <Modal
        open={passwordModalOpen}
        title="Change password"
        onClose={() => setPasswordModalOpen(false)}
        className="workspace-password-modal"
      >
        <p className="workspace-password-modal__intro">Choose a new password for your account.</p>
        <ErrorMessage message={passwordError} />
        <form className="form-stack" onSubmit={changePassword}>
          <FormField
            label="Current password"
            name="currentPassword"
            type="password"
            autoComplete="current-password"
            value={passwordForm.currentPassword}
            onChange={(event) => setPasswordForm((form) => ({ ...form, currentPassword: event.target.value }))}
            required
          />
          <FormField
            label="New password"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            value={passwordForm.newPassword}
            onChange={(event) => setPasswordForm((form) => ({ ...form, newPassword: event.target.value }))}
            help="Use at least 8 characters."
            required
          />
          <div className="ui-modal__actions">
            <Button variant="secondary" onClick={() => setPasswordModalOpen(false)}>Cancel</Button>
            <Button type="submit" loading={passwordSaving}>Update password</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
