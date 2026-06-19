<template>
  <div class="seckill-list">
    <div class="page-header">
      <div class="header-bg" />
      <div class="header-content">
        <h1 class="title">
          <el-icon :size="28"><Lightning /></el-icon>
          限时秒杀
        </h1>
        <p class="subtitle">超值特价，数量有限，先到先得！</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="seckill-tabs">
      <el-tab-pane label="正在进行" name="ongoing" />
      <el-tab-pane label="即将开始" name="upcoming" />
    </el-tabs>

    <div v-loading="loading" class="session-list">
      <template v-if="filteredSessions.length > 0">
        <div
          v-for="session in filteredSessions"
          :key="session.id"
          class="session-card"
          @click="goDetail(session.id)"
        >
          <div class="session-img">
            <img
              :src="session.productImage || '/images/default-product.svg'"
              alt=""
              @error="$event.target.src = '/images/default-product.svg'"
            />
            <div class="status-tag" :class="getStatusClass(session.activityStatus)">
              {{ getStatusText(session.activityStatus) }}
            </div>
          </div>
          <div class="session-info">
            <h3 class="product-name">{{ session.productName }}</h3>
            <div class="price-row">
              <span class="seckill-price">¥ {{ session.seckillPrice }}</span>
              <span class="original-price">¥ {{ session.originalPrice }}</span>
              <el-tag type="danger" size="small" effect="dark" round>
                省¥{{ formatDiscount(session) }}
              </el-tag>
            </div>
            <div class="progress-row">
              <el-progress
                :percentage="session.soldPercent"
                :stroke-width="10"
                color="#ef4444"
                :text-inside="true"
              />
              <span class="sold-text">
                已售 {{ session.soldStock }} / {{ session.totalStock }}
              </span>
            </div>
            <div class="meta-row">
              <div class="countdown" v-if="session.activityStatus === 1">
                <el-icon :size="16"><Timer /></el-icon>
                距结束：<span class="cd-num">{{ formatCountdown(getEndRemain(session.id)) }}</span>
              </div>
              <div class="countdown" v-else-if="session.activityStatus === 0">
                <el-icon :size="16"><Clock /></el-icon>
                距开始：<span class="cd-num">{{ formatCountdown(getStartRemain(session.id)) }}</span>
              </div>
              <div class="limit">
                <el-tag size="small" type="warning">每人限购 {{ session.perUserLimit }} 件</el-tag>
              </div>
            </div>
            <el-button
              class="action-btn"
              type="danger"
              size="large"
              :disabled="session.activityStatus === 3 || session.activityStatus === 2 || session.activityStatus === 4"
            >
              {{ getButtonText(session.activityStatus) }}
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无秒杀活动" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lightning, Timer, Clock } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const loading = ref(true)
const sessions = ref([])
const activeTab = ref('ongoing')
const endCounters = reactive({})
const startCounters = reactive({})
let tickTimer = null
let fetchTimer = null

const filteredSessions = computed(() => {
  if (activeTab.value === 'upcoming') {
    return sessions.value.filter(s => s.activityStatus === 0)
  }
  return sessions.value.filter(s => s.activityStatus === 1)
})

function formatDiscount(s) {
  return (s.originalPrice - s.seckillPrice).toFixed(2)
}

function getStatusText(status) {
  const map = { 0: '即将开始', 1: '抢购中', 2: '已结束', 3: '已售罄', 4: '已下架' }
  return map[status] || '未知'
}

function getStatusClass(status) {
  const map = { 0: 'status-upcoming', 1: 'status-ongoing', 2: 'status-ended', 3: 'status-soldout', 4: 'status-disabled' }
  return map[status] || ''
}

function getButtonText(status) {
  const map = { 0: '即将开始', 1: '立即抢购', 2: '已结束', 3: '已售罄', 4: '已下架' }
  return map[status] || '查看详情'
}

function formatCountdown(total) {
  if (!total || total <= 0) return '00:00:00'
  total = Math.floor(total)
  const d = Math.floor(total / 86400)
  const h = Math.floor((total % 86400) / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n) => String(Math.floor(n)).padStart(2, '0')
  if (d > 0) return d + '天' + pad(h) + ':' + pad(m) + ':' + pad(s)
  return pad(h) + ':' + pad(m) + ':' + pad(s)
}

function getEndRemain(id) {
  return endCounters[id] ? endCounters[id].end : 0
}
function getStartRemain(id) {
  return startCounters[id] ? startCounters[id].start : 0
}

function goDetail(id) {
  router.push('/seckill/' + id)
}

async function fetchSessions() {
  try {
    const res = await api.get('/seckill/sessions')
    if (res.data.code === 200) {
      sessions.value = res.data.data || []
      sessions.value.forEach(s => {
        if (s.activityStatus === 1) {
          if (!endCounters[s.id]) endCounters[s.id] = { end: s.remainSeconds }
          else endCounters[s.id].end = s.remainSeconds
        } else if (s.activityStatus === 0) {
          if (!startCounters[s.id]) startCounters[s.id] = { start: s.remainSeconds }
          else startCounters[s.id].start = s.remainSeconds
        }
      })
      const hasOngoing = sessions.value.some(s => s.activityStatus === 1)
      if (!hasOngoing && activeTab.value === 'ongoing') activeTab.value = 'upcoming'
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

function tickCounters() {
  Object.keys(endCounters).forEach(id => {
    if (endCounters[id].end > 0) endCounters[id].end--
  })
  Object.keys(startCounters).forEach(id => {
    if (startCounters[id].start > 0) startCounters[id].start--
  })
}

onMounted(() => {
  fetchSessions()
  tickTimer = setInterval(tickCounters, 1000)
  fetchTimer = setInterval(fetchSessions, 30000)
})

onBeforeUnmount(() => {
  if (tickTimer) clearInterval(tickTimer)
  if (fetchTimer) clearInterval(fetchTimer)
})
</script>

<style scoped>
.seckill-list { padding-bottom: 48px; }
.page-header {
  position: relative; border-radius: var(--radius-lg); overflow: hidden;
  margin-bottom: 32px; min-height: 140px;
  display: flex; align-items: center; justify-content: center;
}
.header-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 50%, #b91c1c 100%);
}
.header-content { position: relative; z-index: 1; text-align: center; padding: 32px 24px; color: #fff; }
.title {
  display: inline-flex; align-items: center; gap: 10px;
  font-size: 2rem; font-weight: 700; margin: 0 0 8px 0; letter-spacing: -0.02em;
}
.subtitle { font-size: 1rem; margin: 0; color: rgba(255,255,255,0.9); }
.seckill-tabs { margin-bottom: 24px; }
.session-list {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 20px;
}
.session-card {
  cursor: pointer; background: var(--color-surface);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg);
  overflow: hidden; display: flex; transition: all var(--transition);
}
.session-card:hover {
  border-color: #ef4444; box-shadow: 0 8px 24px rgba(239,68,68,0.15); transform: translateY(-2px);
}
.session-img {
  width: 180px; min-width: 180px; height: auto; background: var(--color-bg);
  position: relative; display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.session-img img { width: 100%; height: 100%; object-fit: cover; }
.status-tag {
  position: absolute; top: 8px; left: 8px; padding: 4px 10px; font-size: 12px;
  font-weight: 600; border-radius: 12px; color: #fff;
}
.status-upcoming { background: #f59e0b; }
.status-ongoing {
  background: #ef4444; animation: pulse-tag 2s ease-in-out infinite;
}
@keyframes pulse-tag { 0%,100% { opacity:1; } 50% { opacity:0.75; } }
.status-ended, .status-soldout, .status-disabled { background: #6b7280; }
.session-info { flex: 1; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.product-name {
  font-size: 1rem; font-weight: 600; color: var(--color-text); margin: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.price-row { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
.seckill-price { font-size: 1.5rem; font-weight: 700; color: #ef4444; }
.original-price { font-size: 0.875rem; color: #9ca3af; text-decoration: line-through; }
.progress-row { display: flex; flex-direction: column; gap: 4px; }
.sold-text { font-size: 0.75rem; color: var(--color-text-muted); text-align: right; }
.meta-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.countdown {
  display: inline-flex; align-items: center; gap: 4px; font-size: 0.875rem;
  color: var(--color-text-secondary); font-weight: 500;
}
.cd-num { font-family: 'Courier New', monospace; font-weight: 700; color: #ef4444; }
.action-btn { margin-top: 4px; width: 100%; }
</style>
