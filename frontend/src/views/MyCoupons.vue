<template>
  <div class="my-coupons-page">
    <h2 class="page-title">我的优惠券</h2>
    <div class="content-card">
      <el-tabs v-model="activeTab" class="coupon-tabs" @tab-change="onTabChange">
        <el-tab-pane label="未使用" name="unused">
          <div class="coupon-list">
            <div
              v-for="coupon in coupons.unused"
              :key="coupon.id"
              class="coupon-item"
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
                  有效期至 {{ formatDate(coupon.expiredAt) }}
                </div>
                <div class="coupon-code">券码：{{ coupon.code }}</div>
              </div>
              <div class="coupon-action">
                <el-button
                  type="primary"
                  size="small"
                  @click="goToUse(coupon)"
                >
                  立即使用
                </el-button>
              </div>
            </div>
            <el-empty v-if="!loading && coupons.unused.length === 0" description="暂无可用优惠券">
              <el-button type="primary" @click="$router.push('/coupon-center')">去领券</el-button>
            </el-empty>
          </div>
        </el-tab-pane>

        <el-tab-pane label="已使用" name="used">
          <div class="coupon-list">
            <div
              v-for="coupon in coupons.used"
              :key="coupon.id"
              class="coupon-item coupon-item--used"
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
                <div class="coupon-used-info">
                  用于订单：{{ coupon.usedOrderId }}
                </div>
                <div class="coupon-used-time">
                  使用时间：{{ formatDateTime(coupon.usedAt) }}
                </div>
              </div>
              <div class="coupon-status">
                <el-tag type="info" size="small">已使用</el-tag>
              </div>
            </div>
            <el-empty v-if="!loading && coupons.used.length === 0" description="暂无已使用优惠券" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="已过期" name="expired">
          <div class="coupon-list">
            <div
              v-for="coupon in coupons.expired"
              :key="coupon.id"
              class="coupon-item coupon-item--expired"
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
                <div class="coupon-expired-info">
                  过期时间：{{ formatDateTime(coupon.expiredAt) }}
                </div>
              </div>
              <div class="coupon-status">
                <el-tag type="info" size="small">已过期</el-tag>
              </div>
            </div>
            <el-empty v-if="!loading && coupons.expired.length === 0" description="暂无已过期优惠券" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '../api'

const activeTab = ref('unused')
const loading = ref(true)
const coupons = reactive({
  unused: [],
  used: [],
  expired: []
})

async function load(status) {
  loading.value = true
  try {
    const statusMap = { unused: 0, used: 1, expired: 2 }
    const res = await api.get('/coupons/my', {
      params: { status: statusMap[status] }
    })
    if (res.data.code === 200) {
      coupons[status] = res.data.data || []
    }
  } finally {
    loading.value = false
  }
}

async function onTabChange(tab) {
  await load(tab)
}

function goToUse(coupon) {
  if (coupon.applicableCategory) {
    window.location.href = `/products?categoryId=${coupon.applicableCategory}`
  } else {
    window.location.href = '/products'
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => load('unused'))
</script>

<style scoped>
.my-coupons-page {
  padding-bottom: 32px;
}

.coupon-tabs {
  padding: 0 24px 24px;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
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
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.coupon-item--used,
.coupon-item--expired {
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

.coupon-item--used .coupon-left,
.coupon-item--expired .coupon-left {
  background: linear-gradient(135deg, #bfbfbf 0%, #8c8c8c 100%);
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
  padding: 16px 20px;
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

.coupon-validity,
.coupon-code,
.coupon-used-info,
.coupon-used-time,
.coupon-expired-info {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.coupon-action {
  width: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  border-left: 1px dashed var(--color-border);
}

.coupon-status {
  width: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  border-left: 1px dashed var(--color-border);
}
</style>
