import { defineStore } from 'pinia'
import {
  getEngineStats,
  getEngineStatsByEvent,
  getEngineStatsByAction,
  getEngineStatsByDay,
  getEngineLogs,
  getEngineLogDetails,
  getEngineActionLogs
} from '../api/modules'
import { normalizePage, toRows } from '../utils'

/** 引擎运行时 store：吞吐统计 / 统计报表 / 执行日志 / 动作日志（全部实时从数据库查询） */
export const useEngineStore = defineStore('engine', {
  state: () => ({
    stats: null,
    statsLoading: false,
    statsByEvent: [],
    statsByAction: [],
    statsByDay: [],
    reportsLoading: false,
    logDetails: { list: [], total: 0 },
    logDetailsLoading: false,
    logs: { list: [], total: 0 },
    logsLoading: false,
    actionLogs: { list: [], total: 0 },
    actionLogsLoading: false
  }),
  actions: {
    async fetchStats() {
      this.statsLoading = true
      try {
        this.stats = await getEngineStats()
      } finally {
        this.statsLoading = false
      }
    },
    async fetchStatsByEvent() {
      try {
        this.statsByEvent = toRows(await getEngineStatsByEvent())
      } catch (e) {
        this.statsByEvent = []
      }
    },
    async fetchStatsByAction() {
      try {
        this.statsByAction = toRows(await getEngineStatsByAction())
      } catch (e) {
        this.statsByAction = []
      }
    },
    async fetchStatsByDay(days = 7) {
      try {
        this.statsByDay = toRows(await getEngineStatsByDay(days))
      } catch (e) {
        this.statsByDay = []
      }
    },
    async fetchReports() {
      this.reportsLoading = true
      try {
        await Promise.all([this.fetchStatsByEvent(), this.fetchStatsByAction(), this.fetchStatsByDay()])
      } finally {
        this.reportsLoading = false
      }
    },
    async fetchLogDetails(params = {}) {
      this.logDetailsLoading = true
      try {
        const data = await getEngineLogDetails(params)
        const { list, total } = normalizePage(data)
        this.logDetails = { list, total }
      } finally {
        this.logDetailsLoading = false
      }
    },
    async fetchLogs(params = {}) {
      this.logsLoading = true
      try {
        const data = await getEngineLogs(params)
        const { list, total } = normalizePage(data)
        this.logs = { list, total }
      } finally {
        this.logsLoading = false
      }
    },
    async fetchActionLogs(params = {}) {
      this.actionLogsLoading = true
      try {
        const data = await getEngineActionLogs(params)
        const { list, total } = normalizePage(data)
        this.actionLogs = { list, total }
      } finally {
        this.actionLogsLoading = false
      }
    }
  }
})
