<template>
  <div class="points-center-page">
    <h2 class="page-title">积分中心</h2>

    <div v-loading="loadingSummary" class="summary-card">
      <div class="summary-left">
        <div class="points-label">当前积分</div>
        <div class="points-value">{{ summary.points || 0 }}</div>
      </div>
      <div class="summary-right">
        <el-tag size="large" type="warning" effect="dark" round class="level-tag">
          {{ summary.level?.levelName || 'Lv1 新手' }}
        </el-tag>
        <div class="level-benefits">{{ summary.level?.benefits || '基础权益：积分1倍累计' }}</div>
      </div>
    </div>

    <div v-loading="loadingLevel" class="content-card level-progress-card">
      <div class="level-header">
        <div class="level-current">
          <span class="level-label">当前等级</span>
          <span class="level-name">{{ levelInfo.levelName || 'Lv1 新手' }}</span>
        </div>
        <div class="level-next">
          <template v-if="levelInfo.nextLevelThreshold">
            <span class="level-label">下一等级</span>
            <span class="level-name level-name--next">{{ levelInfo.nextLevelName }}</span>
          </template>
          <template v-else>
            <span class="level-name level-name--max">{{ levelInfo.nextLevelName || '已达最高等级' }}</span>
          </template>
        </div>
      </div>

      <div class="progress-section">
        <el-progress
          :percentage="progressPercent"
          :stroke-width="14"
          :show-text="false"
          color="var(--color-primary)"
        />
        <div class="progress-meta">
          <span class="progress-current">累计消费 ¥{{ formatMoney(levelInfo.totalConsume) }}</span>
          <span class="progress-percent">{{ progressPercent }}%</span>
          <template v-if="levelInfo.nextLevelThreshold">
            <span class="progress-next">距下一等级还差 ¥{{ formatMoney(diffAmount) }}</span>
          </template>
          <template v-else>
            <span class="progress-max">已达最高等级</span>
          </template>
        </div>
      </div>
    </div>

    <div class="content-card benefits-card">
      <h3 class="section-title">会员等级权益</h3>
      <div class="benefits-grid">
        <div
          v-for="lv in levelList"
          :key="lv.level"
          class="benefit-item"
          :class="{ 'benefit-item--active': (summary.level?.level || 1) >= lv.level }"
        >
          <div class="benefit-level">
            <span class="benefit-level-text">Lv{{ lv.level }}</span>
            <span class="benefit-level-name">{{ lv.name.replace(/^Lv\d\s*/, '') }}</span>
          </div>
          <div class="benefit-threshold">
            累计消费 ¥{{ formatMoney(lv.threshold) }}
          </div>
          <div class="benefit-desc">{{ lv.benefits }}</div>
          <el-tag
            v-if="(summary.level?.level || 1) === lv.level"
            size="small"
            type="success"
            effect="dark"
            class="benefit-current-tag"
          >
            当前等级
          </el-tag>
        </div>
      </div>
    </div>

    <div class="content-card logs-card">
      <h3 class="section-title">积分流水</h3>
      <el-table
        v-loading="loadingLogs"
        :data="logs.list"
        stripe
        style="width: 100%"
        :empty-text="'暂无积分流水'"
      >
        <el-table-column label="变动积分" width="140" align="center">
          <template #default="{ row }">
            <span :class="row.changePoints >= 0 ? 'points-positive' : 'points-negative'">
              {{ row.changePoints >= 0 ? '+' : '' }}{{ row.changePoints }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="logTypeTagType(row.type)" size="small">
              {{ logTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="120" align="center">
          <template #default="{ row }">
            {{ row.balance }}
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="200">
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div v-if="logs.total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="logs.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '../api'

const loadingSummary = ref(true)
const loadingLevel = ref(true)
const loadingLogs = ref(true)

const summary = reactive({
  points: 0,
  level: null
})

const levelInfo = reactive({
  level: 1,
  levelName: '',
  totalConsume: 0,
  nextLevelThreshold: null,
  progress: 0,
  pointRate: 1,
  nextLevelName: '',
  benefits: ''
})

const logs = reactive({
  list: [],
  total: 0,
  page: 1,
  size: 20
})

const pagination = reactive({
  page: 1,
  size: 20
})

const levelList = [
  { level: 1, name: 'Lv1 新手', threshold: 0, benefits: '积分1倍累计' },
  { level: 2, name: 'Lv2 铜牌', threshold: 500, benefits: '积分1.2倍累计' },
  { level: 3, name: 'Lv3 银牌', threshold: 2000, benefits: '积分1.5倍累计' },
  { level: 4, name: 'Lv4 金牌', threshold: 5000, benefits: '积分2倍累计' },
  { level: 5, name: 'Lv5 钻石', threshold: 20000, benefits: '积分3倍累计，专属客服' }
]

const progressPercent = computed(() => {
  if (!levelInfo.progress) return 0
  return Math.round(Number(levelInfo.progress) * 100)
})

const diffAmount = computed(() => {
  if (!levelInfo.nextLevelThreshold) return 0
  return Math.max(0, Number(levelInfo.nextLevelThreshold) - Number(levelInfo.totalConsume || 0))
})

function logTypeText(type) {
  const map = {
    1: '收货奖励',
    2: '下单抵扣',
    3: '兑换扣减',
    4: '管理员调整',
    5: '退款退回'
  }
  return map[type] || '未知'
}

function logTypeTagType(type) {
  const map = {
    1: 'success',
    2: 'warning',
    3: 'danger',
    4: 'info',
    5: 'success'
  }
  return map[type] || 'info'
}

function formatMoney(val) {
  if (val === null || val === undefined || val === '') return '0.00'
  return Number(val).toFixed(2)
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadSummary() {
  loadingSummary.value = true
  try {
    const res = await api.get('/points/summary')
    if (res.data.code === 200) {
      const data = res.data.data || {}
      summary.points = data.points || 0
      summary.level = data.level || null
      if (data.level) {
        Object.assign(levelInfo, data.level)
      }
    }
  } finally {
    loadingSummary.value = false
    loadingLevel.value = false
  }
}

async function loadLevel() {
  loadingLevel.value = true
  try {
    const res = await api.get('/points/level')
    if (res.data.code === 200) {
      Object.assign(levelInfo, res.data.data || {})
    }
  } finally {
    loadingLevel.value = false
  }
}

async function loadLogs() {
  loadingLogs.value = true
  try {
    const res = await api.get('/points/logs', {
      params: { page: pagination.page, size: pagination.size }
    })
    if (res.data.code === 200) {
      const data = res.data.data || {}
      logs.list = data.list || []
      logs.total = data.total || 0
      logs.page = data.page || 1
      logs.size = data.size || 20
    }
  } finally {
    loadingLogs.value = false
  }
}

onMounted(async () => {
  await loadSummary()
  await loadLogs()
})
</script>

<style scoped>
.points-center-page {
  padding-bottom: 32px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.summary-card {
  background: linear-gradient(135deg, var(--color-primary) 0%, #0d9488 100%);
  border-radius: var(--radius-lg);
  padding: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  box-shadow: var(--shadow-lg);
  position: relative;
  overflow: hidden;
}

.summary-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.summary-left {
  position: relative;
  z-index: 1;
}

.points-label {
  font-size: 0.9375rem;
  opacity: 0.85;
  margin-bottom: 8px;
}

.points-value {
  font-size: 3rem;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
}

.summary-right {
  text-align: right;
  position: relative;
  z-index: 1;
}

.level-tag {
  font-size: 1rem;
  padding: 4px 16px;
  margin-bottom: 12px;
}

.level-benefits {
  font-size: 0.9375rem;
  opacity: 0.9;
}

.level-progress-card {
  padding: 28px 32px;
}

.level-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.level-current,
.level-next {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.level-label {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.level-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-primary);
}

.level-name--next {
  color: var(--color-text-secondary);
}

.level-name--max {
  color: var(--color-text-muted);
  font-size: 1rem;
}

.progress-section {
  padding: 0 4px;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  font-size: 0.875rem;
}

.progress-current {
  color: var(--color-text-secondary);
}

.progress-percent {
  font-weight: 600;
  color: var(--color-primary);
}

.progress-next {
  color: var(--color-text-muted);
}

.progress-max {
  color: var(--color-text-muted);
  font-weight: 500;
}

.benefits-card,
.logs-card {
  padding: 28px 32px;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 20px 0;
}

.benefits-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.benefit-item {
  position: relative;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  transition: all var(--transition);
}

.benefit-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.benefit-item--active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.benefit-level {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.benefit-level-text {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-primary);
}

.benefit-level-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
}

.benefit-threshold {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-bottom: 10px;
}

.benefit-desc {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.benefit-current-tag {
  position: absolute;
  top: 12px;
  right: 12px;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.points-positive {
  color: #52c41a;
  font-weight: 600;
  font-size: 1rem;
}

.points-negative {
  color: #f5222d;
  font-weight: 600;
  font-size: 1rem;
}
</style>
