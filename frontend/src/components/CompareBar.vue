<template>
  <div v-if="visible" class="compare-bar">
    <div class="compare-bar-inner">
      <div class="compare-bar-header">
        <div class="compare-bar-title">
          <el-icon :size="18"><ScaleToOriginal /></el-icon>
          <span>商品对比</span>
          <el-tag size="small" type="primary" effect="dark" round>
            {{ compareStore.count }}/{{ compareStore.maxItems }}
          </el-tag>
        </div>
        <div class="compare-bar-actions">
          <el-button
            v-if="compareStore.count > 0"
            link
            type="danger"
            :icon="Delete"
            @click="handleClear"
          >
            清空
          </el-button>
          <el-button
            link
            type="primary"
            :icon="ArrowDown"
            @click="toggleExpand"
          >
            {{ expanded ? '收起' : '展开' }}
          </el-button>
        </div>
      </div>
      <transition name="slide">
        <div v-show="expanded" class="compare-bar-content">
          <div class="compare-slots">
            <div
              v-for="i in compareStore.maxItems"
              :key="i"
              class="compare-slot"
              :class="{ 'is-empty': !compareStore.getSlot(i - 1) }"
            >
              <template v-if="compareStore.getSlot(i - 1)">
                <div class="slot-product">
                  <el-button
                    class="slot-remove"
                    size="small"
                    circle
                    type="danger"
                    plain
                    :icon="Close"
                    @click.stop="handleRemove(compareStore.getSlot(i - 1).id)"
                  />
                  <div class="slot-img" @click="goDetail(compareStore.getSlot(i - 1).id)">
                    <img
                      :src="compareStore.getSlot(i - 1).mainImage || '/images/default-product.svg'"
                      alt=""
                      @error="$event.target.src = '/images/default-product.svg'"
                    />
                  </div>
                  <div
                    class="slot-name"
                    :title="compareStore.getSlot(i - 1).name"
                    @click="goDetail(compareStore.getSlot(i - 1).id)"
                  >
                    {{ compareStore.getSlot(i - 1).name }}
                  </div>
                  <div class="slot-price">¥ {{ compareStore.getSlot(i - 1).price }}</div>
                  <el-button
                    size="small"
                    type="primary"
                    @click="goDetail(compareStore.getSlot(i - 1).id)"
                  >
                    去购买
                  </el-button>
                </div>
              </template>
              <template v-else>
                <div class="slot-empty" @click="openSearch">
                  <el-icon :size="28" class="slot-empty-icon"><Plus /></el-icon>
                  <span>添加商品</span>
                </div>
              </template>
            </div>
          </div>
          <div class="compare-bar-footer">
            <el-button
              type="primary"
              :disabled="compareStore.count < 2"
              @click="goCompare"
            >
              开始对比 ({{ compareStore.count }})
            </el-button>
            <el-text type="info" size="small">
              {{ compareStore.count < 2 ? '请至少添加 2 个商品进行对比' : '已满足对比条件' }}
            </el-text>
          </div>
        </div>
      </transition>
    </div>
    <CompareSearchDialog v-model="searchVisible" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  ScaleToOriginal,
  Delete,
  ArrowDown,
  ArrowUp,
  Close,
  Plus,
} from '@element-plus/icons-vue'
import { useCompareStore } from '../stores/compare'
import CompareSearchDialog from './CompareSearchDialog.vue'

const compareStore = useCompareStore()
const router = useRouter()
const route = useRoute()
const expanded = ref(true)
const searchVisible = ref(false)

const visible = computed(() => {
  const hiddenRoutes = ['Login', 'Register']
  return !hiddenRoutes.includes(route.name)
})

function toggleExpand() {
  expanded.value = !expanded.value
}

async function handleClear() {
  if (compareStore.count === 0) return
  try {
    await ElMessageBox.confirm('确定要清空对比栏吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    compareStore.clearAll()
    ElMessage.success('已清空对比栏')
  } catch {}
}

function handleRemove(productId) {
  compareStore.removeProduct(productId)
  ElMessage.success('已移除')
}

function goDetail(productId) {
  router.push(`/products/${productId}`)
}

function goCompare() {
  router.push('/compare')
}

function openSearch() {
  searchVisible.value = true
}
</script>

<style scoped>
.compare-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
}

.compare-bar-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
}

.compare-bar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
}

.compare-bar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
}

.compare-bar-actions {
  display: flex;
  gap: 4px;
}

.compare-bar-content {
  padding-bottom: 16px;
}

.compare-slots {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.compare-slot {
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-md);
  min-height: 220px;
  display: flex;
  align-items: stretch;
  transition: all var(--transition);
}

.compare-slot.is-empty {
  background: var(--color-bg);
}

.slot-product {
  width: 100%;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
}

.slot-remove {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
}

.slot-img {
  width: 90px;
  height: 90px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}

.slot-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slot-name {
  font-size: 0.8125rem;
  color: var(--color-text);
  text-align: center;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
  width: 100%;
}

.slot-name:hover {
  color: var(--color-primary);
}

.slot-price {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-primary);
}

.slot-empty {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all var(--transition);
}

.slot-empty:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.slot-empty-icon {
  color: inherit;
}

.compare-bar-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: center;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
