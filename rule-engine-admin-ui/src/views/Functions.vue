<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listFunctions,
  registerFunction,
  updateFunction,
  deleteFunction,
  setFunctionEnabled,
  testFunction,
  uploadFunctionJar
} from '../api/modules'
import ParamSchemaEditor from '../components/ParamSchemaEditor.vue'
import { parseJsonText, toJsonText, formatTime } from '../utils'

const loading = ref(false)
const rows = ref([])

// 注册 / 编辑弹窗
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const form = ref(emptyForm())
const typeOptions = [
  { value: 'JAVA_SPI', label: 'JAVA_SPI（Bean 实现）', type: 'info' },
  { value: 'JAR', label: 'JAR（外部 Jar 加载）', type: 'warning' },
  { value: 'EXPRESSION', label: 'EXPRESSION（在线脚本）', type: 'success' }
]

// Jar 上传弹窗
const uploadVisible = ref(false)
const uploadLoading = ref(false)
const uploadForm = ref({ functionName: '', className: '', displayName: '', description: '' })
const uploadFile = ref(null)

// 在线测试弹窗
const testVisible = ref(false)
const testLoading = ref(false)
const testTarget = ref(null)
const testForm = ref({ eventParamsText: '{}', bindingsText: '{}' })
const testResult = ref('')
// 案例列表（来自函数定义 testCases）
const testCases = ref([])
const activeCaseIndex = ref(-1)
const activeCase = computed(() =>
  activeCaseIndex.value >= 0 && testCases.value[activeCaseIndex.value] ? testCases.value[activeCaseIndex.value] : null
)

function emptyForm() {
  return {
    functionName: '',
    displayName: '',
    type: 'EXPRESSION',
    description: '',
    output: '',
    outputName: '',
    className: '',
    script: '',
    params: [],
    configText: '{}',
    testCasesText: '[]',
    enabled: true
  }
}

function typeLabel(t) {
  const found = typeOptions.find((o) => o.value === t)
  return found ? found.label.split('（')[0] : t
}

function typeTag(t) {
  const found = typeOptions.find((o) => o.value === t)
  return found ? found.type : 'info'
}

async function load() {
  loading.value = true
  try {
    rows.value = (await listFunctions()) || []
  } catch (e) {
    ElMessage.error(`函数列表加载失败：${e.message}`)
  } finally {
    loading.value = false
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
    functionName: row.functionName,
    displayName: row.displayName || '',
    type: row.type || 'EXPRESSION',
    description: row.description || '',
    output: row.output || '',
    outputName: row.outputName || '',
    className: row.className || '',
    script: row.script || '',
    params: (row.params || []).map((p) => ({ ...p })),
    configText: toJsonText(row.config),
    testCasesText: toJsonText(row.testCases || []),
    enabled: row.enabled !== false
  }
  dialogVisible.value = true
}

/** 解析测试案例数组并做基本校验 */
function parseTestCases(text) {
  const arr = parseJsonText(text, [])
  if (!Array.isArray(arr)) throw new Error('必须是数组')
  for (const c of arr) {
    if (!c || typeof c !== 'object') throw new Error('数组元素必须是对象')
    if (!c.name) throw new Error('案例缺少 name')
    if (c.eventParams && typeof c.eventParams !== 'object') throw new Error('eventParams 必须是对象')
    if (c.bindings && typeof c.bindings !== 'object') throw new Error('bindings 必须是对象')
  }
  return arr
}

async function save() {
  if (!form.value.functionName.trim()) {
    ElMessage.warning('请填写函数名')
    return
  }
  let config = {}
  try {
    config = parseJsonText(form.value.configText, {})
  } catch (e) {
    ElMessage.error('config JSON 格式错误')
    return
  }
  let testCases = []
  try {
    testCases = parseTestCases(form.value.testCasesText)
  } catch (e) {
    ElMessage.error(`测试案例 JSON 格式错误：${e.message}`)
    return
  }
  const body = {
    functionName: form.value.functionName,
    displayName: form.value.displayName,
    type: form.value.type,
    description: form.value.description,
    output: form.value.output,
    outputName: form.value.outputName,
    className: form.value.type === 'EXPRESSION' ? '' : form.value.className,
    script: form.value.type === 'EXPRESSION' ? form.value.script : '',
    params: form.value.params,
    config,
    testCases,
    enabled: form.value.enabled
  }
  dialogLoading.value = true
  try {
    if (isEdit.value) {
      await updateFunction(form.value.functionName, body)
      ElMessage.success('函数已更新（热更新生效）')
    } else {
      await registerFunction(body)
      ElMessage.success('函数注册成功')
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
    await setFunctionEnabled(row.functionName, val)
    ElMessage.success(val ? '函数已启用' : '函数已停用')
  } catch (e) {
    row.enabled = !val
    ElMessage.error(`操作失败：${e.message}`)
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除函数「${row.functionName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteFunction(row.functionName)
    ElMessage.success('函数已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) {
      ElMessage.error(`删除失败：${e.message}`)
    }
  }
}

// ---------------- Jar 上传 ----------------
function openUpload() {
  uploadForm.value = { functionName: '', className: '', displayName: '', description: '' }
  uploadFile.value = null
  uploadVisible.value = true
}

function onFileChange(uploadFileObj) {
  uploadFile.value = uploadFileObj.raw
}

function onFileRemove() {
  uploadFile.value = null
}

function onFileExceed(files) {
  // 仅允许单个文件：超出时替换为最新选择的文件
  uploadFile.value = files[0]
}

async function submitUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择 Jar 文件')
    return
  }
  if (!uploadForm.value.functionName.trim()) {
    ElMessage.warning('请填写函数名')
    return
  }
  if (!uploadForm.value.className.trim()) {
    ElMessage.warning('请填写实现类全限定名')
    return
  }
  uploadLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', uploadFile.value)
    fd.append('functionName', uploadForm.value.functionName)
    fd.append('className', uploadForm.value.className)
    fd.append('displayName', uploadForm.value.displayName)
    fd.append('description', uploadForm.value.description)
    await uploadFunctionJar(fd)
    ElMessage.success('Jar 上传并注册成功')
    uploadVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(`上传失败：${e.message}`)
  } finally {
    uploadLoading.value = false
  }
}

// ---------------- 在线测试 ----------------
function openTest(row) {
  testTarget.value = row
  testForm.value = { eventParamsText: '{}', bindingsText: '{}' }
  testResult.value = ''
  testCases.value = Array.isArray(row.testCases) ? row.testCases : []
  activeCaseIndex.value = -1
  if (testCases.value.length) {
    // 默认填入第一个案例，开箱即测
    applyTestCase(testCases.value[0], 0)
  }
  testVisible.value = true
}

function applyTestCase(tc, index) {
  activeCaseIndex.value = index
  testForm.value.eventParamsText = toJsonText(tc.eventParams || {})
  testForm.value.bindingsText = toJsonText(tc.bindings || {})
  testResult.value = ''
}

async function runTest() {
  let eventParams = {}
  let bindings = {}
  try {
    eventParams = parseJsonText(testForm.value.eventParamsText, {})
    bindings = parseJsonText(testForm.value.bindingsText, {})
  } catch (e) {
    ElMessage.error('测试入参 JSON 格式错误')
    return
  }
  testLoading.value = true
  try {
    const data = await testFunction(testTarget.value.functionName, { eventParams, bindings })
    testResult.value = JSON.stringify(data ?? {}, null, 2)
    ElMessage.success('测试执行完成')
  } catch (e) {
    ElMessage.error(`测试失败：${e.message}`)
  } finally {
    testLoading.value = false
  }
}

/** 把当前输入保存为一条新案例（调用函数更新接口持久化） */
async function saveAsTestCase() {
  let eventParams = {}
  let bindings = {}
  try {
    eventParams = parseJsonText(testForm.value.eventParamsText, {})
    bindings = parseJsonText(testForm.value.bindingsText, {})
  } catch (e) {
    ElMessage.error('当前入参 JSON 格式错误，无法保存为案例')
    return
  }
  const row = testTarget.value
  const name = `${row.functionName}-案例${testCases.value.length + 1}`
  const list = [...testCases.value, { name, eventParams, bindings, expect: '自建案例' }]
  testLoading.value = true
  try {
    await updateFunction(row.functionName, {
      functionName: row.functionName,
      displayName: row.displayName || '',
      type: row.type,
      description: row.description || '',
      className: row.className || '',
      script: row.script || '',
      params: (row.params || []).map((p) => ({ ...p })),
      config: row.config || {},
      testCases: list,
      enabled: row.enabled !== false
    })
    testCases.value = list
    activeCaseIndex.value = list.length - 1
    ElMessage.success(`已保存为案例「${name}」`)
    await load()
  } catch (e) {
    ElMessage.error(`保存案例失败：${e.message}`)
  } finally {
    testLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="table-toolbar" style="margin-bottom: 0">
        <span style="font-weight: 600">函数管理</span>
        <div>
          <el-button type="warning" plain style="margin-right: 8px" @click="openUpload">
            <el-icon style="margin-right: 4px"><Upload /></el-icon>上传 Jar
          </el-button>
          <el-button type="primary" @click="openCreate">
            <el-icon style="margin-right: 4px"><Plus /></el-icon>注册函数
          </el-button>
        </div>
      </div>
    </template>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="functionName" label="函数名" min-width="150" />
      <el-table-column prop="displayName" label="展示名" min-width="130" />
      <el-table-column label="类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="出参名" width="130">
        <template #default="{ row }">{{ row.outputName || row.functionName }}</template>
      </el-table-column>
      <el-table-column label="版本" width="70" align="center">
        <template #default="{ row }">{{ row.version ?? 1 }}</template>
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="success" link @click="openTest(row)">在线测试</el-button>
          <el-button size="small" type="danger" link @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 注册 / 编辑 -->
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑函数' : '注册函数'"
    width="720px"
    :close-on-click-modal="false"
  >
    <el-form :model="form" label-width="90px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="函数名" required>
            <el-input v-model="form.functionName" :disabled="isEdit" placeholder="如 rebateCalculator" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="展示名">
            <el-input v-model="form.displayName" placeholder="如 阶梯返利核算" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="函数类型" required>
        <el-select v-model="form.type" style="width: 100%">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="函数说明" />
      </el-form-item>
      <el-form-item label="出参说明">
        <el-input
          v-model="form.output"
          type="textarea"
          :rows="2"
          placeholder="函数结果说明，如 返回档位奖励值（数字）；画布中展示给运营"
        />
      </el-form-item>
      <el-form-item label="出参名">
        <el-input
          v-model="form.outputName"
          placeholder="规则画布中的固定出参名（不可改），缺省=函数名；如 rewardPoints"
        />
        <div class="form-tip">画布中函数结果写入该名称，条件/动作引用 #{出参名}；请使用英文标识符</div>
      </el-form-item>
      <el-form-item :label="form.type === 'EXPRESSION' ? '脚本内容' : '实现类'">
        <el-input
          v-if="form.type !== 'EXPRESSION'"
          v-model="form.className"
          :placeholder="form.type === 'JAR' ? 'Jar 内实现类全限定名' : 'Bean 名或 SPI 类名'"
        />
        <el-input
          v-else
          v-model="form.script"
          type="textarea"
          :rows="3"
          placeholder="表达式脚本，如 orderCount * 10 + checkinStreak * 5"
        />
      </el-form-item>
      <el-form-item label="入参定义">
        <div style="width: 100%">
          <ParamSchemaEditor v-model="form.params" with-editable />
        </div>
        <div class="form-tip" style="margin-top: 4px">「画布可填」关闭的参数不在规则画布中展示赋值（由函数内部/默认值决定）</div>
      </el-form-item>
      <el-form-item label="config">
        <el-input v-model="form.configText" type="textarea" :rows="2" class="mono" placeholder='扩展配置 JSON，如 {"tiers": [{"min": 100, "rate": 0.1}]}' />
      </el-form-item>
      <el-form-item label="测试案例">
        <el-input
          v-model="form.testCasesText"
          type="textarea"
          :rows="8"
          class="mono"
          placeholder='案例数组 JSON，如 [{"name":"案例名","eventParams":{},"bindings":{},"expect":"预期说明"}]'
        />
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

  <!-- Jar 上传 -->
  <el-dialog v-model="uploadVisible" title="上传 Jar 注册函数" width="560px" :close-on-click-modal="false">
    <el-form :model="uploadForm" label-width="90px">
      <el-form-item label="函数名" required>
        <el-input v-model="uploadForm.functionName" placeholder="如 rebateCalculator" />
      </el-form-item>
      <el-form-item label="实现类" required>
        <el-input v-model="uploadForm.className" placeholder="Jar 内实现类全限定名，如 com.mkt.fn.RebateCalc" />
      </el-form-item>
      <el-form-item label="展示名">
        <el-input v-model="uploadForm.displayName" placeholder="如 阶梯返利核算" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="uploadForm.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="Jar 文件" required>
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          accept=".jar"
          :on-change="onFileChange"
          :on-remove="onFileRemove"
          :on-exceed="onFileExceed"
          style="width: 100%"
        >
          <el-icon style="font-size: 40px; color: #c0c4cc"><UploadFilled /></el-icon>
          <div style="font-size: 13px">将 Jar 文件拖到此处，或<em>点击选择</em></div>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="uploadVisible = false">取消</el-button>
      <el-button type="primary" :loading="uploadLoading" @click="submitUpload">上传并注册</el-button>
    </template>
  </el-dialog>

  <!-- 在线测试 -->
  <el-dialog v-model="testVisible" :title="`在线测试 · ${testTarget?.functionName || ''}`" width="680px">
    <!-- 案例区：点击一键填入入参 -->
    <div v-if="testCases.length" style="margin-bottom: 14px">
      <div style="font-size: 12px; color: #909399; margin-bottom: 6px">测试案例（点击一键填入入参）：</div>
      <div style="display: flex; flex-wrap: wrap; gap: 8px">
        <el-button
          v-for="(tc, i) in testCases"
          :key="i"
          size="small"
          :type="activeCaseIndex === i ? 'primary' : 'default'"
          :plain="activeCaseIndex !== i"
          @click="applyTestCase(tc, i)"
        >
          {{ tc.name }}
        </el-button>
      </div>
      <div v-if="activeCase?.expect" style="font-size: 12px; color: #e6a23c; margin-top: 8px">
        预期结果：{{ activeCase.expect }}
      </div>
    </div>
    <el-form :model="testForm" label-width="90px">
      <el-form-item label="事件参数">
        <el-input
          v-model="testForm.eventParamsText"
          type="textarea"
          :rows="7"
          class="mono"
          placeholder='事件参数 JSON，如 {"orderCount": 5, "userId": "u1001"}'
        />
      </el-form-item>
      <el-form-item label="绑定参数">
        <el-input
          v-model="testForm.bindingsText"
          type="textarea"
          :rows="7"
          class="mono"
          placeholder='绑定参数 JSON，如 {"tiers": [{"min": 100, "rate": 0.1}]}'
        />
      </el-form-item>
    </el-form>
    <div style="margin-bottom: 10px; display: flex; align-items: center; gap: 8px">
      <el-button type="primary" :loading="testLoading" @click="runTest">运行测试</el-button>
      <el-button :disabled="testLoading" @click="saveAsTestCase">存为案例</el-button>
    </div>
    <div v-if="testResult">
      <div style="font-size: 12px; color: #909399; margin-bottom: 4px">测试结果：</div>
      <pre class="test-result">{{ testResult }}</pre>
    </div>
    <template #footer>
      <el-button @click="testVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.test-result {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  max-height: 280px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
}
</style>
