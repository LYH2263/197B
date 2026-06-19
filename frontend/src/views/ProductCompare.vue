<template>
  <div class="compare-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">商品对比</h1>
        <el-text type="info" size="small">
          已添加 {{ compareStore.count }} 个商品，最多可对比 {{ compareStore.maxItems }} 个
        </el-text>
      </div>
      <div class="header-actions">
        <el-button
          v-if="compareStore.count > 0"
          :icon="Delete"
          @click="handleClear"
        >
          清空对比
        </el-button>
      </div>
    </div>

    <div v-if="compareStore.count === 0" class="empty-compare">
      <el-empty description="暂无对比商品">
        <template #description>
          <p>点击下方按钮或在商品列表/详情页添加商品到对比栏</p>
        </template>
        <el-button type="primary" @click="openSearch">添加商品</el-button>
      </el-empty>
    </div>

    <div v-else class="compare-container">
      <el-table
        :data="basicRows"
        border
        :cell-class-name="cellClassName"
        class="compare-table"
      >
        <el-table-column
          label="属性"
          width="120"
          fixed="left"
          prop="label"
          class-name="attr-col"
        />
        <el-table-column
          v-for="(item, idx) in displaySlots"
          :key="'basic-' + idx"
          :label="`商品 ${idx + 1}`"
          align="center"
        >
          <template #default="{ row }">
            <div v-if="item" class="cell-content">
              <template v-if="row.key === 'image'">
                <div class="product-cell">
                  <el-button
                    class="remove-btn"
                    size="small"
                    circle
                    type="danger"
                    plain
                    :icon="Close"
                    @click="handleRemove(item.id)"
                  />
                  <div class="product-img" @click="goDetail(item.id)">
                    <img
                      :src="item.mainImage || '/images/default-product.svg'"
                      alt=""
                      @error="$event.target.src = '/images/default-product.svg'"
                    />
                  </div>
                  <div
                    class="product-name"
                    :title="item.name"
                    @click="goDetail(item.id)"
                  >
                    {{ item.name }}
                  </div>
                  <el-button
                    type="primary"
                    size="small"
                    @click="goDetail(item.id)"
                  >
                    去购买
                  </el-button>
                </div>
              </template>
              <template v-else-if="row.key === 'price'">
                <span class="price">¥ {{ item.price }}</span>
              </template>
              <template v-else-if="row.key === 'stock'">
                <span :class="{ 'text-danger': item.stock === 0 }">
                  {{ item.stock > 0 ? `${item.stock} 件` : '缺货' }}
                </span>
              </template>
              <template v-else-if="row.key === 'category'">
                {{ getCategoryName(item.categoryId) }}
              </template>
              <template v-else-if="row.key === 'avgRating'">
                <el-rate
                  :model-value="Number(item.avgRating) || 0"
                  disabled
                  size="small"
                />
                <span class="rating-text">{{ item.avgRating ?? '暂无评分' }}</span>
              </template>
              <template v-else-if="row.key === 'salesCount'">
                {{ item.salesCount ?? 0 }} 件
              </template>
            </div>
            <div v-else class="empty-slot" @click="openSearch">
              <el-icon :size="24"><Plus /></el-icon>
              <span>添加商品</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-divider>规格参数</el-divider>

      <el-table
        v-if="paramRows.length > 0"
        :data="paramRows"
        border
        :row-class-name="paramRowClassName"
        class="compare-table param-table"
      >
        <el-table-column
          label="参数"
          width="120"
          fixed="left"
          prop="label"
          class-name="attr-col"
        />
        <el-table-column
          v-for="(item, idx) in displaySlots"
          :key="'param-' + idx"
          :label="`商品 ${idx + 1}`"
          align="center"
        >
          <template #default="{ row }">
            <div v-if="item" class="cell-content">
              {{ allParams[item.id]?.[row.key] ?? '-' }}
            </div>
            <div v-else class="empty-slot" @click="openSearch">
              <el-icon :size="24"><Plus /></el-icon>
              <span>添加商品</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-else
        description="暂无规格参数可对比"
        :image-size="80"
      />
    </div>

    <CompareSearchDialog v-model="searchVisible" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Close, Plus } from '@element-plus/icons-vue'
import { useCompareStore, parseProductParams } from '../stores/compare'
import api from '../api'
import CompareSearchDialog from '../components/CompareSearchDialog.vue'

const compareStore = useCompareStore()
const router = useRouter()
const searchVisible = ref(false)
const categories = ref([])

const displaySlots = computed(() => {
  const slots = []
  for (let i = 0; i < compareStore.maxItems; i++) {
    slots.push(compareStore.getSlot(i))
  }
  return slots
})

const basicRows = computed(() => [
  { key: 'image', label: '商品' },
  { key: 'price', label: '价格' },
  { key: 'stock', label: '库存' },
  { key: 'category', label: '分类' },
  { key: 'avgRating', label: '均分' },
  { key: 'salesCount', label: '销量' },
])

const allParams = computed(() => {
  const result = {}
  compareStore.items.forEach((item) => {
    result[item.id] = parseProductParams(item)
  })
  return result
})

const allParamKeys = computed(() => {
  const keySet = new Set()
  Object.values(allParams.value).forEach((params) => {
    Object.keys(params).forEach((k) => keySet.add(k))
  })
  return Array.from(keySet)
})

const paramRows = computed(() => {
  return allParamKeys.value.map((key) => ({ key, label: key }))
})

function paramRowClassName({ row }) {
  const values = compareStore.items.map(
    (item) => String(allParams.value[item.id]?.[row.key] ?? '')
  )
  const uniqueValues = new Set(values.filter((v) => v !== ''))
  return uniqueValues.size > 1 ? 'diff-row' : ''
}

function cellClassName() {
  return ''
}

function getCategoryName(categoryId) {
  if (!categoryId) return '-'
  const cat = categories.value.find((c) => c.id === categoryId)
  return cat ? cat.name : '-'
}

async function loadCategories() {
  try {
    const res = await api.get('/categories')
    if (res.data.code === 200) categories.value = res.data.data || []
  } catch {}
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空所有对比商品吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    compareStore.clearAll()
    ElMessage.success('已清空对比')
  } catch {}
}

function handleRemove(productId) {
  compareStore.removeProduct(productId)
  ElMessage.success('已移除')
}

function goDetail(productId) {
  router.push(`/products/${productId}`)
}

function openSearch() {
  searchVisible.value = true
}

onMounted(() => {
  loadCategories()
  if (compareStore.count > 0) {
    compareStore.refreshProducts()
  }
})
</script>

<style scoped>
.compare-page {
  padding-bottom: 80px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.empty-compare {
  padding: 60px 0;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.compare-container {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: 24px;
  overflow-x: auto;
}

.compare-table {
  width: 100%;
  min-width: 600px;
}

.compare-table :deep(.attr-col) {
  background: var(--color-bg) !important;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.compare-table :deep(.diff-row) {
  background: #fef3c7 !important;
}

.compare-table :deep(.diff-row .cell) {
  background: #fef3c7 !important;
}

.compare-table :deep(.diff-row td) {
  background: #fef3c7 !important;
}

.compare-table :deep(.diff-row:hover > td) {
  background: #fde68a !important;
}

.cell-content {
  padding: 8px;
  min-height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.product-cell {
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.remove-btn {
  position: absolute;
  top: 0;
  right: 0;
}

.product-img {
  width: 120px;
  height: 120px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
}

.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  text-align: center;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
  max-width: 160px;
}

.product-name:hover {
  color: var(--color-primary);
}

.price {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-primary);
}

.rating-text {
  margin-left: 6px;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.text-danger {
  color: #ef4444;
}

.empty-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 8px;
  color: var(--color-text-muted);
  cursor: pointer;
  min-height: 60px;
  transition: all var(--transition);
  border-radius: var(--radius-sm);
}

.empty-slot:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.param-table {
  margin-top: 8px;
}
</style>
