<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRule, updateRule, listFunctions, listActions, getEvent } from '../api/modules'
import ConditionTreeNode from '../components/ConditionTreeNode.vue'
import GrayConfigForm from '../components/GrayConfigForm.vue'
import JsonTextarea from '../components/JsonTextarea.vue'
import { defaultTree, sanitizeTree, hydrateTree, OPERATORS, LOGIC_TYPES } from '../utils/condition'
import { parseJsonText, toJsonText } from '../utils'

const route = useRoute()
const router = useRouter()
const ruleCode = String(route.params.ruleCode || '')

const loading = ref(true)
const saving = ref(false)

/** 分步向导：0 基础信息&灰度 / 1 条件与函数 / 2 动作 */
const stepIndex = ref(0)

const steps = [
  { title: '基础信息 & 灰度', desc: '规则名称、优先级、启停与灰度开关' },
  { title: '条件与函数', desc: '拖拽搭建条件树，选择前置增强函数' },
  { title: '动作', desc: '勾选执行动作并配置参数' }
]

const form = ref({
  ruleCode,
  ruleName: '',
  eventCode: '',
  description: '',
  priority: 100,
  enabled: false
})

const conditionTree = ref(defaultTree())
const functions = ref([])
const actions = ref([])
const gray = ref(emptyGray())

const eventParams = ref([])
const functionOptions = ref([])
const actionOptions = ref([])

/** 动作编码 → 动作定义（含参数 schema / frontDisplay） */
const actionDefMap = computed(() => {
  const map = {}
  ;(actionOptions.value || []).forEach((a) => {
    map[a.actionCode] = a
  })
  return map
})

/** 动作的"前端展示"参数列表（frontDisplay !== false） */
function displayParams(action) {
  const def = actionDefMap.value[action.actionCode]
  if (!def || !Array.isArray(def.params)) return []
  return def.params.filter((p) => p.frontDisplay !== false)
}

function hasDisplayParams(action) {
  return displayParams(action).length > 0
}

function emptyGray() {
  return { enabled: false, strategy: 'OFF', percent: 0, channels: [], bucketKey: 'userId' }
}

const VALID_VALUE_TYPES = ['STRING', 'NUMBER', 'BOOLEAN', 'DATETIME', 'LIST']

/** 叶子字段下拉选项：标准属性 + 事件入参 + 函数别名 */
const fields = computed(() => {
  const list = []
  const push = (code, name, type) => {
    if (!list.some((f) => f.code === code)) {
      list.push({ code, name, type: VALID_VALUE_TYPES.includes(type) ? type : 'STRING' })
    }
  }
  push('userId', '用户ID', 'STRING')
  push('channelId', '渠道ID', 'STRING')
  push('eventCode', '事件编码', 'STRING')
  eventParams.value.forEach((p) => push(p.code, p.name || p.code, p.type))
  functions.value.forEach((f) => push(f.alias || f.functionName, `函数:${f.functionName}`, 'STRING'))
  return list
})

/** 左侧面板"条件字段"预设 */
const paletteFields = computed(() => {
  const base = [
    { code: 'userId', valueType: 'STRING', name: '用户ID' },
    { code: 'channelId', valueType: 'STRING', name: '渠道ID' },
    { code: 'eventCode', valueType: 'STRING', name: '事件编码' }
  ]
  const params = eventParams.value.map((p) => ({
    code: p.code,
    valueType: VALID_VALUE_TYPES.includes(p.type) ? p.type : 'STRING',
    name: p.name || p.code
  }))
  return [...base, ...params]
})

onMounted(async () => {
  try {
    const data = await getRule(ruleCode)
    applyRule(data)
  } catch (e) {
    ElMessage.error(`加载规则失败：${e.message}`)
  }
  // 加载注册中心（失败仅提示，不影响画布使用）
  try {
    functionOptions.value = (await listFunctions()) || []
  } catch (e) {
    ElMessage.warning(`函数注册中心加载失败：${e.message}`)
  }
  try {
    actionOptions.value = (await listActions()) || []
  } catch (e) {
    ElMessage.warning(`动作配置加载失败：${e.message}`)
  }
  loading.value = false
})

function applyRule(data) {
  if (!data) return
  form.value = {
    ruleCode: data.ruleCode || ruleCode,
    ruleName: data.ruleName || '',
    eventCode: data.eventCode || '',
    description: data.description || '',
    priority: data.priority ?? 100,
    enabled: data.enabled !== false
  }
  conditionTree.value = hydrateTree(data.conditionTree) || defaultTree()
  functions.value = (data.functions || []).map((f) => ({
    functionName: f.functionName,
    alias: f.alias || f.functionName,
    bindingsText: toJsonText(f.bindings)
  }))
  actions.value = (data.actions || []).map((a) => ({
    actionCode: a.actionCode,
    async: a.async !== false,
    params: a.params || {}
  }))
  gray.value = {
    enabled: !!data.gray?.enabled,
    strategy: data.gray?.strategy || 'OFF',
    percent: data.gray?.percent ?? 0,
    channels: data.gray?.channels || [],
    bucketKey: data.gray?.bucketKey || 'userId'
  }
  if (data.eventCode) loadEventParams(data.eventCode)
}

async function loadEventParams(code) {
  try {
    const ev = await getEvent(code)
    eventParams.value = ev?.params || []
  } catch (e) {
    // 事件详情加载失败不阻塞画布
  }
}

// ---------------- 分步导航 ----------------

/** 条件树是否包含有效叶子（递归） */
function treeHasContent(node) {
  if (!node) return false
  if (node.nodeType === 'LEAF') return true
  if (node.nodeType === 'LOGIC') {
    return (node.children || []).some(treeHasContent)
  }
  return false
}

/** 校验当前步骤，通过返回 true */
function validateStep(step) {
  if (step === 0) {
    if (!form.value.ruleName.trim()) {
      ElMessage.warning('请填写规则名称')
      return false
    }
    if (!form.value.eventCode) {
      ElMessage.warning('请选择绑定事件')
      return false
    }
    return true
  }
  if (step === 1) {
    if (!treeHasContent(conditionTree.value)) {
      ElMessage.warning('请先搭建至少一个条件（可拖入字段或逻辑节点）')
      return false
    }
    for (const f of functions.value) {
      try {
        parseJsonText(f.bindingsText, {})
      } catch (e) {
        ElMessage.error(`函数「${f.functionName}」的绑定参数 JSON 格式错误`)
        return false
      }
    }
    return true
  }
  if (step === 2) {
    if (!actions.value.length) {
      ElMessage.warning('请至少配置一个执行动作')
      return false
    }
    return true
  }
  return true
}

function next() {
  if (!validateStep(stepIndex.value)) return
  if (stepIndex.value < steps.length - 1) {
    stepIndex.value += 1
  }
}

function prev() {
  if (stepIndex.value > 0) {
    stepIndex.value -= 1
  }
}

function goStep(index) {
  // 只能回到已通过的步骤；向前跳转需逐步校验
  if (index < stepIndex.value) {
    stepIndex.value = index
    return
  }
  for (let i = stepIndex.value; i < index; i++) {
    if (!validateStep(i)) return
  }
  stepIndex.value = index
}

// ---------------- 拖拽 ----------------
function dragStart(e, payload) {
  e.dataTransfer.setData('application/x-rule', JSON.stringify(payload))
  e.dataTransfer.effectAllowed = 'copy'
}

function readPayload(e) {
  try {
    return JSON.parse(e.dataTransfer.getData('application/x-rule'))
  } catch (err) {
    return null
  }
}

/** 画布中部：接收函数 / 动作拖放 */
function onCanvasDrop(e) {
  const p = readPayload(e)
  if (!p) return
  if (p.kind === 'function') addFunction(p.functionName)
  else if (p.kind === 'action') addAction(p.actionCode)
}

function onFunctionsZoneDrop(e) {
  const p = readPayload(e)
  if (p && p.kind === 'function') addFunction(p.functionName)
}

function onActionsZoneDrop(e) {
  const p = readPayload(e)
  if (p && p.kind === 'action') addAction(p.actionCode)
}

// ---------------- 函数 / 动作 ----------------
function addFunction(name) {
  if (!name) return
  if (functions.value.some((f) => f.functionName === name)) {
    ElMessage.warning(`函数「${name}」已添加`)
    return
  }
  functions.value.push({ functionName: name, alias: name, bindingsText: '{}' })
}

function removeFunction(i) {
  functions.value.splice(i, 1)
}

function addAction(code) {
  if (!code || !String(code).trim()) {
    return
  }
  const actionCode = String(code).trim()
  if (actions.value.some((a) => a.actionCode === actionCode)) {
    ElMessage.warning(`动作「${actionCode}」已添加`)
    return
  }
  actions.value.push({ actionCode, async: true, params: {}, paramsTextFallback: '{}' })
}

function removeAction(i) {
  actions.value.splice(i, 1)
}

// ---------------- 保存 ----------------
async function save() {
  if (!validateStep(0)) return
  if (!treeHasContent(conditionTree.value)) {
    ElMessage.warning('请先搭建至少一个条件（可拖入字段或逻辑节点）')
    stepIndex.value = 1
    return
  }
  if (!actions.value.length) {
    ElMessage.warning('请至少配置一个执行动作')
    stepIndex.value = 2
    return
  }

  const tree = JSON.parse(JSON.stringify(conditionTree.value))
  sanitizeTree(tree)

  const body = {
    ruleCode: form.value.ruleCode,
    ruleName: form.value.ruleName,
    eventCode: form.value.eventCode,
    description: form.value.description,
    priority: Number(form.value.priority) || 100,
    enabled: !!form.value.enabled,
    conditionTree: tree,
    functions: functions.value.map((f) => ({
      functionName: f.functionName,
      alias: f.alias || f.functionName,
      bindings: parseJsonText(f.bindingsText, {})
    })),
    actions: actions.value.map((a) => {
      let params = a.params || {}
      // 动作定义缺失时（兜底 JSON 编辑），解析文本作为参数
      if (!hasDisplayParams(a)) {
        try {
          params = parseJsonText(a.paramsTextFallback, {})
        } catch (e) {
          params = a.params || {}
        }
      }
      return { actionCode: a.actionCode, async: !!a.async, params }
    }),
    gray: { ...gray.value }
  }

  saving.value = true
  try {
    await updateRule(body.ruleCode, body)
    ElMessage.success('画布草稿保存成功')
  } catch (e) {
    ElMessage.error(`保存失败：${e.message}`)
  } finally {
    saving.value = false
  }
}

async function resetTree() {
  try {
    await ElMessageBox.confirm('确定清空当前条件树吗？', '清空确认', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    })
    conditionTree.value = defaultTree()
  } catch (e) {
    // 取消
  }
}
</script>

<template>
  <div class="canvas-page">
    <!-- ============ 顶部：标题 + 分步进度 ============ -->
    <div class="canvas-header">
      <div class="canvas-head-left">
        <span class="canvas-title">{{ form.ruleName || ruleCode }}</span>
        <el-tag size="small" type="primary" effect="plain">{{ form.eventCode || '未绑定事件' }}</el-tag>
      </div>
      <el-steps :active="stepIndex" align-center finish-status="success" class="canvas-steps">
        <el-step
          v-for="(s, i) in steps"
          :key="i"
          :title="s.title"
          :description="s.desc"
          @click="goStep(i)"
        />
      </el-steps>
      <div class="canvas-head-right">
        <el-button size="small" @click="router.push('/rules')">
          <el-icon style="margin-right: 4px"><Back /></el-icon>返回列表
        </el-button>
        <el-button size="small" type="primary" :loading="saving" @click="save">
          <el-icon style="margin-right: 4px"><Select /></el-icon>保存草稿
        </el-button>
      </div>
    </div>

    <!-- ============ 步骤 1：基础信息 & 灰度 ============ -->
    <div v-show="stepIndex === 0" class="step-body" v-loading="loading">
      <div class="step-row">
        <el-card shadow="never" class="step-card">
          <template #header><span class="side-title">基础信息</span></template>
          <el-form label-width="90px" size="default">
            <el-form-item label="规则编码" required>
              <el-input v-model="form.ruleCode" disabled />
            </el-form-item>
            <el-form-item label="规则名称" required>
              <el-input v-model="form.ruleName" placeholder="如 签到拉新-连续打卡返积分" />
            </el-form-item>
            <el-form-item label="绑定事件" required>
              <el-input :model-value="form.eventCode" disabled placeholder="创建时指定，不可修改" />
            </el-form-item>
            <el-form-item label="优先级">
              <el-input-number v-model="form.priority" :min="0" :max="99999" style="width: 100%" />
              <div class="form-tip">数值越小越先匹配（同事件多规则）</div>
            </el-form-item>
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" />
              <div class="form-tip">停用后引擎不再执行该规则（上下线开关）</div>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="规则说明" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="step-card">
          <template #header><span class="side-title">灰度配置</span></template>
          <GrayConfigForm v-model="gray" />
          <el-alert
            type="info"
            :closable="false"
            show-icon
            style="margin-top: 12px"
            title="灰度未开启时规则全量生效；开启后可按百分比分桶或渠道白名单逐步放量，同用户同规则分桶结果一致。"
          />
        </el-card>
      </div>
    </div>

    <!-- ============ 步骤 2：条件与函数 ============ -->
    <div v-show="stepIndex === 1" class="step-body">
      <!-- 左侧组件库 -->
      <div class="palette">
        <div class="palette-title">组件库（拖拽到画布）</div>
        <div class="palette-section">
          <div class="palette-section-title">条件字段</div>
          <div class="palette-grid">
            <div
              v-for="f in paletteFields"
              :key="f.code"
              class="palette-item"
              draggable="true"
              @dragstart="dragStart($event, { kind: 'leaf', field: f.code, valueType: f.valueType })"
            >
              <el-tag size="small" type="warning" effect="plain">{{ f.valueType }}</el-tag>
              <span>{{ f.code }}</span>
              <span v-if="f.name && f.name !== f.code" class="palette-item-name">{{ f.name }}</span>
            </div>
          </div>
        </div>

        <div class="palette-section">
          <div class="palette-section-title">操作符（拖到条件卡片上设置）</div>
          <div class="palette-grid">
            <div
              v-for="op in OPERATORS"
              :key="op.value"
              class="palette-item"
              draggable="true"
              @dragstart="dragStart($event, { kind: 'operator', operator: op.value })"
            >
              <span>{{ op.label }}</span>
            </div>
          </div>
        </div>

        <div class="palette-section">
          <div class="palette-section-title">逻辑节点</div>
          <div class="palette-grid">
            <div
              v-for="l in LOGIC_TYPES"
              :key="l"
              class="palette-item palette-logic"
              draggable="true"
              @dragstart="dragStart($event, { kind: 'logic', logic: l })"
            >
              <span style="font-weight: 700">{{ l }}</span>
              <span class="palette-item-name">逻辑组合</span>
            </div>
          </div>
        </div>

        <div class="palette-section">
          <div class="palette-section-title">前置函数（拖拽或点击添加到右侧）</div>
          <div class="palette-grid">
            <div
              v-for="f in functionOptions"
              :key="f.functionName"
              class="palette-item"
              draggable="true"
              @dragstart="dragStart($event, { kind: 'function', functionName: f.functionName })"
              @click="addFunction(f.functionName)"
            >
              <span>{{ f.functionName }}</span>
              <span v-if="f.displayName" class="palette-item-name">{{ f.displayName }}</span>
            </div>
            <div v-if="!functionOptions.length" class="palette-empty">暂无已注册函数</div>
          </div>
        </div>
      </div>

      <!-- 中间：条件树 -->
      <div class="canvas-center" @dragover.prevent @drop.prevent="onCanvasDrop">
        <div class="center-toolbar">
          <div class="tree-hint" style="margin-bottom: 0">
            <el-icon><InfoFilled /></el-icon>
            从左侧拖入「条件 / 逻辑节点」添加子节点；将「操作符」拖到条件卡片可快速切换操作符。
          </div>
          <el-button size="small" @click="resetTree">清空条件树</el-button>
        </div>
        <div class="tree-area">
          <ConditionTreeNode
            :node="conditionTree"
            :root="true"
            :depth="0"
            :fields="fields"
          />
        </div>
      </div>

      <!-- 右侧：前置函数 -->
      <div class="side-panel fn-panel">
        <el-card shadow="never" class="side-card" size="small">
          <template #header><span class="side-title">前置函数（匹配前增强）</span></template>
          <div class="drop-zone" @dragover.prevent.stop @drop.prevent.stop="onFunctionsZoneDrop">
            <div v-for="(f, i) in functions" :key="i" class="fn-item">
              <div class="fn-head">
                <el-tag size="small" type="success" effect="plain">{{ f.functionName }}</el-tag>
                <el-button size="small" type="danger" link @click="removeFunction(i)">删除</el-button>
              </div>
              <div class="fn-row">
                <span class="fn-label">别名</span>
                <el-input v-model="f.alias" size="small" placeholder="结果写入的属性名" />
              </div>
              <div class="fn-row fn-row-bindings">
                <span class="fn-label">绑定参数</span>
                <JsonTextarea
                  v-model="f.bindingsText"
                  :rows="6"
                  placeholder='绑定参数 JSON，如阶梯档位：{"keyField":"checkinStreak","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4}]}'
                />
              </div>
            </div>
            <div v-if="!functions.length" class="drop-hint">从左侧面板点击或拖拽函数添加</div>
          </div>
        </el-card>
        <div class="side-tip">
          <el-icon><InfoFilled /></el-icon>
          函数在规则匹配前执行，结果写入运行时属性，可在条件（如 checkinStreak &gt;= 3）与动作参数（如 #{rebateAmount * 10}）中引用。
        </div>
      </div>
    </div>

    <!-- ============ 步骤 3：动作 ============ -->
    <div v-show="stepIndex === 2" class="step-body">
      <!-- 左侧动作库 -->
      <div class="palette">
        <div class="palette-title">动作库（拖拽或点击添加到右侧）</div>
        <div class="palette-section">
          <div class="palette-grid">
            <div
              v-for="a in actionOptions"
              :key="a.actionCode"
              class="palette-item"
              draggable="true"
              @dragstart="dragStart($event, { kind: 'action', actionCode: a.actionCode })"
              @click="addAction(a.actionCode)"
            >
              <el-tag size="small" type="danger" effect="plain">{{ a.actionType }}</el-tag>
              <span>{{ a.actionCode }}</span>
              <span v-if="a.actionName" class="palette-item-name">{{ a.actionName }}</span>
            </div>
            <div v-if="!actionOptions.length" class="palette-empty">暂无已配置动作</div>
          </div>
        </div>
      </div>

      <!-- 右侧：动作列表（占满剩余空间，动作项多列网格） -->
      <div class="side-panel act-panel">
        <el-card shadow="never" class="side-card" size="small">
          <template #header>
            <span class="side-title">执行动作（命中后依次执行；参数按动作配置的「前端展示」项填写，支持 ${} 引用与 #{} 表达式）</span>
          </template>
          <div class="drop-zone" @dragover.prevent.stop @drop.prevent.stop="onActionsZoneDrop">
            <div v-if="actions.length" class="act-list">
              <div v-for="(a, i) in actions" :key="i" class="act-item">
                <div class="act-head">
                  <el-tag size="small" type="danger" effect="plain">{{ a.actionCode }}</el-tag>
                  <span class="act-async">
                    异步
                    <el-switch v-model="a.async" size="small" />
                  </span>
                  <el-button size="small" type="danger" link @click="removeAction(i)">删除</el-button>
                </div>

                <!-- 按动作参数 schema 渲染"名称-值"行（仅前端展示项） -->
                <template v-if="hasDisplayParams(a)">
                  <div
                    v-for="p in displayParams(a)"
                    :key="p.code"
                    class="act-param-row"
                  >
                    <span class="act-param-label" :title="p.description || p.code">
                      {{ p.name || p.code }}<i v-if="p.required" class="req-star">*</i>
                    </span>
                    <el-input-number
                      v-if="p.type === 'NUMBER'"
                      v-model="a.params[p.code]"
                      :placeholder="p.description || p.code"
                      :controls="false"
                      size="small"
                      style="flex: 1"
                    />
                    <el-switch
                      v-else-if="p.type === 'BOOLEAN'"
                      v-model="a.params[p.code]"
                      size="small"
                    />
                    <el-input
                      v-else
                      v-model="a.params[p.code]"
                      size="small"
                      :placeholder="`${p.description || p.name || p.code}（支持 ${'${'}字段${'}'} / #{表达式}）`"
                    />
                  </div>
                  <div class="act-param-empty" v-if="!displayParams(a).length" />
                </template>

                <!-- 动作定义缺失时的兜底 JSON 编辑 -->
                <JsonTextarea
                  v-else
                  v-model="a.paramsTextFallback"
                  :rows="3"
                  placeholder="动作定义未加载，可先填 JSON 参数，如 {&quot;couponTemplateId&quot;: &quot;CT-1&quot;}"
                />
              </div>
            </div>
            <div v-if="!actions.length" class="drop-hint">从左侧动作库点击或拖拽添加</div>
          </div>
        </el-card>
        <div class="side-tip">
          <el-icon><InfoFilled /></el-icon>
          动作参数只能填写值（名称来自动作配置，不可修改）；仅「前端展示 = 是」的参数会显示。支持 ${字段} 引用与 #{表达式} 动态计算。
        </div>
      </div>
    </div>

    <!-- ============ 底部导航 ============ -->
    <div class="canvas-footer">
      <el-button :disabled="stepIndex === 0" @click="prev">
        <el-icon style="margin-right: 4px"><ArrowLeft /></el-icon>上一步
      </el-button>
      <el-button v-if="stepIndex < steps.length - 1" type="primary" @click="next">
        下一步<el-icon style="margin-left: 4px"><ArrowRight /></el-icon>
      </el-button>
      <el-button v-else type="primary" :loading="saving" @click="save">
        <el-icon style="margin-right: 4px"><Select /></el-icon>保存草稿
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.canvas-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  min-height: 560px;
  gap: 10px;
}

/* ---------- 顶部 ---------- */
.canvas-header {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  padding: 12px 16px;
  flex-shrink: 0;
}

.canvas-head-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 220px;
}

.canvas-title {
  font-weight: 700;
  font-size: 15px;
}

.canvas-steps {
  flex: 1;
  min-width: 420px;
}

.canvas-head-right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* ---------- 步骤内容 ---------- */
.step-body {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

.step-row {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  overflow-y: auto;
}

.step-card {
  border-radius: 8px;
  align-self: start;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  width: 100%;
}

.side-title {
  font-weight: 600;
  font-size: 13px;
}

/* ---------- 左侧面板 ---------- */
.palette {
  width: 250px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  padding: 12px;
  overflow-y: auto;
}

.palette-title {
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 10px;
  color: #303133;
}

.palette-section {
  margin-bottom: 14px;
}

.palette-section-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  border-left: 3px solid #409eff;
  padding-left: 6px;
}

.palette-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 5px 8px;
  font-size: 12px;
  background: #fafafa;
  cursor: grab;
  transition: all 0.15s;
  flex-wrap: wrap;
}

.palette-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateX(2px);
}

.palette-item:active {
  cursor: grabbing;
}

.palette-item-name {
  color: #909399;
}

.palette-logic {
  background: #f0f9eb;
  border-color: #67c23a;
}

.palette-empty {
  font-size: 12px;
  color: #c0c4cc;
  padding: 4px 0;
}

/* ---------- 中间画布 ---------- */
.canvas-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.center-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #ebeef5;
  gap: 8px;
}

.tree-area {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}

.tree-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 8px 10px;
}

/* ---------- 右侧面板 ---------- */
.side-panel {
  width: 360px;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 前置函数面板：加宽，便于完整查看绑定参数 */
.fn-panel {
  width: 460px;
}

/* 动作步骤：右侧面板占满剩余空间 */
.act-panel {
  flex: 1;
  width: auto;
  min-width: 0;
}

/* 动作项上下单列（每行一个动作，添加时追加到下方） */
.act-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.act-list .act-item {
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}

.side-card {
  border-radius: 8px;
}

.side-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 8px 10px;
  line-height: 1.6;
}

.drop-zone {
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 8px;
  min-height: 60px;
}

.drop-hint {
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  padding: 10px 0;
}

.fn-item,
.act-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 10px;
  background: #fafafa;
}

.fn-head,
.act-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.act-async {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

/* 动作参数"名称-值"行 */
.act-param-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.act-param-label {
  width: 120px;
  flex-shrink: 0;
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.req-star {
  color: #f56c6c;
  font-style: normal;
  margin-left: 2px;
}

.fn-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.fn-row-bindings {
  margin-bottom: 2px;
}

.fn-label {
  width: 72px;
  font-size: 12px;
  color: #606266;
  line-height: 28px;
  flex-shrink: 0;
}

/* ---------- 底部导航 ---------- */
.canvas-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 4px 2px;
  flex-shrink: 0;
}
</style>
