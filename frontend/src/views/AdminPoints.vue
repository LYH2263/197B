<template>
  <div class="admin-points">
    <h2 class="page-title">积分等级管理</h2>
    <div class="content content-card">
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名或昵称"
          style="width: 240px"
          clearable
          @input="onSearch"
          @clear="onSearch"
        />
      </div>

      <el-table v-loading="loading" :data="filteredList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column label="当前积分" width="120">
          <template #default="{ row }">
            <span class="points-value">{{ row.points || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计消费" width="140">
          <template #default="{ row }">
            <span class="amount-value">¥{{ (row.totalSpent || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="120">
          <template #default="{ row }">
            <el-tag :type="getLevelTagType(row.level)" size="small">
              {{ getLevelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openPointsDialog(row)">
              调整积分
            </el-button>
            <el-button type="warning" link size="small" @click="openLevelDialog(row)">
              调整等级
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredList.length === 0" description="暂无用户" />
    </div>

    <el-dialog
      v-model="pointsDialogVisible"
      title="调整积分"
      width="400px"
      @close="resetPointsForm"
    >
      <el-form :model="pointsForm" label-width="100px" ref="pointsFormRef">
        <el-form-item label="用户">
          <span>{{ currentUser?.username }} ({{ currentUser?.nickname }})</span>
        </el-form-item>
        <el-form-item label="当前积分">
          <span>{{ currentUser?.points || 0 }}</span>
        </el-form-item>
        <el-form-item
          label="变动积分"
          prop="changePoints"
          :rules="[{ required: true, message: '请输入变动积分', trigger: 'blur' }]"
        >
          <el-input-number
            v-model="pointsForm.changePoints"
            :step="10"
            :precision="0"
            style="width: 100%"
            placeholder="正数增加，负数减少"
          />
          <div class="form-hint">正数为增加积分，负数为扣除积分</div>
        </el-form-item>
        <el-form-item
          label="备注"
          prop="remark"
          :rules="[{ required: true, message: '请输入备注', trigger: 'blur' }]"
        >
          <el-input
            v-model="pointsForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入调整原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pointsSubmitting" @click="submitPoints">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="levelDialogVisible"
      title="调整等级"
      width="400px"
      @close="resetLevelForm"
    >
      <el-form :model="levelForm" label-width="100px" ref="levelFormRef">
        <el-form-item label="用户">
          <span>{{ currentUser?.username }} ({{ currentUser?.nickname }})</span>
        </el-form-item>
        <el-form-item label="当前等级">
          <el-tag :type="getLevelTagType(currentUser?.level)" size="small">
            {{ getLevelText(currentUser?.level) }}
          </el-tag>
        </el-form-item>
        <el-form-item
          label="目标等级"
          prop="level"
          :rules="[{ required: true, message: '请选择等级', trigger: 'change' }]"
        >
          <el-select v-model="levelForm.level" style="width: 100%">
            <el-option v-for="lv in levels" :key="lv.value" :value="lv.value" :label="lv.label" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="levelSubmitting" @click="submitLevel">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const loading = ref(true)
const list = ref([])
const searchKeyword = ref('')

const pointsDialogVisible = ref(false)
const pointsSubmitting = ref(false)
const pointsFormRef = ref(null)
const currentUser = ref(null)
const pointsForm = reactive({
  changePoints: null,
  remark: ''
})

const levelDialogVisible = ref(false)
const levelSubmitting = ref(false)
const levelFormRef = ref(null)
const levelForm = reactive({
  level: null
})

const levels = [
  { value: 1, label: 'Lv1' },
  { value: 2, label: 'Lv2' },
  { value: 3, label: 'Lv3' },
  { value: 4, label: 'Lv4' },
  { value: 5, label: 'Lv5' }
]

const filteredList = computed(() => {
  if (!searchKeyword.value.trim()) return list.value
  const kw = searchKeyword.value.trim().toLowerCase()
  return list.value.filter(
    (u) =>
      (u.username && u.username.toLowerCase().includes(kw)) ||
      (u.nickname && u.nickname.toLowerCase().includes(kw))
  )
})

function getLevelText(level) {
  return `Lv${level || 1}`
}

function getLevelTagType(level) {
  const types = {
    1: 'info',
    2: '',
    3: 'success',
    4: 'warning',
    5: 'danger'
  }
  return types[level] || 'info'
}

function onSearch() {}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/users')
    list.value = res.data.code === 200 ? (res.data.data || []) : []
  } finally {
    loading.value = false
  }
}

function openPointsDialog(row) {
  currentUser.value = row
  pointsDialogVisible.value = true
}

function resetPointsForm() {
  pointsForm.changePoints = null
  pointsForm.remark = ''
  pointsFormRef.value?.clearValidate()
  currentUser.value = null
}

async function submitPoints() {
  if (!pointsFormRef.value) return
  try {
    await pointsFormRef.value.validate()
  } catch (e) {
    return
  }

  if (pointsForm.changePoints === 0) {
    ElMessage.warning('变动积分不能为0')
    return
  }

  pointsSubmitting.value = true
  try {
    const res = await api.post(`/admin/users/${currentUser.value.id}/points`, {
      changePoints: pointsForm.changePoints,
      remark: pointsForm.remark
    })
    if (res.data.code === 200) {
      ElMessage.success('积分调整成功')
      pointsDialogVisible.value = false
      load()
    }
  } finally {
    pointsSubmitting.value = false
  }
}

function openLevelDialog(row) {
  currentUser.value = row
  levelForm.level = row.level || 1
  levelDialogVisible.value = true
}

function resetLevelForm() {
  levelForm.level = null
  levelFormRef.value?.clearValidate()
  currentUser.value = null
}

async function submitLevel() {
  if (!levelFormRef.value) return
  try {
    await levelFormRef.value.validate()
  } catch (e) {
    return
  }

  levelSubmitting.value = true
  try {
    const res = await api.post(`/admin/users/${currentUser.value.id}/level`, {
      level: levelForm.level
    })
    if (res.data.code === 200) {
      ElMessage.success('等级调整成功')
      levelDialogVisible.value = false
      load()
    }
  } finally {
    levelSubmitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-points {
  padding-bottom: 32px;
}

.content {
  padding: 24px;
}

.filter-bar {
  margin-bottom: 16px;
}

.points-value {
  font-weight: 600;
  color: var(--el-color-primary);
}

.amount-value {
  font-weight: 600;
  color: var(--el-color-danger);
}

.form-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}
</style>
