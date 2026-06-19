<template>
  <div v-loading="loading" class="product-detail">
    <template v-if="product">
      <el-row :gutter="24">
        <el-col :span="10">
          <div class="main-image">
            <img
              :src="product.mainImage || '/images/default-product.svg'"
              alt=""
              @error="$event.target.src = '/images/default-product.svg'"
            />
          </div>
        </el-col>
        <el-col :span="14">
          <h1 class="title">{{ product.name }}</h1>
          <p class="subtitle">{{ product.subtitle }}</p>
          <p class="price">¥ {{ product.price }}</p>
          <p class="stock">库存：{{ product.stock }}</p>
          <div class="actions">
            <el-input-number v-model="quantity" :min="1" :max="product.stock" />
            <el-button type="primary" :disabled="product.stock < 1" @click="addToCart">
              加入购物车
            </el-button>
            <el-button
              v-if="compareStore.isInCompare(product.id)"
              type="success"
              :icon="ScaleToOriginal"
              disabled
            >
              已加入对比
            </el-button>
            <el-button
              v-else
              :icon="ScaleToOriginal"
              :disabled="compareStore.isFull"
              @click="handleAddCompare"
            >
              加入对比
            </el-button>
            <el-button
              v-if="favorited === true"
              type="warning"
              :icon="StarFilled"
              :loading="favoriteLoading"
              @click="toggleFavorite"
            >
              已收藏
            </el-button>
            <el-button
              v-else
              :icon="Star"
              :loading="favoriteLoading"
              @click="toggleFavorite"
            >
              收藏
            </el-button>
            <el-button :icon="Bell" :disabled="!userStore.isLoggedIn" @click="openPriceAlert">
              降价提醒
            </el-button>
          </div>
          <div v-if="product.detail" class="detail" v-html="product.detail" />
        </el-col>
      </el-row>
      <el-divider />
      <section class="reviews">
        <h3>商品评价</h3>
        <div v-if="userStore.isLoggedIn" class="add-review">
          <el-button type="primary" :loading="loadingReviewable" @click="openAddReview">
            我要评价
          </el-button>
        </div>
        <div v-else class="add-review-hint">
          <el-text type="info">登录后可发表评价</el-text>
        </div>
        <div v-for="r in reviews" :key="r.id" class="review-item">
          <el-rate :model-value="r.rating" disabled />
          <span class="review-content">{{ r.content || '（无文字）' }}</span>
          <span class="review-time">{{ r.createdAt }}</span>
        </div>
        <el-empty v-if="reviews.length === 0" description="暂无评价" />
      </section>

      <el-dialog v-model="reviewVisible" title="发表评价" width="420px" @close="resetReviewForm">
        <div v-if="loadingReviewable" class="review-dialog-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在加载可评价订单…</span>
        </div>
        <template v-else-if="reviewableOrders.length === 0">
          <el-empty description="您暂无可评价的订单">
            <template #description>
              <p>需已付款及之后的订单且包含本商品方可评价</p>
              <el-button type="primary" link @click="goOrders">去我的订单</el-button>
            </template>
          </el-empty>
        </template>
        <el-form v-else :model="reviewForm" label-width="80px">
          <el-form-item v-if="reviewableOrders.length > 1" label="选择订单">
            <el-select v-model="reviewForm.orderId" placeholder="请选择订单" style="width: 100%">
              <el-option
                v-for="o in reviewableOrders"
                :key="o.orderId"
                :label="`订单号 ${o.orderNo}`"
                :value="o.orderId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="评分" required>
            <el-rate v-model="reviewForm.rating" />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input v-model="reviewForm.content" type="textarea" :rows="3" placeholder="选填" />
          </el-form-item>
        </el-form>
        <template v-if="reviewableOrders.length > 0" #footer>
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="alertVisible" title="设置目标价提醒" width="420px">
        <el-form :model="alertForm" label-width="100px">
          <el-form-item label="当前价格">
            <span class="current-price">¥ {{ product?.price }}</span>
          </el-form-item>
          <el-form-item label="目标价格" required>
            <el-input-number
              v-model="alertForm.targetPrice"
              :min="0.01"
              :precision="2"
              :step="1"
              :controls="false"
              style="width: 100%"
              placeholder="请输入目标价格"
            />
            <p class="hint">当商品价格 ≤ 目标价时，将为您触发站内提醒</p>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button v-if="hasActiveAlert" type="danger" @click="cancelPriceAlert">取消提醒</el-button>
          <el-button @click="alertVisible = false">关闭</el-button>
          <el-button type="primary" :loading="alertSubmitting" @click="submitPriceAlert">确认设置</el-button>
        </template>
      </el-dialog>
    </template>
    <el-empty v-else-if="!loading" description="商品不存在" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Star, StarFilled, Bell, ScaleToOriginal } from '@element-plus/icons-vue'
import api from '../api'
import { useUserStore } from '../stores/user'
import { useViewHistoryStore } from '../stores/viewHistory'
import { useCompareStore } from '../stores/compare'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const viewHistory = useViewHistoryStore()
const compareStore = useCompareStore()
const loading = ref(true)
const product = ref(null)
const quantity = ref(1)
const reviews = ref([])
const reviewVisible = ref(false)
const reviewableOrders = ref([])
const loadingReviewable = ref(false)
const reviewSubmitting = ref(false)
const reviewForm = reactive({ orderId: null, productId: null, rating: 5, content: '' })

const favorited = ref(false)
const favoriteLoading = ref(false)

const alertVisible = ref(false)
const alertSubmitting = ref(false)
const hasActiveAlert = ref(false)
const alertForm = reactive({ targetPrice: null })

const productId = computed(() => Number(route.params.id))

onMounted(async () => {
  try {
    const [pRes, rRes] = await Promise.all([
      api.get(`/products/${productId.value}`),
      api.get(`/reviews/product/${productId.value}`),
    ])
    if (pRes.data.code === 200) {
      product.value = pRes.data.data
      if (product.value) {
        viewHistory.addRecord(
          productId.value,
          product.value.price,
          {
            name: product.value.name,
            mainImage: product.value.mainImage,
            categoryId: product.value.categoryId,
            status: product.value.status,
            stock: product.value.stock,
          }
        )
      }
    }
    if (rRes.data.code === 200) reviews.value = rRes.data.data || []
  } finally {
    loading.value = false
  }
  if (userStore.isLoggedIn) {
    try {
      const fRes = await api.get(`/favorites/check/${productId.value}`)
      if (fRes.data.code === 200) favorited.value = !!fRes.data.data
      const aRes = await api.get(`/favorites/price-alert/${productId.value}`)
      if (aRes.data.code === 200 && aRes.data.data) {
        hasActiveAlert.value = true
        alertForm.targetPrice = aRes.data.data.targetPrice
      }
    } catch {}
  }
})

async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  favoriteLoading.value = true
  try {
    if (favorited.value) {
      await api.delete(`/favorites/${productId.value}`)
      favorited.value = false
      ElMessage.success('已取消收藏')
      userStore.favoriteCount = Math.max(0, (userStore.favoriteCount || 0) - 1)
    } else {
      await api.post('/favorites/add', { productId: productId.value })
      favorited.value = true
      ElMessage.success('已加入收藏夹')
      userStore.favoriteCount = (userStore.favoriteCount || 0) + 1
    }
  } catch (e) {
  } finally {
    favoriteLoading.value = false
  }
}

function openPriceAlert() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  alertVisible.value = true
  if (!alertForm.targetPrice && product.value) {
    alertForm.targetPrice = Number(product.value.price) * 0.9
  }
}

async function submitPriceAlert() {
  if (!alertForm.targetPrice || alertForm.targetPrice <= 0) {
    ElMessage.warning('请输入有效的目标价格')
    return
  }
  alertSubmitting.value = true
  try {
    await api.post('/favorites/price-alert', {
      productId: productId.value,
      targetPrice: alertForm.targetPrice,
    })
    ElMessage.success('已设置目标价提醒')
    hasActiveAlert.value = true
    alertVisible.value = false
  } catch (e) {
  } finally {
    alertSubmitting.value = false
  }
}

async function cancelPriceAlert() {
  try {
    await ElMessageBox.confirm('确定要取消该商品的价格提醒吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  alertSubmitting.value = true
  try {
    await api.delete(`/favorites/price-alert/${productId.value}`)
    ElMessage.success('已取消价格提醒')
    hasActiveAlert.value = false
    alertForm.targetPrice = null
    alertVisible.value = false
  } catch (e) {
  } finally {
    alertSubmitting.value = false
  }
}

async function loadReviewableOrders() {
  loadingReviewable.value = true
  reviewableOrders.value = []
  try {
    const [ordersRes, myReviewsRes] = await Promise.all([
      api.get('/orders'),
      api.get('/reviews/me'),
    ])
    const orders = (ordersRes.data.code === 200 ? ordersRes.data.data : []) || []
    const myReviews = (myReviewsRes.data.code === 200 ? myReviewsRes.data.data : []) || []
    const reviewedSet = new Set(myReviews.map((r) => `${r.orderId}-${r.productId}`))
    const reviewable = orders.filter((o) => o.status >= 1 && o.status <= 3)
    const itemPromises = reviewable.map((o) => api.get(`/orders/${o.id}/items`))
    const itemResults = await Promise.all(itemPromises)
    const list = []
    reviewable.forEach((o, i) => {
      const items = (itemResults[i].data.code === 200 ? itemResults[i].data.data : []) || []
      const hasProduct = items.some((it) => Number(it.productId) === productId.value)
      if (hasProduct && !reviewedSet.has(`${o.id}-${productId.value}`)) {
        list.push({ orderId: o.id, orderNo: o.orderNo })
      }
    })
    reviewableOrders.value = list
    if (list.length > 0) {
      reviewForm.orderId = list[0].orderId
      reviewForm.productId = productId.value
      reviewForm.rating = 5
      reviewForm.content = ''
    }
  } finally {
    loadingReviewable.value = false
  }
}

function openAddReview() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  reviewVisible.value = true
  loadReviewableOrders()
}

function resetReviewForm() {
  reviewForm.orderId = null
  reviewForm.productId = null
  reviewForm.rating = 5
  reviewForm.content = ''
}

function goOrders() {
  reviewVisible.value = false
  router.push({ name: 'OrderList' })
}

async function submitReview() {
  if (!reviewForm.orderId || !reviewForm.productId) return
  reviewSubmitting.value = true
  try {
    await api.post('/reviews', {
      orderId: reviewForm.orderId,
      productId: reviewForm.productId,
      rating: reviewForm.rating,
      content: reviewForm.content || undefined,
    })
    ElMessage.success('评价成功')
    reviewVisible.value = false
    const rRes = await api.get(`/reviews/product/${productId.value}`)
    if (rRes.data.code === 200) reviews.value = rRes.data.data || []
  } catch (e) {
  } finally {
    reviewSubmitting.value = false
  }
}

async function addToCart() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await api.post('/cart/add', { productId: productId.value, quantity: quantity.value })
    ElMessage.success('已加入购物车')
    userStore.cartCount = (userStore.cartCount || 0) + quantity.value
  } catch (e) {
  }
}

async function handleAddCompare() {
  if (!product.value) return
  const result = await compareStore.addProduct(product.value)
  if (result.success) {
    ElMessage.success(result.message)
  } else {
    ElMessage.warning(result.message)
  }
}
</script>

<style scoped>
.product-detail {
  padding-bottom: 48px;
}

.main-image {
  aspect-ratio: 1;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.title {
  font-size: 1.625rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.subtitle {
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  font-size: 1rem;
}

.price {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 8px;
}

.stock {
  color: var(--color-text-muted);
  margin-bottom: 20px;
  font-size: 0.9375rem;
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.detail {
  margin-top: 20px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.reviews {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.reviews h3 {
  font-size: 1.125rem;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--color-text);
}

.add-review {
  margin-bottom: 16px;
}

.add-review-hint {
  margin-bottom: 16px;
}

.review-dialog-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--color-text-muted);
}

.review-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.review-content {
  flex: 1;
  color: var(--color-text-secondary);
}

.review-time {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.current-price {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 1.125rem;
}

.hint {
  margin: 6px 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}
</style>
