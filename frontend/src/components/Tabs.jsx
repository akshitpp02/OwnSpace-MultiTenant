export default function Tabs({
  tabs,
  value,
  onChange,
  label = 'Sections',
  className = '',
}) {
  return (
    <div className={`ui-tabs${className ? ` ${className}` : ''}`} role="tablist" aria-label={label}>
      {tabs.map((tab) => {
        const tabValue = typeof tab === 'object' ? tab.value : tab
        const tabLabel = typeof tab === 'object' ? tab.label : tab
        const selected = tabValue === value

        return (
          <button
            key={tabValue}
            type="button"
            role="tab"
            aria-selected={selected}
            className={`ui-tab${selected ? ' ui-tab--active' : ''}`}
            onClick={() => onChange(tabValue)}
          >
            {tabLabel}
          </button>
        )
      })}
    </div>
  )
}
