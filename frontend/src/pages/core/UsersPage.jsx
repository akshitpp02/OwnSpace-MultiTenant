import { useCallback, useEffect, useMemo, useState } from 'react'
import api from '../../api/axios'
import ConfirmDialog from '../../components/ConfirmDialog'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import { useAuth } from '../../context/useAuth'

const emptyForm = { name: '', email: '', role: 'CASHIER', password: '' }

function errorText(error) {
  return error.response?.data?.message || 'The request could not be completed. Please try again.'
}

export default function UsersPage() {
  const { user } = useAuth()
  const [result, setResult] = useState({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })
  const [query, setQuery] = useState('')
  const [appliedQuery, setAppliedQuery] = useState('')
  const [form, setForm] = useState(emptyForm)
  const [editing, setEditing] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [confirmUser, setConfirmUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadUsers = useCallback(async (page = 0, search = appliedQuery) => {
    try {
      const { data } = await api.get('/users', { params: { search: search || undefined, page, size: 10 } })
      setResult(data.data)
    } catch (requestError) {
      setError(errorText(requestError))
    } finally {
      setLoading(false)
    }
  }, [appliedQuery])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadUsers(0) }, [loadUsers])

  const roleOptions = useMemo(() => {
    const roles = user?.businessType === 'RESTAURANT'
      ? ['MANAGER', 'WAITER', 'KITCHEN', 'KITCHEN_STAFF', 'CASHIER']
      : ['MANAGER', 'CASHIER', 'INVENTORY_MANAGER']
    if (user?.role === 'OWNER') roles.unshift('OWNER')
    return roles.map((role) => ({ value: role, label: role.replaceAll('_', ' ') }))
  }, [user])

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setError('')
    setShowForm(true)
  }

  function openEdit(target) {
    setEditing(target)
    setForm({ name: target.name, email: target.email, role: target.role, password: '' })
    setError('')
    setShowForm(true)
  }

  function closeForm() {
    setShowForm(false)
    setEditing(null)
    setForm(emptyForm)
  }

  async function saveUser(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    const body = { name: form.name.trim(), email: form.email.trim(), role: form.role }
    if (form.password) body.password = form.password
    try {
      if (editing) {
        await api.put(`/users/${editing.id}`, body)
        setNotice('Team member updated.')
      } else {
        await api.post('/users', body)
        setNotice('Team member added.')
      }
      closeForm()
      await loadUsers(result.page)
    } catch (requestError) {
      setError(errorText(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function toggleActive() {
    if (!confirmUser) return
    const target = confirmUser
    setConfirmUser(null)
    setError('')
    setNotice('')
    try {
      await api.patch(`/users/${target.id}/active`, { active: !target.active })
      setNotice(`${target.name} is now ${target.active ? 'inactive' : 'active'}.`)
      await loadUsers(result.page)
    } catch (requestError) {
      setError(errorText(requestError))
    }
  }

  function search(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setAppliedQuery(query.trim())
  }

  const columns = [
    { key: 'name', label: 'Team member', render: (item) => <span className="table-person"><span className="table-avatar">{item.name?.[0]?.toUpperCase()}</span><span><strong>{item.name}</strong><small>{item.email}</small></span></span> },
    { key: 'role', label: 'Role', render: (item) => <span className="role-chip">{item.role?.replaceAll('_', ' ')}</span> },
    { key: 'active', label: 'Status', render: (item) => <span className={`status-pill ${item.active ? 'is-active' : 'is-inactive'}`}><span />{item.active ? 'Active' : 'Inactive'}</span> },
    { key: 'actions', label: '', render: (item) => <div className="table-actions"><button className="text-button" type="button" onClick={() => openEdit(item)}>Edit</button><button className="text-button muted-action" type="button" disabled={item.id === user?.id || (item.role === 'OWNER' && user?.role !== 'OWNER')} title={item.role === 'OWNER' && user?.role !== 'OWNER' ? "Only an owner can change another owner's access." : undefined} onClick={() => setConfirmUser(item)}>{item.active ? 'Deactivate' : 'Activate'}</button></div> },
  ]

  return (
    <div className="content-column">
      <div className="page-heading users-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> YOUR PEOPLE</div><h1>Team members</h1><p>Build a team you can count on, one person at a time.</p></div><button className="button button-primary" type="button" onClick={openCreate}>＋ Add team member</button></div>
      <ErrorMessage message={error} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {showForm && <section className="surface-card user-form-card"><div className="section-heading"><div><span className="eyebrow">{editing ? 'TEAM DETAILS' : 'A NEW TEAMMATE'}</span><h2>{editing ? 'Update team member' : 'Add a team member'}</h2></div><button className="icon-button close-button" type="button" onClick={closeForm} aria-label="Close form">×</button></div>
        <form className="form-stack user-form" onSubmit={saveUser}>
          <div className="field-row"><FormField label="Full name" name="name" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} placeholder="Taylor Morgan" required /><FormField label="Email address" name="email" type="email" value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} placeholder="taylor@business.com" required /></div>
          <div className="field-row"><FormField label="Role" name="role" value={form.role} onChange={(event) => setForm((current) => ({ ...current, role: event.target.value }))} options={roleOptions} required /><FormField label={editing ? 'New password (optional)' : 'Temporary password'} name="password" type="password" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} placeholder={editing ? 'Leave blank to keep current' : 'At least 8 characters'} required={!editing} autoComplete="new-password" /></div>
          <div className="form-actions"><button className="button button-secondary" type="button" onClick={closeForm}>Cancel</button><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Add team member'}</button></div>
        </form>
      </section>}
      <section className="surface-card users-table-card">
        <div className="table-toolbar"><div><h2>Your team</h2><p>{result.totalElements} {result.totalElements === 1 ? 'person' : 'people'} in this workspace</p></div><form className="search-form" onSubmit={search}><label className="search-box"><span aria-hidden="true">⌕</span><input aria-label="Search team members" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search people" /></label><button className="button button-secondary button-small" type="submit">Search</button></form></div>
        {loading ? <Loader label="Loading team members" /> : <DataTable
          columns={columns}
          rows={result.content || []}
          emptyMessage={appliedQuery ? 'No team members match your search.' : 'No team members have been added yet.'}
          page={result.page}
          totalPages={result.totalPages}
          onPageChange={(page) => { setLoading(true); setError(''); loadUsers(page) }}
        />}
      </section>
      <ConfirmDialog open={Boolean(confirmUser)} title={`${confirmUser?.active ? 'Deactivate' : 'Activate'} team member?`} message={confirmUser ? `${confirmUser.name} will ${confirmUser.active ? 'no longer be able to sign in' : 'be able to sign in again'} to this workspace.` : ''} confirmLabel={confirmUser?.active ? 'Deactivate' : 'Activate'} onCancel={() => setConfirmUser(null)} onConfirm={toggleActive} />
    </div>
  )
}
