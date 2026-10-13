import Input from './Input'
import Select from './Select'
import Textarea from './Textarea'

export default function FormField({
  label,
  name,
  type = 'text',
  value,
  onChange,
  required = false,
  placeholder,
  autoComplete,
  min,
  max,
  step,
  options,
  rows,
  help,
  error,
  disabled = false,
}) {
  const commonProps = {
    id: name,
    name,
    value: value ?? '',
    onChange,
    required,
    placeholder,
    autoComplete,
    disabled,
    min,
    max,
    step,
    hint: help,
    error,
    wrapperClassName: 'form-field',
  }

  if (options) return <Select {...commonProps} label={label} options={options} />
  if (rows) return <Textarea {...commonProps} label={label} rows={rows} />
  return <Input {...commonProps} label={label} type={type} />
}
