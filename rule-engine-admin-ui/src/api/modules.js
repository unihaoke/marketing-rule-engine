import http from './index'

// ============================================================
// 事件管理 /api/events
// ============================================================
export const listEvents = () => http.get('/events')
export const getEvent = (eventCode) => http.get(`/events/${eventCode}`)
export const createEvent = (body) => http.post('/events', body)
export const updateEvent = (eventCode, body) => http.put(`/events/${eventCode}`, body)
export const deleteEvent = (eventCode) => http.delete(`/events/${eventCode}`)
export const setEventEnabled = (eventCode, enabled) =>
  http.post(`/events/${eventCode}/enable`, null, { params: { enabled } })

// ============================================================
// 规则配置 /api/rules
// ============================================================
export const listRules = () => http.get('/rules')
export const getRule = (ruleCode) => http.get(`/rules/${ruleCode}`)
export const createRule = (body) => http.post('/rules', body)
export const updateRule = (ruleCode, body) => http.put(`/rules/${ruleCode}`, body)
export const publishRule = (ruleCode, { changeLog = '', operator = '' } = {}) =>
  http.post(`/rules/${ruleCode}/publish`, null, { params: { changeLog, operator } })
export const rollbackVersion = (versionId, { changeLog = '', operator = '' } = {}) =>
  http.post(`/rules/versions/${versionId}/rollback`, null, { params: { changeLog, operator } })
export const setGray = (ruleCode, gray) => http.post(`/rules/${ruleCode}/gray`, gray)
export const onlineRule = (ruleCode) => http.post(`/rules/${ruleCode}/online`)
export const offlineRule = (ruleCode) => http.post(`/rules/${ruleCode}/offline`)
export const listVersions = (ruleCode) => http.get(`/rules/${ruleCode}/versions`)
export const getVersion = (versionId) => http.get(`/rules/versions/${versionId}`)
export const getVersionContent = (versionId) => http.get(`/rules/versions/${versionId}/content`)

// ============================================================
// 函数管理 /api/functions
// ============================================================
export const listFunctions = () => http.get('/functions')
export const getFunction = (functionName) => http.get(`/functions/${functionName}`)
export const registerFunction = (body) => http.post('/functions', body)
export const updateFunction = (functionName, body) => http.put(`/functions/${functionName}`, body)
export const deleteFunction = (functionName) => http.delete(`/functions/${functionName}`)
export const setFunctionEnabled = (functionName, enabled) =>
  http.post(`/functions/${functionName}/enable`, null, { params: { enabled } })
export const testFunction = (functionName, body) =>
  http.post(`/functions/${functionName}/test`, body)
export const uploadFunctionJar = (formData) =>
  http.post('/functions/upload-jar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
export const getExecutorTypes = () => http.get('/functions/executor-types')

// ============================================================
// 动作配置 /api/actions
// ============================================================
export const listActions = () => http.get('/actions')
export const getAction = (actionCode) => http.get(`/actions/${actionCode}`)
export const createAction = (body) => http.post('/actions', body)
export const updateAction = (actionCode, body) => http.put(`/actions/${actionCode}`, body)
export const deleteAction = (actionCode) => http.delete(`/actions/${actionCode}`)
export const setActionEnabled = (actionCode, enabled) =>
  http.post(`/actions/${actionCode}/enable`, null, { params: { enabled } })

// ============================================================
// 引擎运行时 /api/engine
// ============================================================
export const triggerEvent = (body) => http.post('/engine/trigger', body)
export const triggerBatch = (body) => http.post('/engine/trigger-batch', body)
export const simulateEvent = (body) => http.post('/engine/simulate', body)
export const getEngineStats = () => http.get('/engine/stats')
export const getEngineStatsByEvent = () => http.get('/engine/stats/by-event')
export const getEngineStatsByAction = () => http.get('/engine/stats/by-action')
export const getEngineStatsByDay = (days = 7) => http.get('/engine/stats/by-day', { params: { days } })
export const getEngineLogs = (params) => http.get('/engine/logs', { params })
export const getEngineLogDetails = (params) => http.get('/engine/logs/detail', { params })
export const getEngineActionLogs = (params) => http.get('/engine/action-logs', { params })
