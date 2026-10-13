const indianRupees = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
})

export function formatCurrency(value) {
  const amount = Number(value ?? 0)
  return indianRupees.format(Number.isFinite(amount) ? amount : 0)
}
