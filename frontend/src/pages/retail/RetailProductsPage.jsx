import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import ConfirmDialog from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import { useAuth } from '../../context/useAuth'
import { errorText, money, responseData, responseList } from './retailUtils'

const emptyProduct = { name: '', sku: '', categoryId: '', price: '0', costPrice: '0', stockQty: '0', lowStockThreshold: '5' }
const emptyCategory = { name: '' }

function productPayload(product, active = true) {
  return {
    name: product.name,
    sku: product.sku,
    categoryId: product.categoryId ?? product.category?.id ?? null,
    price: Number(product.price || 0),
    costPrice: Number(product.costPrice ?? product.cost_price ?? 0),
    stockQty: Number(product.stockQty || 0),
    lowStockThreshold: Number(product.lowStockThreshold ?? 5),
    active,
  }
}

export default function RetailProductsPage() {
  const { user } = useAuth()
  const canManage = ['OWNER', 'MANAGER', 'STOCK_KEEPER'].includes(user?.role)
  const canManagePrices = ['OWNER', 'MANAGER'].includes(user?.role)
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [filters, setFilters] = useState({ search: '', categoryId: '' })
  const [productForm, setProductForm] = useState(emptyProduct)
  const [categoryForm, setCategoryForm] = useState(emptyCategory)
  const [editingProduct, setEditingProduct] = useState(null)
  const [productModalOpen, setProductModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [categoryToDelete, setCategoryToDelete] = useState(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [productResponse, categoryResponse] = await Promise.all([
        api.get('/products', { params: { search: filters.search || undefined, categoryId: filters.categoryId || undefined } }),
        canManage ? api.get('/retail/categories') : Promise.resolve({ data: { data: [] } }),
      ])
      const productRows = responseList(responseData(productResponse))
      setProducts(productRows)
      setCategories(canManage
        ? responseList(responseData(categoryResponse))
        : [...new Map(productRows.flatMap((product) => {
          const category = product.category || (product.categoryId ? { id: product.categoryId, name: product.categoryName } : null)
          return category?.id ? [[category.id, category]] : []
        })).values()])
    } catch (requestError) {
      setError(errorText(requestError, 'Products and categories could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [canManage, filters])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadData() }, [loadData])

  function updateProduct(event) {
    setProductForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function saveProduct(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    const payload = {
      name: productForm.name.trim(),
      sku: productForm.sku.trim(),
      categoryId: productForm.categoryId ? Number(productForm.categoryId) : null,
      price: Number(productForm.price || 0),
      costPrice: Number(productForm.costPrice || 0),
      stockQty: Number(productForm.stockQty || 0),
      lowStockThreshold: Number(productForm.lowStockThreshold || 0),
      active: editingProduct?.active ?? true,
    }
    try {
      if (editingProduct) await api.put(`/products/${editingProduct.id}`, payload)
      else await api.post('/products', payload)
      setProductForm(emptyProduct)
      setEditingProduct(null)
      setProductModalOpen(false)
      setNotice(editingProduct ? 'Product updated.' : 'Product added.')
      await loadData()
    } catch (requestError) {
      setError(errorText(requestError, 'The product could not be saved.'))
    } finally {
      setSaving(false)
    }
  }

  function editProduct(product) {
    setEditingProduct(product)
    setProductModalOpen(true)
    setProductForm({
      name: product.name || '',
      sku: product.sku || '',
      categoryId: product.categoryId ?? product.category?.id ?? '',
      price: String(product.price ?? 0),
      costPrice: String(product.costPrice ?? 0),
      stockQty: String(product.stockQty ?? 0),
      lowStockThreshold: String(product.lowStockThreshold ?? 5),
    })
  }

  function openNewProduct() {
    setEditingProduct(null)
    setProductForm(emptyProduct)
    setProductModalOpen(true)
  }

  function closeProductModal() {
    if (saving) return
    setProductModalOpen(false)
    setEditingProduct(null)
    setProductForm(emptyProduct)
  }

  async function toggleActive(product) {
    setError('')
    try {
      await api.put(`/products/${product.id}`, productPayload(product, !product.active))
      setNotice(`${product.name} ${product.active ? 'deactivated' : 'activated'}.`)
      await loadData()
    } catch (requestError) {
      setError(errorText(requestError, 'Product status could not be updated.'))
    }
  }

  async function saveCategory(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      if (editingCategory) await api.put(`/retail/categories/${editingCategory.id}`, { name: categoryForm.name.trim() })
      else await api.post('/retail/categories', { name: categoryForm.name.trim() })
      setCategoryForm(emptyCategory)
      setEditingCategory(null)
      setNotice(editingCategory ? 'Category updated.' : 'Category added.')
      await loadData()
    } catch (requestError) {
      setError(errorText(requestError, 'The category could not be saved.'))
    } finally {
      setSaving(false)
    }
  }

  async function deleteCategory(category) {
    setError('')
    try {
      await api.delete(`/retail/categories/${category.id}`)
      setNotice('Category deleted.')
      await loadData()
      setCategoryToDelete(null)
    } catch (requestError) {
      setError(errorText(requestError, 'The category could not be deleted.'))
    }
  }

  const columns = [
    { key: 'name', label: 'Product', render: (product) => <div className="retail-product-name"><strong>{product.name}</strong><small>{product.sku}</small></div> },
    { key: 'categoryName', label: 'Category', render: (product) => product.categoryName || product.category?.name || 'Uncategorized' },
    { key: 'price', label: 'Price', render: (product) => money(product.price) },
    { key: 'stockQty', label: 'In stock', render: (product) => {
      const lowStock = product.lowStock ?? Number(product.stockQty) <= Number(product.lowStockThreshold)
      return <span className="retail-stock-cell"><span className={`retail-stock-count${lowStock ? ' is-low' : ''}`}>{product.stockQty}</span>{lowStock && <span className="retail-low-stock-badge">Low stock</span>}</span>
    } },
    { key: 'active', label: 'Status', render: (product) => <span className={`retail-status-pill${product.active ? ' is-active' : ''}`}>{product.active ? 'Active' : 'Inactive'}</span> },
    ...(canManage ? [{ key: 'actions', label: 'Actions', render: (product) => <div className="retail-row-actions"><button className="text-button" type="button" onClick={() => editProduct(product)}>Edit</button><button className="text-button" type="button" onClick={() => toggleActive(product)}>{product.active ? 'Deactivate' : 'Activate'}</button></div> }] : []),
  ]

  return (
    <div className="content-column retail-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> CATALOGUE</div><h1>Products</h1><p>Keep your catalogue, selling prices and on-hand quantities current.</p></div><div className="retail-products-heading-actions">{canManage && <button className="button button-primary" type="button" onClick={openNewProduct}>＋ Add product</button>}<button className="button button-secondary" type="button" onClick={loadData}>Refresh</button></div></div>
      <ErrorMessage message={error} onRetry={loadData} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      {canManage && <section className="surface-card retail-category-card">
        <div className="section-heading"><div><span className="eyebrow">ORGANIZE</span><h2>Categories</h2></div></div>
        <form className="retail-category-form" onSubmit={saveCategory}>
          <FormField label={editingCategory ? 'Edit category' : 'New category'} name="categoryName" value={categoryForm.name} onChange={(event) => setCategoryForm({ name: event.target.value })} required placeholder="e.g. Pantry" />
          <div className="form-actions"><button className="button button-secondary button-small" type="submit" disabled={saving}>{editingCategory ? 'Save category' : 'Add category'}</button>{editingCategory && <button className="text-button" type="button" onClick={() => { setEditingCategory(null); setCategoryForm(emptyCategory) }}>Cancel</button>}</div>
        </form>
        <div className="retail-category-list">{categories.map((category) => <span className="retail-category-chip" key={category.id}>{category.name}<button type="button" onClick={() => { setEditingCategory(category); setCategoryForm({ name: category.name }) }} aria-label={`Edit ${category.name}`}>✎</button><button type="button" onClick={() => setCategoryToDelete(category)} aria-label={`Delete ${category.name}`}>×</button></span>)}{!categories.length && <span className="retail-hint">No categories yet. Categories help you filter the catalogue.</span>}</div>
      </section>}
      <section className="surface-card retail-table-card">
        <div className="retail-table-toolbar">
          <form className="retail-filter-form" onSubmit={(event) => { event.preventDefault(); loadData() }}>
            <FormField label="Search name or SKU" name="productSearch" value={filters.search} onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value }))} placeholder="Search products…" />
            <label className="field"><span className="field-label">Category</span><select value={filters.categoryId} onChange={(event) => setFilters((current) => ({ ...current, categoryId: event.target.value }))}><option value="">All categories</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
            <button className="button button-secondary button-small" type="submit">Apply filters</button>
          </form>
          <span className="retail-result-count">{products.length} products</span>
        </div>
        {loading ? <Loader label="Loading products" /> : <DataTable columns={columns} rows={products} emptyMessage="No products found. Add a product to start your catalogue." />}
      </section>
      <Modal open={productModalOpen} title={editingProduct ? `Edit ${editingProduct.name}` : 'Add a product'} onClose={closeProductModal} className="retail-product-modal">
        <form className="form-stack retail-product-form" onSubmit={saveProduct}>
          <div className="retail-form-grid">
            <FormField label="Product name" name="name" value={productForm.name} onChange={updateProduct} required placeholder="e.g. Organic green tea" />
            <FormField label="SKU / barcode" name="sku" value={productForm.sku} onChange={updateProduct} required placeholder="Unique product code" />
            <label className="field"><span className="field-label">Category</span><select name="categoryId" value={productForm.categoryId} onChange={updateProduct}><option value="">Uncategorized</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
            <FormField label="Selling price" name="price" type="number" min="0" step="0.01" value={productForm.price} onChange={updateProduct} disabled={!canManagePrices} required />
            <FormField label="Cost price" name="costPrice" type="number" min="0" step="0.01" value={productForm.costPrice} onChange={updateProduct} disabled={!canManagePrices} required />
            <FormField label="Opening stock" name="stockQty" type="number" min="0" step="1" value={productForm.stockQty} onChange={updateProduct} required />
            <FormField label="Low-stock alert at" name="lowStockThreshold" type="number" min="0" step="1" value={productForm.lowStockThreshold} onChange={updateProduct} required />
          </div>
          {!canManagePrices && <p className="retail-hint">Only owners and managers can change product prices. Use Stock in to receive inventory.</p>}
          <div className="form-actions"><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editingProduct ? 'Save product' : 'Add product'}</button><button className="button button-secondary" type="button" onClick={closeProductModal} disabled={saving}>Cancel</button></div>
        </form>
      </Modal>
      <ConfirmDialog
        open={Boolean(categoryToDelete)}
        title="Delete category?"
        message={categoryToDelete ? `“${categoryToDelete.name}” will be removed from your catalogue.` : ''}
        confirmLabel="Delete category"
        onCancel={() => setCategoryToDelete(null)}
        onConfirm={() => categoryToDelete && deleteCategory(categoryToDelete)}
      />
    </div>
  )
}
