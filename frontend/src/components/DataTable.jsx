import { useMemo, useState } from 'react'
import Button from './Button'
import EmptyState from './EmptyState'
import Input from './Input'

export default function DataTable({
  columns,
  rows,
  rowKey = 'id',
  emptyMessage = 'Nothing to show yet.',
  page,
  totalPages,
  onPageChange,
  searchable = false,
  searchLabel = 'Search table',
  searchPlaceholder = 'Search…',
  searchValue,
  onSearch,
  pageSize,
}) {
  const [internalSearch, setInternalSearch] = useState('')
  const [localPage, setLocalPage] = useState(0)
  const controlledSearch = searchValue !== undefined
  const query = controlledSearch ? searchValue : internalSearch
  const externalPagination = Number.isInteger(totalPages) && totalPages > 0 && onPageChange
  const filteredRows = useMemo(() => {
    if (!searchable || onSearch) return rows
    const normalizedQuery = String(query || '').trim().toLocaleLowerCase()
    if (!normalizedQuery) return rows
    return rows.filter((row) => columns.some((column) => {
      const value = row[column.key]
      return value !== null && value !== undefined
        && String(value).toLocaleLowerCase().includes(normalizedQuery)
    }))
  }, [columns, onSearch, query, rows, searchable])
  const localTotalPages = Number.isInteger(pageSize) && pageSize > 0
    ? Math.ceil(filteredRows.length / pageSize)
    : 1
  const displayedRows = !externalPagination && Number.isInteger(pageSize) && pageSize > 0
    ? filteredRows.slice(localPage * pageSize, (localPage + 1) * pageSize)
    : filteredRows
  const activePage = externalPagination ? page ?? 0 : localPage
  const activeTotalPages = externalPagination ? totalPages : localTotalPages

  function updateSearch(value) {
    if (!controlledSearch) setInternalSearch(value)
    setLocalPage(0)
    onSearch?.(value)
  }

  function changePage(nextPage) {
    if (externalPagination) onPageChange(nextPage)
    else setLocalPage(nextPage)
  }

  if (!rows.length && !query) {
    return <EmptyState description={emptyMessage} className="ui-data-table-empty" />
  }
  if (!filteredRows.length) {
    return (
      <>
        {searchable && <Input label={searchLabel} name="data-table-search" type="search" value={query} onChange={(event) => updateSearch(event.target.value)} placeholder={searchPlaceholder} />}
        <EmptyState description="No matching records found." className="ui-data-table-empty" />
      </>
    )
  }

  return (
    <div className="ui-data-table">
      {searchable && (
        <div className="ui-data-table__search">
          <Input
            label={searchLabel}
            name="data-table-search"
            type="search"
            value={query}
            onChange={(event) => updateSearch(event.target.value)}
            placeholder={searchPlaceholder}
          />
        </div>
      )}
      <div className="table-scroll ui-data-table__scroll">
        <table className="data-table ui-data-table__table">
          <thead>
            <tr>{columns.map((column) => <th key={column.key} scope="col">{column.label}</th>)}</tr>
          </thead>
          <tbody>
            {displayedRows.map((row, index) => (
              <tr key={row[rowKey] ?? index}>
                {columns.map((column) => (
                  <td key={column.key}>
                    {column.render ? column.render(row) : row[column.key]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {activeTotalPages > 1 && (
        <div className="pagination ui-data-table__pagination">
          <span>Page {activePage + 1} of {activeTotalPages}</span>
          <div>
            <Button variant="secondary" size="small" disabled={activePage <= 0} onClick={() => changePage(activePage - 1)}>Previous</Button>
            <Button variant="secondary" size="small" disabled={activePage + 1 >= activeTotalPages} onClick={() => changePage(activePage + 1)}>Next</Button>
          </div>
        </div>
      )}
    </div>
  )
}
