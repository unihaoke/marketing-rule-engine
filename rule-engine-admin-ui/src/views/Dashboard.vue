<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useEngineStore } from '../stores/engine'
import { formatTime, prettyKey } from '../utils'
import EChart from '../components/EChart.vue'

const store = useEngineStore()

/** 吞吐统计卡片 */
const statEntries = computed(() => {
  const s = store.stats
  if (!s || typeof s !== 'object') return []
  return Object.entries(s).filter(([, v]) => v !== null && v !== undefined)
})

function statDisplay(v) {
  if (v === null || v === undefined) return '-'
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

const AXIS_LABEL = { color: '#606266', fontSize: 12 }
const TOOLTIP_TEXT = {
  trigger: 'axis',
  backgroundColor: 'rgba(255,255,255,0.95)',
  borderColor: '#e4e7ed',
  textStyle: { color: '#303133' }
}

/** 按事件统计：触发量 / 命中量 柱状图 */
const eventChartOption = computed(() => {
  const rows = store.statsByEvent || []
  return {
    tooltip: {
      ...TOOLTIP_TEXT,
      formatter(params) {
        const row = rows.find((r) => r.eventCode === params[0]?.name)
        const lines = params.map((p) => `${p.marker}${p.seriesName}：${p.value ?? 0}`).join('<br/>')
        return `${params[0]?.name}<br/>${lines}${row?.avgCostMs != null ? `<br/>平均耗时：${row.avgCostMs} ms` : ''}`
      }
    },
    legend: { data: ['触发量', '命中量'], top: 0, textStyle: AXIS_LABEL },
    grid: { left: 40, right: 16, top: 32, bottom: 28 },
    xAxis: { type: 'category', data: rows.map((r) => r.eventCode), axisLabel: AXIS_LABEL },
    yAxis: { type: 'value', axisLabel: AXIS_LABEL },
    series: [
      { name: '触发量', type: 'bar', barMaxWidth: 34, data: rows.map((r) => r.events ?? 0), itemStyle: { color: '#409eff' } },
      { name: '命中量', type: 'bar', barMaxWidth: 34, data: rows.map((r) => r.matchedEvents ?? 0), itemStyle: { color: '#67c23a' } }
    ]
  }
})

/** 按动作统计：执行量 / 成功量 柱状图 */
const actionChartOption = computed(() => {
  const rows = store.statsByAction || []
  return {
    tooltip: {
      ...TOOLTIP_TEXT,
      formatter(params) {
        const row = rows.find((r) => r.actionCode === params[0]?.name)
        const lines = params.map((p) => `${p.marker}${p.seriesName}：${p.value ?? 0}`).join('<br/>')
        return `${params[0]?.name}<br/>${lines}${row?.avgCostMs != null ? `<br/>平均耗时：${row.avgCostMs} ms` : ''}`
      }
    },
    legend: { data: ['执行量', '成功量'], top: 0, textStyle: AXIS_LABEL },
    grid: { left: 40, right: 16, top: 32, bottom: 28 },
    xAxis: { type: 'category', data: rows.map((r) => r.actionCode), axisLabel: AXIS_LABEL },
    yAxis: { type: 'value', axisLabel: AXIS_LABEL },
    series: [
      { name: '执行量', type: 'bar', barMaxWidth: 34, data: rows.map((r) => r.actions ?? 0), itemStyle: { color: '#e6a23c' } },
      { name: '成功量', type: 'bar', barMaxWidth: 34, data: rows.map((r) => r.successActions ?? 0), itemStyle: { color: '#409eff' } }
    ]
  }
})

/** 每日触发趋势：折线图 */
const dayChartOption = computed(() => {
  const rows = store.statsByDay || []
  return {
    tooltip: TOOLTIP_TEXT,
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: rows.map((r) => r.statDate), boundaryGap: false, axisLabel: AXIS_LABEL },
    yAxis: { type: 'value', axisLabel: AXIS_LABEL },
    series: [
      {
        name: '触发量',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: rows.map((r) => r.events ?? 0),
        lineStyle: { width: 2, color: '#409eff' },
        itemStyle: { color: '#409eff' },
        areaStyle: { opacity: 0.12, color: '#409eff' }
      }
    ]
  }
})

/** 最近执行日志（明细，10 条） */
const recentLogs = computed(() => store.logDetails.list)

async function fetchAll() {
  try {
    await store.fetchStats()
  } catch (e) {
    ElMessage.error(`统计加载失败：${e.message}`)
  }
  try {
    await store.fetchReports()
  } catch (e) {
    ElMessage.error(`报表加载失败：${e.message}`)
  }
  try {
    await store.fetchLogDetails({ page: 1, size: 10 })
  } catch (e) {
    ElMessage.error(`最近日志加载失败：${e.message}`)
  }
}

onMounted(fetchAll)
</script>

<template>
  <div>
    <!-- 引擎吞吐统计（实时数据库聚合） -->
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="table-toolbar" style="margin-bottom: 0">
          <span style="font-weight: 600">引擎吞吐统计</span>
          <el-button size="small" :loading="store.statsLoading" @click="fetchAll">
            <el-icon style="margin-right: 4px"><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>
      <el-row v-if="statEntries.length" :gutter="16">
        <el-col
          v-for="([key, value]) in statEntries"
          :key="key"
          :span="Math.max(4, Math.floor(24 / Math.min(statEntries.length, 6)))"
          style="margin-bottom: 12px"
        >
          <div class="stat-card">
            <div class="stat-label">{{ prettyKey(key) }}</div>
            <div class="stat-value" :title="statDisplay(value)">{{ statDisplay(value) }}</div>
          </div>
        </el-col>
      </el-row>
      <el-empty v-else :description="store.statsLoading ? '统计加载中...' : '暂无统计数据'" :image-size="70" />
    </el-card>

    <!-- 数据统计报表（ECharts） -->
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="table-toolbar" style="margin-bottom: 0">
          <span style="font-weight: 600">数据统计报表</span>
          <el-button size="small" :loading="store.reportsLoading" @click="store.fetchReports()">
            <el-icon style="margin-right: 4px"><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>

      <el-row v-if="store.statsByEvent.length || store.statsByAction.length || store.statsByDay.length" :gutter="16">
        <!-- 按事件统计 -->
        <el-col :span="12">
          <div style="font-weight: 600; margin-bottom: 6px">按事件统计（触发量 / 命中量）</div>
          <EChart :option="eventChartOption" height="300px" />
        </el-col>
        <!-- 按动作统计 -->
        <el-col :span="12">
          <div style="font-weight: 600; margin-bottom: 6px">按动作统计（执行量 / 成功量）</div>
          <EChart :option="actionChartOption" height="300px" />
        </el-col>
      </el-row>

      <!-- 每日趋势 -->
      <template v-if="store.statsByDay.length">
        <div style="font-weight: 600; margin: 14px 0 6px">近 7 日触发趋势</div>
        <EChart :option="dayChartOption" height="260px" />
      </template>
      <el-empty
        v-else-if="!store.statsByEvent.length && !store.statsByAction.length && !store.statsByDay.length"
        :description="store.reportsLoading ? '报表加载中...' : '暂无统计数据（触发事件后自动生成）'"
        :image-size="70"
      />
    </el-card>

    <!-- 最近执行日志（明细，10 条） -->
    <el-card shadow="never">
      <template #header>
        <div class="table-toolbar" style="margin-bottom: 0">
          <span style="font-weight: 600">最近执行日志（10 条）</span>
          <el-button size="small" :loading="store.logDetailsLoading" @click="fetchAll">
            <el-icon style="margin-right: 4px"><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>
      <template v-if="recentLogs.length">
        <el-table :data="recentLogs" border stripe size="small" max-height="420">
          <el-table-column prop="eventCode" label="事件编码" min-width="130" />
          <el-table-column prop="userId" label="用户" min-width="110" />
          <el-table-column label="结果" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'" size="small">
                {{ row.success ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="命中规则" min-width="200">
            <template #default="{ row }">
              <template v-if="(row.matchedRuleCodes || []).length">
                <el-tag v-for="code in row.matchedRuleCodes" :key="code" type="success" size="small" style="margin-right: 4px">
                  {{ code }}
                </el-tag>
              </template>
              <span v-else style="color: #909399">未命中</span>
            </template>
          </el-table-column>
          <el-table-column label="执行动作" min-width="190">
            <template #default="{ row }">
              <template v-if="(row.actions || []).length">
                <el-tag
                  v-for="(a, i) in row.actions"
                  :key="i"
                  :type="a.success ? 'primary' : 'danger'"
                  size="small"
                  style="margin-right: 4px"
                >
                  {{ a.actionCode }}{{ a.success ? ' ✓' : ' ✗' }}
                </el-tag>
              </template>
              <span v-else style="color: #909399">-</span>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="80" align="center">
            <template #default="{ row }">{{ row.costMs }} ms</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty
        v-else
        :description="store.logDetailsLoading ? '日志加载中...' : '暂无执行日志（触发事件后自动记录）'"
        :image-size="70"
      />
    </el-card>
  </div>
</template>

<style scoped>
.stat-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 21, 41, 0.1);
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
