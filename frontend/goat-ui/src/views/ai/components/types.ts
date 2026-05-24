export interface AiTableColumn {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  type?: 'text' | 'status' | 'boolean'
  tooltip?: boolean
}

export type AiFormFieldType = 'input' | 'textarea' | 'number' | 'select' | 'switch' | 'datetime' | 'selectAsync' | 'upload'

export interface AiFormField {
  prop: string
  label: string
  type?: AiFormFieldType
  required?: boolean
  placeholder?: string
  span?: number
  rows?: number
  options?: Array<{ label: string; value: string | number | boolean }>
  defaultValue?: unknown
  // For selectAsync type - dynamically load options from API
  apiPath?: string
  labelField?: string
  valueField?: string
  // For upload type
  accept?: string
  tip?: string
}

export interface AiMetricCard {
  title: string
  value: string | number
  description: string
}
