<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useEngineStore } from '../stores/engine'
import { formatCell, formatTime, prettyKey } from '../utils'

const store = useEngineStore()
const activeTab = ref('detail')

// 执行明细细过滤（命中规则 / 动作 / 耗时 / 属性）
const detailPage = ref(1)
const detailSize = ref(10)
const detailFilters = ref({ eventCode: '', userId: '' })

// 执行日志过滤
const execPage = ref(1)
const execSize = ref(10)
const execFilters = ref({ eventCode: '', userId: '' })

// 动作日志过滤
const actionPage = ref(1)
const actionSize = ref(10)
const actionFilters = ref({ eventId: '', ruleCode: '', actionCode: '' })

const execColumns = computed(() => buildColumns(store.logs.list))
const actionColumns = computed(() => buildColumns(store.actionLogs.list))

function buildColumns(list) {
  const first = list[0]
  if (!first || typeof first !== 'object') return []
  return Object.keys(first).filter((k) => k !== '_uid')
}

function isTimeField(key) {
  return ['startedAt', 'finishedAt', 'eventTime', 'createdAt', 'updatedAt'].includes(key)
}

async function loadDetails() {
  try {
    await store.fetchLogDetails({
      page: detailPage.value,
      size: detailSize.value,
      eventCode: detailFilters.value.eventCode || undefined,
      userId: detailFilters.value.userId || undefined
    })
  } catch (e) {
    ElMessage.error(`执行明细加载失败：${e.message}`)
  }
}

async function loadExec() {
  try {
    await store.fetchLogs({
      page: execPage.value,
      size: execSize.value,
      eventCode: execFilters.value.eventCode || undefined,
      userId: execFilters.value.userId || undefined
    })
  } catch (e) {
    ElMessage.error(`执行日志加载失败：${e.message}`)
  }
}

async function loadActionLogs() {
  try {
    await store.fetchActionLogs({
      page: actionPage.value,
      size: actionSize.value,
      eventId: actionFilters.value.eventId || undefined,
      ruleCode: actionFilters.value.ruleCode || undefined,
      actionCode: actionFilters.value.actionCode || undefined
    })
  } catch (e) {
    ElMessage.error(`动作日志加载失败：${e.message}`)
  }
}

function searchDetails() {
  detailPage.value = 1
  loadDetails()
}

function resetDetails() {
  detailFilters.value = { eventCode: '', userId: '' }
  detailPage.value = 1
  loadDetails()
}

function searchExec() {
  execPage.value = 1
  loadExec()
}

function resetExec() {
  execFilters.value = { eventCode: '', userId: '' }
  execPage.value = 1
  loadExec()
}

function searchAction() {
  actionPage.value = 1
  loadActionLogs()
}

function resetAction() {
  actionFilters.value = { eventId: '', ruleCode: '', actionCode: '' }
  actionPage.value = 1
  loadActionLogs()
}

watch(activeTab, (tab) => {
  if (tab === 'detail' && !store.logDetails.list.length) {
    loadDetails()
  }
  if (tab === 'action' && !store.actionLogs.list.length) {
    loadActionLogs()
  }
})

onMounted(() => {
  loadDetails()
})
</script>

<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab">
      <!-- ==================== 执行明细 ==================== -->
      <el-tab-pane label="执行明细" name="detail">
        <div class="table-toolbar">
          <div style="display: flex; gap: 8px; flex-wrap: wrap">
            <el-input
              v-model="detailFilters.eventCode"
              placeholder="事件编码"
              clearable
              style="width: 180px"
              @keyup.enter="searchDetails"
            />
            <el-input
              v-model="detailFilters.userId"
              placeholder="用户ID"
              clearable
              style="width: 180px"
              @keyup.enter="searchDetails"
            />
            <el-button type="primary" :loading="store.logDetailsLoading" @click="searchDetails">
              <el-icon style="margin-right: 4px"><Search /></el-icon>查询
            </el-button>
            <el-button @click="resetDetails">重置</el-button>
          </div>
        </div>

        <el-table v-loading="store.logDetailsLoading" :data="store.logDetails.list" border stripe size="small">
          <el-table-column prop="eventCode" label="事件编码" min-width="130" />
          <el-table-column prop="userId" label="用户" min-width="110" />
          <el-table-column label="结果" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'" size="small">
                {{ row.success ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="命中规则" min-width="210">
            <template #default="{ row }">
              <template v-if="(row.matchedRuleCodes || []).length">
                <el-tag v-for="code in row.matchedRuleCodes" :key="code" type="success" size="small" style="margin-right: 4px">
                  {{ code }}
                </el-tag>
              </template>
              <span v-else style="color: #909399">未命中</span>
            </template>
          </el-table-column>
          <el-table-column label="执行动作" min-width="210">
            <template #default="{ row }">
              <template v-if="(row.actions || []).length">
                <el-tag
                  v-for="(a, i) in row.actions"
                  :key="i"
                  :type="a.success ? 'primary' : 'danger'"
                  size="small"
                  style="margin-right: 4px"
                  :title="a.detail"
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
          <el-table-column label="属性 / 增强输出" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span style="font-family: monospace; font-size: 12px">{{ formatCell(row.attributes) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="错误信息" min-width="150" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="detailPage"
            v-model:page-size="detailSize"
            background
            layout="total, sizes, prev, pager, next"
            :total="store.logDetails.total"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadDetails"
            @size-change="searchDetails"
          />
        </div>
      </el-tab-pane>

      <!-- ==================== 执行日志 ==================== -->
      <el-tab-pane label="执行日志" name="exec">
        <div class="table-toolbar">
          <div style="display: flex; gap: 8px; flex-wrap: wrap">
            <el-input
              v-model="execFilters.eventCode"
              placeholder="事件编码"
              clearable
              style="width: 180px"
              @keyup.enter="searchExec"
            />
            <el-input
              v-model="execFilters.userId"
              placeholder="用户ID"
              clearable
              style="width: 180px"
              @keyup.enter="searchExec"
            />
            <el-button type="primary" :loading="store.logsLoading" @click="searchExec">
              <el-icon style="margin-right: 4px"><Search /></el-icon>查询
            </el-button>
            <el-button @click="resetExec">重置</el-button>
          </div>
        </div>

        <el-table v-loading="store.logsLoading" :data="store.logs.list" border stripe size="small">
          <el-table-column
            v-for="c in execColumns"
            :key="c"
            :prop="c"
            :label="prettyKey(c)"
            :min-width="isTimeField(c) ? 165 : c === 'errorMessage' ? 200 : 120"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <template v-if="isTimeField(c) && typeof row[c] === 'number'">
                {{ formatTime(row[c]) }}
              </template>
              <template v-else>{{ formatCell(row[c]) }}</template>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="execPage"
            v-model:page-size="execSize"
            background
            layout="total, sizes, prev, pager, next"
            :total="store.logs.total"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadExec"
            @size-change="searchExec"
          />
        </div>
      </el-tab-pane>

      <!-- ==================== 动作日志 ==================== -->
      <el-tab-pane label="动作日志" name="action">
        <div class="table-toolbar">
          <div style="display: flex; gap: 8px; flex-wrap: wrap">
            <el-input
              v-model="actionFilters.eventId"
              placeholder="事件ID"
              clearable
              style="width: 180px"
              @keyup.enter="searchAction"
            />
            <el-input
              v-model="actionFilters.ruleCode"
              placeholder="规则编码"
              clearable
              style="width: 180px"
              @keyup.enter="searchAction"
            />
            <el-input
              v-model="actionFilters.actionCode"
              placeholder="动作编码"
              clearable
              style="width: 180px"
              @keyup.enter="searchAction"
            />
            <el-button type="primary" :loading="store.actionLogsLoading" @click="searchAction">
              <el-icon style="margin-right: 4px"><Search /></el-icon>查询
            </el-button>
            <el-button @click="resetAction">重置</el-button>
          </div>
        </div>

        <el-table
          v-loading="store.actionLogsLoading"
          :data="store.actionLogs.list"
          border
          stripe
          size="small"
        >
          <el-table-column
            v-for="c in actionColumns"
            :key="c"
            :prop="c"
            :label="prettyKey(c)"
            :min-width="isTimeField(c) ? 165 : c === 'detail' ? 220 : 120"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <template v-if="isTimeField(c) && typeof row[c] === 'number'">
                {{ formatTime(row[c]) }}
              </template>
              <template v-else>{{ formatCell(row[c]) }}</template>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="actionPage"
            v-model:page-size="actionSize"
            background
            layout="total, sizes, prev, pager, next"
            :total="store.actionLogs.total"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadActionLogs"
            @size-change="searchAction"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>
