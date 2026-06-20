<template>
  <div class="my-invoices content-card" v-loading="loading">
    <h3>我的发票</h3>
    <el-table :data="invoices" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column label="发票类型" width="100">
        <template #default="{ row }">
          {{ row.invoiceType === 'enterprise' ? '企业' : '个人' }}
        </template>
      </el-table-column>
      <el-table-column prop="title" label="抬头" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="invoiceNumber" label="发票号码" width="140">
        <template #default="{ row }">
          {{ row.invoiceNumber || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="receiveEmail" label="接收邮箱" width="180" />
      <el-table-column prop="createdAt" label="申请时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 2"
            type="primary"
            link
            size="small"
            @click="editInvoice(row.id)"
          >
            修改重提
          </el-button>
          <el-button
            v-if="row.status === 1"
            type="success"
            link
            size="small"
            @click="downloadPdf(row.id)"
          >
            下载PDF
          </el-button>
          <el-button
            type="info"
            link
            size="small"
            @click="viewDetail(row.id)"
          >
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="invoices.length === 0 && !loading" description="暂无发票申请记录" />

    <el-dialog v-model="detailVisible" title="发票详情" width="600px">
      <div v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="发票号码">{{ currentDetail.invoiceNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发票类型">
            {{ currentDetail.invoiceType === 'enterprise' ? '企业' : '个人' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentDetail.status)" size="small">
              {{ statusText(currentDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="抬头">{{ currentDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="税号">{{ currentDetail.taxNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开户行">{{ currentDetail.bankName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="银行账号">{{ currentDetail.bankAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="企业地址">{{ currentDetail.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="企业电话">{{ currentDetail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收邮箱">{{ currentDetail.receiveEmail }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(currentDetail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ currentDetail.adminName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ formatTime(currentDetail.reviewedAt) }}</el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-if="currentDetail.rejectReason"
          :title="'驳回原因：' + currentDetail.rejectReason"
          type="error"
          show-icon
          style="margin-top: 16px"
        />

        <h4 style="margin-top: 24px;">订单明细</h4>
        <el-table :data="currentDetail.orderItems || []" size="small" style="width: 100%">
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
          订单总金额：¥{{ currentDetail.orderAmount }}
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentDetail && currentDetail.status === 1"
          type="primary"
          @click="downloadPdf(currentDetail.id)"
        >
          下载PDF
        </el-button>
        <el-button
          v-if="currentDetail && currentDetail.status === 2"
          type="primary"
          @click="editInvoice(currentDetail.id)"
        >
          修改重提
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { invoiceApi } from '../api'
import { generateInvoicePdf } from '../utils/invoicePdf'

const router = useRouter()
const loading = ref(true)
const invoices = ref([])
const detailVisible = ref(false)
const currentDetail = ref(null)

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

async function load() {
  loading.value = true
  try {
    const res = await invoiceApi.list()
    if (res.data.code === 200) {
      invoices.value = res.data.data || []
    }
  } finally {
    loading.value = false
  }
}

async function viewDetail(id) {
  try {
    const res = await invoiceApi.getDetail(id)
    if (res.data.code === 200) {
      currentDetail.value = res.data.data
      detailVisible.value = true
    }
  } catch (e) {}
}

async function downloadPdf(id) {
  try {
    const res = await invoiceApi.getDetail(id)
    if (res.data.code === 200) {
      generateInvoicePdf(res.data.data)
      ElMessage.success('PDF下载成功')
    }
  } catch (e) {}
}

function editInvoice(id) {
  detailVisible.value = false
  router.push(`/invoices/${id}/edit`)
}

onMounted(load)
</script>

<style scoped>
.my-invoices {
  max-width: 1200px;
  padding: 28px;
}
.my-invoices h3 {
  margin-bottom: 24px;
}
</style>
