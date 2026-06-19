<template>
  <div class="after-sale-list-page">
    <h2 class="page-title">我的售后</h2>
    <div v-loading="loading" class="after-sale-content">
      <div
        v-for="item in list"
        :key="item.id"
        class="as-card content-card"
        @click="$router.push(`/after-sale/${item.id}`)"
      >
        <div class="as-card-header">
          <div class="as-card-header-left">
            <span class="as-no">售后单号：{{ item.afterSaleNo }}</span>
            <el-tag :type="typeType(item.type)" size="small" class="as-type-tag">
              {{ typeText(item.type) }}
            </el-tag>
          </div>
          <el-tag :type="statusType(item.status)" size="small" class="as-status-tag">
            {{ statusText(item.status) }}
          </el-tag>
        </div>
        <div class="as-card-body">
          <el-image
            v-if="item.productImage"
            :src="item.productImage"
            fit="cover"
            class="as-product-img"
          />
          <div class="as-product-info">
            <div class="as-product-name">{{ item.productName }}</div>
            <div class="as-product-price">
              ¥ {{ item.originalPrice }} × {{ item.originalQuantity }}
            </div>
            <div class="as-reason">原因：{{ item.reason }}</div>
          </div>
          <div class="as-right">
            <div class="as-amount-row">
              <span class="as-label">实付：</span>
              <span class="as-value">¥ {{ item.originalTotalAmount }}</span>
            </div>
            <div v-if="item.refundAmount != null" class="as-amount-row">
              <span class="as-label">退款：</span>
              <span class="as-value as-refund">¥ {{ item.refundAmount }}</span>
            </div>
            <div class="as-time">{{ item.createdAt }}</div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无售后记录" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const loading = ref(true)
const list = ref([])

function typeText(t) {
  return { 1: '退货退款', 2: '换货', 3: '仅退款' }[t] || '未知'
}
function typeType(t) {
  return { 1: 'warning', 2: 'primary', 3: 'success' }[t] || 'info'
}
function statusText(s) {
  return {
    0: '待审核', 1: '已通过', 2: '已拒绝', 3: '待寄回',
    4: '仓库确认', 5: '已退款', 6: '已换货发出', 7: '已取消'
  }[s] || '未知'
}
function statusType(s) {
  return {
    0: 'warning', 1: 'success', 2: 'danger', 3: 'primary',
    4: 'info', 5: 'success', 6: 'success', 7: 'info'
  }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/after-sale')
    list.value = res.data.code === 200 ? res.data.data || [] : []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.after-sale-list-page {
  padding-bottom: 32px;
}

.after-sale-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.as-card {
  padding: 20px 24px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.as-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.as-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--color-border);
}

.as-card-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.as-no {
  font-weight: 600;
  color: var(--color-text);
  font-size: 0.9375rem;
}

.as-type-tag,
.as-status-tag {
  border-radius: 6px;
}

.as-card-body {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.as-product-img {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  background: var(--color-bg-secondary);
}

.as-product-info {
  flex: 1;
  min-width: 0;
}

.as-product-name {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
  line-height: 1.4;
}

.as-product-price {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  margin-bottom: 6px;
}

.as-reason {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.as-right {
  width: 160px;
  text-align: right;
  flex-shrink: 0;
}

.as-amount-row {
  margin-bottom: 4px;
  font-size: 0.9375rem;
}

.as-label {
  color: var(--color-text-muted);
}

.as-value {
  color: var(--color-text);
  font-weight: 500;
}

.as-refund {
  color: var(--color-primary) !important;
  font-weight: 600 !important;
}

.as-time {
  margin-top: 8px;
  color: var(--color-text-muted);
  font-size: 0.8125rem;
}
</style>
