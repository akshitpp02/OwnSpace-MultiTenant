import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import api from '../../api/axios'
import { useAuth } from '../../context/useAuth'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Button from '../../components/Button'
import Brand from '../../components/Brand'

export default function LoginPage({ platform = false }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { login } = useAuth()
  const [values, setValues] = useState({
    businessCode: location.state?.businessCode || '',
    email: '',
    password: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function update(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await api.post(platform ? '/auth/platform-login' : '/auth/login', platform
        ? { email: values.email.trim(), password: values.password }
        : { businessCode: values.businessCode.trim().toLowerCase(), email: values.email.trim(), password: values.password })
      login(data.data.token, data.data.user)
      const destination = location.state?.from?.pathname
      navigate(destination || (data.data.user.role === 'PLATFORM_ADMIN' ? '/platform' : '/dashboard'), { replace: true })
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'We couldn’t sign you in. Check your details and try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page login-page auth-page--simple">
      <header className="auth-simple-header"><Brand /><Link className="auth-simple-header__link" to={platform ? '/login' : '/register'}>{platform ? 'Business sign in' : 'Create a workspace'}</Link></header>
      <section className="auth-main auth-main--simple">
        <div className="auth-form-wrap login-form-wrap auth-card">
          <div className="auth-card__mark" aria-hidden="true">{platform ? '◈' : '↗'}</div>
          <div className="eyebrow"><span className="eyebrow-dot" /> {platform ? 'PLATFORM ADMINISTRATION' : 'WELCOME BACK'}</div>
          <h1>{platform ? 'Platform sign in' : 'Sign in to your workspace'}</h1><p className="form-intro">{platform ? 'Manage your OwnSpace platform account.' : 'Continue where your business left off.'}</p>
          <ErrorMessage message={error} />
          <form className="form-stack" onSubmit={submit}>
            {!platform && <FormField label="Workspace code" name="businessCode" value={values.businessCode} onChange={update} placeholder="e.g. CORNERTABLE" autoComplete="organization" required />}
            <FormField label="Email address" name="email" type="email" value={values.email} onChange={update} placeholder="you@yourbusiness.com" autoComplete="email" required />
            <FormField label="Password" name="password" type="password" value={values.password} onChange={update} placeholder="Your password" autoComplete="current-password" required />
            <Button className="button-full" type="submit" loading={loading}>{loading ? 'Signing in…' : <>Sign in <span aria-hidden="true">→</span></>}</Button>
          </form>
          {!platform && <Link className="admin-link" to="/admin/login">Platform administrator sign in <span aria-hidden="true">→</span></Link>}
          {platform && <p className="auth-card__footnote">This sign-in is for OwnSpace platform administrators.</p>}
        </div>
      </section>
      <footer className="auth-simple-footer">OwnSpace · A simpler day starts here.</footer>
    </main>
  )
}
