import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Button from '../../components/Button'
import Brand from '../../components/Brand'

const initialValues = {
  businessName: '',
  businessCode: '',
  businessType: 'RESTAURANT',
  ownerName: '',
  ownerEmail: '',
  ownerPassword: '',
}

function getError(error) {
  return error.response?.data?.message || 'We could not create your workspace. Please try again.'
}

export default function RegisterPage() {
  const [values, setValues] = useState(initialValues)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [complete, setComplete] = useState(false)
  const navigate = useNavigate()

  function update(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await api.post('/platform/tenants/register', {
        ...values,
        businessCode: values.businessCode.trim().toLowerCase(),
        ownerEmail: values.ownerEmail.trim(),
      })
      setComplete(true)
    } catch (requestError) {
      setError(getError(requestError))
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page auth-page--simple auth-register-page">
      <header className="auth-simple-header"><Brand /><Link className="auth-simple-header__link" to="/login">Already have a workspace? <strong>Sign in</strong></Link></header>
      <section className="auth-main auth-main--simple auth-main--register">
        <div className="auth-form-wrap auth-card auth-card--register">
          {complete ? (
            <div className="success-panel">
              <span className="success-mark">✓</span><div className="eyebrow">WORKSPACE CREATED</div><h1>You’re ready to get started.</h1><p>Your business workspace is set up. Sign in using your business code and owner account.</p>
              <Button className="button-full" type="button" onClick={() => navigate('/login', { state: { businessCode: values.businessCode.toUpperCase() } })}>Continue to sign in <span aria-hidden="true">→</span></Button>
            </div>
          ) : (
            <>
              <div className="auth-card__mark" aria-hidden="true">＋</div>
              <div className="eyebrow"><span className="eyebrow-dot" /> A FRESH START</div>
              <h1>Create your workspace</h1><p className="form-intro">A few details, and you’re on your way.</p>
              <ErrorMessage message={error} />
              <form className="form-stack" onSubmit={submit}>
                <div className="form-section-label">YOUR BUSINESS</div>
                <FormField label="Business name" name="businessName" value={values.businessName} onChange={update} placeholder="e.g. The Corner Table" required />
                <div className="field-row">
                  <FormField label="Workspace code" name="businessCode" value={values.businessCode} onChange={update} placeholder="e.g. CORNERTABLE" required help="You’ll use this code to sign in." />
                </div>
                <fieldset className="business-type-picker">
                  <legend>Business type</legend>
                  <div className="business-type-picker__options">
                    {[{ value: 'RESTAURANT', title: 'Restaurant / Cafe', icon: '☕' }, { value: 'RETAIL', title: 'Retail shop', icon: '▤' }].map((type) => (
                      <label className={`business-type-option${values.businessType === type.value ? ' is-selected' : ''}`} key={type.value}>
                        <input type="radio" name="businessType" value={type.value} checked={values.businessType === type.value} onChange={update} required />
                        <span className="business-type-option__icon" aria-hidden="true">{type.icon}</span>
                        <span>{type.title}</span>
                      </label>
                    ))}
                  </div>
                </fieldset>
                <div className="form-section-label form-section-spaced">YOUR OWNER ACCOUNT</div>
                <FormField label="Your name" name="ownerName" value={values.ownerName} onChange={update} placeholder="Jordan Lee" autoComplete="name" required />
                <FormField label="Email address" name="ownerEmail" type="email" value={values.ownerEmail} onChange={update} placeholder="you@yourbusiness.com" autoComplete="email" required />
                <FormField label="Password" name="ownerPassword" type="password" value={values.ownerPassword} onChange={update} placeholder="At least 8 characters" autoComplete="new-password" required />
                <Button className="button-full" type="submit" loading={loading}>{loading ? 'Creating workspace…' : <>Create workspace <span aria-hidden="true">→</span></>}</Button>
                <p className="terms-copy">By creating a workspace, you agree to use OwnSpace responsibly. Your data stays yours.</p>
              </form>
            </>
          )}
        </div>
      </section>
      <footer className="auth-simple-footer">OwnSpace · A simpler day starts here.</footer>
    </main>
  )
}
