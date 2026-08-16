/** 条件树工具：节点工厂 / 操作符常量 / 提交前值归一化 */

let uidSeed = 0

export const VALUE_TYPES = ['STRING', 'NUMBER', 'BOOLEAN', 'DATETIME', 'LIST']
export const LOGIC_TYPES = ['AND', 'OR', 'NOT']
export const LIST_OPERATORS = ['IN', 'NOT_IN', 'BETWEEN']
export const EXISTS_OPERATORS = ['EXISTS', 'NOT_EXISTS']

export const OPERATORS = [
  { value: 'EQUALS', label: '等于 =' },
  { value: 'NOT_EQUALS', label: '不等于 ≠' },
  { value: 'GT', label: '大于 >' },
  { value: 'GTE', label: '大于等于 ≥' },
  { value: 'LT', label: '小于 <' },
  { value: 'LTE', label: '小于等于 ≤' },
  { value: 'IN', label: '属于 IN' },
  { value: 'NOT_IN', label: '不属于 NOT IN' },
  { value: 'BETWEEN', label: '区间 BETWEEN' },
  { value: 'CONTAINS', label: '包含 CONTAINS' },
  { value: 'STARTS_WITH', label: '前缀 STARTS_WITH' },
  { value: 'EXISTS', label: '存在 EXISTS' },
  { value: 'NOT_EXISTS', label: '不存在 NOT EXISTS' },
  { value: 'EXPRESSION', label: '表达式 EXPRESSION' }
]

/** 新建叶子节点（_uid 用于递归渲染 key，不参与后端序列化） */
export function newLeafNode(payload = {}) {
  return {
    _uid: ++uidSeed,
    nodeType: 'LEAF',
    field: payload.field ?? '',
    operator: payload.operator ?? 'EQUALS',
    value: payload.value ?? null,
    valueType: payload.valueType ?? 'STRING',
    expression: payload.expression ?? '',
    not: !!payload.not
  }
}

/** 新建逻辑节点 */
export function newLogicNode(logic = 'AND') {
  return {
    _uid: ++uidSeed,
    nodeType: 'LOGIC',
    logic,
    children: []
  }
}

/** 默认条件树：AND 根节点 */
export function defaultTree() {
  return newLogicNode('AND')
}

/**
 * 提交前归一化（深拷贝后的普通对象上执行）：
 * - EXPRESSION / EXISTS / NOT_EXISTS：清空 value
 * - IN / NOT_IN / BETWEEN：逗号分隔字符串 -> 数组（NUMBER 转数值）
 * - 单值：按 valueType 转换为 NUMBER / BOOLEAN / LIST / STRING
 */
export function sanitizeTree(node) {
  if (!node) return
  delete node._uid // 内部渲染标记，不下发后端
  if (node.nodeType === 'LOGIC') {
    node.children = Array.isArray(node.children) ? node.children : []
    node.children.forEach(sanitizeTree)
    return
  }
  if (node.operator === 'EXPRESSION' || EXISTS_OPERATORS.includes(node.operator)) {
    node.value = null
    return
  }
  if (LIST_OPERATORS.includes(node.operator)) {
    node.value = toArrayValue(node.value, node.valueType)
    return
  }
  node.value = toScalarValue(node.value, node.valueType)
}

function toArrayValue(value, valueType) {
  const raw = Array.isArray(value) ? value.join(',') : String(value ?? '').trim()
  if (raw === '') return []
  const parts = raw.split(',').map((s) => s.trim()).filter(Boolean)
  return valueType === 'NUMBER' ? parts.map((s) => Number(s)) : parts
}

function toScalarValue(value, valueType) {
  if (valueType === 'NUMBER') {
    if (value === null || value === undefined || value === '') return null
    return typeof value === 'number' ? value : Number(String(value).trim())
  }
  if (valueType === 'BOOLEAN') {
    return value === true || value === 'true' || value === 1
  }
  if (valueType === 'LIST') return toArrayValue(value, valueType)
  return value === null || value === undefined ? '' : String(value)
}

/** 叶子显示值：数组转逗号分隔字符串（编辑框用） */
export function leafDisplayValue(node) {
  const v = node.value
  if (Array.isArray(v)) return v.join(', ')
  if (v === null || v === undefined) return ''
  return String(v)
}

/** 后端树反序列化：补充 _uid，便于递归组件渲染 key */
export function hydrateTree(node) {
  if (!node) return null
  node._uid = ++uidSeed
  if (node.nodeType === 'LOGIC') {
    if (!Array.isArray(node.children)) node.children = []
    node.children.forEach(hydrateTree)
  }
  return node
}
