<script setup>
import { computed, ref } from 'vue'
import {
  newLeafNode,
  newLogicNode,
  OPERATORS,
  VALUE_TYPES,
  LOGIC_TYPES,
  LIST_OPERATORS,
  EXISTS_OPERATORS,
  leafDisplayValue
} from '../utils/condition'

/**
 * 条件树递归编辑器节点组件。
 * - LOGIC 节点：逻辑选择(AND/OR/NOT) + children 容器（拖入条件/逻辑节点追加子节点）+ 增删按钮
 * - LEAF 节点：字段 / 操作符 / 值类型 / 值 / 表达式 / 取反 编辑表单；
 *   卡片整体接收"操作符"拖放；底部条带接收"条件/逻辑节点"拖放（作为兄弟节点插入其后）
 * 支持任意深度递归嵌套，拖放使用原生 HTML5 dragstart / dragover / drop。
 */
const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  root: { type: Boolean, default: false },
  siblings: { type: Array, default: null },
  index: { type: Number, default: -1 },
  fields: { type: Array, default: () => [] }
})

const isLogic = computed(() => props.node.nodeType === 'LOGIC')
const isExpression = computed(() => props.node.operator === 'EXPRESSION')
const isExistsOp = computed(() => EXISTS_OPERATORS.includes(props.node.operator))
const isListOp = computed(() => LIST_OPERATORS.includes(props.node.operator))
const showValue = computed(() => !isExpression.value && !isExistsOp.value)

/** 值编辑：数组 <-> 逗号分隔字符串双向映射 */
const displayValue = computed({
  get: () => leafDisplayValue(props.node),
  set: (val) => {
    props.node.value = val
  }
})

// 拖放高亮状态
const childZoneOver = ref(false)
const insertOver = ref(false)

function parsePayload(e) {
  try {
    return JSON.parse(e.dataTransfer.getData('application/x-rule'))
  } catch (err) {
    return null
  }
}

/** 逻辑节点 children 容器：拖入条件/逻辑节点 -> 追加为子节点 */
function onChildZoneDrop(e) {
  childZoneOver.value = false
  const p = parsePayload(e)
  if (!p) return
  ensureChildren()
  if (p.kind === 'leaf') props.node.children.push(newLeafNode(p))
  else if (p.kind === 'logic') props.node.children.push(newLogicNode(p.logic))
}

function ensureChildren() {
  if (!Array.isArray(props.node.children)) props.node.children = []
}

/** 叶子卡片：接收"操作符"拖放 */
function onLeafCardDrop(e) {
  const p = parsePayload(e)
  if (p && p.kind === 'operator') {
    props.node.operator = p.operator
    if (p.operator === 'EXPRESSION' && !props.node.expression) {
      props.node.expression = ''
    }
  }
}

/** 叶子底部条带：拖入条件/逻辑节点 -> 在自身之后插入兄弟节点 */
function onInsertDrop(e) {
  insertOver.value = false
  const p = parsePayload(e)
  if (!p || props.siblings == null || props.index < 0) return
  const at = props.index + 1
  if (p.kind === 'leaf') props.siblings.splice(at, 0, newLeafNode(p))
  else if (p.kind === 'logic') props.siblings.splice(at, 0, newLogicNode(p.logic))
}

function addLeafChild() {
  ensureChildren()
  props.node.children.push(newLeafNode())
}

function addLogicChild() {
  ensureChildren()
  props.node.children.push(newLogicNode('AND'))
}

function removeSelf() {
  if (props.siblings != null && props.index >= 0) {
    props.siblings.splice(props.index, 1)
  }
}
</script>

<template>
  <!-- ==================== 逻辑节点 ==================== -->
  <div v-if="isLogic" class="logic-node">
    <div class="logic-header">
      <el-select v-model="node.logic" size="small" style="width: 92px">
        <el-option v-for="l in LOGIC_TYPES" :key="l" :label="l" :value="l" />
      </el-select>
      <span class="logic-tip">逻辑组合（拖入子节点或点击添加）</span>
      <div class="logic-actions">
        <el-button size="small" type="primary" plain @click="addLeafChild">+ 条件</el-button>
        <el-button size="small" type="primary" plain @click="addLogicChild">+ 逻辑</el-button>
        <el-button v-if="!root" size="small" type="danger" plain @click="removeSelf">删除</el-button>
      </div>
    </div>

    <div
      class="logic-children"
      :class="{ 'drag-over': childZoneOver }"
      @dragover.prevent.stop="childZoneOver = true"
      @dragleave="childZoneOver = false"
      @drop.prevent.stop="onChildZoneDrop"
    >
      <template v-if="node.children && node.children.length">
        <ConditionTreeNode
          v-for="(child, i) in node.children"
          :key="child._uid ?? i"
          :node="child"
          :siblings="node.children"
          :index="i"
          :depth="depth + 1"
          :fields="fields"
        />
      </template>
      <div v-else class="empty-hint">
        暂无子条件 —— 从左侧拖入「条件」或「逻辑节点」，或点击上方 + 添加
      </div>
    </div>
  </div>

  <!-- ==================== 叶子节点 ==================== -->
  <div
    v-else
    class="leaf-node"
    :class="{ 'drag-over': insertOver }"
    @dragover.prevent.stop
    @drop.prevent.stop="onLeafCardDrop"
  >
    <div class="leaf-head">
      <el-tag size="small" type="warning" effect="plain">条件</el-tag>
      <span class="leaf-desc">字段 · 操作符 · 值</span>
      <div class="leaf-actions">
        <el-checkbox v-model="node.not" size="small">取反</el-checkbox>
        <el-button v-if="siblings != null && index >= 0" size="small" type="danger" link @click="removeSelf">
          删除
        </el-button>
      </div>
    </div>

    <div class="leaf-form">
      <el-select
        v-model="node.field"
        filterable
        allow-create
        default-first-option
        placeholder="选择或输入字段"
        style="flex: 1.4; min-width: 140px"
      >
        <el-option
          v-for="f in fields"
          :key="f.code"
          :label="f.name ? `${f.code}（${f.name}）` : f.code"
          :value="f.code"
        />
      </el-select>

      <el-select v-model="node.operator" placeholder="操作符" style="flex: 1.2; min-width: 150px">
        <el-option v-for="o in OPERATORS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>

      <el-select
        v-if="!isExpression"
        v-model="node.valueType"
        placeholder="值类型"
        style="width: 120px"
      >
        <el-option v-for="t in VALUE_TYPES" :key="t" :label="t" :value="t" />
      </el-select>
    </div>

    <div v-if="isExpression" class="leaf-value">
      <el-input
        v-model="node.expression"
        type="textarea"
        :rows="2"
        placeholder="表达式，如 orderAmount >= 100 && userId != null（SpEL / QLExpress）"
      />
    </div>
    <div v-else-if="isExistsOp" class="leaf-value">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="该操作符无需填写值，仅判断字段是否存在"
      />
    </div>
    <div v-else-if="showValue" class="leaf-value">
      <el-input
        v-if="node.valueType === 'NUMBER' && !isListOp"
        v-model="node.value"
        placeholder="数值，提交时自动转为 Number"
      />
      <el-select v-else-if="node.valueType === 'BOOLEAN' && !isListOp" v-model="node.value" placeholder="布尔值">
        <el-option label="true" :value="true" />
        <el-option label="false" :value="false" />
      </el-select>
      <el-input
        v-else
        v-model="displayValue"
        :placeholder="isListOp ? '多个值用英文逗号分隔，如 NEW_USER, ACTIVE' : '阈值'"
      />
      <div v-if="isListOp" class="leaf-tip">IN / NOT_IN / BETWEEN：提交前自动将逗号分隔字符串转换为数组</div>
    </div>

    <div
      class="insert-strip"
      :class="{ 'drag-over': insertOver }"
      @dragover.prevent.stop="insertOver = true"
      @dragleave="insertOver = false"
      @drop.prevent.stop="onInsertDrop"
    >
      <el-icon><Bottom /></el-icon> 拖入条件 / 逻辑节点到此处，插入到本条件之后
    </div>
  </div>
</template>

<style scoped>
.logic-node {
  border: 1px solid #d9d9d9;
  border-left: 3px solid #409eff;
  border-radius: 6px;
  background: #f7faff;
  padding: 10px;
  margin-bottom: 10px;
}

.logic-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.logic-tip {
  font-size: 12px;
  color: #909399;
  flex: 1;
}

.logic-actions {
  display: flex;
  gap: 4px;
}

.logic-children {
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  padding: 8px;
  min-height: 40px;
  transition: border-color 0.2s, background 0.2s;
}

.logic-children.drag-over {
  border-color: #409eff;
  background: #ecf5ff;
}

.empty-hint {
  color: #c0c4cc;
  font-size: 12px;
  text-align: center;
  line-height: 26px;
}

.leaf-node {
  border: 1px solid #e4e7ed;
  border-left: 3px solid #e6a23c;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
  margin-bottom: 10px;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.leaf-node.drag-over {
  border-color: #e6a23c;
  box-shadow: 0 0 0 2px rgba(230, 162, 60, 0.2);
}

.leaf-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.leaf-desc {
  font-size: 12px;
  color: #909399;
  flex: 1;
}

.leaf-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.leaf-form {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.leaf-value {
  margin-bottom: 8px;
}

.leaf-tip {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}

.insert-strip {
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  cursor: copy;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.2s;
}

.insert-strip.drag-over {
  border-color: #e6a23c;
  background: #fdf6ec;
  color: #e6a23c;
}
</style>
