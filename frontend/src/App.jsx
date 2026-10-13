import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './context/useAuth'
import AppLayout from './layouts/AppLayout'
import DashboardPage from './pages/core/DashboardPage'
import SettingsPage from './pages/core/SettingsPage'
import UsersPage from './pages/core/UsersPage'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import NotAllowedPage from './pages/NotAllowedPage'
import PlatformPage from './pages/PlatformPage'
import ProtectedRoute from './components/ProtectedRoute'
import Loader from './components/Loader'
import './App.css'

const RestaurantBillingPage = lazy(() => import('./pages/restaurant/RestaurantBillingPage'))
const RestaurantDashboardPage = lazy(() => import('./pages/restaurant/RestaurantDashboardPage'))
const RestaurantMenuPage = lazy(() => import('./pages/restaurant/RestaurantMenuPage'))
const RestaurantNewOrderPage = lazy(() => import('./pages/restaurant/RestaurantNewOrderPage'))
const RestaurantOrdersPage = lazy(() => import('./pages/restaurant/RestaurantOrdersPage'))
const RestaurantTablesPage = lazy(() => import('./pages/restaurant/RestaurantTablesPage'))
const RetailContactsPage = lazy(() => import('./pages/retail/RetailContactsPage'))
const RetailDashboardPage = lazy(() => import('./pages/retail/RetailDashboardPage'))
const RetailLowStockPage = lazy(() => import('./pages/retail/RetailLowStockPage'))
const RetailPosPage = lazy(() => import('./pages/retail/RetailPosPage'))
const RetailProductsPage = lazy(() => import('./pages/retail/RetailProductsPage'))
const RetailSalesPage = lazy(() => import('./pages/retail/RetailSalesPage'))
const RetailStockAdjustmentsPage = lazy(() => import('./pages/retail/RetailStockAdjustmentsPage'))
const RetailStockInPage = lazy(() => import('./pages/retail/RetailStockInPage'))

function WorkspaceDashboard() {
  const { user } = useAuth()
  return user?.businessType === 'RESTAURANT'
    ? <Suspense fallback={<Loader label="Loading restaurant dashboard" />}><RestaurantDashboardPage /></Suspense>
    : user?.businessType === 'RETAIL'
      ? <Suspense fallback={<Loader label="Loading retail dashboard" />}><RetailDashboardPage /></Suspense>
      : <DashboardPage />
}

function RestaurantOnlyRoute() {
  const { user } = useAuth()
  return user?.businessType === 'RESTAURANT'
    ? <Suspense fallback={<Loader label="Loading restaurant workspace" />}><Outlet /></Suspense>
    : <Navigate to="/not-allowed" replace />
}

function RetailOnlyRoute() {
  const { user } = useAuth()
  return user?.businessType === 'RETAIL'
    ? <Suspense fallback={<Loader label="Loading retail workspace" />}><Outlet /></Suspense>
    : <Navigate to="/not-allowed" replace />
}

function HomeRedirect() {
  const { isAuthenticated, user } = useAuth()
  if (!isAuthenticated) return <LandingPage />
  return <Navigate to={user.role === 'PLATFORM_ADMIN' ? '/platform' : '/dashboard'} replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/admin/login" element={<LoginPage platform />} />
      <Route path="/not-allowed" element={<NotAllowedPage />} />
      <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER', 'CASHIER', 'KITCHEN', 'KITCHEN_STAFF', 'WAITER', 'INVENTORY_MANAGER', 'STOCK_KEEPER']} />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<WorkspaceDashboard />} />
          <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER']} />}>
            <Route path="/users" element={<UsersPage />} />
          </Route>
          <Route element={<ProtectedRoute roles={['OWNER']} />}>
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
          <Route element={<RestaurantOnlyRoute />}>
            <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER', 'CASHIER', 'KITCHEN', 'KITCHEN_STAFF', 'WAITER']} />}>
              <Route path="/restaurant/tables" element={<RestaurantTablesPage />} />
              <Route path="/restaurant/orders" element={<RestaurantOrdersPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER']} />}>
              <Route path="/restaurant/menu" element={<RestaurantMenuPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER', 'WAITER']} />}>
              <Route path="/restaurant/new-order" element={<RestaurantNewOrderPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER', 'KITCHEN', 'KITCHEN_STAFF']} />}>
              <Route path="/restaurant/kitchen" element={<RestaurantOrdersPage kitchenOnly />} />
            </Route>
            <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER', 'CASHIER']} />}>
              <Route path="/restaurant/billing" element={<RestaurantBillingPage />} />
            </Route>
          </Route>
          <Route element={<RetailOnlyRoute />}>
            <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER', 'STOCK_KEEPER', 'CASHIER']} />}>
              <Route path="/retail/products" element={<RetailProductsPage />} />
              <Route path="/retail/low-stock" element={<RetailLowStockPage />} />
            </Route>
            <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER', 'CASHIER']} />}>
              <Route path="/retail/pos" element={<RetailPosPage />} />
              <Route path="/retail/sales" element={<RetailSalesPage />} />
              <Route path="/retail/customers" element={<RetailContactsPage kind="customers" />} />
            </Route>
            <Route element={<ProtectedRoute roles={['OWNER', 'MANAGER', 'STOCK_KEEPER']} />}>
              <Route path="/retail/suppliers" element={<RetailContactsPage kind="suppliers" />} />
              <Route path="/retail/stock-in" element={<RetailStockInPage />} />
              <Route path="/retail/stock-adjustments" element={<RetailStockAdjustmentsPage />} />
            </Route>
          </Route>
        </Route>
      </Route>
      <Route element={<ProtectedRoute roles={['PLATFORM_ADMIN']} />}>
        <Route path="/platform" element={<PlatformPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
