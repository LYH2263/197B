<template>
  <div class="admin-coupons-page">
    <div class="page-header">
      <h2 class="page-title">优惠券管理</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        新建优惠券
      </el-button>
    </div>

    <div class="content-card">
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="全部状态" style="width: 140px" @change="load">
          <el-option :value="null" label="全部状态" />
          <el-option :value="1" label="正常" />
          <el-option :value="0" label="已作废" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="coupons" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="券名称" min-width="160" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getCouponTypeTag(row.type)" size="small">
              {{ getCouponTypeDesc(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="面值" width="120">
          <template #default="{ row }">
            <template v-if="row.type === 1 || row.type === 3">
              ¥{{ row.faceValue }}
            </template>
            <template v-else-if="row.type === 2">
              {{ (row.discountRate * 10).toFixed(1) }}折
            </template>
          </template>
        </el-table-column>
        <el-table-column label="门槛" width="100">
          <template #default="{ row }">
            {{ row.threshold > 0 ? '¥' + row.threshold : '无门槛' }}
          </template>
        </el-table-column>
        <el-table-column label="适用范围" width="120">
          <template #default="{ row }">
            {{ getCategoryName(row.applicableCategory) || '全场通用' }}
          </template>
        </el-table-column>
        <el-table-column label="库存" width="140">
          <template #default="{ row }">
            {{ row.claimedQuantity }}/{{ row.totalQuantity }}
            <el-progress
              :percentage="Math.round(row.claimedQuantity / row.totalQuantity * 100)"
              :stroke-width="6"
              :show-text="false"
              style="margin-top: 4px"
            />
          </template>
        </el-table-column>
        <el-table-column label="已使用" width="80">
          <template #default="{ row }">
            {{ row.usedQuantity }}
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="220">
          <template #default="{ row }">
            <div class="validity-text">{{ formatDate(row.validStart) }}</div>
            <div class="validity-text">至 {{ formatDate(row.validEnd) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '已作废' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewClaims(row)">
              领取记录
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              :disabled="row.status === 0"
              @click="invalidate(row)"
            >
              作废
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="showCreateDialog"
      title="新建优惠券"
      width="560px"
      @close="resetCreateForm"
    >
      <el-form :model="createForm" label-width="120px" ref="createFormRef">
        <el-form-item label="券名称" prop="name" :rules="[{ required: true, message: '请输入券名称' }]">
          <el-input v-model="createForm.name" placeholder="请输入券名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="券类型" prop="type" :rules="[{ required: true, message: '请选择券类型' }]">
          <el-select v-model="createForm.type" placeholder="请选择券类型" style="width: 100%">
            <el-option :value="1" label="满减券" />
            <el-option :value="2" label="折扣券" />
            <el-option :value="3" label="立减券（无门槛）" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用门槛" prop="threshold" :rules="[{ required: true, message: '请输入使用门槛' }]">
          <el-input-number
            v-model="createForm.threshold"
            :min="0"
            :precision="2"
            :step="10"
            style="width: 100%"
            placeholder="0表示无门槛"
          />
          <div class="form-hint">0表示无门槛</div>
        </el-form-item>
        <el-form-item
          v-if="createForm.type === 1 || createForm.type === 3"
          label="面额"
          prop="faceValue"
          :rules="[{ required: true, message: '请输入面额' }]"
        >
          <el-input-number
            v-model="createForm.faceValue"
            :min="0.01"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          v-if="createForm.type === 2"
          label="折扣率"
          prop="discountRate"
          :rules="[{ required: true, message: '请输入折扣率' }]"
        >
          <el-input-number
            v-model="createForm.discountRate"
            :min="0.01"
            :max="0.99"
            :step="0.05"
            :precision="2"
            style="width: 100%"
          />
          <div class="form-hint">例如：0.8表示8折</div>
        </el-form-item>
        <el-form-item label="发放总量" prop="totalQuantity" :rules="[{ required: true, message: '请输入发放总量' }]">
          <el-input-number
            v-model="createForm.totalQuantity"
            :min="1"
            :step="100"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="每人限领" prop="perUserLimit" :rules="[{ required: true, message: '请输入每人限领数量' }]">
          <el-input-number
            v-model="createForm.perUserLimit"
            :min="1"
            :max="10"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="适用分类" prop="applicableCategory">
          <el-select v-model="createForm.applicableCategory" placeholder="全场通用" clearable style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :value="cat.id"
              :label="cat.name"
            />
          </el-select>
          <div class="form-hint">不选表示全场通用</div>
        </el-form-item>
        <el-form-item label="有效期开始" prop="validStart" :rules="[{ required: true, message: '请选择有效期开始时间' }]">
          <el-date-picker
            v-model="createForm.validStart"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="有效期结束" prop="validEnd" :rules="[{ required: true, message: '请选择有效期结束时间' }]">
          <el-date-picker
            v-model="createForm.validEnd"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createCoupon">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showClaimsDialog"
      :title="`「${currentCoupon?.name}」领取记录`"
      width="720px"
    >
      <el-table v-loading="claimsLoading" :data="claims" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="code" label="券码" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getUserCouponStatusTag(row.status)" size="small">
              {{ getUserCouponStatusDesc(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="usedOrderId" label="使用订单" width="120">
          <template #default="{ row }">
            {{ row.usedOrderId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="claimedAt" label="领取时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.claimedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="usedAt" label="使用时间" width="180">
          <template #default="{ row }">
            {{ row.usedAt ? formatDateTime(row.usedAt) : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showClaimsDialog = false">关闭</el-button>
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
const creating = ref(false)
const claimsLoading = ref(false)
const coupons = ref([])
const categories = ref([])
const claims = ref([])
const filterStatus = ref(null)
const showCreateDialog = ref(false)
const showClaimsDialog = ref(false)
const currentCoupon = ref(null)
const createFormRef = ref(null)

const createForm = reactive({
  name: '',
  type: null,
  threshold: 0,
  faceValue: null,
  discountRate: null,
  totalQuantity: 100,
  perUserLimit: 1,
  applicableCategory: null,
  validStart: '',
  validEnd: ''
})

function getCouponTypeDesc(type) {
  return { 1: '满减券', 2: '折扣券', 3: '立减券' }[type] || '未知'
}

function getCouponTypeTag(type) {
  return { 1: '', 2: 'success', 3: 'danger' }[type] || 'info'
}

function getUserCouponStatusDesc(status) {
  return { 0: '未使用', 1: '已使用', 2: '已过期', 3: '已作废' }[status] || '未知'
}

function getUserCouponStatusTag(status) {
  return { 0: 'warning', 1: 'success', 2: 'info', 3: 'info' }[status] || 'info'
}

function getCategoryName(categoryId) {
  if (!categoryId) return ''
  const cat = categories.value.find(c => c.id === categoryId)
  return cat ? cat.name : ''
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
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
    const res = await api.get('/admin/coupons', {
      params: { status: filterStatus.value }
    })
    if (res.data.code === 200) {
      coupons.value = res.data.data || []
    }
  } finally {
    loading.value = false
  }
}

async function createCoupon() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch (e) {
    return
  }

  if (createForm.type === 2 && (!createForm.discountRate || createForm.discountRate <= 0 || createForm.discountRate >= 1)) {
    ElMessage.warning('折扣率必须在0-1之间')
    return
  }

  if (new Date(createForm.validEnd) <= new Date(createForm.validStart)) {
    ElMessage.warning('有效期结束时间必须晚于开始时间')
    return
  }

  creating.value = true
  try {
    const res = await api.post('/admin/coupons', createForm)
    if (res.data.code === 200) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      load()
    }
  } finally {
    creating.value = false
  }
}

async function invalidate(coupon) {
  try {
    await ElMessageBox.confirm(`确定要作废优惠券「${coupon.name}」吗？`, '提示', {
      type: 'warning'
    })
    const res = await api.post(`/admin/coupons/${coupon.id}/invalidate`)
    if (res.data.code === 200) {
      ElMessage.success('作废成功')
      load()
    }
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

async function viewClaims(coupon) {
  currentCoupon.value = coupon
  claimsLoading.value = true
  try {
    const res = await api.get(`/admin/coupons/${coupon.id}/claims`)
    if (res.data.code === 200) {
      claims.value = res.data.data || []
    }
    showClaimsDialog.value = true
  } finally {
    claimsLoading.value = false
  }
}

function resetCreateForm() {
  Object.assign(createForm, {
    name: '',
    type: null,
    threshold: 0,
    faceValue: null,
    discountRate: null,
    totalQuantity: 100,
    perUserLimit: 1,
    applicableCategory: null,
    validStart: '',
    validEnd: ''
  })
  createFormRef.value?.clearValidate()
}

onMounted(async () => {
  await loadCategories()
  load()
})
</script>

<style scoped>
.admin-coupons-page {
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

.validity-text {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.form-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}
</style>
