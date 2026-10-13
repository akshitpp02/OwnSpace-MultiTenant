import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import ConfirmDialog from '../../components/ConfirmDialog'
import { useAuth } from '../../context/useAuth'
import { errorText } from './restaurantUtils'

const emptyForm = { name: '', capacity: '2' }

export default function RestaurantTablesPage() {
  const { user } = useAuth()
  const canManage = ['OWNER', 'MANAGER'].includes(user?.role)
  const [tables, setTables] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [editing, setEditing] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [tableToDelete, setTableToDelete] = useState(null)

  const loadTables = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await api.get('/tables')
      setTables(data.data || [])
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Dining tables could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadTables() }, [loadTables])

  async function saveTable(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const payload = { name: form.name.trim(), capacity: Number(form.capacity) }
      if (editing) await api.put(`/tables/${editing.id}`, payload)
      else await api.post('/tables', payload)
      setForm(emptyForm)
      setEditing(null)
      setNotice(editing ? 'Table updated.' : 'Table created.')
      await loadTables()
    } catch (requestError) {
      setError(errorText(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function deleteTable(table) {
    setError('')
    try {
      await api.delete(`/tables/${table.id}`)
      setNotice('Table deleted.')
      await loadTables()
      setTableToDelete(null)
    } catch (requestError) {
      setError(errorText(requestError))
    }
  }

  return (
    <div className="content-column restaurant-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> FLOOR PLAN</div><h1>Dining tables</h1><p>See which tables are free or serving guests.</p></div><button className="button button-secondary" type="button" onClick={loadTables}>Refresh</button></div>
      <ErrorMessage message={error} onRetry={loadTables} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {canManage && <section className="surface-card restaurant-form-card">
        <div className="section-heading"><div><span className="eyebrow">{editing ? 'TABLE DETAILS' : 'SEATING'}</span><h2>{editing ? `Update ${editing.name}` : 'Add a table'}</h2></div></div>
        <form className="restaurant-inline-form" onSubmit={saveTable}>
          <FormField label="Table name" name="tableName" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} placeholder="e.g. Table 1" required />
          <FormField label="Seats" name="tableCapacity" type="number" min="1" value={form.capacity} onChange={(event) => setForm((current) => ({ ...current, capacity: event.target.value }))} required />
          <div className="form-actions"><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save table' : 'Add table'}</button>{editing && <button className="button button-secondary" type="button" onClick={() => { setEditing(null); setForm(emptyForm) }}>Cancel</button>}</div>
        </form>
      </section>}
      {loading ? <Loader label="Loading dining tables" /> : <div className="restaurant-tables-overview">
        <div className="restaurant-table-legend" aria-label="Table status legend"><span><i className="is-free" /> Free</span><span><i className="is-occupied" /> Occupied</span></div>
        <div className="restaurant-table-grid">
        {tables.map((table) => <article className={`restaurant-table-tile table-${table.status?.toLowerCase()}`} key={table.id}>
          <div className="restaurant-table-top"><span className="table-icon" aria-hidden="true">▦</span><span className={`restaurant-table-status ${table.status?.toLowerCase()}`}><i />{table.status === 'FREE' ? 'Free' : 'Occupied'}</span></div>
          <h2>{table.name}</h2><p>{table.capacity} {table.capacity === 1 ? 'seat' : 'seats'}</p>
          {canManage && <div className="table-actions"><button className="text-button" type="button" disabled={table.status !== 'FREE'} onClick={() => { setEditing(table); setForm({ name: table.name, capacity: String(table.capacity) }) }}>Edit</button><button className="text-button muted-action" type="button" disabled={table.status !== 'FREE'} onClick={() => setTableToDelete(table)}>Delete</button></div>}
        </article>)}
        {!tables.length && <div className="surface-card empty-state">No tables yet. Add a table to start taking orders.</div>}
        </div>
      </div>}
      <ConfirmDialog
        open={Boolean(tableToDelete)}
        title="Delete dining table?"
        message={tableToDelete ? `${tableToDelete.name} will be removed from your floor plan.` : ''}
        confirmLabel="Delete table"
        onCancel={() => setTableToDelete(null)}
        onConfirm={() => tableToDelete && deleteTable(tableToDelete)}
      />
    </div>
  )
}
