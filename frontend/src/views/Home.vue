<template>
  <div class="home">
    <section class="hero">
      <div class="hero-bg" />
      <div class="hero-content">
        <h1>精选好物，品质生活</h1>
        <p>发现心仪商品，享受便捷购物体验</p>
        <el-button type="primary" size="large" class="hero-cta" @click="$router.push('/products')">
          去逛逛
        </el-button>
      </div>
      <div class="hero-privacy">
        <el-tooltip :content="viewHistory.privacyMode ? '隐私浏览已开启：期间不记录浏览历史' : '开启隐私浏览：期间不记录浏览历史'">
          <el-switch
            v-model="viewHistory.privacyMode"
            @change="viewHistory.setPrivacyMode($event)"
            active-color="#8b5cf6"
            inactive-color="#d1d5db"
            size="large"
          />
        </el-tooltip>
        <span class="privacy-label" :class="{ active: viewHistory.privacyMode }">
          <el-icon :size="16"><View /></el-icon>
          {{ viewHistory.privacyMode ? '隐私中' : '隐私模式' }}
        </span>
      </div>
    </section>

    <section
      v-if="recentItems.length > 0 && !viewHistory.privacyMode"
      class="section recent-section"
    >
      <div class="section-header">
        <h2 class="section-title">
          <el-icon :size="22" color="#6366f1"><Clock /></el-icon>
          继续浏览
        </h2>
        <el-button type="primary" link @click="$router.push('/my-history')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="recent-scroll-wrap">
        <div
          ref="scrollRef"
          class="recent-scroll"
          @wheel.prevent="onWheel"
        >
          <div
            v-for="(item, idx) in recentItems"
            :key="item.id + '_' + idx"
            class="recent-card"
            @click="goDetail(item.productId)"
          >
            <div class="recent-img">
              <img
                :src="item.productImage || '/images/default-product.svg'"
                :alt="item.productName"
                @error="$event.target.src = '/images/default-product.svg'"
              />
              <div v-if="(item.status ?? 1) === 0" class="recent-off-badge">已下架</div>
            </div>
            <div class="recent-info">
              <div class="recent-name">{{ item.productName }}</div>
              <div class="recent-prices">
                <span class="recent-price-current">¥ {{ item.currentPrice }}</span>
                <span
                  v-if="(item.priceChangePercent ?? 0) !== 0"
                  class="recent-price-change"
                  :class="(item.priceChangePercent ?? 0) > 0 ? 'up' : 'down'"
                >
                  {{ (item.priceChangePercent ?? 0) > 0 ? '↑' : '↓' }}
                  {{ formatPercent(item.priceChangePercent) }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <el-button
          v-if="canScrollLeft"
          class="scroll-btn scroll-btn--left"
          circle
          size="small"
          @click="scrollLeft"
        >
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <el-button
          v-if="canScrollRight"
          class="scroll-btn scroll-btn--right"
          circle
          size="small"
          @click="scrollRight"
        >
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </section>

    <section class="section seckill-banner" @click="$router.push('/seckill')">
      <div class="seckill-banner-bg" />
      <div class="seckill-banner-content">
        <div class="seckill-banner-text">
          <h2 class="seckill-title">
            <el-icon :size="28"><Lightning /></el-icon>
            限时秒杀
          </h2>
          <p class="seckill-subtitle">超值特价 · 数量有限 · 先到先得</p>
        </div>
        <el-button size="large" type="danger" class="seckill-banner-btn">
          立即抢购
          <el-icon><Right /></el-icon>
        </el-button>
      </div>
    </section>

    <section class="section categories">
      <h2 class="section-title">商品分类</h2>
      <div v-loading="loading" class="category-list">
        <div
          v-for="c in categories"
          :key="c.id"
          class="category-card"
          @click="$router.push({ path: '/products', query: { categoryId: c.id } })"
        >
          <span>{{ c.name }}</span>
        </div>
      </div>
    </section>

    <section class="section products">
      <h2 class="section-title">热门商品</h2>
      <div v-loading="loading" class="product-grid">
        <div
          v-for="p in products"
          :key="p.id"
          class="product-card"
          @click="$router.push(`/products/${p.id}`)"
        >
          <div class="product-img">
            <img
              :src="p.mainImage || '/images/default-product.svg'"
              alt=""
              @error="$event.target.src = '/images/default-product.svg'"
            />
          </div>
          <div class="product-info">
            <div class="product-name">{{ p.name }}</div>
            <div class="product-price">¥ {{ p.price }}</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  Lightning, Right, Clock, View, ArrowRight, ArrowLeft,
} from '@element-plus/icons-vue'
import api from '../api'
import { useViewHistoryStore } from '../stores/viewHistory'
import { useUserStore } from '../stores/user'

const router = useRouter()
const viewHistory = useViewHistoryStore()
const userStore = useUserStore()

const loading = ref(true)
const categories = ref([])
const products = ref([])
const recentItems = ref([])
const loadingRecent = ref(false)
const scrollRef = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)

function formatPercent(v) {
  if (v == null) return '0.00%'
  const n = Number(v)
  return (n >= 0 ? '+' : '') + n.toFixed(2) + '%'
}

function goDetail(pid) {
  router.push({ name: 'ProductDetail', params: { id: pid } })
}

async function enrichRecent(localItems) {
  if (!Array.isArray(localItems) || localItems.length === 0) return []
  const idsMap = {}
  for (const i of localItems) {
    idsMap[Number(i.productId)] = 1
  }
  const ids = Object.keys(idsMap).map(Number)
  if (ids.length === 0) return []

  let allProducts = []
  try {
    const res = await api.get('/products')
    if (res.data.code === 200 && Array.isArray(res.data.data)) {
      allProducts = res.data.data
    }
  } catch {}
  const pMap = {}
  for (const p of allProducts) {
    if (ids.includes(Number(p.id))) pMap[Number(p.id)] = p
  }

  const result = []
  for (const i of localItems) {
    const pid = Number(i.productId)
    const p = pMap[pid]
    const name = i.productName || (p ? p.name : `商品 #${pid}`)
    const image = i.productImage || (p ? p.mainImage : '')
    const status = i.status != null ? i.status : (p ? p.status : 1)
    const stock = i.stock != null ? i.stock : (p ? p.stock : 0)
    const currentPrice = i.currentPrice ?? (p ? p.price : i.viewedPrice)
    const diff = Number(currentPrice) - Number(i.viewedPrice || 0)
    const viewedPrice = Number(i.viewedPrice || 0)
    let percent = 0
    if (viewedPrice > 0) percent = (diff / viewedPrice) * 100
    result.push({
      ...i,
      productName: name,
      productImage: image,
      status,
      stock,
      currentPrice,
      priceChange: diff,
      priceChangePercent: percent,
    })
  }
  return result
}

async function loadRecent() {
  if (viewHistory.privacyMode) {
    recentItems.value = []
    return
  }
  loadingRecent.value = true
  try {
    if (viewHistory.isLoggedIn) {
      const res = await api.get('/view-history/recent', { params: { limit: 6 } })
      if (res.data.code === 200 && Array.isArray(res.data.data)) {
        recentItems.value = res.data.data.slice(0, 6)
        const local = viewHistory.getLocalRecent(6)
        if (local.length > 0) {
          const enriched = await enrichRecent(local)
          const existingIds = new Set(recentItems.value.map(i => Number(i.productId)))
          for (const li of enriched) {
            if (!existingIds.has(Number(li.productId))) {
              recentItems.value.push(li)
            }
          }
          recentItems.value.sort((a, b) => {
            const ta = parseTime(a.viewedAt)
            const tb = parseTime(b.viewedAt)
            return tb - ta
          })
          recentItems.value = recentItems.value.slice(0, 6)
        }
      }
    } else {
      const local = viewHistory.getLocalRecent(6)
      recentItems.value = await enrichRecent(local)
    }
  } finally {
    loadingRecent.value = false
    await nextTick()
    updateScrollButtons()
  }
}

function parseTime(v) {
  if (!v) return 0
  const d = typeof v === 'string' ? new Date(v) : new Date(v)
  return isNaN(d.getTime()) ? 0 : d.getTime()
}

function updateScrollButtons() {
  const el = scrollRef.value
  if (!el) return
  canScrollLeft.value = el.scrollLeft > 4
  canScrollRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 4
}

function onWheel(e) {
  const el = scrollRef.value
  if (!el) return
  if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
    el.scrollLeft += e.deltaY
    updateScrollButtons()
  }
}

function scrollLeft() {
  const el = scrollRef.value
  if (!el) return
  el.scrollBy({ left: -320, behavior: 'smooth' })
  setTimeout(updateScrollButtons, 300)
}

function scrollRight() {
  const el = scrollRef.value
  if (!el) return
  el.scrollBy({ left: 320, behavior: 'smooth' })
  setTimeout(updateScrollButtons, 300)
}

watch(
  () => viewHistory.localList.length,
  () => loadRecent(),
)

watch(
  () => userStore.isLoggedIn,
  () => loadRecent(),
)

watch(
  () => viewHistory.privacyMode,
  () => loadRecent(),
)

onMounted(async () => {
  try {
    const [catRes, prodRes] = await Promise.all([
      api.get('/categories'),
      api.get('/products'),
    ])
    if (catRes.data.code === 200) categories.value = (catRes.data.data || []).filter(c => c.parentId === 0)
    if (prodRes.data.code === 200) products.value = (prodRes.data.data || []).slice(0, 8)
  } finally {
    loading.value = false
  }
  loadRecent()
})
</script>

<style scoped>
.home {
  padding-bottom: 48px;
}

.hero {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 48px;
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #0f766e 0%, #134e4a 50%, #0d9488 100%);
  opacity: 0.96;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 48px 24px;
}

.hero-privacy {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  padding: 6px 12px 6px 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  transition: all 0.2s;
}

.hero-privacy:hover {
  background: rgba(255, 255, 255, 0.28);
}

.privacy-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.8125rem;
  font-weight: 500;
  transition: color 0.2s;
}

.privacy-label.active {
  color: #fff;
  font-weight: 600;
}

.hero h1 {
  font-size: 2.25rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  letter-spacing: -0.03em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.hero p {
  font-size: 1.0625rem;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 28px;
}

.hero-cta {
  padding: 12px 28px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  background: #fff !important;
  color: var(--color-primary) !important;
  border: none !important;
}

.hero-cta:hover {
  background: rgba(255, 255, 255, 0.95) !important;
  color: var(--color-primary-hover) !important;
}

.section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  letter-spacing: -0.02em;
}

.recent-section {
  padding: 24px;
  background: linear-gradient(180deg, #f5f3ff 0%, #fff 100%);
  border: 1px solid #e9d5ff;
  border-radius: var(--radius-lg);
}

.recent-scroll-wrap {
  position: relative;
}

.recent-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 4px 4px 12px;
  scroll-snap-type: x mandatory;
  scrollbar-width: thin;
  scrollbar-color: #c4b5fd transparent;
}

.recent-scroll::-webkit-scrollbar {
  height: 6px;
}

.recent-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.recent-scroll::-webkit-scrollbar-thumb {
  background: #c4b5fd;
  border-radius: 3px;
}

.recent-scroll::-webkit-scrollbar-thumb:hover {
  background: #a78bfa;
}

.recent-card {
  flex: 0 0 180px;
  scroll-snap-align: start;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all 0.25s;
}

.recent-card:hover {
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
  border-color: #a78bfa;
  transform: translateY(-2px);
}

.recent-img {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: var(--color-bg);
  overflow: hidden;
}

.recent-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.recent-card:hover .recent-img img {
  transform: scale(1.06);
}

.recent-off-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.recent-info {
  padding: 10px 12px 12px;
}

.recent-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.recent-prices {
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-wrap: wrap;
}

.recent-price-current {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-primary);
}

.recent-price-change {
  font-size: 0.75rem;
  font-weight: 600;
}

.recent-price-change.up {
  color: #ef4444;
}

.recent-price-change.down {
  color: #10b981;
}

.scroll-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--color-border);
  z-index: 5;
}

.scroll-btn--left {
  left: -8px;
}

.scroll-btn--right {
  right: -8px;
}

.seckill-banner {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 40px;
  min-height: 120px;
  cursor: pointer;
  transition: all var(--transition);
}

.seckill-banner:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}

.seckill-banner-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 50%, #f97316 100%);
}

.seckill-banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 36px;
  color: #fff;
}

.seckill-banner-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.seckill-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.02em;
}

.seckill-subtitle {
  font-size: 0.9375rem;
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
}

.seckill-banner-btn {
  font-weight: 600;
  background: #fff !important;
  color: #ef4444 !important;
  border: none !important;
}

.seckill-banner-btn:hover {
  background: rgba(255, 255, 255, 0.95) !important;
  color: #dc2626 !important;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-card {
  cursor: pointer;
  min-width: 120px;
  padding: 14px 20px;
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all var(--transition);
}

.category-card:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.product-card {
  cursor: pointer;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all var(--transition);
}

.product-card:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.product-img {
  height: 220px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-card:hover .product-img img {
  transform: scale(1.04);
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-primary);
  margin-top: 8px;
}
</style>
