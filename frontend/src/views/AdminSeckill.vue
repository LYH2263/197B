<template>
  <div class="admin-seckill">
    <div class="page-head">
      <h2 class="page-title">秒杀场次管理</h2>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新建场次
      </el-button>
    </div>

    <el-table :data="sessions" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="商品" min-width="180">
        <template #default="{ row }">
          <div class="product-cell">
            <img
              :src="productMap[row.productId]?.mainImage || '/images/default-product.svg'"
              @error="$event.target.src = '/images/default-product.svg'"
              class="product-thumb"
            />
            <span>{{ productMap[row.productId]?.name || '商品#' + row.productId }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="秒杀价" width="100">
        <template #default="{ row }">
          <span class="price-seckill">¥{{ row.seckillPrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="原价" width="100">
        <template #default="{ row }">
          <span class="price-origin">¥{{ productMap[row.productId]?.price || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="库存" width="140">
        <template #default="{ row }">
          <el-progress
            :percentage="row.totalStock ? Math.floor(row.soldStock * 100 / row.totalStock) : 0"
            :stroke-width="8"
          />
          <div class="stock-text">{{ row.soldStock }} / {{ row.totalStock }}</div>
        </template>
      </el-table-column>
      <el-table-column label="每人限购" width="100">
        <template #default="{ row }">{{ row.perUserLimit }} 件</template>
      </el-table-column>
      <el-table-column label="活动时间" min-width="320">
        <template #default="{ row }">
          <div class="time-cell">
            <div><el-tag size="small" type="info">开始</el-tag> {{ formatDateTime(row.startTime) }}</div>
            <div><el-tag size="small" type="warning">结束</el-tag> {{ formatDateTime(row.endTime) }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success" effect="light">启用</el-tag>
          <el-tag v-else type="info" effect="light">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button
            size="small"
            :type="row.status === 1 ? 'warning' : 'success'"
            link
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" link @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑秒杀场次' : '新建秒杀场次'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="关联商品" prop="productId">
          <el-select v-model="form.productId" placeholder="选择商品" filterable style="width: 100%">
            <el-option
              v-for="p in products"
              :key="p.id"
              :label="p.name + ' (库存:' + p.stock + ', 原价:¥' + p.price + ')'"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价" prop="seckillPrice">
          <el-input-number
            v-model="form.seckillPrice"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 100%"
            placeholder="必须低于原价"
          />
        </el-form-item>
        <el-form-item label="秒杀库存" prop="totalStock">
          <el-input-number
            v-model="form.totalStock"
            :min="1"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="每人限购" prop="perUserLimit">
          <el-input-number
            v-model="form.perUserLimit"
            :min="1"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'

const loading = ref(false)
const sessions = ref([])
const products = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const productMap = computed(() => {
  const map = {}
  products.value.forEach(p => { map[p.id] = p })
  return map
})

const form = reactive({
  productId: null,
  seckillPrice: 0.01,
  totalStock: 1,
  perUserLimit: 1,
  startTime: null,
  endTime: null,
  status: 1,
})

const rules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  perUserLimit: [{ required: true, message: '请输入限购数量', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

function formatDateTime(dt) {
  if (!dt) return ''
  return String(dt).replace('T', ' ')
}

async function fetchSessions() {
  loading.value = true
  try {
    const res = await api.get('/admin/seckill/sessions')
    if (res.data.code === 200) sessions.value = res.data.data || []
    else ElMessage.error(res.data.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function fetchProducts() {
  const res = await api.get('/products')
  if (res.data.code === 200) products.value = res.data.data || []
}

function resetForm() {
  form.productId = null
  form.seckillPrice = 0.01
  form.totalStock = 1
  form.perUserLimit = 1
  form.startTime = null
  form.endTime = null
  form.status = 1
}

function openCreate() {
  resetForm()
  isEdit.value = false
  editId.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  form.productId = row.productId
  form.seckillPrice = Number(row.seckillPrice)
  form.totalStock = row.totalStock
  form.perUserLimit = row.perUserLimit
  form.startTime = formatDateTime(row.startTime)
  form.endTime = formatDateTime(row.endTime)
  form.status = row.status
  dialogVisible.value = true
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    const res = await api.patch('/admin/seckill/sessions/' + row.id + '/status?status=' + newStatus)
    if (res.data.code === 200) {
      ElMessage.success('操作成功')
      fetchSessions()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {}
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该秒杀场次？此操作不可撤销', '提示', {
      type: 'warning',
    })
    const res = await api.delete('/admin/seckill/sessions/' + row.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchSessions()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {}
}

async function onSubmit() {
  await formRef.value.validate()
  if (new Date(form.startTime) >= new Date(form.endTime)) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  const p = productMap.value[form.productId]
  if (p && Number(form.seckillPrice) >= Number(p.price)) {
    ElMessage.warning('秒杀价必须低于原价 ¥' + p.price)
    return
  }
  submitting.value = true
  try {
    const body = {
      productId: form.productId,
      seckillPrice: Number(form.seckillPrice),
      totalStock: form.totalStock,
      perUserLimit: form.perUserLimit,
      startTime: form.startTime,
      endTime: form.endTime,
      status: form.status,
    }
    const res = isEdit.value
      ? await api.put('/admin/seckill/sessions/' + editId.value, body)
      : await api.post('/admin/seckill/sessions', body)
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      fetchSessions()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {} finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchSessions()
  fetchProducts()
})
</script>

<style scoped>
.admin-seckill { padding-bottom: 48px; }
.page-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.page-title { font-size: 1.25rem; font-weight: 700; margin: 0; }
.product-cell { display: flex; align-items: center; gap: 10px; }
.product-thumb {
  width: 40px; height: 40px; object-fit: cover;
  border-radius: var(--radius-sm);
}
.price-seckill { color: #ef4444; font-weight: 700; }
.price-origin { color: #9ca3af; text-decoration: line-through; }
.stock-text {
  font-size: 0.75rem; color: var(--color-text-muted);
  margin-top: 4px; text-align: right;
}
.time-cell {
  display: flex; flex-direction: column; gap: 4px;
  font-size: 0.875rem; color: var(--color-text-secondary);
}
</style>
