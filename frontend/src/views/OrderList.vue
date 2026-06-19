<template>
  <div class="order-list-page">
    <h2 class="page-title">我的订单</h2>
    <div v-loading="loading" class="order-content">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card content-card"
      >
        <div class="order-card-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small" class="order-tag">
            {{ statusText(order.status) }}
          </el-tag>
        </div>
        <p class="order-amount">
          合计：<strong>¥ {{ order.totalAmount }}</strong>
          <span v-if="order.discountAmount > 0" class="discount-amount">
            (券优惠 ¥ {{ order.discountAmount }})
          </span>
          <span v-if="order.pointsDiscount > 0" class="discount-amount">
            (积分抵 ¥ {{ order.pointsDiscount }})
          </span>
          <span v-if="order.pointsEarned > 0" class="earn-amount">
            (返 {{ order.pointsEarned }} 积分)
          </span>
        </p>
        <p class="order-time">下单时间：{{ order.createdAt }}</p>
        <div class="order-actions">
          <el-button size="small" @click="$router.push(`/orders/${order.id}`)">查看详情</el-button>
          <template v-if="order.status >= 1 && order.status <= 3">
            <el-button type="primary" size="small" @click="$router.push(`/orders/${order.id}`)">
              去评价
            </el-button>
          </template>
          <template v-if="order.status === 2">
            <el-button type="success" size="small" @click="confirm(order.id)">确认收货</el-button>
          </template>
          <template v-if="order.status === 0">
            <el-button type="primary" size="small" @click="pay(order.id)">去支付</el-button>
            <el-button size="small" @click="cancel(order.id)">取消订单</el-button>
          </template>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </div>

    <el-dialog
      v-model="showCheckout"
      title="确认订单"
      width="680px"
      class="checkout-dialog"
      @close="onCheckoutClose"
    >
      <div v-loading="calculating" class="checkout-content">
        <div class="checkout-section">
          <h4 class="section-title">收货信息</h4>
          <el-form :model="checkoutForm" label-width="100px">
            <el-form-item label="收货人" required>
              <el-input v-model="checkoutForm.receiverName" placeholder="收货人姓名" />
            </el-form-item>
            <el-form-item label="联系电话" required>
              <el-input v-model="checkoutForm.receiverPhone" placeholder="手机号" />
            </el-form-item>
            <el-form-item label="收货地址" required>
              <el-input v-model="checkoutForm.receiverAddress" type="textarea" placeholder="详细地址" :rows="2" />
            </el-form-item>
          </el-form>
        </div>

        <div class="checkout-section">
          <div class="section-header">
            <h4 class="section-title">优惠券</h4>
            <el-button type="primary" link @click="showCouponPicker = true">
              {{ checkoutForm.userCouponId ? '更换优惠券' : '选择优惠券' }}
            </el-button>
          </div>
          <div v-if="couponCalcResult.selectedCoupon" class="selected-coupon">
            <div class="selected-coupon-info">
              <div class="coupon-amount-mini">
                <template v-if="couponCalcResult.selectedCoupon.type === 1 || couponCalcResult.selectedCoupon.type === 3">
                  <span class="currency">¥</span>
                  <span class="amount">{{ couponCalcResult.selectedCoupon.faceValue }}</span>
                </template>
                <template v-else-if="couponCalcResult.selectedCoupon.type === 2">
                  <span class="discount">{{ (couponCalcResult.selectedCoupon.discountRate * 10).toFixed(1) }}</span>
                  <span class="discount-suffix">折</span>
                </template>
              </div>
              <div class="coupon-detail">
                <div class="coupon-name">{{ couponCalcResult.selectedCoupon.name }}</div>
                <div class="coupon-desc">{{ couponCalcResult.selectedCoupon.desc }}</div>
              </div>
            </div>
            <el-button type="danger" link @click="clearCoupon">不使用</el-button>
          </div>
          <div v-else-if="couponCalcResult.availableCoupons && couponCalcResult.availableCoupons.length > 0" class="coupon-hint">
            <el-icon><Ticket /></el-icon>
            <span>有 <strong>{{ couponCalcResult.availableCoupons.length }}</strong> 张优惠券可用，已为您推荐最优方案</span>
          </div>
          <div v-else class="no-coupon">
            <el-icon class="text-muted"><Ticket /></el-icon>
            <span>暂无可用优惠券</span>
            <el-button type="primary" link @click="$router.push('/coupon-center')">去领券</el-button>
          </div>
        </div>

        <div class="checkout-section">
          <div class="section-header">
            <h4 class="section-title">积分抵扣</h4>
            <el-switch v-model="usePoints" :disabled="pointsCalcResult.maxPointsUsable === 0" />
          </div>
          <div v-if="usePoints" class="points-use">
            <div class="points-info">
              <div>
                <span class="points-label">当前积分：</span>
                <span class="points-value">{{ pointsCalcResult.availablePoints || 0 }}</span>
              </div>
              <div>
                <span class="points-label">最多可用：</span>
                <span class="points-value points-value--highlight">{{ pointsCalcResult.maxPointsUsable || 0 }} 积分</span>
                <span class="points-hint">（抵 ¥{{ pointsCalcResult.maxDiscountAmount || '0.00' }}，最多抵实付30%）</span>
              </div>
              <div>
                <span class="points-label">当前等级：</span>
                <span class="points-value">Lv{{ pointsCalcResult.level || 1 }}，{{ pointsCalcResult.pointRate || 1 }}倍积分</span>
              </div>
            </div>
            <div class="points-slider">
              <el-slider
                v-model="checkoutForm.pointsToUse"
                :min="0"
                :max="pointsCalcResult.maxPointsUsable || 0"
                :step="100"
                :disabled="pointsCalcResult.maxPointsUsable === 0"
                show-input
                :show-input-controls="false"
                input-size="small"
              />
              <div class="points-deduct-display">
                抵扣金额：<strong class="points-deduct-amount">-¥{{ pointsDeductAmount.toFixed(2) }}</strong>
              </div>
            </div>
          </div>
          <div v-else class="no-points">
            <span>不使用积分抵扣（可用 {{ pointsCalcResult.availablePoints || 0 }} 积分）</span>
          </div>
        </div>

        <div class="checkout-section price-section">
          <h4 class="section-title">金额明细</h4>
          <div class="price-row">
            <span class="label">商品金额</span>
            <span class="value">¥ {{ displayOriginalAmount.toFixed(2) }}</span>
          </div>
          <div v-if="couponCalcResult.discountAmount > 0" class="price-row price-row--discount">
            <span class="label">优惠券抵扣</span>
            <span class="value">-¥ {{ Number(couponCalcResult.discountAmount).toFixed(2) }}</span>
          </div>
          <div v-if="pointsDeductAmount > 0" class="price-row price-row--discount">
            <span class="label">积分抵扣</span>
            <span class="value">-¥ {{ pointsDeductAmount.toFixed(2) }}</span>
          </div>
          <div class="price-row price-row--total">
            <span class="label">应付金额</span>
            <span class="value total">¥ {{ displayFinalAmount.toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCheckout = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCouponPicker" title="选择优惠券" width="560px">
      <div v-loading="calculating" class="coupon-picker">
        <div v-if="couponCalcResult.availableCoupons && couponCalcResult.availableCoupons.length > 0">
          <div class="picker-section-title">可用优惠券</div>
          <div
            v-for="coupon in couponCalcResult.availableCoupons"
            :key="coupon.id"
            class="coupon-option"
            :class="{ 'coupon-option--selected': checkoutForm.userCouponId === coupon.id }"
            @click="selectCoupon(coupon)"
          >
            <div class="coupon-radio">
              <el-radio :model-value="checkoutForm.userCouponId" :label="coupon.id" />
            </div>
            <div class="coupon-option-left">
              <div class="coupon-amount-mini">
                <template v-if="coupon.type === 1 || coupon.type === 3">
                  <span class="currency">¥</span>
                  <span class="amount">{{ coupon.faceValue }}</span>
                </template>
                <template v-else-if="coupon.type === 2">
                  <span class="discount">{{ (coupon.discountRate * 10).toFixed(1) }}</span>
                  <span class="discount-suffix">折</span>
                </template>
              </div>
            </div>
            <div class="coupon-option-right">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-desc">{{ coupon.desc }}</div>
              <div class="coupon-saving">可省 ¥{{ coupon.discountAmount }}</div>
              <div class="coupon-expire">有效期至 {{ formatDate(coupon.expiredAt) }}</div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无可用优惠券" />
      </div>
      <template #footer>
        <el-button @click="showCouponPicker = false">取消</el-button>
        <el-button type="primary" @click="confirmCoupon">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Ticket } from '@element-plus/icons-vue'
import api from '../api'
import { useUserStore } from '../stores/user'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(true)
const orders = ref([])
const showCheckout = ref(false)
const showCouponPicker = ref(false)
const calculating = ref(false)
const submitting = ref(false)
const tempCouponId = ref(null)
const usePoints = ref(false)

const checkoutForm = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  userCouponId: null,
  pointsToUse: 0
})

const couponCalcResult = reactive({
  originalAmount: 0,
  discountAmount: 0,
  finalAmount: 0,
  bestUserCouponId: null,
  selectedCoupon: null,
  availableCoupons: [],
  unavailableCoupons: []
})

const pointsCalcResult = reactive({
  availablePoints: 0,
  maxPointsUsable: 0,
  maxDiscountAmount: '0.00',
  level: 1,
  pointRate: '1'
})

const statusText = (s) => ({ 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' })[s] || '未知'
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'info', 3: 'success', 4: 'info' })[s] || 'info'

const displayOriginalAmount = computed(() => Number(couponCalcResult.originalAmount) || 0)

const pointsDeductAmount = computed(() => {
  if (!usePoints.value) return 0
  const pts = checkoutForm.pointsToUse || 0
  return pts / 100
})

const displayFinalAmount = computed(() => {
  const original = displayOriginalAmount.value
  const couponDisc = Number(couponCalcResult.discountAmount) || 0
  const afterCoupon = Math.max(0, original - couponDisc)
  const pointsDisc = pointsDeductAmount.value
  return Math.max(0, afterCoupon - pointsDisc)
})

async function load() {
  loading.value = true
  try {
    const res = await api.get('/orders')
    orders.value = res.data.code === 200 ? res.data.data || [] : []
  } finally {
    loading.value = false
  }
}

async function loadCart() {
  try {
    const res = await api.get('/cart')
    if (res.data.code === 200 && Array.isArray(res.data.data)) {
      const checked = res.data.data.filter(i => i.checked === 1)
      userStore.cartCount = res.data.data.reduce((s, i) => s + (i.quantity || 0), 0)
      return checked
    }
    return []
  } catch {
    return []
  }
}

async function recalcAll() {
  if (!userStore.isLoggedIn) return
  calculating.value = true
  try {
    const items = await loadCart()
    if (items.length === 0) {
      ElMessage.warning('请先选择要结算的商品')
      showCheckout.value = false
      return
    }

    const couponReq = {
      items: items.map(i => ({
        productId: i.productId,
        categoryId: i.categoryId,
        price: i.price,
        quantity: i.quantity
      })),
      userCouponId: checkoutForm.userCouponId
    }
    const couponRes = await api.post('/coupons/calculate', couponReq)
    if (couponRes.data.code === 200) {
      const data = couponRes.data.data
      Object.assign(couponCalcResult, data)
      if (!checkoutForm.userCouponId && data.bestUserCouponId) {
        checkoutForm.userCouponId = data.bestUserCouponId
      }
    }

    const original = Number(couponCalcResult.originalAmount) || 0
    const couponDisc = Number(couponCalcResult.discountAmount) || 0
    const afterCoupon = Math.max(0, original - couponDisc)

    try {
      const pointsRes = await api.get('/points/calculate', { params: { amount: afterCoupon } })
      if (pointsRes.data.code === 200) {
        Object.assign(pointsCalcResult, pointsRes.data.data)
        if (usePoints.value) {
          checkoutForm.pointsToUse = pointsCalcResult.maxPointsUsable || 0
        } else {
          checkoutForm.pointsToUse = 0
        }
      }
    } catch {}
  } finally {
    calculating.value = false
  }
}

watch(usePoints, (v) => {
  if (v) {
    checkoutForm.pointsToUse = pointsCalcResult.maxPointsUsable || 0
  } else {
    checkoutForm.pointsToUse = 0
  }
})

async function pay(orderId) {
  try {
    await api.post(`/orders/${orderId}/pay`)
    ElMessage.success('支付成功（模拟）')
    load()
  } catch (e) {}
}

async function confirm(orderId) {
  try {
    await ElMessageBox.confirm('确认已收到商品？确认后将发放积分', '提示', { type: 'warning' })
    await api.post(`/orders/${orderId}/confirm`)
    ElMessage.success('已确认收货')
    await userStore.fetchUser()
    load()
  } catch (e) {
    if (e !== 'cancel') {}
  }
}

async function cancel(orderId) {
  try {
    await api.post(`/orders/${orderId}/cancel`)
    ElMessage.success('已取消')
    load()
  } catch (e) {}
}

function selectCoupon(coupon) {
  tempCouponId.value = coupon.id
}

function confirmCoupon() {
  checkoutForm.userCouponId = tempCouponId.value
  showCouponPicker.value = false
  nextTick(() => recalcAll())
}

function clearCoupon() {
  checkoutForm.userCouponId = null
  tempCouponId.value = null
  recalcAll()
}

function onCheckoutClose() {
  Object.assign(checkoutForm, {
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    userCouponId: null,
    pointsToUse: 0
  })
  tempCouponId.value = null
  usePoints.value = false
}

async function submitOrder() {
  if (!checkoutForm.receiverName || !checkoutForm.receiverPhone || !checkoutForm.receiverAddress) {
    ElMessage.warning('请填写完整收货信息')
    return
  }
  submitting.value = true
  try {
    const data = {
      receiverName: checkoutForm.receiverName,
      receiverPhone: checkoutForm.receiverPhone,
      receiverAddress: checkoutForm.receiverAddress,
      userCouponId: checkoutForm.userCouponId,
      pointsToUse: usePoints.value ? checkoutForm.pointsToUse : 0
    }
    const res = await api.post('/orders', data)
    if (res.data.code === 200) {
      ElMessage.success('订单创建成功')
      showCheckout.value = false
      onCheckoutClose()
      await userStore.fetchUser()
      load()
      loadCart()
    }
  } finally {
    submitting.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

watch(
  () => route.query.checkout,
  async (v) => {
    if (v === '1') {
      showCheckout.value = true
      await nextTick()
      recalcAll()
    }
  },
  { immediate: true }
)

watch(
  () => showCouponPicker.value,
  (v) => {
    if (v) {
      tempCouponId.value = checkoutForm.userCouponId
    }
  }
)

onMounted(load)
</script>

<style scoped>
.order-list-page {
  padding-bottom: 32px;
}

.order-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  padding: 24px;
}

.order-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.order-no {
  font-weight: 600;
  color: var(--color-text);
}

.order-tag {
  border-radius: 6px;
}

.order-amount {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  font-size: 1rem;
}

.order-amount strong {
  color: var(--color-primary);
  font-size: 1.125rem;
}

.discount-amount {
  font-size: 0.875rem;
  color: #f5222d;
  font-weight: normal;
  margin-left: 8px;
}

.earn-amount {
  font-size: 0.875rem;
  color: #52c41a;
  font-weight: normal;
  margin-left: 8px;
}

.order-time {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.order-actions {
  display: flex;
  gap: 8px;
  padding-top: 4px;
}

.checkout-content {
  max-height: 60vh;
  overflow-y: auto;
}

.checkout-section {
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.checkout-section:last-of-type {
  border-bottom: none;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 12px 0;
}

.section-header .section-title {
  margin: 0;
}

.selected-coupon {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--color-primary-light);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-primary);
}

.selected-coupon-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.coupon-amount-mini {
  display: flex;
  align-items: baseline;
  color: var(--color-primary);
  min-width: 70px;
}

.coupon-amount-mini .currency {
  font-size: 0.875rem;
  margin-right: 2px;
}

.coupon-amount-mini .amount {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1;
}

.coupon-amount-mini .discount {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1;
}

.coupon-amount-mini .discount-suffix {
  font-size: 0.875rem;
  margin-left: 2px;
}

.coupon-detail .coupon-name {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.coupon-detail .coupon-desc {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.coupon-hint,
.no-coupon,
.no-points {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}

.coupon-hint strong {
  color: var(--color-primary);
  margin: 0 4px;
}

.points-use {
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
  border-radius: var(--radius-sm);
}

.points-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 0.9375rem;
  color: var(--color-text);
}

.points-label {
  color: var(--color-text-secondary);
}

.points-value {
  font-weight: 600;
  color: var(--color-text);
}

.points-value--highlight {
  color: #fa8c16;
}

.points-hint {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  margin-left: 8px;
}

.points-slider {
  padding: 0 8px;
}

.points-deduct-display {
  margin-top: 8px;
  font-size: 0.9375rem;
  color: var(--color-text);
}

.points-deduct-amount {
  color: #f5222d;
  font-size: 1.125rem;
  margin-left: 4px;
}

.price-section {
  padding-bottom: 0;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 0.9375rem;
}

.price-row .label {
  color: var(--color-text-secondary);
}

.price-row .value {
  color: var(--color-text);
  font-weight: 500;
}

.price-row--discount .value {
  color: #f5222d;
}

.price-row--total {
  padding-top: 12px;
  margin-top: 8px;
  border-top: 1px dashed var(--color-border);
}

.price-row--total .label {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
}

.price-row--total .value.total {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-primary);
}

.coupon-picker {
  max-height: 50vh;
  overflow-y: auto;
}

.picker-section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 12px 0;
  padding-left: 4px;
}

.coupon-option {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition);
}

.coupon-option:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.coupon-option--selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.coupon-radio {
  margin-right: 12px;
}

.coupon-option-left {
  padding: 0 16px;
  border-right: 1px dashed var(--color-border);
  color: var(--color-primary);
}

.coupon-option-right {
  flex: 1;
  padding-left: 16px;
}

.coupon-option-right .coupon-name {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.coupon-option-right .coupon-desc {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.coupon-option-right .coupon-saving {
  font-size: 0.8125rem;
  color: #f5222d;
  font-weight: 600;
  margin-bottom: 2px;
}

.coupon-option-right .coupon-expire {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.text-muted {
  color: var(--color-text-muted);
}
</style>
