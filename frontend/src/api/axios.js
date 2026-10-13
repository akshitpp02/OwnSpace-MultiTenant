import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('saas-platform-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem('saas-platform-token')) {
      localStorage.removeItem('saas-platform-token')
      localStorage.removeItem('saas-platform-user')
      window.dispatchEvent(new Event('saas-platform:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export default api
