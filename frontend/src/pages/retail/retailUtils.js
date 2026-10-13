import { formatCurrency } from '../../utils/format'

export function errorText(error, fallback = 'The request could not be completed. Please try again.') {
  return error.response?.data?.message || fallback
}

export function responseData(response) {
  return response?.data?.data ?? response?.data ?? null
}

export function responseList(value) {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  if (Array.isArray(value?.items)) return value.items
  if (Array.isArray(value?.records)) return value.records
  return []
}

export function money(value) {
  return formatCurrency(value)
}

export function dateTime(value) {
  return value ? new Date(value).toLocaleString() : '—'
}

export function productLabel(product) {
  return `${product.name} · ${product.sku}`
}

export function saleItems(sale) {
  return sale?.items || sale?.saleItems || []
}
