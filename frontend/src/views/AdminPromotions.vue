<template>
  <div class="admin-promotions-page">
    <div class="page-header">
      <h2 class="page-title">满减活动管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建满减活动
      </el-button>
    </div>

    <div class="content-card">
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="全部状态" style="width: 140px" @change="load">
          <el-option :value="null" label="全部状态" />
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="关闭" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="promotions" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="活动名称" min-width="180" />
        <el-table-column label="满减档位" min-width="320">
          <template #default="{ row }">
            <div class="tier-list">
              <el-tag
                v-for="(t, i) in row.tiers"
                :key="i"
                class="tier-tag"
                size="small"
                :type="i === row.tiers.length - 1 ? 'danger' : 'warning'"
              >
                满{{ t.threshold }}减{{ t.discount }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="适用范围" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="row.scopeType === 1 ? 'success' : 'primary'">
              {{ row.scopeTypeDesc || (row.scopeType === 1 ? '全场通用' : '指定分类') }}
            </el-tag>
            <div v-if="row.categoryName" class="category-name">
              {{ row.categoryName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="300">
          <template #default="{ row }">
            <div class="time-text">{{ formatDateTime(row.startTime) }}</div>
            <div class="time-text">至 {{ formatDateTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.statusDesc || (row.status === 1 ? '启用' : '关闭') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="warning"
              link
              size="small"
              @click="disablePromotion(row)"
            >
              关闭
            </el-button>
            <el-button
              v-else
              type="success"
              link
              size="small"
              @click="enablePromotion(row)"
            >
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="showDialog"
      :title="editingId ? '编辑满减活动' : '新建满减活动'"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" label-width="120px" ref="formRef">
        <el-form-item label="活动名称" prop="name" :rules="[{ required: true, message: '请输入活动名称' }]">
          <el-input v-model="form.name" placeholder="请输入活动名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="活动开始" prop="startTime" :rules="[{ required: true, message: '请选择开始时间' }]">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="活动结束" prop="endTime" :rules="[{ required: true, message: '请选择结束时间' }]">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="适用范围" prop="scopeType" :rules="[{ required: true, message: '请选择适用范围' }]">
          <el-radio-group v-model="form.scopeType">
            <el-radio :value="1">全场通用</el-radio>
            <el-radio :value="2">指定分类</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scopeType === 2" label="指定分类" prop="applicableCategory" :rules="[{ required: true, message: '请选择分类' }]">
          <el-select v-model="form.applicableCategory" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :value="cat.id"
              :label="cat.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="满减档位" prop="tiers" :rules="tiersRule">
          <div class="tiers-editor">
            <div
              v-for="(tier, index) in form.tiers"
              :key="index"
              class="tier-row"
            >
              <span class="tier-label">第{{ index + 1 }}档</span>
              <span>满</span>
              <el-input-number
                v-model="tier.threshold"
                :min="0.01"
                :precision="2"
                :step="10"
                size="small"
                style="width: 130px"
              />
              <span>元，减</span>
              <el-input-number
                v-model="tier.discount"
                :min="0.01"
                :precision="2"
                :step="5"
                size="small"
                style="width: 130px"
              />
              <span>元</span>
              <el-button
                v-if="form.tiers.length > 1"
                type="danger"
                link
                size="small"
                @click="removeTier(index)"
              >
                删除
              </el-button>
            </div>
            <el-button type="primary" link size="small" @click="addTier">
              <el-icon><Plus /></el-icon>
              添加档位
            </el-button>
            <div class="form-hint">说明：系统会自动取满足条件的最优档位（减免最多的一档）</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ editingId ? '保存修改' : '创建活动' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'

const loading = ref(true)
const submitting = ref(false)
const promotions = ref([])
const categories = ref([])
const filterStatus = ref(null)
const showDialog = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  startTime: '',
  endTime: '',
  scopeType: 1,
  applicableCategory: null,
  tiers: [
    { threshold: 200, discount: 30 }
  ]
})

const tiersRule = {
  validator: (_rule, value, callback) => {
    if (!value || value.length === 0) {
      callback(new Error('至少配置一档满减'))
      return
    }
    for (let i = 0; i < value.length; i++) {
      const t = value[i]
      if (!t.threshold || t.threshold <= 0) {
        callback(new Error(`第${i + 1}档门槛必须大于0`))
        return
      }
      if (!t.discount || t.discount <= 0) {
        callback(new Error(`第${i + 1}档减免必须大于0`))
        return
      }
      if (t.discount >= t.threshold) {
        callback(new Error(`第${i + 1}档减免不能大于等于门槛`))
        return
      }
    }
    callback()
  }
}

function addTier() {
  form.tiers.push({ threshold: 0, discount: 0 })
}

function removeTier(index) {
  form.tiers.splice(index, 1)
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

async function loadCategories() {
  try {
    const res = await api.get('/categories')
    if (res.data.code === 200) {
      categories.value = res.data.data || []
    }
  } catch (e) {}
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/promotions', {
      params: { status: filterStatus.value }
    })
    if (res.data.code === 200) {
      promotions.value = res.data.data || []
    }
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingId.value = null
  showDialog.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  form.name = row.name
  form.startTime = row.startTime
  form.endTime = row.endTime
  form.scopeType = row.scopeType
  form.applicableCategory = row.applicableCategory
  form.tiers = (row.tiers || []).map(t => ({
    threshold: Number(t.threshold),
    discount: Number(t.discount)
  }))
  if (form.tiers.length === 0) {
    form.tiers = [{ threshold: 200, discount: 30 }]
  }
  showDialog.value = true
}

function resetForm() {
  Object.assign(form, {
    name: '',
    startTime: '',
    endTime: '',
    scopeType: 1,
    applicableCategory: null,
    tiers: [{ threshold: 200, discount: 30 }]
  })
  formRef.value?.clearValidate()
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  if (new Date(form.endTime) <= new Date(form.startTime)) {
    ElMessage.warning('活动结束时间必须晚于开始时间')
    return
  }

  const sortedTiers = [...form.tiers].sort((a, b) => a.threshold - b.threshold)
  for (let i = 1; i < sortedTiers.length; i++) {
    if (sortedTiers[i].discount <= sortedTiers[i - 1].discount) {
      ElMessage.warning(`高门槛档位（满${sortedTiers[i].threshold}）的减免必须大于低门槛档位（满${sortedTiers[i - 1].threshold}）`)
      return
    }
  }

  submitting.value = true
  try {
    const req = { ...form, tiers: sortedTiers }
    let res
    if (editingId.value) {
      res = await api.put(`/admin/promotions/${editingId.value}`, req)
    } else {
      res = await api.post('/admin/promotions', req)
    }
    if (res.data.code === 200) {
      ElMessage.success(editingId.value ? '修改成功' : '创建成功')
      showDialog.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

async function enablePromotion(row) {
  try {
    const res = await api.post(`/admin/promotions/${row.id}/enable`)
    if (res.data.code === 200) {
      ElMessage.success('已启用')
      load()
    }
  } catch (e) {}
}

async function disablePromotion(row) {
  try {
    await ElMessageBox.confirm(`确定要关闭满减活动「${row.name}」吗？`, '提示', { type: 'warning' })
    const res = await api.post(`/admin/promotions/${row.id}/disable`)
    if (res.data.code === 200) {
      ElMessage.success('已关闭')
      load()
    }
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

onMounted(async () => {
  await loadCategories()
  load()
})
</script>

<style scoped>
.admin-promotions-page {
  padding-bottom: 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
}

.filter-bar {
  margin-bottom: 16px;
}

.tier-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tier-tag {
  border: none;
}

.category-name {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.time-text {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.tiers-editor {
  width: 100%;
}

.tier-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  flex-wrap: wrap;
}

.tier-label {
  width: 56px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.form-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 8px;
}
</style>
