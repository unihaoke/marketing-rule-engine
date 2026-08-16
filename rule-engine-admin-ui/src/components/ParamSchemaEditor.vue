<script setup>
import { ref, watch } from 'vue'
import { VALUE_TYPES } from '../utils/condition'

/**
 * 参数 Schema 动态行编辑器：code / name / type / required / description / defaultValue(可选) / frontDisplay(可选)
 * 用于事件入参、函数入参、动作入参的编辑，支持增删行。
 *
 * 类型：
 * - 基础类型 STRING / NUMBER / BOOLEAN / DATETIME / LIST
 * - USER：用户 ID（画布中提示填用户 ID 或 ${userId} 引用）
 * - LIST_OBJECT：对象数组（如阶梯档位 [{key,value}]），展开行编辑 itemSchema（元素子字段）
 * - JSON：兜底自由 JSON
 *
 * withFrontDisplay 仅用于动作参数：控制该参数是否在规则画布动作配置中展示给运营填写。
 */
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  withDefault: { type: Boolean, default: false },
  withFrontDisplay: { type: Boolean, default: false },
  /** 是否展示"画布可填"开关（函数绑定参数：false 的参数不在规则画布中展示赋值） */
  withEditable: { type: Boolean, default: false },
  placeholderCode: { type: String, default: '如 adSlotId' }
})

const emit = defineEmits(['update:modelValue'])

const rows = ref(cloneRows(props.modelValue))

/** 子字段类型：对象数组元素支持基础类型（不嵌套 LIST_OBJECT，避免无限递归） */
const SUB_TYPES = ['STRING', 'NUMBER', 'BOOLEAN', 'DATETIME']

const TYPE_OPTIONS = [
  ...VALUE_TYPES.map((t) => ({ value: t, label: t })),
  { value: 'USER', label: 'USER（用户ID）' },
  { value: 'LIST_OBJECT', label: 'LIST_OBJECT（对象数组）' },
  { value: 'JSON', label: 'JSON（自由 JSON）' }
]

function typeLabel(t) {
  const found = TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t
}

/**
 * 外部值同步：仅当外部值与内部行内容不一致时重置内部行，
 * 避免"内部编辑 → emit → 外部回写 → 重置"的更新循环（此前的 bug 导致添加参数无效/递归更新）。
 */
watch(
  () => props.modelValue,
  (v) => {
    if (JSON.stringify(cloneRows(v)) !== JSON.stringify(rows.value)) {
      rows.value = cloneRows(v)
    }
  }
)

watch(
  rows,
  () => {
    emit('update:modelValue', rows.value.map((r) => ({ ...r })))
  },
  { deep: true }
)

function cloneRows(list) {
  return (list || []).map((r) => ({
    code: r.code ?? '',
    name: r.name ?? '',
    type: r.type ?? 'STRING',
    required: !!r.required,
    description: r.description ?? '',
    defaultValue: r.defaultValue ?? null,
    frontDisplay: r.frontDisplay !== false,
    editable: r.editable !== false,
    itemSchema: (r.itemSchema || []).map((s) => ({
      code: s.code ?? '',
      name: s.name ?? '',
      type: s.type ?? 'STRING',
      required: !!s.required,
      description: s.description ?? ''
    }))
  }))
}

function addRow() {
  rows.value.push({
    code: '',
    name: '',
    type: 'STRING',
    required: false,
    description: '',
    defaultValue: null,
    frontDisplay: true,
    editable: true,
    itemSchema: []
  })
}

function removeRow(i) {
  rows.value.splice(i, 1)
}

function addSubRow(row) {
  if (!row.itemSchema) row.itemSchema = []
  row.itemSchema.push({ code: '', name: '', type: 'STRING', required: false, description: '' })
}

function removeSubRow(row, i) {
  row.itemSchema.splice(i, 1)
}
</script>

<template>
  <div>
    <el-table :data="rows" size="small" border>
      <!-- 展开行：LIST_OBJECT 子字段编辑 -->
      <el-table-column type="expand">
        <template #default="{ row }">
          <div v-if="row.type === 'LIST_OBJECT'" class="sub-schema">
            <div class="sub-schema-title">
              「{{ row.name || row.code }}」为对象数组，定义元素格式（子字段）：
            </div>
            <el-table :data="row.itemSchema || []" size="small" border>
              <el-table-column label="子字段编码" min-width="110">
                <template #default="{ row: s }">
                  <el-input v-model="s.code" size="small" placeholder="如 key" />
                </template>
              </el-table-column>
              <el-table-column label="名称" min-width="100">
                <template #default="{ row: s }">
                  <el-input v-model="s.name" size="small" placeholder="如 天数" />
                </template>
              </el-table-column>
              <el-table-column label="类型" width="100">
                <template #default="{ row: s }">
                  <el-select v-model="s.type" size="small" style="width: 100%">
                    <el-option v-for="t in SUB_TYPES" :key="t" :label="t" :value="t" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="60" align="center">
                <template #default="{ row: s }">
                  <el-switch v-model="s.required" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="描述" min-width="130">
                <template #default="{ row: s }">
                  <el-input v-model="s.description" size="small" placeholder="说明" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60" align="center">
                <template #default="{ row: s, $index }">
                  <el-button size="small" type="danger" link @click="removeSubRow(row, $index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" plain style="margin-top: 6px" @click="addSubRow(row)">
              <el-icon style="margin-right: 4px"><Plus /></el-icon>添加子字段
            </el-button>
          </div>
          <div v-else class="sub-schema-empty">（非对象数组参数，无需子字段）</div>
        </template>
      </el-table-column>
      <el-table-column label="编码" min-width="120">
        <template #default="{ row }">
          <el-input v-model="row.code" size="small" :placeholder="placeholderCode" />
        </template>
      </el-table-column>
      <el-table-column label="名称" min-width="100">
        <template #default="{ row }">
          <el-input v-model="row.name" size="small" placeholder="如 广告位ID" />
        </template>
      </el-table-column>
      <el-table-column label="类型" width="130">
        <template #default="{ row }">
          <el-select v-model="row.type" size="small" style="width: 100%">
            <el-option v-for="o in TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="必填" width="60" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.required" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="描述" min-width="120">
        <template #default="{ row }">
          <el-input v-model="row.description" size="small" placeholder="说明" />
        </template>
      </el-table-column>
      <el-table-column v-if="withDefault" label="默认值" min-width="100">
        <template #default="{ row }">
          <el-input v-model="row.defaultValue" size="small" placeholder="默认值" />
        </template>
      </el-table-column>
      <el-table-column v-if="withFrontDisplay" label="前端展示" width="90" align="center">
        <template #default="{ row }">
          <el-select v-model="row.frontDisplay" size="small" style="width: 100%">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column v-if="withEditable" label="画布可填" width="90" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.editable" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="60" align="center">
        <template #default="{ $index }">
          <el-button size="small" type="danger" link @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 8px">
      <el-button size="small" type="primary" plain @click="addRow">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>添加参数
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.sub-schema {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px;
}

.sub-schema-title {
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.sub-schema-empty {
  font-size: 12px;
  color: #c0c4cc;
  padding: 4px 0;
}
</style>
