<template>
  <div class="coupon-center-page">
    <h2 class="page-title">领券中心</h2>
    <div v-loading="loading" class="coupon-list content-card">
      <div class="coupon-grid">
        <div
          v-for="coupon in coupons"
          :key="coupon.id"
          class="coupon-item"
          :class="{ 'coupon-item--disabled': !coupon.canClaim }"
        >
          <div class="coupon-left">
            <div class="coupon-amount">
              <template v-if="coupon.type === 1 || coupon.type === 3">
                <span class="currency">¥</span>
                <span class="amount">{{ coupon.faceValue }}</span>
              </template>
              <template v-else-if="coupon.type === 2">
                <span class="discount">{{ (coupon.discountRate * 10).toFixed(1) }}</span>
                <span class="discount-suffix">折</span>
              </template>
            </div>
            <div class="coupon-condition">
              {{ coupon.threshold > 0 ? `满${coupon.threshold}可用` : '无门槛' }}
            </div>
          </div>
          <div class="coupon-right">
            <div class="coupon-name">{{ coupon.name }}</div>
            <div class="coupon-type-tag" :class="`type-${coupon.type}`">
              {{ coupon.typeDesc }}
            </div>
            <div class="coupon-range">
              {{ coupon.categoryName || '全场通用' }}
            </div>
            <div class="coupon-validity">
              {{ formatDate(coupon.validStart) }} - {{ formatDate(coupon.validEnd) }}
            </div>
            <div class="coupon-stock">
              已领 {{ coupon.claimedQuantity }}/{{ coupon.totalQuantity }}
              <span v-if="coupon.perUserLimit > 1" class="per-limit">
                · 每人限领{{ coupon.perUserLimit }}张
              </span>
            </div>
          </div>
          <div class="coupon-action">
            <el-button
              type="primary"
              :disabled="!coupon.canClaim || claimingId === coupon.id"
              :loading="claimingId === coupon.id"
              size="small"
              @click="claim(coupon)"
            >
              {{ coupon.canClaim ? '立即领取' : '已领完' }}
            </el-button>
            <div v-if="coupon.userClaimedCount > 0" class="claimed-hint">
              已领取 {{ coupon.userClaimedCount }} 张
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && coupons.length === 0" description="暂无优惠券可领取" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const loading = ref(true)
const coupons = ref([])
const claimingId = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/coupons/available')
    if (res.data.code === 200) {
      coupons.value = res.data.data || []
    }
  } finally {
    loading.value = false
  }
}

async function claim(coupon) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!coupon.canClaim) return

  claimingId.value = coupon.id
  try {
    const res = await api.post(`/coupons/${coupon.id}/claim`)
    if (res.data.code === 200) {
      ElMessage.success('领取成功')
      load()
    }
  } finally {
    claimingId.value = null
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(load)
</script>

<style scoped>
.coupon-center-page {
  padding-bottom: 32px;
}

.coupon-list {
  padding: 24px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.coupon-item {
  position: relative;
  display: flex;
  background: linear-gradient(135deg, #fff 0%, #fafafa 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all var(--transition);
}

.coupon-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.coupon-item--disabled {
  opacity: 0.6;
}

.coupon-left {
  width: 140px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff7875 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
}

.coupon-left::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  background: var(--color-bg);
  border-radius: 50%;
}

.coupon-amount {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.currency {
  font-size: 1rem;
  margin-right: 2px;
}

.amount {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1;
}

.discount {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1;
}

.discount-suffix {
  font-size: 1rem;
  margin-left: 2px;
}

.coupon-condition {
  font-size: 0.875rem;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.coupon-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
}

.coupon-type-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 0.75rem;
  border-radius: 4px;
  width: fit-content;
}

.type-1 {
  background: #e6f7ff;
  color: #1890ff;
}

.type-2 {
  background: #f6ffed;
  color: #52c41a;
}

.type-3 {
  background: #fff1f0;
  color: #f5222d;
}

.coupon-range {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.coupon-validity {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.coupon-stock {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.per-limit {
  color: var(--color-primary);
}

.coupon-action {
  width: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  border-left: 1px dashed var(--color-border);
}

.claimed-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}
</style>
