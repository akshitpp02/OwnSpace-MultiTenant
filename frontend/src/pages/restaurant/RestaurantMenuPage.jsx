import { useCallback, useEffect, useState } from 'react'
import api from '../../api/axios'
import ErrorMessage from '../../components/ErrorMessage'
import FormField from '../../components/FormField'
import Loader from '../../components/Loader'
import ConfirmDialog from '../../components/ConfirmDialog'
import { errorText, money } from './restaurantUtils'

const emptyItem = { categoryId: '', name: '', description: '', price: '' }

export default function RestaurantMenuPage() {
  const [categories, setCategories] = useState([])
  const [items, setItems] = useState([])
  const [categoryName, setCategoryName] = useState('')
  const [form, setForm] = useState(emptyItem)
  const [editingItem, setEditingItem] = useState(null)
  const [editingCategory, setEditingCategory] = useState(null)
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [pendingDelete, setPendingDelete] = useState(null)

  const loadMenu = useCallback(async () => {
    setLoading(true)
    try {
      const [categoryResponse, itemResponse] = await Promise.all([
        api.get('/categories', { params: { size: 100 } }),
        api.get('/menu-items', {
          params: {
            size: 100,
            categoryId: categoryFilter || undefined,
            search: appliedSearch || undefined,
          },
        }),
      ])
      setCategories(categoryResponse.data.data.content || [])
      setItems(itemResponse.data.data.content || [])
      setError('')
    } catch (requestError) {
      setError(errorText(requestError, 'Menu data could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [appliedSearch, categoryFilter])

  // Fetch completion updates the page asynchronously after the effect returns.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { loadMenu() }, [loadMenu])

  async function saveCategory(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      if (editingCategory) await api.put(`/categories/${editingCategory.id}`, { name: categoryName.trim() })
      else await api.post('/categories', { name: categoryName.trim() })
      setCategoryName('')
      setEditingCategory(null)
      setNotice(editingCategory ? 'Category updated.' : 'Category created.')
      await loadMenu()
    } catch (requestError) {
      setError(errorText(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function deleteCategory(category) {
    setError('')
    try {
      await api.delete(`/categories/${category.id}`)
      setNotice('Category deleted.')
      await loadMenu()
      setPendingDelete(null)
    } catch (requestError) {
      setError(errorText(requestError))
    }
  }

  function editCategory(category) {
    setEditingCategory(category)
    setCategoryName(category.name)
  }

  async function saveItem(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    const payload = {
      ...form,
      categoryId: Number(form.categoryId),
      price: Number(form.price),
      description: form.description.trim() || null,
    }
    try {
      if (editingItem) await api.put(`/menu-items/${editingItem.id}`, payload)
      else await api.post('/menu-items', payload)
      setForm(emptyItem)
      setEditingItem(null)
      setNotice(editingItem ? 'Menu item updated.' : 'Menu item added.')
      await loadMenu()
    } catch (requestError) {
      setError(errorText(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function toggleAvailability(item) {
    setError('')
    try {
      await api.patch(`/menu-items/${item.id}/availability`, { available: !item.available })
      setNotice(`${item.name} is now ${item.available ? 'unavailable' : 'available'}.`)
      await loadMenu()
    } catch (requestError) {
      setError(errorText(requestError))
    }
  }

  async function deleteItem(item) {
    setError('')
    try {
      await api.delete(`/menu-items/${item.id}`)
      setNotice('Menu item deleted.')
      await loadMenu()
      setPendingDelete(null)
    } catch (requestError) {
      setError(errorText(requestError))
    }
  }

  function editItem(item) {
    setEditingItem(item)
    setForm({
      categoryId: String(item.categoryId),
      name: item.name,
      description: item.description || '',
      price: String(item.price),
    })
  }

  return (
    <div className="content-column restaurant-content">
      <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> MENU MANAGEMENT</div><h1>Menu and categories</h1><p>Organize dishes, pricing and availability for your team.</p></div></div>
      <ErrorMessage message={error} onRetry={loadMenu} />
      {notice && <div className="alert alert-success" role="status">{notice}<button className="alert-close" type="button" onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
      <div className="restaurant-two-column">
        <section className="surface-card restaurant-form-card">
          <div className="section-heading"><div><span className="eyebrow">CATEGORIES</span><h2>Menu groups</h2></div></div>
          <form className="restaurant-inline-form" onSubmit={saveCategory}>
            <FormField label={editingCategory ? 'Edit category' : 'New category'} name="categoryName" value={categoryName} onChange={(event) => setCategoryName(event.target.value)} placeholder="e.g. Starters" required />
            <div className="form-actions"><button className="button button-primary" type="submit" disabled={saving}>{editingCategory ? 'Save category' : 'Add category'}</button>{editingCategory && <button className="button button-secondary" type="button" onClick={() => { setEditingCategory(null); setCategoryName('') }}>Cancel</button>}</div>
          </form>
          <div className="restaurant-category-list">
            {categories.map((category) => <div className="restaurant-category-row" key={category.id}><strong>{category.name}</strong><div className="table-actions"><button className="text-button" type="button" onClick={() => editCategory(category)}>Edit</button><button className="text-button muted-action" type="button" onClick={() => setPendingDelete({ type: 'category', item: category })}>Delete</button></div></div>)}
            {!categories.length && <div className="empty-state">Add a category to start building the menu.</div>}
          </div>
        </section>
        <section className="surface-card restaurant-form-card">
          <div className="section-heading"><div><span className="eyebrow">{editingItem ? 'EDIT DISH' : 'ADD A DISH'}</span><h2>{editingItem ? 'Update menu item' : 'New menu item'}</h2></div></div>
          <form className="form-stack" onSubmit={saveItem}>
            <FormField label="Category" name="itemCategory" value={form.categoryId} onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))} options={[{ value: '', label: 'Choose a category' }, ...categories.map((category) => ({ value: category.id, label: category.name }))]} required />
            <FormField label="Dish name" name="itemName" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} placeholder="e.g. Tomato soup" required />
            <div className="field-row"><FormField label="Price" name="itemPrice" type="number" min="0.01" step="0.01" value={form.price} onChange={(event) => setForm((current) => ({ ...current, price: event.target.value }))} required /><FormField label="Description" name="itemDescription" value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} placeholder="Optional" /></div>
            <div className="form-actions"><button className="button button-primary" type="submit" disabled={saving || !categories.length}>{saving ? 'Saving…' : editingItem ? 'Save changes' : 'Add to menu'}</button>{editingItem && <button className="button button-secondary" type="button" onClick={() => { setEditingItem(null); setForm(emptyItem) }}>Cancel</button>}</div>
          </form>
        </section>
      </div>
      <section className="surface-card restaurant-list-card restaurant-menu-catalog">
        <div className="table-toolbar"><div><h2>Menu items</h2><p>{items.length} dishes shown</p></div><form className="restaurant-toolbar-controls" onSubmit={(event) => { event.preventDefault(); setAppliedSearch(search.trim()) }}>
          <label className="search-box"><span aria-hidden="true">⌕</span><input aria-label="Search menu items" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search dishes" /></label>
          <button className="button button-secondary button-small" type="submit">Search</button>
        </form></div>
        <div className="restaurant-menu-category-tabs" role="tablist" aria-label="Filter menu by category">
          <button type="button" role="tab" aria-selected={!categoryFilter} className={!categoryFilter ? 'is-active' : ''} onClick={() => setCategoryFilter('')}>All categories</button>
          {categories.map((category) => <button type="button" role="tab" aria-selected={String(categoryFilter) === String(category.id)} className={String(categoryFilter) === String(category.id) ? 'is-active' : ''} key={category.id} onClick={() => setCategoryFilter(String(category.id))}>{category.name}</button>)}
        </div>
        {loading ? <Loader label="Loading menu" /> : <div className="table-scroll"><table className="data-table"><thead><tr><th>Dish</th><th>Category</th><th>Price</th><th>Availability</th><th /></tr></thead><tbody>
          {items.map((item) => <tr key={item.id}><td><strong>{item.name}</strong>{item.description && <small className="restaurant-subtext">{item.description}</small>}</td><td>{item.categoryName}</td><td>{money(item.price)}</td><td><div className="restaurant-menu-availability"><button className={`restaurant-availability-switch${item.available ? ' is-on' : ''}`} type="button" role="switch" aria-checked={item.available} aria-label={`${item.available ? 'Disable' : 'Enable'} ${item.name}`} onClick={() => toggleAvailability(item)}><span /></button><span>{item.available ? 'Available' : 'Unavailable'}</span></div></td><td><div className="table-actions"><button className="text-button" type="button" onClick={() => editItem(item)}>Edit</button><button className="text-button muted-action" type="button" onClick={() => setPendingDelete({ type: 'item', item })}>Delete</button></div></td></tr>)}
        </tbody></table>{!items.length && <div className="empty-state">No menu items match the selected filters.</div>}</div>}
      </section>
      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title={pendingDelete?.type === 'category' ? 'Delete menu category?' : 'Delete menu item?'}
        message={pendingDelete ? `“${pendingDelete.item.name}” will be removed from the menu.` : ''}
        confirmLabel={pendingDelete?.type === 'category' ? 'Delete category' : 'Delete item'}
        onCancel={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete?.type === 'category') deleteCategory(pendingDelete.item)
          if (pendingDelete?.type === 'item') deleteItem(pendingDelete.item)
        }}
      />
    </div>
  )
}
