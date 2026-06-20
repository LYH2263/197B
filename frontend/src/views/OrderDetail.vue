<template>
  <div v-loading="loading" class="order-detail">
    <template v-if="order">
      <div class="main-card content-card">
        <div class="main-card-header">
          <span>订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small">{{ statusText(order.status) }}</el-tag>
        </div>
        <p><strong>收货人：</strong>{{ order.receiverName }} {{ order.receiverPhone }}</p>
        <p><strong>收货地址：</strong>{{ order.receiverAddress }}</p>
        <p><strong>订单金额：</strong>¥ {{ order.totalAmount }}</p>
        <p v-if="order.discountAmount > 0"><strong>优惠券抵扣：</strong>-¥ {{ order.discountAmount }}</p>
        <p v-if="order.pointsDiscount > 0"><strong>积分抵扣：</strong>-¥ {{ order.pointsDiscount }}（使用 {{ order.pointsUsed }} 积分）</p>
        <p v-if="order.pointsEarned > 0"><strong>获得积分：</strong>+{{ order.pointsEarned }} 积分</p>
        <p><strong>下单时间：</strong>{{ order.createdAt }}</p>
      </div>

      <div v-if="shipmentInfo" class="shipment-card content-card">
        <div class="shipment-header">
          <div>
            <h4 style="margin: 0 0 8px 0;">物流信息</h4>
            <p class="shipment-company">
              <strong>{{ shipmentInfo.shipment.expressCompanyName }}</strong>
              <span class="tracking-no">运单号：{{ shipmentInfo.shipment.trackingNo }}</span>
            </p>
            <p v-if="shipmentInfo.shipment.shippedAt" class="shipment-time">
              发货时间：{{ formatTime(shipmentInfo.shipment.shippedAt) }}
            </p>
          </div>
          <div class="shipment-actions">
            <el-button
              v-if="shipmentInfo.canUrgeToday"
              type="warning"
              :loading="urging"
              @click="urgeDelivery"
            >
              催物流
            </el-button>
            <el-tooltip v-else-if="order.status >= 2 && order.status < 3" content="今日已催单，请耐心等待" placement="top">
              <el-button type="warning" disabled>催物流</el-button>
            </el-tooltip>
            <el-button
              v-if="shipmentInfo.canReportIssue"
              type="danger"
              @click="openIssueDialog"
            >
              异常签收
            </el-button>
          </div>
        </div>

        <el-divider />

        <div class="timeline-section">
          <h5 style="margin: 0 0 16px 0;">物流轨迹</h5>
          <el-timeline>
            <el-timeline-item
              v-for="(trace, idx) in shipmentInfo.traces"
              :key="trace.id || idx"
              :timestamp="formatTime(trace.traceTime)"
              :type="idx === 0 ? 'primary' : ''"
              :hollow="idx !== 0"
              placement="top"
            >
              <div class="trace-item">
                <div class="trace-status" :class="'status-' + trace.status">
                  {{ traceStatusText(trace.status) }}
                </div>
                <div class="trace-desc">{{ trace.description }}</div>
                <div v-if="trace.location" class="trace-location">
                  <el-icon><Location /></el-icon> {{ trace.location }}
                </div>
                <div v-if="trace.operator" class="trace-operator">
                  快递员：{{ trace.operator }}
                  <span v-if="trace.operatorPhone">（{{ trace.operatorPhone }}）</span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div v-if="shipmentInfo.urges && shipmentInfo.urges.length > 0" class="urge-section">
          <el-divider />
          <h5 style="margin: 0 0 12px 0;">催单记录</h5>
          <el-table :data="shipmentInfo.urges" size="small" style="width: 100%">
            <el-table-column prop="urgeDate" label="催单日期" width="140" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column label="处理状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.handled === 1 ? 'success' : 'warning'" size="small">
                  {{ row.handled === 1 ? '已处理' : '待处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="处理时间" width="180">
              <template #default="{ row }">
                {{ row.handledAt ? formatTime(row.handledAt) : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="main-card content-card" style="margin-top: 24px;">
        <h4>商品明细</h4>
        <el-table :data="items" style="width: 100%">
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="price" label="单价" width="120">
            <template #default="{ row }">¥ {{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="totalAmount" label="小计" width="120">
            <template #default="{ row }">¥ {{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="评价/售后" width="160">
            <template #default="{ row }">
              <div class="col-actions">
                <el-button
                  v-if="canReview(order.status) && !hasReview(row.productId)"
                  type="primary"
                  link
                  size="small"
                  @click="openReview(row)"
                >
                  评价
                </el-button>
                <span v-else-if="hasReview(row.productId)" style="color: var(--color-text-muted); font-size: 0.875rem;">已评价</span>
                <el-button
                  v-if="canAfterSale(order.status) && !hasAfterSale(row.id)"
                  type="warning"
                  link
                  size="small"
                  @click="applyAfterSale(row)"
                >
                  申请售后
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="invoice-section" v-if="order.status === 3">
          <el-divider />
          <div class="invoice-info">
            <div style="display: flex; align-items: center; justify-content: space-between;">
              <div>
                <h4 style="margin: 0 0 8px 0;">发票信息</h4>
                <div v-if="invoice">
                  <el-tag :type="invoiceStatusType(invoice.status)" size="small" style="margin-right: 8px;">
                    {{ invoiceStatusText(invoice.status) }}
                  </el-tag>
                  <span v-if="invoice.invoiceNumber" style="margin-right: 8px;">发票号：{{ invoice.invoiceNumber }}</span>
                  <span>抬头：{{ invoice.title }}</span>
                </div>
                <div v-else style="color: var(--color-text-muted);">
                  您可以为该订单申请开具发票
                </div>
              </div>
              <div class="invoice-actions">
                <template v-if="!invoice">
                  <el-button type="primary" @click="applyInvoice">申请发票</el-button>
                </template>
                <template v-else>
                  <el-button
                    v-if="invoice.status === 2"
                    type="primary"
                    @click="editInvoice(invoice.id)"
                  >
                    修改重提
                  </el-button>
                  <el-button
                    v-if="invoice.status === 1"
                    type="success"
                    @click="downloadInvoice(invoice.id)"
                  >
                    下载PDF
                  </el-button>
                  <el-button type="info" @click="viewInvoiceDetail(invoice.id)">查看详情</el-button>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div class="actions">
          <template v-if="order.status === 0">
            <el-button type="primary" @click="pay">去支付</el-button>
            <el-button @click="cancel">取消订单</el-button>
          </template>
          <template v-if="order.status === 2">
            <el-button type="success" @click="confirm">确认收货</el-button>
          </template>
        </div>
      </div>

      <el-dialog v-model="reviewVisible" title="商品评价" width="400px" @close="reviewForm = {}">
        <el-form :model="reviewForm" label-width="80px">
          <el-form-item label="评分" required>
            <el-rate v-model="reviewForm.rating" />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input v-model="reviewForm.content" type="textarea" :rows="3" placeholder="选填" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="issueVisible" title="异常签收反馈" width="500px" @close="resetIssueForm">
        <el-form :model="issueForm" label-width="100px" ref="issueFormRef" :rules="issueRules">
          <el-form-item label="问题类型" prop="issueType">
            <el-select v-model="issueForm.issueType" placeholder="请选择问题类型" style="width: 100%">
              <el-option label="商品破损" value="damaged" />
              <el-option label="错发商品" value="wrong" />
              <el-option label="丢件/少件" value="missing" />
              <el-option label="其他问题" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="问题描述" prop="description">
            <el-input
              v-model="issueForm.description"
              type="textarea"
              :rows="4"
              placeholder="请详细描述遇到的问题..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="照片凭证">
            <el-upload
              v-model:file-list="issueForm.fileList"
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :on-preview="handlePreview"
              :limit="6"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div style="font-size: 12px; color: var(--color-text-muted); margin-top: 4px;">
              最多上传6张图片，支持 jpg/png 格式
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="issueVisible = false">取消</el-button>
          <el-button type="primary" :loading="issueSubmitting" @click="submitIssue">提交</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="previewVisible" title="图片预览" width="600px">
        <img :src="previewUrl" style="width: 100%; height: auto; display: block;" />
      </el-dialog>

      <el-dialog v-model="invoiceDetailVisible" title="发票详情" width="600px">
        <div v-if="currentInvoiceDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ currentInvoiceDetail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="发票号码">{{ currentInvoiceDetail.invoiceNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发票类型">
              {{ currentInvoiceDetail.invoiceType === 'enterprise' ? '企业' : '个人' }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="invoiceStatusType(currentInvoiceDetail.status)" size="small">
                {{ invoiceStatusText(currentInvoiceDetail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="抬头">{{ currentInvoiceDetail.title }}</el-descriptions-item>
            <el-descriptions-item label="税号">{{ currentInvoiceDetail.taxNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="接收邮箱">{{ currentInvoiceDetail.receiveEmail }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ formatTime(currentInvoiceDetail.createdAt) }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="currentInvoiceDetail.rejectReason"
            :title="'驳回原因：' + currentInvoiceDetail.rejectReason"
            type="error"
            show-icon
            style="margin-top: 16px"
          />
          <h4 style="margin-top: 20px; margin-bottom: 12px;">订单明细</h4>
          <el-table :data="currentInvoiceDetail.orderItems || []" size="small" style="width: 100%">
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
            订单总金额：¥{{ currentInvoiceDetail.orderAmount }}
          </div>
        </div>
        <template #footer>
          <el-button @click="invoiceDetailVisible = false">关闭</el-button>
          <el-button
            v-if="currentInvoiceDetail && currentInvoiceDetail.status === 1"
            type="primary"
            @click="downloadInvoice(currentInvoiceDetail.id)"
          >
            下载PDF
          </el-button>
          <el-button
            v-if="currentInvoiceDetail && currentInvoiceDetail.status === 2"
            type="primary"
            @click="editInvoice(currentInvoiceDetail.id)"
          >
            修改重提
          </el-button>
        </template>
      </el-dialog>
    </template>
    <el-empty v-else-if="!loading" description="订单不存在" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Plus } from '@element-plus/icons-vue'
import api, { invoiceApi } from '../api'
import { useUserStore } from '../stores/user'
import { generateInvoicePdf } from '../utils/invoicePdf'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const order = ref(null)
const items = ref([])
const productReviews = ref(new Set())
const afterSaleItemIds = ref(new Set())
const reviewVisible = ref(false)
const reviewForm = reactive({ orderId: null, productId: null, rating: 5, content: '' })
const reviewSubmitting = ref(false)

const shipmentInfo = ref(null)
const urging = ref(false)

const issueVisible = ref(false)
const issueFormRef = ref(null)
const issueForm = reactive({
  shipmentId: null,
  issueType: '',
  description: '',
  fileList: [],
})
const issueRules = {
  issueType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
  description: [{ required: true, message: '请描述问题', trigger: 'blur' }],
}
const issueSubmitting = ref(false)

const previewVisible = ref(false)
const previewUrl = ref('')

const invoice = ref(null)
const invoiceDetailVisible = ref(false)
const currentInvoiceDetail = ref(null)

const orderId = computed(() => Number(route.params.id))

function statusText(s) {
  return { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' }[s] || '未知'
}
function statusType(s) {
  return { 0: 'warning', 1: 'primary', 2: 'info', 3: 'success', 4: 'info' }[s] || 'info'
}
function canReview(status) {
  return status >= 1 && status <= 3
}
function canAfterSale(status) {
  return status === 3
}

function invoiceStatusText(s) {
  return { 0: '待开票', 1: '已开票', 2: '已驳回' }[s] || '未知'
}
function invoiceStatusType(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info'
}

function traceStatusText(s) {
  return { 0: '已发货', 1: '已揽收', 2: '运输中', 3: '派送中', 4: '已签收', 5: '异常签收' }[s] || '未知'
}

function hasReview(productId) {
  return productReviews.value.has(productId)
}

function hasAfterSale(orderItemId) {
  return afterSaleItemIds.value.has(orderItemId)
}

function applyAfterSale(row) {
  router.push(`/after-sale/apply/${row.id}`)
}

function openReview(row) {
  reviewForm.orderId = orderId.value
  reviewForm.productId = row.productId
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

async function submitReview() {
  reviewSubmitting.value = true
  try {
    await api.post('/reviews', {
      orderId: reviewForm.orderId,
      productId: reviewForm.productId,
      rating: reviewForm.rating,
      content: reviewForm.content,
    })
    ElMessage.success('评价成功')
    productReviews.value.add(reviewForm.productId)
    reviewVisible.value = false
  } finally {
    reviewSubmitting.value = false
  }
}

async function pay() {
  try {
    await api.post(`/orders/${orderId.value}/pay`)
    ElMessage.success('支付成功（模拟）')
    load()
  } catch (e) {}
}

async function confirm() {
  try {
    await ElMessageBox.confirm('确认已收到商品？确认后将发放积分', '提示', { type: 'warning' })
    await api.post(`/orders/${orderId.value}/confirm`)
    ElMessage.success('已确认收货，积分已发放')
    await userStore.fetchUser()
    load()
  } catch (e) {
    if (e !== 'cancel') {}
  }
}

async function cancel() {
  try {
    await api.post(`/orders/${orderId.value}/cancel`)
    ElMessage.success('已取消')
    load()
  } catch (e) {}
}

async function urgeDelivery() {
  if (!shipmentInfo.value) return
  try {
    urging.value = true
    await api.post('/shipments/urge', {
      shipmentId: shipmentInfo.value.shipment.id,
      remark: '',
    })
    ElMessage.success('催单成功，请耐心等待处理')
    loadShipment()
  } finally {
    urging.value = false
  }
}

function openIssueDialog() {
  if (!shipmentInfo.value) return
  issueForm.shipmentId = shipmentInfo.value.shipment.id
  issueForm.issueType = ''
  issueForm.description = ''
  issueForm.fileList = []
  issueVisible.value = true
}

function resetIssueForm() {
  issueForm.shipmentId = null
  issueForm.issueType = ''
  issueForm.description = ''
  issueForm.fileList = []
}

function handlePreview(file) {
  previewUrl.value = file.url
  previewVisible.value = true
}

async function submitIssue() {
  if (!issueFormRef.value) return
  await issueFormRef.value.validate(async (valid) => {
    if (!valid) return
    issueSubmitting.value = true
    try {
      const photoUrls = issueForm.fileList
        .map((f) => f.url)
        .filter(Boolean)
        .join(',')
      await api.post('/shipments/issue', {
        shipmentId: issueForm.shipmentId,
        issueType: issueForm.issueType,
        description: issueForm.description,
        photos: photoUrls,
      })
      ElMessage.success('已提交异常签收，管理员将尽快处理')
      issueVisible.value = false
      loadShipment()
    } finally {
      issueSubmitting.value = false
    }
  })
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function applyInvoice() {
  router.push({ name: 'InvoiceApply', query: { orderId: orderId.value } })
}

function editInvoice(id) {
  router.push({ name: 'InvoiceEdit', params: { id } })
}

async function loadInvoice() {
  if (!order.value || order.value.status !== 3) {
    invoice.value = null
    return
  }
  try {
    const res = await invoiceApi.getByOrderId(orderId.value)
    if (res.data.code === 200) {
      invoice.value = res.data.data
    }
  } catch (e) {
    invoice.value = null
  }
}

async function viewInvoiceDetail(id) {
  try {
    const res = await invoiceApi.getDetail(id)
    if (res.data.code === 200) {
      currentInvoiceDetail.value = res.data.data
      invoiceDetailVisible.value = true
    }
  } catch (e) {}
}

async function downloadInvoice(id) {
  try {
    const res = await invoiceApi.getDetail(id)
    if (res.data.code === 200) {
      generateInvoicePdf(res.data.data)
      ElMessage.success('PDF下载成功')
    }
  } catch (e) {}
}

async function loadShipment() {
  if (!order.value || order.value.status < 2) {
    shipmentInfo.value = null
    return
  }
  try {
    const res = await api.get(`/shipments/order/${orderId.value}`)
    if (res.data.code === 200) {
      shipmentInfo.value = res.data.data
    } else {
      shipmentInfo.value = null
    }
  } catch (e) {
    shipmentInfo.value = null
  }
}

async function load() {
  loading.value = true
  try {
    const [oRes, iRes, rRes, aRes] = await Promise.all([
      api.get(`/orders/${orderId.value}`),
      api.get(`/orders/${orderId.value}/items`),
      api.get('/reviews/me').catch(() => ({ data: { code: 401, data: [] } })),
      api.get('/after-sale').catch(() => ({ data: { code: 401, data: [] } })),
    ])
    if (oRes.data.code === 200) order.value = oRes.data.data
    if (iRes.data.code === 200) items.value = iRes.data.data || []
    const myReviews = (rRes?.data?.code === 200 ? rRes.data.data : []) || []
    productReviews.value = new Set(
      myReviews.filter((r) => Number(r.orderId) === orderId.value).map((r) => r.productId)
    )
    const myAfterSales = (aRes?.data?.code === 200 ? aRes.data.data : []) || []
    afterSaleItemIds.value = new Set(
      myAfterSales
        .filter((a) => Number(a.orderId) === orderId.value && a.status !== 2 && a.status !== 7)
        .map((a) => a.orderItemId)
    )
    await loadShipment()
    await loadInvoice()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-detail {
  padding-bottom: 32px;
}

.main-card {
  max-width: 800px;
  padding: 28px;
}

.main-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
  font-weight: 600;
  color: var(--color-text);
}

.order-detail .actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.col-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.shipment-card {
  max-width: 800px;
  padding: 28px;
  margin-top: 24px;
  background: linear-gradient(135deg, #f8faff 0%, #fff 100%);
  border-left: 4px solid var(--el-color-primary);
}

.shipment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.shipment-company {
  margin: 0;
  color: var(--color-text);
}

.tracking-no {
  margin-left: 16px;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  font-weight: normal;
}

.shipment-time {
  margin: 4px 0 0 0;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.shipment-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.timeline-section {
  padding-top: 4px;
}

.trace-item {
  line-height: 1.6;
}

.trace-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: 600;
  margin-bottom: 6px;
}

.trace-status.status-0 {
  background: #e6f4ff;
  color: #1677ff;
}
.trace-status.status-1 {
  background: #e6fffb;
  color: #13c2c2;
}
.trace-status.status-2 {
  background: #f0f5ff;
  color: #2f54eb;
}
.trace-status.status-3 {
  background: #fff7e6;
  color: #fa8c16;
}
.trace-status.status-4 {
  background: #f6ffed;
  color: #52c41a;
}
.trace-status.status-5 {
  background: #fff1f0;
  color: #f5222d;
}

.trace-desc {
  font-size: 0.9375rem;
  color: var(--color-text);
}

.trace-location {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.trace-operator {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.urge-section {
  padding-top: 4px;
}

.invoice-section {
  margin-top: 24px;
}

.invoice-info {
  padding-top: 8px;
}

.invoice-actions {
  display: flex;
  gap: 8px;
}
</style>
