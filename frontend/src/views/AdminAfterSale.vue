<template>
  <div class="admin-after-sale">
    <h2 class="page-title">售后审核管理</h2>

    <div class="filter-bar content-card">
      <span class="filter-label">状态筛选：</span>
      <el-radio-group v-model="filterStatus" size="default" @change="loadList">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">已通过</el-radio-button>
        <el-radio-button :value="2">已拒绝</el-radio-button>
        <el-radio-button :value="3">待寄回</el-radio-button>
        <el-radio-button :value="4">仓库确认</el-radio-button>
        <el-radio-button :value="5">已退款</el-radio-button>
        <el-radio-button :value="6">已换货发出</el-radio-button>
        <el-radio-button :value="7">已取消</el-radio-button>
      </el-radio-group>
      <el-button style="margin-left: auto;" @click="loadList">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="table-card content-card" v-loading="loading">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="afterSaleNo" label="售后单号" width="190" />
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
                <div class="cell-meta">
                  <el-tag :type="typeType(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="实付/退款" width="140">
          <template #default="{ row }">
            <div>实付：¥{{ row.originalTotalAmount }}</div>
            <div v-if="row.refundAmount != null" style="color: var(--color-primary); font-weight: 600;">
              退款：¥{{ row.refundAmount }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <template v-if="row.status === 0">
              <el-button link type="success" size="small" @click="openApprove(row)">通过</el-button>
              <el-button link type="danger" size="small" @click="openReject(row)">拒绝</el-button>
            </template>
            <template v-else-if="row.status === 3 && row.returnTrackingNo">
              <el-button link type="primary" size="small" @click="warehouseConfirm(row)">仓库确认</el-button>
            </template>
            <template v-else-if="row.status === 4">
              <el-button link type="warning" size="small" @click="openShip(row)">换货发出</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无售后申请" />
    </div>

    <el-dialog v-model="detailVisible" title="售后详情" width="720px" destroy-on-close>
      <div v-if="detail" class="detail-dialog">
        <div class="detail-header">
          <span>单号：{{ detail.afterSale.afterSaleNo }}</span>
          <el-tag :type="typeType(detail.afterSale.type)">{{ typeText(detail.afterSale.type) }}</el-tag>
          <el-tag :type="statusType(detail.afterSale.status)" effect="dark">
            {{ statusText(detail.afterSale.status) }}
          </el-tag>
        </div>

        <div class="detail-product">
          <el-image
            v-if="detail.afterSale.productImage"
            :src="detail.afterSale.productImage"
            fit="cover"
            class="dp-img"
          />
          <div>
            <div class="dp-name">{{ detail.afterSale.productName }}</div>
            <div>¥{{ detail.afterSale.originalPrice }} × {{ detail.afterSale.originalQuantity }}
              = <strong>¥{{ detail.afterSale.originalTotalAmount }}</strong></div>
          </div>
        </div>

        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="售后原因">{{ detail.afterSale.reason }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detail.afterSale.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">
            {{ detail.afterSale.description || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="拒绝原因" v-if="detail.afterSale.rejectReason" :span="2">
            <span style="color: var(--color-danger);">{{ detail.afterSale.rejectReason }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="退款金额" v-if="detail.afterSale.refundAmount != null">
            <span style="color: var(--color-primary); font-weight: 600;">
              ¥{{ detail.afterSale.refundAmount }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="退货物流" v-if="detail.afterSale.returnCompany">
            {{ detail.afterSale.returnCompany }} {{ detail.afterSale.returnTrackingNo }}
          </el-descriptions-item>
          <el-descriptions-item label="换货物流" v-if="detail.afterSale.exchangeCompany">
            {{ detail.afterSale.exchangeCompany }} {{ detail.afterSale.exchangeTrackingNo }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="voucherImages.length" class="voucher-block">
          <div class="voucher-title">凭证图片：</div>
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

        <div class="timeline-block">
          <div class="tl-title">进度时间线</div>
          <el-timeline>
            <el-timeline-item
              v-for="log in detail.logs"
              :key="log.id"
              :timestamp="log.createdAt"
              :type="log.action === 'REJECT' || log.action === 'CANCEL' ? 'danger' : log.action === 'CREATE' ? 'primary' : 'success'"
            >
              <div>
                <el-tag size="small" :type="log.operatorRole === 'admin' ? 'danger' : log.operatorRole === 'system' ? 'info' : ''">
                  {{ { user: '用户', admin: '管理员', system: '系统' }[log.operatorRole] }}
                </el-tag>
                <strong v-if="log.operatorName" style="margin-left: 4px;">{{ log.operatorName }}</strong>
                <div style="margin-top: 4px; color: var(--color-text-secondary);">{{ log.remark }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="approveVisible" title="审核通过" width="480px" destroy-on-close @close="approveForm = {}">
      <el-form :model="approveForm" label-width="110px">
        <template v-if="currentRow && (currentRow.type === 1 || currentRow.type === 3)">
          <el-form-item label="退款金额" required>
            <el-input-number
              v-model="approveForm.refundAmount"
              :min="0.01"
              :max="currentRow ? Number(currentRow.originalTotalAmount) : 999999"
              :precision="2"
              :step="1"
              style="width: 220px;"
            />
            <span style="margin-left: 8px; color: var(--color-text-muted);">
              实付 ¥{{ currentRow?.originalTotalAmount }}（可部分退款）
            </span>
          </el-form-item>
        </template>
        <template v-else>
          <el-alert type="info" :closable="false" show-icon title="换货审核通过后，等待用户寄回商品。" />
        </template>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="审核拒绝" width="460px" destroy-on-close @close="rejectForm = {}">
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请填写拒绝原因"
            maxlength="256"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="换货发出" width="520px" destroy-on-close @close="shipForm = {}">
      <el-form :model="shipForm" label-width="110px">
        <el-form-item label="快递公司" required>
          <el-input v-model="shipForm.company" placeholder="如：顺丰速运" />
        </el-form-item>
        <el-form-item label="运单号" required>
          <el-input v-model="shipForm.trackingNo" placeholder="物流单号" />
        </el-form-item>
        <el-form-item label="差价补款">
          <el-input-number
            v-model="shipForm.priceDiff"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 200px;"
          />
          <span style="margin-left: 8px; color: var(--color-text-muted);">
            元（0 或空表示无差价）
          </span>
        </el-form-item>
        <el-alert v-if="shipForm.priceDiff > 0" type="warning" :closable="false" show-icon
          title="有差价将生成补款单，用户需完成补款后流程才会标记为已发出。" />
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitShip">确认发出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import api from '../api'

const loading = ref(false)
const list = ref([])
const filterStatus = ref(null)

const detailVisible = ref(false)
const detail = ref(null)

const approveVisible = ref(false)
const rejectVisible = ref(false)
const shipVisible = ref(false)
const currentRow = ref(null)
const submitting = ref(false)

const approveForm = reactive({ refundAmount: null })
const rejectForm = reactive({ rejectReason: '' })
const shipForm = reactive({ company: '', trackingNo: '', priceDiff: 0 })

const voucherImages = computed(() => {
  if (!detail.value?.afterSale?.voucherImages) return []
  return detail.value.afterSale.voucherImages.split(',').filter(Boolean)
})

function typeText(t) { return { 1: '退货退款', 2: '换货', 3: '仅退款' }[t] || '未知' }
function typeType(t) { return { 1: 'warning', 2: 'primary', 3: 'success' }[t] || 'info' }
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

async function loadList() {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value != null) params.status = filterStatus.value
    const res = await api.get('/admin/after-sale', { params })
    list.value = res.data.code === 200 ? res.data.data || [] : []
  } finally {
    loading.value = false
  }
}

async function viewDetail(row) {
  try {
    const res = await api.get(`/admin/after-sale/${row.id}`)
    if (res.data.code === 200) {
      detail.value = res.data.data
      detailVisible.value = true
    }
  } catch {}
}

function openApprove(row) {
  currentRow.value = row
  approveForm.refundAmount = Number(row.originalTotalAmount)
  approveVisible.value = true
}

function openReject(row) {
  currentRow.value = row
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

function openShip(row) {
  currentRow.value = row
  shipForm.company = ''
  shipForm.trackingNo = ''
  shipForm.priceDiff = 0
  shipVisible.value = true
}

async function submitApprove() {
  if (!currentRow.value) return
  if ((currentRow.value.type === 1 || currentRow.value.type === 3) &&
      (approveForm.refundAmount == null || approveForm.refundAmount <= 0)) {
    ElMessage.warning('请填写退款金额')
    return
  }
  submitting.value = true
  try {
    await api.post(`/admin/after-sale/${currentRow.value.id}/approve`, {
      refundAmount: approveForm.refundAmount,
    })
    ElMessage.success('审核通过')
    approveVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function submitReject() {
  if (!rejectForm.rejectReason.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  submitting.value = true
  try {
    await api.post(`/admin/after-sale/${currentRow.value.id}/reject`, {
      rejectReason: rejectForm.rejectReason,
    })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function warehouseConfirm(row) {
  try {
    await ElMessageBox.confirm('确认仓库已收到用户寄回的商品？', '仓库确认', { type: 'warning' })
    await api.post(`/admin/after-sale/${row.id}/warehouse-confirm`)
    ElMessage.success('仓库确认成功')
    loadList()
  } catch (e) { if (e !== 'cancel') {} }
}

async function submitShip() {
  if (!shipForm.company.trim() || !shipForm.trackingNo.trim()) {
    ElMessage.warning('请填写完整物流信息')
    return
  }
  submitting.value = true
  try {
    await api.post(`/admin/after-sale/${currentRow.value.id}/ship-exchange`, {
      company: shipForm.company,
      trackingNo: shipForm.trackingNo,
      priceDiff: shipForm.priceDiff > 0 ? shipForm.priceDiff : null,
    })
    ElMessage.success(shipForm.priceDiff > 0 ? '已生成补款单，等待用户补款' : '换货已发出')
    shipVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

loadList()
</script>

<style scoped>
.admin-after-sale {
  padding-bottom: 32px;
}

.filter-bar {
  padding: 16px 20px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-weight: 500;
  color: var(--color-text-secondary);
}

.table-card {
  padding: 0;
  overflow: hidden;
}

:deep(.el-table .cell) {
  padding-top: 10px;
  padding-bottom: 10px;
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
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-meta {
  display: flex;
  align-items: center;
}

.detail-dialog .detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
  font-weight: 600;
}

.detail-product {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
}

.dp-img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  background: #fff;
}

.dp-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.detail-desc {
  margin-bottom: 16px;
}

.voucher-block {
  margin-bottom: 16px;
}

.voucher-title {
  margin-bottom: 8px;
  font-weight: 500;
}

.voucher-list {
  display: flex;
  gap: 10px;
}

.voucher-img {
  width: 100px;
  height: 100px;
  border-radius: 6px;
  background: var(--color-bg-secondary);
}

.timeline-block {
  border-top: 1px dashed var(--color-border);
  padding-top: 16px;
}

.tl-title {
  font-weight: 600;
  margin-bottom: 10px;
}
</style>
