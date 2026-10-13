import { useEffect, useState } from 'react'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import { useAuth } from '../../context/useAuth'

export default function SettingsPage() {
  const { user } = useAuth()
  const [form, setForm] = useState({ businessName: '', phone: '', address: '', taxPercent: '' })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function loadSettings() {
    try {
      const { data } = await api.get('/settings')
      setForm({
        businessName: data.data.businessName || '',
        phone: data.data.phone || '',
        address: data.data.address || '',
        taxPercent: data.data.taxPercent ?? '',
      })
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Business settings could not be loaded.')
    } finally {
      setLoading(false)
    }
  }

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadSettings() }, [])

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function save(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    setSaving(true)
    try {
      await api.put('/settings', {
        businessName: form.businessName.trim(),
        phone: form.phone.trim(),
        address: form.address.trim(),
        taxPercent: form.taxPercent === '' ? null : Number(form.taxPercent),
      })
      setNotice('Your business settings have been saved.')
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Your settings could not be saved.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="content-column">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> THE DETAILS THAT MATTER</div><h1>Business settings</h1><p>Keep your business information accurate and up to date.</p></div></div>
      {error && <ErrorMessage message={error} onRetry={() => { setError(''); setLoading(true); loadSettings() }} />}
      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {loading ? <Loader label="Loading business settings" /> : <section className="surface-card settings-card"><div className="section-heading"><div><span className="eyebrow">YOUR BUSINESS</span><h2>Business information</h2><p>These details are used across your workspace.</p></div><span className="settings-icon">◈</span></div>
        <form className="form-stack settings-form" onSubmit={save}>
          <FormField label="Business name" name="businessName" value={form.businessName} onChange={update} placeholder="Your business name" required />
          <div className="field-row"><FormField label="Phone number" name="phone" type="tel" value={form.phone} onChange={update} placeholder="+1 (555) 000-0000" /><FormField label="Tax rate" name="taxPercent" type="number" value={form.taxPercent} onChange={update} placeholder="0" min="0" max="100" step="0.01" help="Enter a percentage from 0 to 100." /></div>
          <FormField label="Business address" name="address" value={form.address} onChange={update} placeholder="Street, city, region" rows={3} />
          <div className="settings-note"><span>ⓘ</span><p>Changes to your business name are reflected in your workspace immediately.</p></div>
          <div className="form-actions"><span className="settings-owner">Editing as {user?.role?.toLowerCase()}</span><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Saving changes…' : 'Save changes'}</button></div>
        </form>
      </section>}
    </div>
  )
}
