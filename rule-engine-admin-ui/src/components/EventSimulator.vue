<script setup>
/**
 * 事件模拟触发器（事件管理页使用）：
 * 按事件入参 schema 动态生成表单 → POST /api/engine/simulate → 展示
 * 命中规则 / 规则评估明细（为何命中/未命中）/ 动作执行明细（含解析后参数）/ 增强属性。
 */
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { simulateEvent, getEvent } from '../api/modules'
import { parseJsonText, toJsonText } from '../utils'

const props = defineProps({
  /** 事件编码（必填） */
  eventCode: { type: String, required: true },
  /** 事件定义（可选；传入则免去拉取） */
  event: { type: Object, default: null }
})

const emit = defineEmits(['simulated'])

const router = useRouter()

const eventDef = ref(props.event)
const loadingDef = ref(false)
const submitting = ref(false)
const result = ref(null)

/** 查看执行日志 */
function goLogs() {
  router.push('/logs')
}

/** 基础上下文 */
const base = reactive({
  userId: 'sim-u1',
  channelId: 'APP',
  eventTime: null // 空 = 当前时间
})

/** 动态参数（按 schema 预填默认值） */
const params = reactive({})

function defaultFor(type) {
  switch (type) {
    case 'NUMBER':
      return null
    case 'BOOLEAN':
      return false
    default:
      return ''
  }
}

function buildParams(schema) {
  Object.keys(params).forEach((k) => delete params[k])
  ;(schema || []).forEach((p) => {
    params[p.code] = p.defaultValue !== undefined && p.defaultValue !== null ? p.defaultValue : defaultFor(p.type)
  })
}

watch(
  () => props.eventCode,
  async (code) => {
    result.value = null
    if (!code) return
    if (props.event && props.event.eventCode === code) {
      eventDef.value = props.event
      buildParams(props.event.params)
    } else {
      await loadDefinition(code)
    }
  },
  { immediate: true }
)

async function loadDefinition(code) {
  loadingDef.value = true
  try {
    eventDef.value = await getEvent(code)
    buildParams(eventDef.value.params || [])
  } catch (e) {
    ElMessage.error(`事件定义加载失败：${e.message}`)
  } finally {
    loadingDef.value = false
  }
}

/** 参数 schema 列（带字段说明） */
const paramRows = computed(() => (eventDef.value?.params || []).map((p) => ({ ...p })))

function normalizeParamValue(p, raw) {
  switch (p.type) {
    case 'NUMBER': {
      if (raw === '' || raw === null || raw === undefined) return null
      const n = Number(raw)
      return Number.isNaN(n) ? raw : n
    }
    case 'BOOLEAN':
      return !!raw
    case 'DATETIME':
      return raw || null
    case 'JSON': {
      try {
        return parseJsonText(raw, null)
      } catch (e) {
        return raw
      }
    }
    case 'LIST': {
      if (Array.isArray(raw)) return raw
      if (raw === '' || raw === null || raw === undefined) return []
      return String(raw)
        .split(',')
        .map((s) => s.trim())
        .filter((s) => s !== '')
    }
    default:
      return raw
  }
}

/** 组装请求体并模拟触发 */
async function runSimulate() {
  if (!props.eventCode) {
    ElMessage.warning('请先选择事件')
    return
  }
  submitting.value = true
  result.value = null
  try {
    const body = {
      eventCode: props.eventCode,
      userId: base.userId || undefined,
      channelId: base.channelId || undefined,
      eventTime: base.eventTime ? new Date(base.eventTime.replace(/-/g, '/')).getTime() : undefined
    }
    const payload = {}
    paramRows.value.forEach((p) => {
      payload[p.code] = normalizeParamValue(p, params[p.code])
    })
    body.params = payload
    const data = await simulateEvent(body)
    result.value = data
    emit('simulated', data)
    if (data?.result && data.result.success === false) {
      ElMessage.error(`模拟触发执行失败：${data.result.errorMessage || '未知错误'}`)
    } else {
      ElMessage.success(`模拟触发完成：命中 ${data.result?.matchedRuleCodes?.length || 0} 条规则，执行日志已写入数据库`)
    }
  } catch (e) {
    ElMessage.error(`模拟触发请求失败：${e.message}`)
  } finally {
    submitting.value = false
  }
}

/** 命中规则（来自追踪明细，含动作） */
const traceRules = computed(() => result.value?.rules || [])

const matchedCount = computed(() => traceRules.value.filter((r) => r.matched).length)
const actionCount = computed(() =>
  traceRules.value.reduce((sum, r) => sum + (r.actions || []).length, 0)
)

const attrsEntries = computed(() => {
  const attrs = result.value?.result?.attributes
  return attrs ? Object.entries(attrs) : []
})

function ruleVerdictTag(r) {
  if (r.matched) return { type: 'success', text: '命中' }
  if (r.skipReason === 'GRAY_SKIP') return { type: 'warning', text: '灰度跳过' }
  if (r.skipReason === 'CONDITION_FAIL') return { type: 'info', text: '条件不满足' }
  return { type: 'danger', text: r.skipReason || '未命中' }
}

function reasonText(r) {
  if (r.matched) return '条件树全部满足'
  if (r.skipReason === 'GRAY_SKIP') return '灰度未放行（分桶未命中 / 渠道不在白名单）'
  if (r.skipReason === 'CONDITION_FAIL') return '条件树求值为 false（检查字段取值 / 函数增强输出）'
  return r.skipReason || ''
}
</script>

<template>
  <div>
    <el-alert
      v-if="eventDef"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 14px"
      :title="`事件「${eventDef.eventCode} ${eventDef.eventName || ''}」· 模拟触发（真实走引擎全链路，动作同步执行，可实时查看结果）`"
    />

    <el-form :model="base" label-width="110px" size="default" @submit.prevent>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="用户 ID">
            <el-input v-model="base.userId" placeholder="如 sim-u1 / u1001" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="渠道 ID">
            <el-input v-model="base.channelId" placeholder="如 APP / AD-ZHITONG" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="事件时间">
            <el-date-picker
              v-model="base.eventTime"
              type="datetime"
              placeholder="空 = 当前时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 动态入参（按事件 schema 渲染） -->
    <el-form label-width="110px" v-loading="loadingDef">
      <el-row :gutter="16">
        <el-col v-for="p in paramRows" :key="p.code" :span="12">
          <el-form-item :label="`${p.name || p.code}`" :required="p.required">
            <el-input
              v-if="p.type === 'STRING'"
              v-model="params[p.code]"
              :placeholder="p.description || p.code"
              clearable
            />
            <el-input-number
              v-else-if="p.type === 'NUMBER'"
              v-model="params[p.code]"
              :placeholder="p.description || p.code"
              :controls="false"
              style="width: 100%"
            />
            <el-switch v-else-if="p.type === 'BOOLEAN'" v-model="params[p.code]" />
            <el-date-picker
              v-else-if="p.type === 'DATETIME'"
              v-model="params[p.code]"
              type="datetime"
              placeholder="选择时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
            <el-input
              v-else-if="p.type === 'LIST'"
              v-model="params[p.code]"
              :placeholder="'逗号分隔，如 NEW_USER,ACTIVE'"
              clearable
            />
            <el-input
              v-else
              v-model="params[p.code]"
              type="textarea"
              :rows="2"
              :placeholder="p.description || 'JSON 对象或文本'"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div style="text-align: right; margin-bottom: 16px">
      <el-button :disabled="!props.eventCode" @click="result = null">清空结果</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!props.eventCode" @click="runSimulate">
        <el-icon style="margin-right: 4px"><CaretRight /></el-icon>开始模拟触发
      </el-button>
    </div>

    <!-- 结果面板 -->
    <template v-if="result">
      <el-alert
        :type="result.result?.success ? 'success' : 'error'"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #title>
          执行{{ result.result?.success ? '成功' : '失败' }} · 命中 {{ matchedCount }} 条规则 ·
          执行 {{ actionCount }} 个动作 · 耗时 {{ result.result?.costMs }} ms
          <span v-if="result.result?.errorMessage" style="margin-left: 8px">（{{ result.result.errorMessage }}）</span>
        </template>
      </el-alert>

      <!-- 日志落库提示 -->
      <div class="sim-log-tip">
        <el-icon><Document /></el-icon>
        <span>
          本次模拟已真实请求引擎接口并执行完整链路（事件归一化 → 函数增强 → 规则匹配 → 动作执行），
          执行日志与动作日志已写入 MySQL，可在「执行日志」页查看。
          <span v-if="result.result?.eventId" style="margin-left: 6px">事件ID：{{ result.result.eventId }}</span>
        </span>
        <el-button size="small" type="primary" link @click="goLogs">查看执行日志 →</el-button>
      </div>

      <!-- 命中规则 -->
      <div style="margin-bottom: 14px">
        <span style="font-weight: 600; margin-right: 8px">命中规则：</span>
        <template v-if="matchedCount">
          <el-tag v-for="r in traceRules.filter((x) => x.matched)" :key="r.ruleCode" type="success" style="margin-right: 6px">
            {{ r.ruleName || r.ruleCode }}（v{{ r.versionNo }}）
          </el-tag>
        </template>
        <el-tag v-else type="info">未命中任何规则</el-tag>
      </div>

      <!-- 规则评估明细 -->
      <el-table :data="traceRules" border stripe size="small" style="margin-bottom: 14px" max-height="280">
        <el-table-column prop="ruleCode" label="规则编码" min-width="160" />
        <el-table-column prop="ruleName" label="规则名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="版本" width="70" align="center">
          <template #default="{ row }">v{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column label="结论" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="ruleVerdictTag(row).type" size="small">{{ ruleVerdictTag(row).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ reasonText(row) }}</template>
        </el-table-column>
        <el-table-column label="动作" width="70" align="center">
          <template #default="{ row }">{{ (row.actions || []).length }}</template>
        </el-table-column>
        <el-table-column label="评估耗时" width="90" align="center">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
      </el-table>

      <!-- 动作执行明细 -->
      <template v-if="actionCount">
        <div style="font-weight: 600; margin-bottom: 8px">动作执行明细（含解析后参数）</div>
        <el-table
          :data="traceRules.flatMap((r) => (r.actions || []).map((a) => ({ ...a, ruleName: r.ruleName || r.ruleCode })))"
          border
          stripe
          size="small"
          style="margin-bottom: 14px"
          max-height="260"
        >
          <el-table-column prop="ruleName" label="规则" min-width="160" show-overflow-tooltip />
          <el-table-column prop="actionCode" label="动作编码" min-width="140" />
          <el-table-column label="参数（解析后）" min-width="240">
            <template #default="{ row }">
              <span style="font-family: monospace; font-size: 12px">{{ toJsonText(row.params) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
          <el-table-column prop="costMs" label="耗时" width="80" align="center">
            <template #default="{ row }">{{ row.costMs }} ms</template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 增强属性（函数输出等） -->
      <template v-if="attrsEntries.length">
        <div style="font-weight: 600; margin-bottom: 8px">运行时属性 / 函数增强输出</div>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item v-for="([k, v]) in attrsEntries" :key="k" :label="k">
            <span style="font-family: monospace; font-size: 12px">{{ toJsonText(v) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </template>
    <el-empty v-else :description="'填写上方参数后点击「开始模拟触发」'" :image-size="60" />
  </div>
</template>

<style scoped>
.sim-log-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  padding: 8px 10px;
  margin-bottom: 12px;
  line-height: 1.6;
}
</style>
