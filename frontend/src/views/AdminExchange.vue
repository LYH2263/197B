<template>
  <div class="admin-exchange">
    <h2 class="page-title">兑换管理</h2>

    <div class="tabs-card content-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="兑换商品管理" name="products">
          <div class="tab-toolbar">
            <el-select v-model="productFilterStatus" placeholder="全部状态" style="width: 140px" @change="loadProducts">
              <el-option :value="null" label="全部状态" />
              <el-option :value="1" label="已上架" />
              <el-option :value="0" label="已下架" />
            </el-select>
            <el-button type="primary" @click="openProductDialog()">
              <el-icon><Plus /></el-icon>
              新增商品
            </el-button>
          </div>

          <el-table v-loading="productsLoading" :data="products" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="图片" width="100">
              <template #default="{ row }">
                <el-image
                  v-if="row.image"
                  :src="row.image"
                  fit="cover"
                  class="product-img"
                  :preview-src-list="[row.image]"
                />
                <span v-else style="color: var(--color-text-muted)">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="商品名称" min-width="160" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="points" label="所需积分" width="120" align="center">
              <template #default="{ row }">
                <span style="color: var(--color-primary); font-weight: 600;">{{ row.points }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="100" align="center" />
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '已上架' : '已下架' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openProductDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!productsLoading && products.length === 0" description="暂无兑换商品" />
        </el-tab-pane>

        <el-tab-pane label="兑换订单管理" name="orders">
          <div class="tab-toolbar">
            <el-select v-model="orderFilterStatus" placeholder="全部状态" style="width: 140px" @change="loadOrders">
              <el-option :value="null" label="全部状态" />
              <el-option :value="0" label="待处理" />
              <el-option :value="1" label="已发货" />
              <el-option :value="2" label="已完成" />
              <el-option :value="3" label="已取消" />
            </el-select>
            <el-button @click="loadOrders">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>

          <el-table v-loading="ordersLoading" :data="orders" border>
            <el-table-column prop="orderNo" label="兑换单号" width="180" />
            <el-table-column label="用户" width="140">
              <template #default="{ row }">
                {{ row.username || row.nickname || ('用户#' + row.userId) }}
              </template>
            </el-table-column>
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div class="product-cell">
                  <el-image
                    v-if="row.productImage"
                    :src="row.productImage"
                    fit="cover"
                    class="cell-img"
                  />
                  <div class="cell-info">
                    <div class="cell-name">{{ row.productName }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="points" label="消耗积分" width="100" align="center">
              <template #default="{ row }">
                <span style="color: var(--color-primary); font-weight: 600;">{{ row.points }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getOrderStatusType(row.status)" size="small">
                  {{ getOrderStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="收货信息" min-width="240">
              <template #default="{ row }">
                <div class="shipping-info">
                  <div><strong>{{ row.receiverName }}</strong> {{ row.receiverPhone }}</div>
                  <div style="color: var(--color-text-secondary); font-size: 0.8125rem;">{{ row.receiverAddress }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="物流信息" width="200">
              <template #default="{ row }">
                <template v-if="row.expressCompany">
                  <div>{{ row.expressCompany }}</div>
                  <div style="color: var(--color-text-secondary); font-size: 0.8125rem;">{{ row.expressNo }}</div>
                </template>
                <span v-else style="color: var(--color-text-muted)">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="下单时间" width="170" />
            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 0"
                  type="primary"
                  link
                  size="small"
                  @click="openShipDialog(row)"
                >
                  发货
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!ordersLoading && orders.length === 0" description="暂无兑换订单" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="productDialogVisible"
      :title="editingProduct ? '编辑兑换商品' : '新增兑换商品'"
      width="560px"
      @close="resetProductForm"
    >
      <el-form :model="productForm" label-width="110px" ref="productFormRef">
        <el-form-item label="商品名称" prop="name" :rules="[{ required: true, message: '请输入商品名称' }]">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="商品图片" prop="image">
          <el-input v-model="productForm.image" placeholder="请输入图片URL" />
          <div class="form-hint">请输入图片完整URL地址</div>
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="productForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="所需积分" prop="points" :rules="[{ required: true, message: '请输入所需积分' }]">
          <el-input-number
            v-model="productForm.points"
            :min="1"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="库存" prop="stock" :rules="[{ required: true, message: '请输入库存' }]">
          <el-input-number
            v-model="productForm.stock"
            :min="0"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status" :rules="[{ required: true, message: '请选择状态' }]">
          <el-radio-group v-model="productForm.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number
            v-model="productForm.sortOrder"
            :min="0"
            :step="1"
            style="width: 100%"
          />
          <div class="form-hint">数值越小越靠前，默认为0</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="productSubmitting" @click="submitProduct">
          {{ editingProduct ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="shipDialogVisible"
      title="订单发货"
      width="480px"
      @close="resetShipForm"
    >
      <el-form :model="shipForm" label-width="100px" ref="shipFormRef">
        <el-form-item label="兑换单号">
          <span style="font-weight: 500;">{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="快递公司" prop="company" :rules="[{ required: true, message: '请输入快递公司' }]">
          <el-input v-model="shipForm.company" placeholder="如：顺丰速运" />
        </el-form-item>
        <el-form-item label="快递单号" prop="expressNo" :rules="[{ required: true, message: '请输入快递单号' }]">
          <el-input v-model="shipForm.expressNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import api from '../api'

const activeTab = ref('products')

const productsLoading = ref(false)
const products = ref([])
const productFilterStatus = ref(null)
const productDialogVisible = ref(false)
const productSubmitting = ref(false)
const editingProduct = ref(null)
const productFormRef = ref(null)

const productForm = reactive({
  name: '',
  image: '',
  description: '',
  points: 100,
  stock: 0,
  status: 1,
  sortOrder: 0
})

const ordersLoading = ref(false)
const orders = ref([])
const orderFilterStatus = ref(null)
const shipDialogVisible = ref(false)
const shipSubmitting = ref(false)
const currentOrder = ref(null)
const shipFormRef = ref(null)

const shipForm = reactive({
  company: '',
  expressNo: ''
})

function getOrderStatusText(status) {
  return { 0: '待处理', 1: '已发货', 2: '已完成', 3: '已取消' }[status] || '未知'
}

function getOrderStatusType(status) {
  return { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }[status] || 'info'
}

async function loadProducts() {
  productsLoading.value = true
  try {
    const params = {}
    if (productFilterStatus.value != null) params.status = productFilterStatus.value
    const res = await api.get('/admin/points-products', { params })
    if (res.data.code === 200) {
      products.value = res.data.data || []
    }
  } finally {
    productsLoading.value = false
  }
}

function openProductDialog(row) {
  if (row) {
    editingProduct.value = row
    Object.assign(productForm, {
      name: row.name,
      image: row.image || '',
      description: row.description || '',
      points: row.points,
      stock: row.stock,
      status: row.status,
      sortOrder: row.sortOrder || 0
    })
  } else {
    editingProduct.value = null
    resetProductForm()
  }
  productDialogVisible.value = true
}

function resetProductForm() {
  Object.assign(productForm, {
    name: '',
    image: '',
    description: '',
    points: 100,
    stock: 0,
    status: 1,
    sortOrder: 0
  })
  editingProduct.value = null
  productFormRef.value?.clearValidate()
}

async function submitProduct() {
  if (!productFormRef.value) return
  try {
    await productFormRef.value.validate()
  } catch (e) {
    return
  }

  productSubmitting.value = true
  try {
    let res
    if (editingProduct.value) {
      res = await api.put(`/admin/points-products/${editingProduct.value.id}`, productForm)
    } else {
      res = await api.post('/admin/points-products', productForm)
    }
    if (res.data.code === 200) {
      ElMessage.success(editingProduct.value ? '保存成功' : '创建成功')
      productDialogVisible.value = false
      loadProducts()
    }
  } finally {
    productSubmitting.value = false
  }
}

async function loadOrders() {
  ordersLoading.value = true
  try {
    const params = {}
    if (orderFilterStatus.value != null) params.status = orderFilterStatus.value
    const res = await api.get('/admin/exchange-orders', { params })
    if (res.data.code === 200) {
      orders.value = res.data.data || []
    }
  } finally {
    ordersLoading.value = false
  }
}

function openShipDialog(row) {
  currentOrder.value = row
  shipForm.company = ''
  shipForm.expressNo = ''
  shipDialogVisible.value = true
}

function resetShipForm() {
  shipForm.company = ''
  shipForm.expressNo = ''
  currentOrder.value = null
  shipFormRef.value?.clearValidate()
}

async function submitShip() {
  if (!shipFormRef.value) return
  try {
    await shipFormRef.value.validate()
  } catch (e) {
    return
  }

  if (!currentOrder.value) return

  shipSubmitting.value = true
  try {
    const res = await api.post(`/admin/exchange-orders/${currentOrder.value.id}/ship`, {
      company: shipForm.company,
      expressNo: shipForm.expressNo
    })
    if (res.data.code === 200) {
      ElMessage.success('发货成功')
      shipDialogVisible.value = false
      loadOrders()
    }
  } finally {
    shipSubmitting.value = false
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.admin-exchange {
  padding-bottom: 32px;
}

.tabs-card {
  padding: 0;
  overflow: hidden;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tab-toolbar .el-button {
  margin-left: auto;
}

.product-img {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  background: var(--color-bg-secondary);
}

.product-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.cell-img {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background: var(--color-bg-secondary);
  flex-shrink: 0;
}

.cell-info {
  min-width: 0;
  flex: 1;
}

.cell-name {
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shipping-info {
  line-height: 1.5;
}

.form-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}

:deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
}

:deep(.el-tabs__content) {
  padding: 20px;
}

:deep(.el-table .cell) {
  padding-top: 10px;
  padding-bottom: 10px;
}
</style>
