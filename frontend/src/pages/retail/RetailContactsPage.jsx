import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import ConfirmDialog from '../../components/ConfirmDialog'
import { useAuth } from '../../context/useAuth'
import { errorText, responseData, responseList } from './retailUtils'

const configs = {
  customers: {
    title: 'Customers',
    description: 'Keep customer contact details handy for checkout and sales history.',
    endpoint: '/customers',
    fields: [
      { name: 'name', label: 'Name', required: true, placeholder: 'Customer name' },
      { name: 'phone', label: 'Phone', required: true, type: 'tel', placeholder: 'Phone number' },
      { name: 'email', label: 'Email', type: 'email', placeholder: 'Email address' },
      { name: 'address', label: 'Address', placeholder: 'Street, city' },
    ],
    columns: [
      { key: 'name', label: 'Customer' },
      { key: 'phone', label: 'Phone' },
      { key: 'email', label: 'Email', render: (item) => item.email || '—' },
      { key: 'address', label: 'Address', render: (item) => item.address || '—' },
    ],
    roles: ['OWNER', 'MANAGER', 'CASHIER'],
  },
  suppliers: {
    title: 'Suppliers',
    description: 'Maintain supplier details used when receiving stock.',
    endpoint: '/suppliers',
    fields: [
      { name: 'name', label: 'Name', required: true, placeholder: 'Supplier name' },
      { name: 'phone', label: 'Phone', type: 'tel', placeholder: 'Phone number' },
      { name: 'address', label: 'Address', placeholder: 'Street, city' },
    ],
    columns: [
      { key: 'name', label: 'Supplier' },
      { key: 'phone', label: 'Phone', render: (item) => item.phone || '—' },
      { key: 'address', label: 'Address', render: (item) => item.address || '—' },
    ],
    roles: ['OWNER', 'MANAGER', 'STOCK_KEEPER'],
  },
}

export default function RetailContactsPage({ kind }) {
  const config = configs[kind]
  const { user } = useAuth()
  const canManage = config.roles.includes(user?.role)
  const [records, setRecords] = useState([])
  const [search, setSearch] = useState('')
  const [form, setForm] = useState({})
  const [editing, setEditing] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [recordToDelete, setRecordToDelete] = useState(null)

  const loadRecords = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await api.get(config.endpoint, { params: search ? { search } : undefined })
      setRecords(responseList(responseData({ data })))
    } catch (requestError) {
      setError(errorText(requestError, `${config.title} could not be loaded.`))
    } finally {
      setLoading(false)
    }
  }, [config, search])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadRecords() }, [loadRecords])

  async function save(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    const payload = Object.fromEntries(config.fields.map(({ name }) => [name, String(form[name] || '').trim() || null]))
    try {
      if (editing) await api.put(`${config.endpoint}/${editing.id}`, payload)
      else await api.post(config.endpoint, payload)
      setForm({})
      setEditing(null)
      setNotice(`${config.title.slice(0, -1)} ${editing ? 'updated' : 'added'}.`)
      await loadRecords()
    } catch (requestError) {
      setError(errorText(requestError, `${config.title.slice(0, -1)} could not be saved.`))
    } finally {
      setSaving(false)
    }
  }

  async function remove(record) {
    setError('')
    try {
      await api.delete(`${config.endpoint}/${record.id}`)
      setNotice(`${config.title.slice(0, -1)} deleted.`)
      await loadRecords()
      setRecordToDelete(null)
    } catch (requestError) {
      setError(errorText(requestError, `${config.title.slice(0, -1)} could not be deleted.`))
    }
  }

  const columns = [
    ...config.columns,
    ...(canManage ? [{
      key: 'actions',
      label: 'Actions',
      render: (record) => <div className="retail-row-actions"><button className="text-button" type="button" onClick={() => { setEditing(record); setForm(Object.fromEntries(config.fields.map(({ name }) => [name, record[name] || '']))) }}>Edit</button><button className="text-button retail-danger-text" type="button" onClick={() => setRecordToDelete(record)}>Delete</button></div>,
    }] : []),
  ]

  return (
    <div className="content-column retail-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> RELATIONSHIPS</div><h1>{config.title}</h1><p>{config.description}</p></div><button className="button button-secondary" type="button" onClick={loadRecords}>Refresh</button></div>
      <ErrorMessage message={error} onRetry={loadRecords} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {canManage && <section className="surface-card retail-form-card">
        <div className="section-heading"><div><span className="eyebrow">{editing ? 'UPDATE DETAILS' : 'NEW RECORD'}</span><h2>{editing ? `Edit ${editing.name}` : `Add ${config.title.slice(0, -1).toLowerCase()}`}</h2></div></div>
        <form className="form-stack" onSubmit={save}>
          <div className="retail-form-grid">{config.fields.map((field) => <FormField key={field.name} {...field} value={form[field.name] || ''} onChange={(event) => setForm((current) => ({ ...current, [field.name]: event.target.value }))} />)}</div>
          <div className="form-actions"><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Add record'}</button>{editing && <button className="button button-secondary" type="button" onClick={() => { setEditing(null); setForm({}) }}>Cancel</button>}</div>
        </form>
      </section>}
      <section className="surface-card retail-table-card">
        <form className="retail-filter-form" onSubmit={(event) => { event.preventDefault(); loadRecords() }}><FormField label={`Search ${config.title.toLowerCase()}`} name={`search-${kind}`} value={search} onChange={(event) => setSearch(event.target.value)} placeholder={kind === 'customers' ? 'Name or phone…' : 'Supplier name…'} /><button className="button button-secondary button-small" type="submit">Search</button></form>
        {loading ? <Loader label={`Loading ${config.title.toLowerCase()}`} /> : <DataTable columns={columns} rows={records} emptyMessage={`No ${config.title.toLowerCase()} found. Add your first record above.`} />}
      </section>
      <ConfirmDialog
        open={Boolean(recordToDelete)}
        title={`Delete ${config.title.slice(0, -1).toLowerCase()}?`}
        message={recordToDelete ? `${recordToDelete.name} will be permanently removed.` : ''}
        confirmLabel="Delete"
        onCancel={() => setRecordToDelete(null)}
        onConfirm={() => recordToDelete && remove(recordToDelete)}
      />
    </div>
  )
}
