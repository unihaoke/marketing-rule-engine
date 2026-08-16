/** 通用工具函数 */

/** 分页响应归一化：兼容 {total, records|list|content|rows} 与纯数组 */
export function normalizePage(data) {
  let list = []
  if (Array.isArray(data)) {
    list = data
  } else if (data && typeof data === 'object') {
    for (const key of ['records', 'list', 'content', 'rows', 'items']) {
      if (Array.isArray(data[key])) {
        list = data[key]
        break
      }
    }
  }
  let total = list.length
  if (data && typeof data === 'object') {
    if (typeof data.total === 'number') total = data.total
    else if (typeof data.totalElements === 'number') total = data.totalElements
    else if (typeof data.count === 'number') total = data.count
  }
  return { list, total }
}

/** 任意响应归一化为行数组（统计 / 演示结果展示用） */
export function toRows(data) {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object') {
    for (const key of ['records', 'list', 'content', 'rows', 'results', 'scenarios', 'items']) {
      if (Array.isArray(data[key])) return data[key]
    }
    return [data]
  }
  return []
}

/** 安全解析 JSON 字符串；空串返回 fallback，解析失败抛出（带 isJsonError 标记） */
export function parseJsonText(text, fallback = {}) {
  if (text === undefined || text === null || String(text).trim() === '') return fallback
  try {
    return JSON.parse(text)
  } catch (e) {
    e.isJsonError = true
    throw e
  }
}

/** 对象转 JSON 字符串（缩进 2），非对象原样返回 */
export function toJsonText(value) {
  if (value === undefined || value === null) return '{}'
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch (e) {
    return '{}'
  }
}

/** 时间格式化：时间戳(number/13位字符串) 或 'yyyy-MM-ddTHH:mm:ss' 字符串 */
export function formatTime(v) {
  if (v === undefined || v === null || v === '') return '-'
  if (typeof v === 'number') {
    const d = new Date(v)
    if (Number.isNaN(d.getTime())) return String(v)
    return formatDate(d)
  }
  if (typeof v === 'string' && /^\d{13}$/.test(v)) return formatTime(Number(v))
  return String(v).replace('T', ' ').slice(0, 19)
}

function formatDate(d) {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/** 表格单元格格式化：对象 / 数组转 JSON 字符串 */
export function formatCell(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

/** 字段名转中文展示：userId -> userId，snake_case -> 空格分隔 */
export function prettyKey(key) {
  if (key === null || key === undefined) return ''
  return String(key).replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}
