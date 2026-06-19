<template>
  <div class="exchange-list-page">
    <h2 class="page-title">我的兑换记录</h2>
    <div v-loading="loading" class="exchange-content">
      <div
        v-for="order in orders"
        :key="order.id"
        class="exchange-card content-card"
      >
        <div class="exchange-card-header">
          <span class="exchange-no">兑换单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small" class="exchange-tag">
            {{ statusText(order.status) }}
          </el-tag>
        </div>

        <div class="exchange-body">
          <div class="exchange-product">
            <el-image
              :src="order.productImage"
              :preview-src-list="[order.productImage]"
              fit="cover"
              class="product-image"
            />
            <div class="product-info">
              <div class="product-name">{{ order.productName }}</div>
              <div class="product-points">
                消耗积分：<strong>{{ order.points }}</strong>
              </div>
            </div>
          </div>

          <div class="exchange-details">
            <div class="detail-row">
              <span class="detail-label">收货人：</span>
              <span class="detail-value">{{ order.receiverName }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">联系电话：</span>
              <span class="detail-value">{{ order.receiverPhone }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">收货地址：</span>
              <span class="detail-value">{{ order.receiverAddress }}</span>
            </div>
            <div v-if="order.status === 1 || order.status === 2" class="detail-row">
              <span class="detail-label">快递公司：</span>
              <span class="detail-value">{{ order.expressCompany || '-' }}</span>
            </div>
            <div v-if="order.status === 1 || order.status === 2" class="detail-row">
              <span class="detail-label">快递单号：</span>
              <span class="detail-value">{{ order.expressNo || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">创建时间：</span>
              <span class="detail-value">{{ order.createdAt }}</span>
            </div>
          </div>
        </div>

        <div class="exchange-actions">
          <template v-if="order.status === 1">
            <el-button type="primary" size="small" @click="confirmReceive(order.id)">确认收货</el-button>
          </template>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="暂无兑换记录" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const loading = ref(true)
const orders = ref([])

const statusText = (s) => ({ 0: '待处理', 1: '已发货', 2: '已完成', 3: '已取消' })[s] || '未知'
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' })[s] || 'info'

async function load() {
  loading.value = true
  try {
    const res = await api.get('/exchange/orders')
    orders.value = res.data.code === 200 ? res.data.data || [] : []
  } finally {
    loading.value = false
  }
}

async function confirmReceive(id) {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '确认收货', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.post(`/exchange/orders/${id}/receive`)
    ElMessage.success('确认收货成功')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(load)
</script>

<style scoped>
.exchange-list-page {
  padding-bottom: 32px;
}

.exchange-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.exchange-card {
  padding: 24px;
}

.exchange-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.exchange-no {
  font-weight: 600;
  color: var(--color-text);
}

.exchange-tag {
  border-radius: 6px;
}

.exchange-body {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.exchange-product {
  display: flex;
  gap: 16px;
  min-width: 280px;
}

.product-image {
  width: 88px;
  height: 88px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  border: 1px solid var(--color-border);
}

.product-info {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4px 0;
}

.product-name {
  font-weight: 600;
  color: var(--color-text);
  font-size: 0.9375rem;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-points {
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
}

.product-points strong {
  color: var(--color-primary);
  font-size: 1.0625rem;
  font-weight: 700;
}

.exchange-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  font-size: 0.875rem;
}

.detail-label {
  color: var(--color-text-muted);
  flex-shrink: 0;
  width: 80px;
}

.detail-value {
  color: var(--color-text);
  flex: 1;
  word-break: break-all;
}

.exchange-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-light);
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .exchange-body {
    flex-direction: column;
    gap: 16px;
  }

  .exchange-product {
    min-width: unset;
  }
}
</style>
