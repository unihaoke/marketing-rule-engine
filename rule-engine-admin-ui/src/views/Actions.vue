<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listActions,
  createAction,
  updateAction,
  deleteAction,
  setActionEnabled,
  getExecutorTypes
} from '../api/modules'
import ParamSchemaEditor from '../components/ParamSchemaEditor.vue'
import { parseJsonText, toJsonText, formatTime } from '../utils'

const loading = ref(false)
const rows = ref([])
const executorTypes = ref([])

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return {
    actionCode: '',
    actionName: '',
    actionType: '',
    description: '',
    params: [],
    enabled: true
  }
}

async function load() {
  loading.value = true
  try {
    rows.value = (await listActions()) || []
  } catch (e) {
    ElMessage.error(`动作列表加载失败：${e.message}`)
  } finally {
    loading.value = false
  }
}

async function loadExecutorTypes() {
  try {
    const data = await getExecutorTypes()
    const list = Array.isArray(data) ? data : []
    executorTypes.value = list.map((t) => {
      if (typeof t === 'string') return { value: t, label: t }
      return { value: t?.code ?? t?.type ?? String(t), label: t?.name ?? t?.code ?? String(t) }
    })
  } catch (e) {
    executorTypes.value = []
  }
}

function openCreate() {
  isEdit.value = false
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = {
    actionCode: row.actionCode,
    actionName: row.actionName || '',
    actionType: row.actionType || '',
    description: row.description || '',
    params: (row.params || []).map((p) => ({ ...p })),
    enabled: row.enabled !== false
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.actionCode.trim()) {
    ElMessage.warning('请填写动作编码')
    return
  }
  if (!form.value.actionName.trim()) {
    ElMessage.warning('请填写动作名称')
    return
  }
  const body = {
    actionCode: form.value.actionCode,
    actionName: form.value.actionName,
    actionType: form.value.actionType,
    description: form.value.description,
    params: form.value.params,
    enabled: form.value.enabled
  }
  dialogLoading.value = true
  try {
    if (isEdit.value) {
      await updateAction(form.value.actionCode, body)
      ElMessage.success('动作更新成功')
    } else {
      await createAction(body)
      ElMessage.success('动作创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(`保存失败：${e.message}`)
  } finally {
    dialogLoading.value = false
  }
}

async function onToggleEnabled(row, val) {
  try {
    await setActionEnabled(row.actionCode, val)
    ElMessage.success(val ? '动作已启用' : '动作已停用')
  } catch (e) {
    row.enabled = !val
    ElMessage.error(`操作失败：${e.message}`)
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除动作「${row.actionCode}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteAction(row.actionCode)
    ElMessage.success('动作已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) {
      ElMessage.error(`删除失败：${e.message}`)
    }
  }
}

onMounted(() => {
  load()
  loadExecutorTypes()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="table-toolbar" style="margin-bottom: 0">
        <span style="font-weight: 600">动作配置</span>
        <el-button type="primary" @click="openCreate">
          <el-icon style="margin-right: 4px"><Plus /></el-icon>新建动作
        </el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="actionCode" label="动作编码" min-width="140" />
      <el-table-column prop="actionName" label="动作名称" min-width="130" />
      <el-table-column label="动作类型" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.actionType" size="small" type="info" effect="plain">{{ row.actionType }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="参数数量" width="90" align="center">
        <template #default="{ row }">{{ (row.params || []).length }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled !== false"
            @change="(val) => onToggleEnabled(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" link @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新建 / 编辑 -->
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑动作' : '新建动作'"
    width="980px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form :model="form" label-width="90px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="动作编码" required>
            <el-input v-model="form.actionCode" :disabled="isEdit" placeholder="如 ISSUE_COUPON" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="动作名称" required>
            <el-input v-model="form.actionName" placeholder="如 发放优惠券" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="动作类型">
        <el-select
          v-model="form.actionType"
          filterable
          allow-create
          default-first-option
          placeholder="选择或输入动作类型"
          style="width: 100%"
        >
          <el-option v-for="t in executorTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="动作说明" />
      </el-form-item>
      <el-form-item label="参数定义">
        <div style="width: 100%">
          <ParamSchemaEditor v-model="form.params" with-default with-front-display />
          <div style="font-size: 12px; color: #909399; margin-top: 4px">
            每行可设置默认值（自动作为动作默认参数）与「前端展示」：标记为「是」的参数才会在规则画布动作配置中展示给运营填写。
          </div>
        </div>
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="dialogLoading" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>
