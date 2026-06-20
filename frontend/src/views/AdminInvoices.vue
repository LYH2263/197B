<template>
  <div class="admin-invoices">
    <h2 class="page-title">发票管理</h2>

    <div class="filter-bar content-card">
      <span class="filter-label">状态筛选：</span>
      <el-radio-group v-model="filterStatus" size="default" @change="loadList">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待开票</el-radio-button>
        <el-radio-button :value="1">已开票</el-radio-button>
        <el-radio-button :value="2">已驳回</el-radio-button>
      </el-radio-group>
      <el-button style="margin-left: auto;" @click="exportSelected" :disabled="selectedIds.length === 0">
        <el-icon><Download /></el-icon>
        导出选中
      </el-button>
      <el-button type="primary" @click="exportAll">
        <el-icon><Download /></el-icon>
        导出全部
      </el-button>
      <el-button @click="loadList">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="table-card content-card" v-loading="loading">
      <el-table :data="list" stripe style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="发票类型" width="80">
          <template #default="{ row }">
            {{ row.invoiceType === 'enterprise' ? '企业' : '个人' }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="抬头" min-width="150" show-overflow-tooltip />
        <el-table-column prop="taxNumber" label="税号" width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceNumber" label="发票号码" width="130">
          <template #default="{ row }">
            {{ row.invoiceNumber || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="receiveEmail" label="接收邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="adminName" label="审核人" width="100">
          <template #default="{ row }">
            {{ row.adminName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <template v-if="row.status === 0">
              <el-button link type="success" size="small" @click="openApprove(row)">开票</el-button>
              <el-button link type="danger" size="small" @click="openReject(row)">驳回</el-button>
            </template>
            <template v-else-if="row.status === 1">
              <el-button link type="warning" size="small" @click="openUpdateInvoiceNumber(row)">改发票号</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无发票申请" />
    </div>

    <el-dialog v-model="detailVisible" title="发票详情" width="720px" destroy-on-close>
      <div v-if="detail" class="detail-dialog">
        <div class="detail-header">
          <span>ID：{{ detail.id }}</span>
          <el-tag :type="statusType(detail.status)" effect="dark">
            {{ statusText(detail.status) }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="发票号码">{{ detail.invoiceNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发票类型">
            {{ detail.invoiceType === 'enterprise' ? '企业' : '个人' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">
              {{ statusText(detail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="抬头">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="税号">{{ detail.taxNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开户行">{{ detail.bankName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="银行账号">{{ detail.bankAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="企业地址">{{ detail.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="企业电话">{{ detail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收邮箱">{{ detail.receiveEmail }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ detail.adminName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ formatTime(detail.reviewedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="detail.rejectReason"
          :title="'驳回原因：' + detail.rejectReason"
          type="error"
          show-icon
          style="margin-top: 16px"
        />

        <h4 style="margin-top: 20px; margin-bottom: 12px;">订单明细</h4>
        <el-table :data="detail.orderItems || []" size="small" style="width: 100%">
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="totalAmount" label="小计" width="100">
            <template #default="{ row }">¥{{ row.totalAmount }}</template>
          </el-table-column>
        </el-table>
        <div style="text-align: right; margin-top: 12px; font-weight: 600;">
          订单总金额：¥{{ detail.orderAmount }}
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="detail && detail.status === 0">
          <el-button type="success" @click="openApprove(detail)">通过并录入发票号</el-button>
          <el-button type="danger" @click="openReject(detail)">驳回</el-button>
        </template>
        <template v-else-if="detail && detail.status === 1">
          <el-button type="warning" @click="openUpdateInvoiceNumber(detail)">修改发票号</el-button>
        </template>
      </template>
    </el-dialog>

    <el-dialog v-model="approveVisible" title="审核通过" width="400px" @close="approveForm.invoiceNumber = ''">
      <el-form :model="approveForm" label-width="80px" ref="approveFormRef">
        <el-form-item label="发票号码" required>
          <el-input v-model="approveForm.invoiceNumber" placeholder="请输入发票号码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="approveLoading" @click="submitApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="审核驳回" width="400px" @close="rejectForm.rejectReason = ''">
      <el-form :model="rejectForm" label-width="80px" ref="rejectFormRef">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectLoading" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invoiceNumberVisible" title="修改发票号码" width="400px" @close="invoiceNumberForm.invoiceNumber = ''">
      <el-form :model="invoiceNumberForm" label-width="80px" ref="invoiceNumberFormRef">
        <el-form-item label="发票号码" required>
          <el-input v-model="invoiceNumberForm.invoiceNumber" placeholder="请输入新的发票号码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invoiceNumberVisible = false">取消</el-button>
        <el-button type="primary" :loading="invoiceNumberLoading" @click="submitUpdateInvoiceNumber">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Download } from '@element-plus/icons-vue'
import { invoiceApi } from '../api'

const loading = ref(false)
const list = ref([])
const filterStatus = ref(null)
const selectedIds = ref([])

const detailVisible = ref(false)
const detail = ref(null)

const approveVisible = ref(false)
const approveLoading = ref(false)
const approveFormRef = ref(null)
const approveForm = reactive({ invoiceNumber: '' })
const currentApproveId = ref(null)

const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({ rejectReason: '' })
const currentRejectId = ref(null)

const invoiceNumberVisible = ref(false)
const invoiceNumberLoading = ref(false)
const invoiceNumberFormRef = ref(null)
const invoiceNumberForm = reactive({ invoiceNumber: '' })
const currentInvoiceNumberId = ref(null)

function statusText(s) {
  return { 0: '待开票', 1: '已开票', 2: '已驳回' }[s] || '未知'
}
function statusType(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info'
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadList() {
  loading.value = true
  try {
    const res = await invoiceApi.adminList(filterStatus.value)
    if (res.data.code === 200) {
      list.value = res.data.data || []
    }
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(val) {
  selectedIds.value = val.map((item) => item.id)
}

async function viewDetail(row) {
  try {
    const res = await invoiceApi.adminGetDetail(row.id)
    if (res.data.code === 200) {
      detail.value = res.data.data
      detailVisible.value = true
    }
  } catch (e) {}
}

function openApprove(row) {
  currentApproveId.value = row.id
  approveForm.invoiceNumber = row.invoiceNumber || ''
  approveVisible.value = true
}

async function submitApprove() {
  if (!approveForm.invoiceNumber?.trim()) {
    ElMessage.warning('请输入发票号码')
    return
  }
  approveLoading.value = true
  try {
    await invoiceApi.adminApprove(currentApproveId.value, approveForm.invoiceNumber)
    ElMessage.success('审核通过')
    approveVisible.value = false
    detailVisible.value = false
    loadList()
  } finally {
    approveLoading.value = false
  }
}

function openReject(row) {
  currentRejectId.value = row.id
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectForm.rejectReason?.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  rejectLoading.value = true
  try {
    await invoiceApi.adminReject(currentRejectId.value, rejectForm.rejectReason)
    ElMessage.success('已驳回')
    rejectVisible.value = false
    detailVisible.value = false
    loadList()
  } finally {
    rejectLoading.value = false
  }
}

function openUpdateInvoiceNumber(row) {
  currentInvoiceNumberId.value = row.id
  invoiceNumberForm.invoiceNumber = row.invoiceNumber || ''
  invoiceNumberVisible.value = true
}

async function submitUpdateInvoiceNumber() {
  if (!invoiceNumberForm.invoiceNumber?.trim()) {
    ElMessage.warning('请输入发票号码')
    return
  }
  invoiceNumberLoading.value = true
  try {
    await invoiceApi.adminUpdateInvoiceNumber(currentInvoiceNumberId.value, invoiceNumberForm.invoiceNumber)
    ElMessage.success('发票号码已更新')
    invoiceNumberVisible.value = false
    detailVisible.value = false
    loadList()
  } finally {
    invoiceNumberLoading.value = false
  }
}

async function exportSelected() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要导出的记录')
    return
  }
  try {
    const res = await invoiceApi.adminExport(selectedIds.value)
    downloadCsv(res.data, `invoices_selected_${Date.now()}.csv`)
    ElMessage.success('导出成功')
  } catch (e) {}
}

async function exportAll() {
  try {
    const res = await invoiceApi.adminExport(null)
    downloadCsv(res.data, `invoices_all_${Date.now()}.csv`)
    ElMessage.success('导出成功')
  } catch (e) {}
}

function downloadCsv(blob, filename) {
  const url = window.URL.createObjectURL(new Blob([blob], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

onMounted(loadList)
</script>

<style scoped>
.admin-invoices {
  padding: 24px;
}
.page-title {
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
}
.filter-label {
  font-weight: 500;
}
.table-card {
  padding: 20px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}
.detail-desc {
  margin-top: 16px 0;
}
</style>
