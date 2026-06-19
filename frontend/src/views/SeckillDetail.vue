<template>
  <div class="seckill-detail" v-loading="loading">
    <el-page-header class="page-header" @back="$router.back()" content="返回活动列表" />

    <div v-if="session" class="detail-content">
      <div class="product-area">
        <div class="product-img">
          <img
            :src="session.productImage || '/images/default-product.svg'"
            alt=""
            @error="$event.target.src = '/images/default-product.svg'"
          />
          <el-tag
            v-if="session.activityStatus === 1"
            class="activity-tag"
            type="danger"
            effect="dark"
            size="large"
          >
            <el-icon><Lightning /></el-icon> 抢购中
          </el-tag>
          <el-tag
            v-else-if="session.activityStatus === 0"
            class="activity-tag"
            type="warning"
            effect="dark"
            size="large"
          >
            即将开始
          </el-tag>
          <el-tag
            v-else
            class="activity-tag"
            type="info"
            effect="dark"
            size="large"
          >
            {{ getStatusText(session.activityStatus) }}
          </el-tag>
        </div>
        <div class="product-info">
          <h1 class="product-name">{{ session.productName }}</h1>
          <div class="price-box">
            <div class="price-main">
              <span class="currency">¥</span>
              <span class="price">{{ session.seckillPrice }}</span>
              <el-tag type="danger" effect="dark" size="large" class="seckill-tag">限时秒杀</el-tag>
            </div>
            <div class="price-origin">
              原价：<span class="line-through">¥ {{ session.originalPrice }}</span>
              <span class="save-tag">立省 ¥{{ formatSave() }}</span>
            </div>
          </div>

          <div class="countdown-section" v-if="session.activityStatus === 1 || session.activityStatus === 0">
            <div class="countdown-title">
              <el-icon v-if="session.activityStatus === 1"><Timer /></el-icon>
              <el-icon v-else><Clock /></el-icon>
              {{ session.activityStatus === 1 ? '距活动结束' : '距活动开始' }}
            </div>
            <div class="countdown-box">
              <span class="cd-item">{{ cdObj.d }}</span><span class="cd-sep">天</span>
              <span class="cd-item">{{ cdObj.h }}</span><span class="cd-sep">时</span>
              <span class="cd-item">{{ cdObj.m }}</span><span class="cd-sep">分</span>
              <span class="cd-item">{{ cdObj.s }}</span><span class="cd-sep">秒</span>
            </div>
          </div>

          <div class="progress-section">
            <div class="progress-header">
              <span>已售进度</span>
              <span class="sold-text">已抢 {{ session.soldStock }} / {{ session.totalStock }} 件</span>
            </div>
            <el-progress
              :percentage="session.soldPercent"
              :stroke-width="18"
              color="#ef4444"
              :text-inside="true"
            />
          </div>

          <div class="info-list">
            <div class="info-row">
              <span class="info-label">活动时间</span>
              <span class="info-value">{{ formatDateTime(session.startTime) }} ~ {{ formatDateTime(session.endTime) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">每人限购</span>
              <span class="info-value highlight">
                <el-tag type="warning" size="small">{{ session.perUserLimit }} 件</el-tag>
                <span v-if="session.userBoughtCount" class="bought-tip">
                  (您已购买 {{ session.userBoughtCount }} 件)
                </span>
              </span>
            </div>
            <div class="info-row" v-if="session.activityStatus === 1 && session.userBoughtCount >= session.perUserLimit">
              <el-alert type="warning" :closable="false" show-icon title="您已达到本场限购数量" />
            </div>
          </div>

          <div class="action-row">
            <el-button
              v-if="session.activityStatus === 0"
              size="large"
              class="main-btn"
              :disabled="true"
            >
              <el-icon><Clock /></el-icon> 活动未开始
            </el-button>
            <el-button
              v-else-if="session.activityStatus === 3"
              size="large"
              class="main-btn"
              :disabled="true"
            >
              <el-icon><Warning /></el-icon> 商品已售罄
            </el-button>
            <el-button
              v-else-if="session.activityStatus === 2"
              size="large"
              class="main-btn"
              :disabled="true"
            >
              <el-icon><Warning /></el-icon> 活动已结束
            </el-button>
            <el-button
              v-else-if="session.activityStatus === 4"
              size="large"
              class="main-btn"
              :disabled="true"
            >
              活动已下架
            </el-button>
            <el-button
              v-else-if="session.userBoughtCount >= session.perUserLimit"
              size="large"
              class="main-btn"
              :disabled="true"
            >
              已达限购数量
            </el-button>
            <el-button
              v-else-if="seckillToken"
              size="large"
              type="danger"
              class="main-btn"
              @click="showOrderForm = true"
            >
              <el-icon><ShoppingCart /></el-icon> 立即下单
            </el-button>
            <el-button
              v-else
              size="large"
              type="danger"
              class="main-btn"
              :loading="acquiringToken"
              @click="acquireToken"
            >
              <el-icon><Lightning /></el-icon> {{ acquiringToken ? '排队获取资格中...' : '立即秒杀' }}
            </el-button>
          </div>

          <div v-if="seckillToken" class="token-info">
            <el-alert type="success" :closable="false" show-icon>
              <template #title>
                您已获得秒杀资格，请尽快下单（资格 10 分钟内有效）
              </template>
            </el-alert>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showOrderForm"
      title="秒杀订单确认"
      width="480px"
      :close-on-click-modal="!submittingOrder"
      @closed="() => {}"
    >
      <el-form :model="orderForm" label-width="90px" class="order-form">
        <div class="order-product">
          <img
            :src="session.productImage || '/images/default-product.svg'"
            class="order-img"
            @error="$event.target.src = '/images/default-product.svg'"
          />
          <div class="order-product-info">
            <div class="order-product-name">{{ session.productName }}</div>
            <div class="order-product-price">
              <span class="sp">¥ {{ session.seckillPrice }}</span>
              <span class="op">/ x 1 件</span>
            </div>
          </div>
        </div>
        <el-divider />
        <el-form-item label="收货人" prop="receiverName" :rules="[{ required: true, message: '请输入收货人' }]">
          <el-input v-model="orderForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="receiverPhone" :rules="[{ required: true, message: '请输入电话' }]">
          <el-input v-model="orderForm.receiverPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="receiverAddress" :rules="[{ required: true, message: '请输入地址' }]">
          <el-input v-model="orderForm.receiverAddress" type="textarea" :rows="2" placeholder="请输入收货地址" />
        </el-form-item>
        <el-divider />
        <div class="order-total">
          <span>合计</span>
          <span class="total-price">¥ {{ session.seckillPrice }}</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showOrderForm = false" :disabled="submittingOrder">取消</el-button>
        <el-button type="danger" :loading="submittingOrder" @click="submitOrder">
          确认下单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lightning, Timer, Clock, Warning, ShoppingCart } from '@element-plus/icons-vue'
import api from '../api'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const session = ref(null)
const seckillToken = ref('')
const acquiringToken = ref(false)
const showOrderForm = ref(false)
const submittingOrder = ref(false)
const remainSeconds = ref(0)
let tickTimer = null
let fetchTimer = null

const orderForm = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
})

const cdObj = computed(() => {
  const total = Math.max(0, Math.floor(remainSeconds.value))
  const d = String(Math.floor(total / 86400)).padStart(2, '0')
  const h = String(Math.floor((total % 86400) / 3600)).padStart(2, '0')
  const m = String(Math.floor((total % 3600) / 60)).padStart(2, '0')
  const s = String(total % 60).padStart(2, '0')
  return { d, h, m, s }
})

function formatSave() {
  if (!session.value) return '0.00'
  return (Number(session.value.originalPrice) - Number(session.value.seckillPrice)).toFixed(2)
}

function formatDateTime(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  const pad = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
    ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

function getStatusText(s) {
  const map = { 0: '即将开始', 1: '抢购中', 2: '已结束', 3: '已售罄', 4: '已下架' }
  return map[s] || '未知'
}

async function fetchDetail() {
  try {
    const res = await api.get('/seckill/sessions/' + route.params.id)
    if (res.data.code === 200) {
      session.value = res.data.data
      if (session.value) {
        remainSeconds.value = session.value.remainSeconds || 0
      }
    } else if (res.data.code === 404) {
      ElMessage.error('秒杀活动不存在')
      router.push('/seckill')
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

async function acquireToken() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  acquiringToken.value = true
  try {
    const res = await api.post('/seckill/sessions/' + session.value.id + '/token')
    if (res.data.code === 200) {
      seckillToken.value = res.data.data.token
      ElMessage.success('恭喜您，秒杀资格获取成功！')
    } else {
      ElMessage.error(res.data.message || '获取资格失败')
      if (res.data.message && (
        res.data.message.includes('售罄') ||
        res.data.message.includes('限购') ||
        res.data.message.includes('未开始') ||
        res.data.message.includes('已结束')
      )) {
        fetchDetail()
      }
    }
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '获取资格失败'
    ElMessageBox.alert(msg, '秒杀提示', {
      type: msg.includes('售罄') || msg.includes('限购') || msg.includes('未开始') || msg.includes('已结束') ? 'warning' : 'error',
      confirmButtonText: '我知道了',
    }).then(() => {
      fetchDetail()
    }).catch(() => {
      fetchDetail()
    })
  } finally {
    acquiringToken.value = false
  }
}

async function submitOrder() {
  if (!orderForm.receiverName || !orderForm.receiverPhone || !orderForm.receiverAddress) {
    ElMessage.warning('请填写完整的收货信息')
    return
  }
  submittingOrder.value = true
  try {
    const res = await api.post('/seckill/order', {
      sessionId: session.value.id,
      token: seckillToken.value,
      receiverName: orderForm.receiverName,
      receiverPhone: orderForm.receiverPhone,
      receiverAddress: orderForm.receiverAddress,
    })
    if (res.data.code === 200) {
      const order = res.data.data
      ElMessage.success('秒杀订单创建成功！')
      showOrderForm.value = false
      router.push('/orders/' + order.id)
    } else {
      ElMessage.error(res.data.message || '下单失败')
      if (res.data.message && (
        res.data.message.includes('售罄') ||
        res.data.message.includes('限购') ||
        res.data.message.includes('过期') ||
        res.data.message.includes('重新')
      )) {
        seckillToken.value = ''
        showOrderForm.value = false
        fetchDetail()
      }
    }
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '下单失败'
    ElMessageBox.alert(msg, '下单失败', {
      type: 'warning',
      confirmButtonText: '我知道了',
    }).then(() => {
      if (msg.includes('售罄') || msg.includes('限购') || msg.includes('过期') || msg.includes('重新')) {
        seckillToken.value = ''
        showOrderForm.value = false
        fetchDetail()
      }
    }).catch(() => {})
  } finally {
    submittingOrder.value = false
  }
}

onMounted(() => {
  fetchDetail()
  tickTimer = setInterval(() => {
    if (remainSeconds.value > 0) remainSeconds.value--
  }, 1000)
  fetchTimer = setInterval(fetchDetail, 15000)
})

onBeforeUnmount(() => {
  if (tickTimer) clearInterval(tickTimer)
  if (fetchTimer) clearInterval(fetchTimer)
})
</script>

<style scoped>
.seckill-detail { padding-bottom: 48px; }
.page-header { margin-bottom: 24px; }
.detail-content {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.product-area { display: flex; gap: 32px; padding: 32px; }
.product-img {
  width: 360px; min-width: 360px; height: 360px;
  background: var(--color-bg); border-radius: var(--radius-md);
  position: relative; display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.product-img img { width: 100%; height: 100%; object-fit: cover; }
.activity-tag {
  position: absolute; top: 16px; left: 16px; font-weight: 600;
}
.product-info { flex: 1; display: flex; flex-direction: column; gap: 20px; }
.product-name {
  font-size: 1.5rem; font-weight: 700; color: var(--color-text); margin: 0;
}
.price-box {
  padding: 20px 24px;
  background: linear-gradient(135deg, #fef2f2 0%, #fff1f2 100%);
  border-radius: var(--radius-md);
}
.price-main { display: flex; align-items: baseline; gap: 12px; }
.currency { font-size: 1.25rem; color: #ef4444; font-weight: 600; }
.price { font-size: 2.5rem; color: #ef4444; font-weight: 700; line-height: 1; }
.seckill-tag { margin-left: 12px; }
.price-origin {
  margin-top: 8px; font-size: 0.9375rem;
  color: var(--color-text-secondary);
}
.line-through { text-decoration: line-through; }
.save-tag {
  margin-left: 12px; color: #ef4444; font-weight: 600;
}
.countdown-section {
  padding: 16px 20px;
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border-radius: var(--radius-md);
}
.countdown-title {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 0.9375rem; font-weight: 500; color: #065f46;
  margin-bottom: 10px;
}
.countdown-box { display: flex; align-items: center; gap: 2px; }
.cd-item {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 48px; height: 40px;
  background: #ef4444; color: #fff;
  font-family: 'Courier New', monospace;
  font-size: 1.25rem; font-weight: 700;
  border-radius: 6px; padding: 0 8px;
}
.cd-sep {
  font-weight: 700; color: #ef4444;
  padding: 0 4px; font-size: 0.875rem;
}
.progress-section { display: flex; flex-direction: column; gap: 8px; }
.progress-header {
  display: flex; justify-content: space-between;
  font-size: 0.9375rem; font-weight: 500; color: var(--color-text-secondary);
}
.sold-text { color: var(--color-text); font-weight: 600; }
.info-list {
  border-top: 1px solid var(--color-border);
  padding-top: 16px;
  display: flex; flex-direction: column; gap: 12px;
}
.info-row { display: flex; align-items: center; gap: 12px; }
.info-label {
  min-width: 80px; color: var(--color-text-muted);
  font-size: 0.9375rem;
}
.info-value { color: var(--color-text); font-size: 0.9375rem; }
.info-value.highlight { display: inline-flex; align-items: center; gap: 8px; }
.bought-tip { font-size: 0.8125rem; color: #f59e0b; }
.action-row { padding-top: 8px; }
.main-btn { width: 240px; height: 52px; font-size: 1.0625rem; font-weight: 600; }
.token-info { padding-top: 8px; }

.order-form .order-product {
  display: flex; gap: 12px; padding: 8px 0;
}
.order-img {
  width: 64px; height: 64px; object-fit: cover;
  border-radius: var(--radius-sm);
}
.order-product-info { flex: 1; display: flex; flex-direction: column; gap: 4px; justify-content: center; }
.order-product-name {
  font-size: 0.9375rem; font-weight: 500; color: var(--color-text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.order-product-price { font-size: 0.9375rem; }
.order-product-price .sp { color: #ef4444; font-weight: 700; font-size: 1.0625rem; }
.order-product-price .op { color: var(--color-text-muted); }
.order-total {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 1.0625rem; font-weight: 600;
}
.order-total .total-price { font-size: 1.5rem; color: #ef4444; }
</style>
