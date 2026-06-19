<template>
  <div class="admin-shipments">
    <h2 class="page-title">物流管理</h2>

    <el-tabs v-model="activeTab" class="content-card" @tab-change="onTabChange">
      <el-tab-pane label="订单发货" name="orders">
        <div class="filter-bar">
          <span class="filter-label">状态筛选：</span>
          <el-radio-group v-model="orderFilter" size="default" @change="loadOrders">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="1">待发货</el-radio-button>
            <el-radio-button :value="2">已发货</el-radio-button>
            <el-radio-button :value="3">已完成</el-radio-button>
          </el-radio-group>
          <el-button style="margin-left: auto;" @click="loadOrders">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <el-table :data="orders" stripe style="width: 100%" v-loading="orderLoading">
          <el-table-column prop="orderNo" label="订单号" width="210" />
          <el-table-column label="收货信息" min-width="220">
            <template #default="{ row }">
              <div>{{ row.receiverName }} {{ row.receiverPhone }}</div>
              <div class="muted">{{ row.receiverAddress }}</div>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="110">
            <template #default="{ row }">¥{{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="orderStatusType(row.status)" size="small">
                {{ orderStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="下单时间" width="170" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 1"
                link
                type="primary"
                size="small"
                @click="openShipDialog(row)"
              >
                发货
              </el-button>
              <el-button
                v-if="row.status >= 2"
                link
                type="info"
                size="small"
                @click="viewShipment(row)"
              >
                查看物流
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!orderLoading && orders.length === 0" description="暂无订单" />
      </el-tab-pane>

      <el-tab-pane label="催单列表" name="urges">
        <div class="filter-bar">
          <span class="filter-label">处理状态：</span>
          <el-radio-group v-model="urgeFilter" size="default" @change="loadUrges">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="0">待处理</el-radio-button>
            <el-radio-button :value="1">已处理</el-radio-button>
          </el-radio-group>
          <el-button style="margin-left: auto;" @click="loadUrges">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <el-table :data="urges" stripe style="width: 100%" v-loading="urgeLoading">
          <el-table-column prop="id" label="催单ID" width="90" />
          <el-table-column prop="orderId" label="订单ID" width="100" />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="urgeDate" label="催单日期" width="130" />
          <el-table-column prop="remark" label="备注" show-overflow-tooltip />
          <el-table-column label="处理状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.handled === 1 ? 'success' : 'warning'" size="small">
                {{ row.handled === 1 ? '已处理' : '待处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="催单时间" width="170" />
          <el-table-column label="管理员处理" width="200">
            <template #default="{ row }">
              <span v-if="row.adminRemark" class="muted" show-overflow-tooltip>{{ row.adminRemark }}</span>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.handled !== 1"
                link
                type="success"
                size="small"
                @click="handleUrge(row)"
              >
                处理
              </el-button>
              <el-button
                link
                type="info"
                size="small"
                @click="viewShipmentById(row.shipmentId)"
              >
                物流
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!urgeLoading && urges.length === 0" description="暂无催单记录" />
      </el-tab-pane>

      <el-tab-pane label="异常签收" name="issues">
        <div class="filter-bar">
          <span class="filter-label">处理状态：</span>
          <el-radio-group v-model="issueFilter" size="default" @change="loadIssues">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="0">待处理</el-radio-button>
            <el-radio-button :value="1">处理中</el-radio-button>
            <el-radio-button :value="2">已解决</el-radio-button>
            <el-radio-button :value="3">已驳回</el-radio-button>
          </el-radio-group>
          <el-button style="margin-left: auto;" @click="loadIssues">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <el-table :data="issues" stripe style="width: 100%" v-loading="issueLoading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="orderId" label="订单ID" width="100" />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column label="问题类型" width="110">
            <template #default="{ row }">
              <el-tag :type="issueTypeTag(row.issueType)" size="small">
                {{ issueTypeText(row.issueType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="issueStatusType(row.status)" size="small">
                {{ issueStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提交时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewIssue(row)">详情</el-button>
              <el-button
                v-if="row.status === 0 || row.status === 1"
                link
                type="warning"
                size="small"
                @click="openIssueHandle(row)"
              >
                处理
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!issueLoading && issues.length === 0" description="暂无异常签收记录" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="shipDialogVisible" title="订单发货" width="500px" destroy-on-close>
      <el-form :model="shipForm" label-width="100px" ref="shipFormRef" :rules="shipRules">
        <el-form-item label="订单号">
          <span>{{ shipOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="快递公司" prop="expressCompanyCode">
          <el-select
            v-model="shipForm.expressCompanyCode"
            placeholder="请选择快递公司"
            style="width: 100%"
            @change="onCompanyChange"
          >
            <el-option
              v-for="c in expressCompanies"
              :key="c.code"
              :label="c.name"
              :value="c.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="公司名称" prop="expressCompanyName">
          <el-input v-model="shipForm.expressCompanyName" placeholder="选择后自动填充" />
        </el-form-item>
        <el-form-item label="运单号" prop="trackingNo">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入运单号" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipmentDetailVisible" title="物流详情" width="640px" destroy-on-close>
      <div v-if="shipmentDetail" class="shipment-detail">
        <div class="sd-header">
          <div>
            <div style="font-weight: 600;">{{ shipmentDetail.shipment.expressCompanyName }}</div>
            <div class="muted">运单号：{{ shipmentDetail.shipment.trackingNo }}</div>
            <div class="muted">发货时间：{{ formatTime(shipmentDetail.shipment.shippedAt) }}</div>
          </div>
          <el-tag :type="shipmentStatusType(shipmentDetail.shipment.status)" size="large">
            {{ shipmentStatusText(shipmentDetail.shipment.status) }}
          </el-tag>
        </div>
        <el-divider />
        <el-timeline>
          <el-timeline-item
            v-for="(trace, idx) in shipmentDetail.traces"
            :key="trace.id || idx"
            :timestamp="formatTime(trace.traceTime)"
            :type="idx === 0 ? 'primary' : ''"
            :hollow="idx !== 0"
            placement="top"
          >
            <div>
              <el-tag size="small" :type="traceStatusType(trace.status)" style="margin-bottom: 4px;">
                {{ shipmentStatusText(trace.status) }}
              </el-tag>
              <div>{{ trace.description }}</div>
              <div v-if="trace.location" class="muted small">{{ trace.location }}</div>
              <div v-if="trace.operator" class="muted small">
                快递员：{{ trace.operator }} {{ trace.operatorPhone || '' }}
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>

    <el-dialog v-model="urgeHandleVisible" title="处理催单" width="440px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="催单ID">{{ urgeHandleRow?.id }}</el-form-item>
        <el-form-item label="用户备注">{{ urgeHandleRow?.remark || '无' }}</el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="urgeHandleRemark" type="textarea" :rows="3" placeholder="请输入处理备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="urgeHandleVisible = false">取消</el-button>
        <el-button type="primary" :loading="urgeHandleSubmitting" @click="submitUrgeHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueDetailVisible" title="异常签收详情" width="600px" destroy-on-close>
      <div v-if="issueDetail" class="issue-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单ID">{{ issueDetail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ issueDetail.userId }}</el-descriptions-item>
          <el-descriptions-item label="问题类型">
            <el-tag :type="issueTypeTag(issueDetail.issueType)" size="small">
              {{ issueTypeText(issueDetail.issueType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(issueDetail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="issueStatusType(issueDetail.status)" size="small">
              {{ issueStatusText(issueDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ issueDetail.handledAt ? formatTime(issueDetail.handledAt) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">{{ issueDetail.description }}</el-descriptions-item>
          <el-descriptions-item label="管理员备注" v-if="issueDetail.adminRemark" :span="2">
            {{ issueDetail.adminRemark }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="issueDetail.photos" class="issue-photos">
          <div class="photos-title">照片凭证：</div>
          <div class="photos-grid">
            <el-image
              v-for="(p, i) in splitPhotos(issueDetail.photos)"
              :key="i"
              :src="p"
              fit="cover"
              class="photo-thumb"
              :preview-src-list="splitPhotos(issueDetail.photos)"
              :initial-index="i"
            />
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="issueHandleVisible" title="处理异常签收" width="500px" destroy-on-close>
      <el-form :model="issueHandleForm" label-width="100px">
        <el-form-item label="问题类型">
          {{ issueTypeText(issueHandleRow?.issueType) }}
        </el-form-item>
        <el-form-item label="处理状态">
          <el-radio-group v-model="issueHandleForm.status">
            <el-radio :value="1">处理中</el-radio>
            <el-radio :value="2">已解决</el-radio>
            <el-radio :value="3">已驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="issueHandleForm.adminRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueHandleVisible = false">取消</el-button>
        <el-button type="primary" :loading="issueHandleSubmitting" @click="submitIssueHandle">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import api from '../api'

const activeTab = ref('orders')
const orderFilter = ref(null)
const orderLoading = ref(false)
const orders = ref([])

const urgeFilter = ref(0)
const urgeLoading = ref(false)
const urges = ref([])

const issueFilter = ref(0)
const issueLoading = ref(false)
const issues = ref([])

const expressCompanies = ref([])
const shipDialogVisible = ref(false)
const shipOrder = ref(null)
const shipFormRef = ref(null)
const shipForm = reactive({ expressCompanyCode: '', expressCompanyName: '', trackingNo: '' })
const shipRules = {
  expressCompanyCode: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  expressCompanyName: [{ required: true, message: '快递公司名称不能为空', trigger: 'blur' }],
  trackingNo: [{ required: true, message: '请输入运单号', trigger: 'blur' }],
}
const shipSubmitting = ref(false)

const shipmentDetailVisible = ref(false)
const shipmentDetail = ref(null)

const urgeHandleVisible = ref(false)
const urgeHandleRow = ref(null)
const urgeHandleRemark = ref('')
const urgeHandleSubmitting = ref(false)

const issueDetailVisible = ref(false)
const issueDetail = ref(null)

const issueHandleVisible = ref(false)
const issueHandleRow = ref(null)
const issueHandleForm = reactive({ status: 1, adminRemark: '' })
const issueHandleSubmitting = ref(false)

function onTabChange() {
  if (activeTab.value === 'orders') loadOrders()
  else if (activeTab.value === 'urges') loadUrges()
  else if (activeTab.value === 'issues') loadIssues()
}

function orderStatusText(s) {
  return { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消' }[s] || '未知'
}
function orderStatusType(s) {
  return { 0: 'warning', 1: 'primary', 2: 'info', 3: 'success', 4: 'info' }[s] || 'info'
}
function shipmentStatusText(s) {
  return { 0: '已发货', 1: '已揽收', 2: '运输中', 3: '派送中', 4: '已签收', 5: '异常签收' }[s] || '未知'
}
function shipmentStatusType(s) {
  return { 0: 'primary', 1: '', 2: '', 3: 'warning', 4: 'success', 5: 'danger' }[s] || 'info'
}
function traceStatusType(s) {
  return { 0: 'primary', 1: 'info', 2: '', 3: 'warning', 4: 'success', 5: 'danger' }[s] || ''
}
function issueTypeText(t) {
  return { damaged: '商品破损', wrong: '错发商品', missing: '丢件/少件', other: '其他' }[t] || t
}
function issueTypeTag(t) {
  return { damaged: 'danger', wrong: 'warning', missing: 'danger', other: 'info' }[t] || ''
}
function issueStatusText(s) {
  return { 0: '待处理', 1: '处理中', 2: '已解决', 3: '已驳回' }[s] || '未知'
}
function issueStatusType(s) {
  return { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }[s] || 'info'
}

function splitPhotos(photos) {
  if (!photos) return []
  return photos.split(',').filter(Boolean)
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadExpressCompanies() {
  try {
    const res = await api.get('/admin/express-companies')
    if (res.data.code === 200) expressCompanies.value = res.data.data || []
  } catch (e) {}
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const res = await api.get('/admin/orders')
    if (res.data.code === 200) {
      let list = res.data.data || []
      if (orderFilter.value != null) {
        if (orderFilter.value === 1) list = list.filter((o) => o.status === 1)
        else if (orderFilter.value === 2) list = list.filter((o) => o.status === 2)
        else if (orderFilter.value === 3) list = list.filter((o) => o.status === 3)
      }
      orders.value = list
    }
  } finally {
    orderLoading.value = false
  }
}

async function loadUrges() {
  urgeLoading.value = true
  try {
    const params = urgeFilter.value != null ? { handled: urgeFilter.value } : {}
    const res = await api.get('/admin/delivery-urges', { params })
    if (res.data.code === 200) urges.value = res.data.data || []
  } finally {
    urgeLoading.value = false
  }
}

async function loadIssues() {
  issueLoading.value = true
  try {
    const params = issueFilter.value != null ? { status: issueFilter.value } : {}
    const res = await api.get('/admin/delivery-issues', { params })
    if (res.data.code === 200) issues.value = res.data.data || []
  } finally {
    issueLoading.value = false
  }
}

function openShipDialog(row) {
  shipOrder.value = row
  shipForm.expressCompanyCode = ''
  shipForm.expressCompanyName = ''
  shipForm.trackingNo = ''
  shipDialogVisible.value = true
}

function onCompanyChange(code) {
  const c = expressCompanies.value.find((x) => x.code === code)
  if (c) shipForm.expressCompanyName = c.name
}

async function submitShip() {
  if (!shipFormRef.value) return
  await shipFormRef.value.validate(async (valid) => {
    if (!valid) return
    shipSubmitting.value = true
    try {
      await api.post(`/admin/orders/${shipOrder.value.id}/ship`, {
        expressCompanyCode: shipForm.expressCompanyCode,
        expressCompanyName: shipForm.expressCompanyName,
        trackingNo: shipForm.trackingNo,
      })
      ElMessage.success('发货成功')
      shipDialogVisible.value = false
      loadOrders()
    } finally {
      shipSubmitting.value = false
    }
  })
}

async function viewShipment(row) {
  try {
    const res = await api.get(`/shipments/order/${row.id}`)
    if (res.data.code === 200) {
      shipmentDetail.value = res.data.data
      shipmentDetailVisible.value = true
    } else {
      ElMessage.warning('暂无物流信息')
    }
  } catch (e) {}
}

async function viewShipmentById(shipmentId) {
  try {
    const res = await api.get(`/admin/shipments/${shipmentId}`)
    if (res.data.code === 200) {
      shipmentDetail.value = res.data.data
      shipmentDetailVisible.value = true
    }
  } catch (e) {}
}

function handleUrge(row) {
  urgeHandleRow.value = row
  urgeHandleRemark.value = '已联系快递方，正在加急处理'
  urgeHandleVisible.value = true
}

async function submitUrgeHandle() {
  urgeHandleSubmitting.value = true
  try {
    await api.post(`/admin/delivery-urges/${urgeHandleRow.value.id}/handle`, {
      remark: urgeHandleRemark.value,
    })
    ElMessage.success('处理成功')
    urgeHandleVisible.value = false
    loadUrges()
  } finally {
    urgeHandleSubmitting.value = false
  }
}

async function viewIssue(row) {
  try {
    const res = await api.get(`/admin/delivery-issues/${row.id}`)
    if (res.data.code === 200) {
      issueDetail.value = res.data.data
      issueDetailVisible.value = true
    }
  } catch (e) {}
}

function openIssueHandle(row) {
  issueHandleRow.value = row
  issueHandleForm.status = row.status === 0 ? 1 : row.status
  issueHandleForm.adminRemark = ''
  issueHandleVisible.value = true
}

async function submitIssueHandle() {
  issueHandleSubmitting.value = true
  try {
    await api.post(`/admin/delivery-issues/${issueHandleRow.value.id}/handle`, {
      status: issueHandleForm.status,
      adminRemark: issueHandleForm.adminRemark,
    })
    ElMessage.success('处理成功')
    issueHandleVisible.value = false
    loadIssues()
  } finally {
    issueHandleSubmitting.value = false
  }
}

onMounted(async () => {
  await loadExpressCompanies()
  loadOrders()
})
</script>

<style scoped>
.admin-shipments {
  padding-bottom: 32px;
}

.page-title {
  margin: 0 0 20px 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 0 16px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}

.filter-label {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  flex-shrink: 0;
}

.muted {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.muted.small {
  font-size: 0.75rem;
}

.sd-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.issue-photos {
  margin-top: 20px;
}

.photos-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.photos-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.photo-thumb {
  width: 100px;
  height: 100px;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  cursor: zoom-in;
}
</style>
