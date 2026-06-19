<template>
  <div class="my-history">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">我的浏览历史</h2>
        <el-tag
          v-if="viewHistory.privacyMode"
          type="info"
          effect="dark"
          size="small"
          style="margin-left: 8px"
        >
          <el-icon><View /></el-icon>
          隐私浏览中
        </el-tag>
      </div>
      <div class="header-actions">
        <el-switch
          v-model="viewHistory.privacyMode"
          @change="viewHistory.setPrivacyMode($event)"
          active-text="隐私浏览"
          inactive-text="正常模式"
          inline-prompt
        />
        <el-button
          v-if="showMergeBtn"
          type="success"
          :icon="Promotion"
          :loading="merging"
          @click="onMerge"
        >
          合并到账户 ({{ localCount }})
        </el-button>
        <el-button
          v-if="selectMode"
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="onBatchRemove"
        >
          批量删除 ({{ selectedIds.length }})
        </el-button>
        <el-button
          v-if="totalCount > 0"
          type="danger"
          plain
          :icon="Delete"
          @click="onClearAll"
        >
          清空
        </el-button>
        <el-button :type="selectMode ? 'warning' : 'default'" @click="selectMode = !selectMode">
          {{ selectMode ? '退出选择' : '批量管理' }}
        </el-button>
      </div>
    </div>

    <div v-if="viewHistory.privacyMode" class="privacy-notice">
      <el-icon :size="20" color="#6366f1"><InfoFilled /></el-icon>
      <span>当前处于<strong>隐私浏览模式</strong>，浏览记录将不会被保存。</span>
      <el-button type="primary" link @click="viewHistory.setPrivacyMode(false)">退出隐私模式</el-button>
    </div>

    <section class="main-content" v-loading="loading">
      <div v-if="selectMode && totalCount > 0" class="select-bar">
        <el-checkbox
          :model-value="isAllSelected"
          :indeterminate="isIndeterminate"
          @change="toggleSelectAll"
        >
          全选
        </el-checkbox>
        <span class="select-hint">已选 {{ selectedIds.length }} 项</span>
      </div>

      <div v-if="items.length > 0" class="history-list">
        <div
          v-for="item in items"
          :key="item.id"
          class="history-item"
          :class="{
            'off-shelf': item.offShelf,
            'selected': selectedIds.includes(item.id),
          }"
        >
          <div v-if="selectMode" class="select-check" @click.stop="toggleSelect(item.id)">
            <el-checkbox :model-value="selectedIds.includes(item.id)" @click.stop />
          </div>

          <div
            v-if="item.offShelf"
            class="off-shelf-badge"
          >
            已下架
          </div>

          <div class="item-img" @click="goDetail(item.productId)">
            <img
              :src="item.productImage || '/images/default-product.svg'"
              :alt="item.productName"
              @error="$event.target.src = '/images/default-product.svg'"
            />
          </div>

          <div class="item-body">
            <h4 class="item-name" @click="goDetail(item.productId)">{{ item.productName }}</h4>
            <div class="price-row">
              <span class="current-price-label">现价</span>
              <span class="current-price">¥ {{ item.currentPrice }}</span>
              <span class="viewed-price-label">浏览时</span>
              <span class="viewed-price">¥ {{ item.viewedPrice }}</span>
            </div>
            <div class="change-row">
              <span
                class="change-value"
                :class="getChangeClass(item.priceChangePercent)"
              >
                <template v-if="(item.priceChangePercent || 0) > 0">
                  ↑ +{{ formatPercent(item.priceChangePercent) }}
                </template>
                <template v-else-if="(item.priceChangePercent || 0) < 0">
                  ↓ {{ formatPercent(item.priceChangePercent) }}
                </template>
                <template v-else>
                  — 持平
                </template>
              </span>
              <span class="stock-info" :class="{ 'out': (item.stock ?? 0) < 1 }">
                {{ (item.stock ?? 0) < 1 ? '无货' : `库存 ${item.stock}` }}
              </span>
              <span class="view-time">{{ formatTime(item.viewedAt) }}</span>
            </div>
            <div class="item-actions">
              <el-button
                v-if="!item.offShelf"
                size="small"
                type="primary"
                :disabled="(item.stock ?? 0) < 1"
                @click="addToCart(item)"
              >
                加入购物车
              </el-button>
              <el-button
                v-else
                size="small"
                type="primary"
                plain
                @click="findSimilar(item)"
              >
                找相似
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :icon="Delete"
                @click="onRemove(item)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <el-empty
        v-else-if="!loading"
        :description="viewHistory.privacyMode ? '隐私模式下不显示浏览记录' : '暂无浏览记录'"
        :image-size="120"
      >
        <template #description>
          <p v-if="viewHistory.privacyMode">关闭隐私模式后即可正常记录浏览历史</p>
          <template v-else>
            <p>去逛逛发现心仪的商品吧～</p>
            <el-button type="primary" link @click="$router.push({ name: 'ProductList' })">去商品列表</el-button>
          </template>
        </template>
      </el-empty>

      <div v-if="totalPages > 1" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="totalCount"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadData"
          @size-change="onSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Promotion, InfoFilled, View } from '@element-plus/icons-vue'
import api from '../api'
import { useUserStore } from '../stores/user'
import { useViewHistoryStore } from '../stores/viewHistory'

const router = useRouter()
const userStore = useUserStore()
const viewHistory = useViewHistoryStore()

const loading = ref(false)
const items = ref([])
const totalCount = ref(0)
const page = ref(1)
const size = ref(10)
const selectMode = ref(false)
const selectedIds = ref([])
const merging = ref(false)

const showMergeBtn = computed(() =>
  viewHistory.isLoggedIn && viewHistory.hasLocalItems()
)

const localCount = computed(() => viewHistory.localList.length)

const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / size.value)))

const isAllSelected = computed(() =>
  items.value.length > 0 && items.value.every(i => selectedIds.value.includes(i.id))
)
const isIndeterminate = computed(() => {
  const count = items.value.filter(i => selectedIds.value.includes(i.id)).length
  return count > 0 && count < items.value.length
})

function formatPercent(v) {
  if (v == null) return '0.00%'
  const n = Number(v)
  return (n >= 0 ? '+' : '') + n.toFixed(2) + '%'
}

function getChangeClass(p) {
  const n = Number(p || 0)
  if (n > 0) return 'up'
  if (n < 0) return 'down'
  return ''
}

function formatTime(v) {
  if (!v) return ''
  const d = typeof v === 'string' ? new Date(v) : new Date(v)
  if (isNaN(d.getTime())) return v
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function enrichLocalItems(localItems) {
  const missingProductIds = localItems
    .filter(i => !i.productName || i.currentPrice == null)
    .map(i => Number(i.productId))
    .filter(Boolean)

  if (missingProductIds.length === 0) {
    return localItems.map(i => {
      const currentPrice = i.currentPrice ?? i.viewedPrice
      const diff = Number(currentPrice) - Number(i.viewedPrice || 0)
      const viewedPrice = Number(i.viewedPrice || 0)
      let percent = 0
      if (viewedPrice > 0) {
        percent = (diff / viewedPrice) * 100
      }
      return {
        ...i,
        currentPrice,
        priceChange: diff,
        priceChangePercent: percent,
        offShelf: i.status === 0,
      }
    })
  }

  const uniqueIds = [...new Set(missingProductIds)]
  const idToProduct = {}
  try {
    const res = await api.get('/products')
    if (res.data.code === 200 && Array.isArray(res.data.data)) {
      for (const p of res.data.data) {
        idToProduct[p.id] = p
      }
    }
  } catch {}

  const results = []
  for (const i of localItems) {
    const p = idToProduct[Number(i.productId)]
    const currentPrice = i.currentPrice ?? (p ? p.price : i.viewedPrice)
    const name = i.productName || (p ? p.name : `商品 #${i.productId}`)
    const image = i.productImage || (p ? p.mainImage : '')
    const stock = i.stock != null ? i.stock : (p ? p.stock : 0)
    const status = i.status != null ? i.status : (p ? p.status : 1)
    const diff = Number(currentPrice) - Number(i.viewedPrice || 0)
    const viewedPrice = Number(i.viewedPrice || 0)
    let percent = 0
    if (viewedPrice > 0) {
      percent = (diff / viewedPrice) * 100
    }
    results.push({
      ...i,
      productName: name,
      productImage: image,
      categoryId: i.categoryId || (p ? p.categoryId : null),
      currentPrice,
      stock,
      status,
      priceChange: diff,
      priceChangePercent: percent,
      offShelf: status === 0,
    })
  }
  return results
}

async function loadData() {
  if (viewHistory.privacyMode) {
    items.value = []
    totalCount.value = 0
    return
  }

  loading.value = true
  selectedIds.value = []
  try {
    if (viewHistory.isLoggedIn) {
      const res = await api.get('/view-history', { params: { page: page.value, size: size.value } })
      if (res.data.code === 200) {
        items.value = res.data.data?.list || []
        totalCount.value = res.data.data?.total || 0
      }
    } else {
      const result = viewHistory.getLocalWithPage(page.value, size.value)
      const enriched = await enrichLocalItems(result.list)
      items.value = enriched
      totalCount.value = result.total
    }
  } finally {
    loading.value = false
  }
}

function onSizeChange(newSize) {
  size.value = newSize
  page.value = 1
  loadData()
}

function clearSelection() {
  selectedIds.value = []
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

function toggleSelectAll(val) {
  if (val) selectedIds.value = items.value.map(i => i.id)
  else selectedIds.value = []
}

function goDetail(pid) {
  router.push({ name: 'ProductDetail', params: { id: pid } })
}

function findSimilar(item) {
  if (item.categoryId) {
    router.push({ name: 'ProductList', query: { categoryId: item.categoryId } })
  } else {
    router.push({ name: 'ProductList' })
  }
}

async function addToCart(item) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  try {
    await api.post('/cart/add', { productId: item.productId, quantity: 1 })
    ElMessage.success('已加入购物车')
    userStore.cartCount = (userStore.cartCount || 0) + 1
  } catch {}
}

async function onRemove(item) {
  try {
    await ElMessageBox.confirm(`确定删除该浏览记录吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    if (viewHistory.isLoggedIn && String(item.id).startsWith('local_') === false) {
      await api.delete(`/view-history/${item.id}`)
    } else {
      viewHistory.removeLocalById(item.id)
    }
    ElMessage.success('已删除')
    const idx = selectedIds.value.indexOf(item.id)
    if (idx >= 0) selectedIds.value.splice(idx, 1)
    await loadData()
  } catch {}
}

async function onBatchRemove() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择记录')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条记录吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    if (viewHistory.isLoggedIn) {
      const dbIds = selectedIds.value.filter(id => !String(id).startsWith('local_'))
      const localIds = selectedIds.value.filter(id => String(id).startsWith('local_'))
      if (dbIds.length > 0) {
        await api.post('/view-history/batch-remove', { ids: dbIds })
      }
      if (localIds.length > 0) {
        viewHistory.removeLocalBatch(localIds)
      }
    } else {
      viewHistory.removeLocalBatch(selectedIds.value)
    }
    ElMessage.success('批量删除成功')
    clearSelection()
    await loadData()
  } catch {}
}

async function onClearAll() {
  try {
    await ElMessageBox.confirm(
      `确定清空全部浏览记录吗？此操作不可恢复。`,
      '清空历史',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    if (viewHistory.isLoggedIn) {
      await api.delete('/view-history/clear')
    }
    viewHistory.clearLocal()
    ElMessage.success('已清空浏览历史')
    clearSelection()
    page.value = 1
    await loadData()
  } catch {}
}

async function onMerge() {
  if (!viewHistory.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将本地的 ${viewHistory.localList.length} 条浏览记录合并到账户中？合并后本地记录将清除。`,
      '合并浏览历史',
      { type: 'info' },
    )
  } catch {
    return
  }
  merging.value = true
  try {
    const result = await viewHistory.mergeToAccount()
    const count = result?.mergedCount || 0
    ElMessage.success(count > 0 ? `成功合并 ${count} 条记录` : '没有需要合并的记录')
    page.value = 1
    await loadData()
  } finally {
    merging.value = false
  }
}

watch(
  () => viewHistory.privacyMode,
  () => {
    page.value = 1
    loadData()
  },
)

watch(
  () => viewHistory.isLoggedIn,
  async (newVal) => {
    page.value = 1
    if (newVal && viewHistory.hasLocalItems()) {
      try {
        await ElMessageBox.confirm(
          `检测到本地有 ${viewHistory.localList.length} 条浏览记录，是否合并到账户？`,
          '合并浏览历史',
          {
            confirmButtonText: '立即合并',
            cancelButtonText: '稍后再说',
            type: 'info',
          },
        )
        merging.value = true
        const result = await viewHistory.mergeToAccount()
        const count = result?.mergedCount || 0
        ElMessage.success(count > 0 ? `成功合并 ${count} 条记录` : '没有需要合并的记录')
      } catch {}
      finally {
        merging.value = false
      }
    }
    await loadData()
  },
)

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-history {
  padding-bottom: 48px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  letter-spacing: -0.02em;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.privacy-notice {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: var(--radius-md);
  color: #4338ca;
  margin-bottom: 20px;
  font-size: 0.9375rem;
  flex-wrap: wrap;
}

.privacy-notice strong {
  color: #4f46e5;
}

.main-content {
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  padding: 20px;
  min-height: 400px;
}

.select-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
}

.select-hint {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  position: relative;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.history-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border-color: #d1d5db;
}

.history-item.selected {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.history-item.off-shelf {
  opacity: 0.85;
}

.select-check {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  padding: 4px 6px;
  border-radius: 6px;
  border: 1px solid var(--color-border);
}

.off-shelf-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 5;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 4px;
}

.item-img {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
}

.item-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.item-name {
  margin: 0 0 10px;
  font-size: 1rem;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.4;
  cursor: pointer;
  transition: color 0.15s;
}

.item-name:hover {
  color: var(--color-primary);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.current-price-label,
.viewed-price-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.current-price {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 1.125rem;
  margin-right: 8px;
}

.viewed-price {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  text-decoration: line-through;
}

.change-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
  font-size: 0.8125rem;
  flex-wrap: wrap;
}

.change-value {
  font-weight: 600;
}

.change-value.up {
  color: #ef4444;
}

.change-value.down {
  color: #10b981;
}

.stock-info {
  color: var(--color-text-secondary);
}

.stock-info.out {
  color: #ef4444;
  font-weight: 600;
}

.view-time {
  color: var(--color-text-muted);
  margin-left: auto;
}

.item-actions {
  margin-top: auto;
  display: flex;
  gap: 8px;
  align-items: center;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 600px) {
  .history-item {
    flex-direction: column;
  }
  .item-img {
    width: 100%;
    height: 200px;
  }
  .view-time {
    margin-left: 0;
  }
}
</style>
