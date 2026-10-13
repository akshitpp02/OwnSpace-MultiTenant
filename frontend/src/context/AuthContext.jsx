import { useCallback, useEffect, useMemo, useState } from 'react'
import AuthContext from './auth-context'

const TOKEN_KEY = 'saas-platform-token'
const USER_KEY = 'saas-platform-user'

function readStoredUser() {
  try {
    const value = localStorage.getItem(USER_KEY)
    return value ? JSON.parse(value) : null
  } catch {
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(TOKEN_KEY)
    return null
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState(readStoredUser)

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    window.addEventListener('saas-platform:unauthorized', logout)
    return () => window.removeEventListener('saas-platform:unauthorized', logout)
  }, [logout])

  const login = useCallback((nextToken, nextUser) => {
    localStorage.setItem(TOKEN_KEY, nextToken)
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
    setToken(nextToken)
    setUser(nextUser)
  }, [])

  const hasRole = useCallback(
    (...roles) => Boolean(user && roles.includes(user.role)),
    [user],
  )

  const value = useMemo(
    () => ({ token, user, login, logout, hasRole, isAuthenticated: Boolean(token && user) }),
    [token, user, login, logout, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
