<template>
  <div class="admin-browse-stats">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon :size="24" color="#6366f1"><DataAnalysis /></el-icon>
        浏览统计
      </h2>
      <el-button :icon="Refresh" @click="loadData" :loading="loading">
        刷新数据
      </el-button>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="notice"
      title="近 7 日热门浏览 Top 10 商品（不含用户个人信息，仅统计浏览次数）"
    />

    <div v-loading="loading" class="stats-content">
      <template v-if="top10.length > 0">
        <div class="stats-header">
          <div class="stats-summary">
            <div class="summary-card">
              <div class="summary-icon"><el-icon :size="28"><TrendCharts /></el-icon></div>
              <div>
                <div class="summary-label">总浏览次数</div>
                <div class="summary-value">{{ totalViews }}</div>
              </div>
            </div>
            <div class="summary-card">
              <div class="summary-icon"><el-icon :size="28" color="#ef4444"><Goods /></el-icon></div>
              <div>
                <div class="summary-label">热门商品数</div>
                <div class="summary-value">{{ top10.length }}</div>
              </div>
            </div>
            <div class="summary-card">
              <div class="summary-icon"><el-icon :size="28" color="#f59e0b"><Lightning /></el-icon></div>
              <div>
                <div class="summary-label">TOP1 浏览量</div>
                <div class="summary-value highlight">{{ top10[0]?.viewCount || 0 }}</div>
              </div>
            </div>
          </div>
        </div>

        <el-table :data="top10" stripe class="top10-table">
          <el-table-column label="排名" width="90" align="center">
            <template #default="{ $index }">
              <span
                class="rank-badge"
                :class="{
                  gold: $index === 0,
                  silver: $index === 1,
                  bronze: $index === 2,
                }"
              >
                <template v-if="$index === 0">
                  <el-icon color="#f59e0b"><Trophy /></el-icon>
                </template>
                <template v-else-if="$index === 1">
                  <el-icon color="#94a3b8"><StarFilled /></el-icon>
                </template>
                <template v-else-if="$index === 2">
                  <el-icon color="#d97706"><StarFilled /></el-icon>
                </template>
                <template v-else>
                  #{{ $index + 1 }}
                </template>
              </span>
            </template>
          </el-table-column>

          <el-table-column label="商品信息" min-width="280">
            <template #default="{ row }">
              <div class="product-cell">
                <div class="product-img-wrap">
                  <img
                    :src="row.productImage || '/images/default-product.svg'"
                    :alt="row.productName"
                    @error="$event.target.src = '/images/default-product.svg'"
                  />
                </div>
                <div class="product-meta">
                  <div class="product-name" :title="row.productName">
                    {{ row.productName }}
                  </div>
                  <div class="product-price">¥ {{ row.currentPrice }}</div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="浏览次数" width="200" align="center">
            <template #default="{ row }">
              <div class="view-count-cell">
                <el-progress
                  :percentage="getPercentage(row.viewCount)"
                  :stroke-width="12"
                  :show-text="false"
                  :color="progressColor"
                  :striped="true"
                />
                <span class="view-count-num">{{ row.viewCount }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="占比" width="140" align="center">
            <template #default="{ row }">
              <el-tag
                :type="getTagType(row.viewCount)"
                effect="light"
                size="large"
                round
              >
                {{ getPercentage(row.viewCount).toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="goProduct(row.productId)">
                查看商品
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <el-empty v-else description="暂无浏览数据" :image-size="160">
        <template #description>
          <p>近 7 日暂无商品被浏览</p>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis, Refresh, TrendCharts, Goods, Lightning, Trophy, StarFilled,
} from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()

const loading = ref(false)
const top10 = ref([])

const totalViews = computed(() =>
  top10.value.reduce((sum, r) => sum + (Number(r.viewCount) || 0), 0),
)

const maxViews = computed(() => {
  if (top10.value.length === 0) return 0
  return Math.max(...top10.value.map(r => Number(r.viewCount) || 0))
})

function getPercentage(count) {
  if (totalViews.value === 0) return 0
  return (Number(count) / totalViews.value) * 100
}

function progressColor(percentage) {
  if (percentage >= 30) return '#ef4444'
  if (percentage >= 15) return '#f59e0b'
  if (percentage >= 8) return '#3b82f6'
  return '#10b981'
}

function getTagType(count) {
  const pct = getPercentage(count)
  if (pct >= 30) return 'danger'
  if (pct >= 15) return 'warning'
  if (pct >= 8) return 'primary'
  return 'success'
}

function goProduct(pid) {
  router.push({ name: 'ProductDetail', params: { id: pid } })
}

async function loadData() {
  loading.value = true
  try {
    const res = await api.get('/admin/view-history/top10')
    if (res.data.code === 200) {
      top10.value = (res.data.data || []).map(r => ({
        ...r,
        viewCount: Number(r.viewCount) || 0,
      }))
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-browse-stats {
  padding-bottom: 48px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  letter-spacing: -0.02em;
}

.notice {
  margin-bottom: 24px;
}

.stats-content {
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  padding: 24px;
  min-height: 400px;
}

.stats-header {
  margin-bottom: 24px;
}

.stats-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #fff 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.summary-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.summary-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%);
  color: #7c3aed;
  border-radius: var(--radius-sm);
}

.summary-label {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-bottom: 2px;
}

.summary-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.summary-value.highlight {
  color: #ef4444;
}

.top10-table {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-weight: 700;
  font-size: 0.875rem;
  background: #f1f5f9;
  color: #64748b;
  border: 2px solid #e2e8f0;
}

.rank-badge.gold {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
  border-color: #f59e0b;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.25);
}

.rank-badge.silver {
  background: linear-gradient(135deg, #f1f5f9 0%, #cbd5e1 100%);
  color: #475569;
  border-color: #94a3b8;
  box-shadow: 0 2px 8px rgba(148, 163, 184, 0.25);
}

.rank-badge.bronze {
  background: linear-gradient(135deg, #ffedd5 0%, #fed7aa 100%);
  color: #9a3412;
  border-color: #ea580c;
  box-shadow: 0 2px 8px rgba(234, 88, 12, 0.25);
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-img-wrap {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.product-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-meta {
  min-width: 0;
  flex: 1;
}

.product-name {
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-primary);
}

.view-count-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
}

.view-count-cell :deep(.el-progress) {
  flex: 1;
  min-width: 100px;
}

.view-count-num {
  font-weight: 700;
  color: var(--color-text);
  font-size: 1rem;
  min-width: 48px;
  text-align: right;
}
</style>
