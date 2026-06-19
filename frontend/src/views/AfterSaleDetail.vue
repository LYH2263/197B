<template>
  <div v-loading="loading" class="after-sale-detail">
    <template v-if="detail">
      <div class="main-card content-card">
        <div class="main-card-header">
          <div class="header-left">
            <span class="as-no">售后单号：{{ detail.afterSale.afterSaleNo }}</span>
            <el-tag :type="typeType(detail.afterSale.type)" size="small">
              {{ typeText(detail.afterSale.type) }}
            </el-tag>
          </div>
          <el-tag :type="statusType(detail.afterSale.status)" size="large" effect="dark">
            {{ statusText(detail.afterSale.status) }}
          </el-tag>
        </div>

        <div class="product-section">
          <el-image
            v-if="detail.afterSale.productImage"
            :src="detail.afterSale.productImage"
            fit="cover"
            class="product-img"
          />
          <div class="product-info">
            <div class="product-name">{{ detail.afterSale.productName }}</div>
            <div class="product-meta">
              单价：¥ {{ detail.afterSale.originalPrice }} × {{ detail.afterSale.originalQuantity }}
            </div>
            <div class="product-meta">
              实付金额：<span class="amount">¥ {{ detail.afterSale.originalTotalAmount }}</span>
            </div>
            <div v-if="detail.afterSale.refundAmount != null" class="product-meta">
              退款金额：<span class="amount refund">¥ {{ detail.afterSale.refundAmount }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="info-section">
          <div class="info-title">售后信息</div>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">售后原因：</span>
              <span class="info-value">{{ detail.afterSale.reason }}</span>
            </div>
            <div class="info-item" v-if="detail.afterSale.description">
              <span class="info-label">问题描述：</span>
              <span class="info-value">{{ detail.afterSale.description }}</span>
            </div>
            <div class="info-item" v-if="detail.afterSale.rejectReason">
              <span class="info-label">拒绝原因：</span>
              <span class="info-value danger">{{ detail.afterSale.rejectReason }}</span>
            </div>
          </div>
          <div v-if="voucherImages.length > 0" class="voucher-section">
            <div class="info-label">凭证图片：</div>
            <div class="voucher-list">
              <el-image
                v-for="(img, idx) in voucherImages"
                :key="idx"
                :src="img"
                fit="cover"
                class="voucher-img"
                :preview-src-list="voucherImages"
                :initial-index="idx"
              />
            </div>
          </div>
        </div>

        <div v-if="detail.afterSale.returnCompany" class="info-section">
          <div class="info-title">退货物流</div>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">快递公司：</span>
              <span class="info-value">{{ detail.afterSale.returnCompany }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">运单号：</span>
              <span class="info-value">{{ detail.afterSale.returnTrackingNo }}</span>
            </div>
          </div>
        </div>

        <div v-if="detail.afterSale.exchangeCompany" class="info-section">
          <div class="info-title">换货发出物流</div>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">快递公司：</span>
              <span class="info-value">{{ detail.afterSale.exchangeCompany }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">运单号：</span>
              <span class="info-value">{{ detail.afterSale.exchangeTrackingNo }}</span>
            </div>
          </div>
        </div>

        <div v-if="detail.supplement" class="info-section supplement-section">
          <div class="info-title">
            补款单
            <el-tag :type="detail.supplement.status === 0 ? 'warning' : detail.supplement.status === 1 ? 'success' : 'info'" size="small" style="margin-left: 8px;">
              {{ detail.supplement.status === 0 ? '待支付' : detail.supplement.status === 1 ? '已支付' : '已取消' }}
            </el-tag>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">补款单号：</span>
              <span class="info-value">{{ detail.supplement.supplementNo }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">补款金额：</span>
              <span class="info-value danger">¥ {{ detail.supplement.priceDiff }}</span>
            </div>
            <div v-if="detail.supplement.paidAt" class="info-item">
              <span class="info-label">支付时间：</span>
              <span class="info-value">{{ detail.supplement.paidAt }}</span>
            </div>
          </div>
        </div>

        <div class="actions">
          <el-button v-if="canShipReturn" type="primary" @click="openShipDialog">填写退货物流</el-button>
          <el-button v-if="canCancel" type="danger" plain @click="cancel">取消申请</el-button>
          <el-button v-if="canPaySupplement" type="warning" @click="paySupplement">补款 ¥{{ detail.supplement.priceDiff }}</el-button>
        </div>
      </div>

      <div class="timeline-card content-card">
        <div class="timeline-title">售后进度时间线</div>
        <el-timeline>
          <el-timeline-item
            v-for="log in detail.logs"
            :key="log.id"
            :timestamp="log.createdAt"
            :type="timelineType(log)"
            :hollow="false"
          >
            <div class="tl-item">
              <div class="tl-header">
                <span class="tl-operator">
                  <el-tag :type="operatorTagType(log.operatorRole)" size="small">
                    {{ operatorText(log.operatorRole) }}
                  </el-tag>
                  <strong v-if="log.operatorName" style="margin-left: 6px;">{{ log.operatorName }}</strong>
                </span>
              </div>
              <div class="tl-remark">{{ log.remark }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <el-dialog v-model="shipVisible" title="填写退货物流" width="450px" @close="shipForm = {}">
        <el-form :model="shipForm" label-width="100px">
          <el-form-item label="快递公司" required>
            <el-input v-model="shipForm.company" placeholder="如：顺丰速运" />
          </el-form-item>
          <el-form-item label="运单号" required>
            <el-input v-model="shipForm.trackingNo" placeholder="物流单号" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="shipVisible = false">取消</el-button>
          <el-button type="primary" :loading="shipSubmitting" @click="submitShip">提交</el-button>
        </template>
      </el-dialog>
    </template>
    <el-empty v-else-if="!loading" description="售后不存在" />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const route = useRoute()
const loading = ref(true)
const detail = ref(null)
const shipVisible = ref(false)
const shipSubmitting = ref(false)
const shipForm = reactive({ company: '', trackingNo: '' })

const afterSaleId = computed(() => Number(route.params.id))

const voucherImages = computed(() => {
  if (!detail.value?.afterSale?.voucherImages) return []
  return detail.value.afterSale.voucherImages.split(',').filter(Boolean)
})

const canShipReturn = computed(() => {
  const s = detail.value?.afterSale?.status
  return s === 3 && !detail.value?.afterSale?.returnTrackingNo
})

const canCancel = computed(() => {
  const s = detail.value?.afterSale?.status
  return s === 0 || (s === 3 && !detail.value?.afterSale?.returnTrackingNo)
})

const canPaySupplement = computed(() => {
  return detail.value?.supplement?.status === 0
})

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

function timelineType(log) {
  if (log.action === 'REJECT' || log.action === 'CANCEL') return 'danger'
  if (log.action === 'APPROVE' || log.action === 'REFUND' || log.action === 'EXCHANGE_SHIP' || log.action === 'WAREHOUSE_CONFIRM') return 'success'
  return 'primary'
}

function operatorText(role) {
  return { user: '用户', admin: '管理员', system: '系统' }[role] || '系统'
}
function operatorTagType(role) {
  return { user: '', admin: 'danger', system: 'info' }[role] || 'info'
}

function openShipDialog() {
  shipForm.company = ''
  shipForm.trackingNo = ''
  shipVisible.value = true
}

async function submitShip() {
  if (!shipForm.company || !shipForm.trackingNo) {
    ElMessage.warning('请填写完整物流信息')
    return
  }
  shipSubmitting.value = true
  try {
    await api.post(`/after-sale/${afterSaleId.value}/ship-return`, shipForm)
    ElMessage.success('物流信息已提交')
    shipVisible.value = false
    load()
  } finally {
    shipSubmitting.value = false
  }
}

async function cancel() {
  try {
    await ElMessageBox.confirm('确定要取消该售后申请吗？', '确认取消', { type: 'warning' })
    await api.post(`/after-sale/${afterSaleId.value}/cancel`)
    ElMessage.success('已取消')
    load()
  } catch (e) {
    if (e !== 'cancel') {}
  }
}

async function paySupplement() {
  try {
    await ElMessageBox.confirm(`确认支付补款 ¥${detail.value.supplement.priceDiff}？`, '补款确认', { type: 'warning' })
    await api.post(`/after-sale/supplement/${detail.value.supplement.id}/pay`)
    ElMessage.success('补款支付成功（模拟）')
    load()
  } catch (e) {
    if (e !== 'cancel') {}
  }
}

async function load() {
  loading.value = true
  try {
    const res = await api.get(`/after-sale/${afterSaleId.value}`)
    detail.value = res.data.code === 200 ? res.data.data : null
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.after-sale-detail {
  padding-bottom: 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.main-card,
.timeline-card {
  padding: 28px;
}

.main-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--color-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.as-no {
  font-weight: 600;
  font-size: 1rem;
  color: var(--color-text);
}

.product-section {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.product-img {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  background: var(--color-bg-secondary);
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 1.0625rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.product-meta {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  margin-bottom: 4px;
}

.amount {
  color: var(--color-text);
  font-weight: 500;
}

.amount.refund {
  color: var(--color-primary);
  font-weight: 600;
}

.info-section {
  margin-top: 4px;
}

.info-title {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 12px;
  font-size: 1rem;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
}

.info-item {
  font-size: 0.9375rem;
}

.info-label {
  color: var(--color-text-muted);
}

.info-value {
  color: var(--color-text);
}

.info-value.danger {
  color: var(--color-danger);
  font-weight: 500;
}

.voucher-section {
  margin-top: 12px;
}

.voucher-list {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.voucher-img {
  width: 120px;
  height: 120px;
  border-radius: var(--radius-sm);
  cursor: zoom-in;
  background: var(--color-bg-secondary);
}

.supplement-section {
  background: var(--color-warning-light);
  padding: 16px 20px;
  border-radius: var(--radius-md);
  margin-top: 12px;
}

.actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: 12px;
}

.timeline-title {
  font-weight: 600;
  font-size: 1.0625rem;
  color: var(--color-text);
  margin-bottom: 20px;
}

.tl-item {
  padding: 4px 0;
}

.tl-header {
  margin-bottom: 4px;
}

.tl-remark {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}
</style>
