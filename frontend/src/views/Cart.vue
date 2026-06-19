<template>
  <div class="cart-page">
    <h2 class="page-title">购物车</h2>
    <div v-loading="loading" class="cart-content content-card">
      <template v-if="items.length">
        <el-table ref="tableRef" :data="items" :row-key="(r) => r.productId" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="50" :reserve-selection="false" />
          <el-table-column label="商品" min-width="280">
            <template #default="{ row }">
              <div class="cart-product">
                <img
                  :src="row.productImage || '/images/default-product.svg'"
                  alt=""
                  class="cart-product-img"
                  @error="$event.target.src = '/images/default-product.svg'"
                />
                <span class="name">{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">¥ {{ row.price }}</template>
          </el-table-column>
          <el-table-column label="数量" width="160">
            <template #default="{ row }">
              <el-input-number
                :model-value="row.quantity"
                :min="1"
                :max="row.stock"
                size="small"
                @update:model-value="(v) => updateQuantity(row, v)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">¥ {{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="danger" link @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="promotion" class="promotion-panel">
          <div class="promotion-header">
            <el-icon class="promotion-icon" color="#f97316"><Present /></el-icon>
            <span class="promotion-title">满减活动</span>
          </div>
          <div v-if="promotion.promotionId" class="promotion-body">
            <div class="promotion-name">{{ promotion.promotionName }}
              <el-tag size="small" type="success" effect="plain">{{ promotion.desc || (promotion.categoryName ? '限' + promotion.categoryName : '全场') }}</el-tag>
            </div>
            <div class="promotion-savings">
              <span class="savings-label">已减</span>
              <span class="savings-amount">¥ {{ formatMoney(promotion.promotionDiscount) }}</span>
            </div>
            <div v-if="promotion.nextTierThreshold" class="promotion-next">
              <el-progress
                :percentage="calculateProgress(promotion)"
                :stroke-width="8"
                :color="nextTierProgressColor"
                :show-text="false"
                class="next-progress"
              />
              <div class="next-text">
                <el-icon><TrendCharts /></el-icon>
                再买 <span class="gap-amount">¥ {{ formatMoney(promotion.gapToNextTier) }}</span>
                可享满{{ promotion.nextTierThreshold }}减{{ promotion.nextTierDiscount }}
              </div>
            </div>
          </div>
          <div v-else class="promotion-body promotion-empty">
            <span>购物车内商品暂未满足任何满减门槛，快去加购吧~</span>
          </div>
        </div>

        <div v-if="promotion?.recommendProducts?.length" class="recommend-panel">
          <div class="recommend-header">
            <el-icon class="recommend-icon" color="#ef4444"><Fire /></el-icon>
            <span class="recommend-title">凑单推荐</span>
            <span class="recommend-hint">（价格在差额内的最低价商品，快捷加购）</span>
          </div>
          <div class="recommend-grid">
            <div
              v-for="p in promotion.recommendProducts"
              :key="p.productId"
              class="recommend-card"
            >
              <img
                :src="p.productImage || '/images/default-product.svg'"
                alt=""
                class="recommend-img"
                @error="$event.target.src = '/images/default-product.svg'"
                @click="goToDetail(p.productId)"
              />
              <div class="recommend-info">
                <div class="recommend-name" @click="goToDetail(p.productId)">{{ p.productName }}</div>
                <div class="recommend-price-row">
                  <span class="recommend-price">¥ {{ formatMoney(p.price) }}</span>
                  <el-button type="primary" size="small" @click="quickAdd(p)" :loading="p.adding">
                    加购
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="cart-footer">
          <div class="cart-footer-left">
            <span class="cart-footer-text">已选 {{ checkedCount }} 件，合计：<strong>¥ {{ total }}</strong></span>
            <span v-if="promotion?.promotionDiscount > 0" class="promotion-discount-tag">
              满减优惠 <strong>-¥ {{ formatMoney(promotion.promotionDiscount) }}</strong>
            </span>
          </div>
          <el-button type="primary" :disabled="checkedCount === 0" @click="goCheckout">
            去结算
          </el-button>
        </div>
      </template>
      <el-empty v-else description="购物车是空的，去逛逛吧">
        <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Present, TrendCharts, Fire } from '@element-plus/icons-vue'
import api from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const items = ref([])
const selected = ref([])
const tableRef = ref(null)
const promotion = ref(null)

const checkedCount = computed(() => selected.value.length)
const total = computed(() => {
  let sum = selected.value.reduce((s, i) => s + Number(i.totalAmount || 0), 0)
  if (promotion.value?.promotionDiscount) {
    sum = Math.max(0, sum - Number(promotion.value.promotionDiscount))
  }
  return sum.toFixed(2)
})

const nextTierProgressColor = computed(() => {
  if (!promotion.value?.nextTierThreshold) return '#e5e7eb'
  const current = Number(promotion.value.applicableAmount || 0)
  const next = Number(promotion.value.nextTierThreshold || 0)
  const pct = next > 0 ? (current / next) * 100 : 0
  if (pct >= 80) return '#ef4444'
  if (pct >= 50) return '#f97316'
  return '#eab308'
})

function formatMoney(v) {
  if (v == null) return '0.00'
  return Number(v).toFixed(2)
}

function calculateProgress(promo) {
  if (!promo.nextTierThreshold) return 100
  const current = Number(promo.applicableAmount || 0)
  const prev = promo.tierThreshold ? Number(promo.tierThreshold) : 0
  const next = Number(promo.nextTierThreshold)
  const range = next - prev
  if (range <= 0) return 0
  const pct = Math.min(100, Math.max(0, ((current - prev) / range) * 100))
  return Math.round(pct)
}

async function onSelectionChange(rows) {
  const prevIds = new Set(selected.value.map((r) => r.productId))
  selected.value = rows
  for (const row of items.value) {
    const nowChecked = rows.some((r) => r.productId === row.productId) ? 1 : 0
    if (row.checked !== nowChecked) {
      try {
        await api.put('/cart/checked', { productId: row.productId, checked: nowChecked })
        row.checked = nowChecked
      } catch (e) {}
    }
  }
  await recalcPromotion()
}

async function recalcPromotion() {
  try {
    const res = await api.get('/cart/with-promotion')
    if (res.data.code === 200) {
      promotion.value = res.data.data?.promotion || null
    }
  } catch (e) {
    promotion.value = null
  }
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/cart/with-promotion')
    if (res.data.code === 200) {
      items.value = res.data.data?.items || []
      promotion.value = res.data.data?.promotion || null
    } else {
      const fallback = await api.get('/cart')
      items.value = fallback.data.code === 200 ? fallback.data.data || [] : []
    }
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
    nextTick(() => {
      items.value.filter((i) => i.checked === 1).forEach((row) => tableRef.value?.toggleRowSelection(row, true))
    })
  } finally {
    loading.value = false
  }
}

async function updateQuantity(row, val) {
  try {
    await api.put('/cart/quantity', { productId: row.productId, quantity: val })
    row.quantity = val
    row.totalAmount = (Number(row.price) * val).toFixed(2)
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
    await recalcPromotion()
  } catch (e) {}
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该商品？', '提示')
    await api.delete(`/cart/${row.productId}`)
    items.value = items.value.filter((i) => i.productId !== row.productId)
    selected.value = selected.value.filter((i) => i.productId !== row.productId)
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
    await recalcPromotion()
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

async function quickAdd(p) {
  p.adding = true
  try {
    const res = await api.post('/cart/add', { productId: p.productId, quantity: 1 })
    if (res.data.code === 200) {
      ElMessage.success(`已加入购物车：${p.productName}`)
      const existing = items.value.find(i => i.productId === p.productId)
      if (existing) {
        existing.quantity = existing.quantity + 1
        existing.totalAmount = (Number(existing.price) * existing.quantity).toFixed(2)
        if (existing.checked !== 1) {
          existing.checked = 1
          tableRef.value?.toggleRowSelection(existing, true)
        }
      } else {
        await load()
      }
      userStore.cartCount = (userStore.cartCount || 0) + 1
    }
  } finally {
    p.adding = false
  }
}

function goToDetail(productId) {
  router.push(`/products/${productId}`)
}

function goCheckout() {
  if (selected.value.length === 0) return
  router.push({ path: '/orders', query: { checkout: '1' } })
}

onMounted(load)
</script>

<style scoped>
.cart-page {
  padding-bottom: 32px;
}

.cart-content {
  padding: 24px;
}

.cart-product {
  display: flex;
  align-items: center;
  gap: 14px;
}

.cart-product-img {
  width: 56px;
  height: 56px;
  object-fit: contain;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}

.cart-product .name {
  font-weight: 500;
  color: var(--color-text);
}

.promotion-panel {
  margin-top: 20px;
  padding: 16px;
  border-radius: var(--radius);
  background: linear-gradient(135deg, #fff7ed 0%, #fff1f2 100%);
  border: 1px solid #fed7aa;
}

.promotion-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #9a3412;
}

.promotion-icon {
  font-size: 18px;
}

.promotion-title {
  font-size: 0.9375rem;
}

.promotion-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.promotion-name {
  font-weight: 500;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

.promotion-savings {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.savings-label {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.savings-amount {
  font-size: 1.25rem;
  font-weight: 700;
  color: #ef4444;
}

.next-progress {
  margin-bottom: 8px;
  max-width: 360px;
}

.next-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #b45309;
  font-size: 0.875rem;
  font-weight: 500;
}

.gap-amount {
  color: #ef4444;
  font-weight: 700;
  font-size: 1rem;
}

.promotion-empty {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  padding: 8px 0;
}

.recommend-panel {
  margin-top: 16px;
  padding: 16px;
  border-radius: var(--radius);
  background: #fff;
  border: 1px dashed #fecaca;
}

.recommend-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.recommend-icon {
  font-size: 18px;
}

.recommend-title {
  font-weight: 600;
  color: #991b1b;
  font-size: 0.9375rem;
}

.recommend-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-left: 4px;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.recommend-card {
  background: #fafafa;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--color-border);
  transition: transform var(--transition), box-shadow var(--transition);
  display: flex;
  flex-direction: column;
}

.recommend-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.recommend-img {
  width: 100%;
  height: 120px;
  object-fit: contain;
  background: #fff;
  cursor: pointer;
  padding: 8px;
}

.recommend-info {
  padding: 10px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.recommend-name {
  font-size: 0.8125rem;
  color: var(--color-text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
  min-height: 36px;
}

.recommend-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.recommend-price {
  font-weight: 700;
  color: #ef4444;
  font-size: 1rem;
}

.cart-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.cart-footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.cart-footer-text {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}

.cart-footer-text strong {
  color: var(--color-primary);
  font-size: 1.125rem;
}

.promotion-discount-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fef2f2;
  border-radius: 9999px;
  color: #dc2626;
  font-size: 0.8125rem;
}

.promotion-discount-tag strong {
  font-weight: 700;
  font-size: 0.9375rem;
}
</style>
