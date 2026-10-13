import { formatCurrency } from '../../utils/format'

export function errorText(error, fallback = 'The request could not be completed. Please try again.') {
  return error.response?.data?.message || fallback
}

export function money(value) {
  return formatCurrency(value)
}

export function roleLabel(value = '') {
  return value.replaceAll('_', ' ')
}
