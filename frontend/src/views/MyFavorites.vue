<template>
  <div class="my-favorites">
    <div class="page-header">
      <h2 class="page-title">我的收藏</h2>
      <div class="header-actions">
        <el-button :icon="FolderAdd" type="primary" @click="openCreateGroup">新建分组</el-button>
        <el-button
          v-if="selectMode"
          type="success"
          :disabled="selectedIds.length === 0"
          @click="openBatchMove"
        >
          批量移组 ({{ selectedIds.length }})
        </el-button>
        <el-button
          v-if="selectMode"
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="batchRemove"
        >
          批量取消 ({{ selectedIds.length }})
        </el-button>
        <el-button :type="selectMode ? 'warning' : 'default'" @click="selectMode = !selectMode">
          {{ selectMode ? '退出选择' : '批量管理' }}
        </el-button>
      </div>
    </div>

    <div class="layout">
      <aside class="sidebar">
        <div class="group-list">
          <div
            class="group-item"
            :class="{ active: activeTab === 'all' }"
            @click="switchTab('all')"
          >
            <el-icon><Folder /></el-icon>
            <span class="group-name">全部收藏</span>
            <el-tag size="small" type="info" effect="plain">{{ totalCount }}</el-tag>
          </div>
          <div
            class="group-item"
            :class="{ active: activeTab === 'ungrouped' }"
            @click="switchTab('ungrouped')"
          >
            <el-icon><Document /></el-icon>
            <span class="group-name">未分组</span>
            <el-tag size="small" type="info" effect="plain">{{ ungroupedCount }}</el-tag>
          </div>
          <el-divider class="group-divider" />
          <div
            v-for="g in groups"
            :key="g.id"
            class="group-item group-item--custom"
            :class="{
              active: activeTab === 'group' && activeGroupId === g.id,
              'drag-over': dragOverGroupId === g.id,
            }"
            @click="switchGroup(g.id)"
            draggable="false"
            @dragover.prevent="onGroupDragOver(g.id)"
            @dragleave="onGroupDragLeave"
            @drop.stop.prevent="onGroupDrop(g.id)"
          >
            <el-icon><FolderOpened /></el-icon>
            <span class="group-name">{{ g.name }}</span>
            <el-tag size="small" type="info" effect="plain">{{ g.count }}</el-tag>
            <el-dropdown trigger="click" @click.stop class="group-menu">
              <el-icon class="menu-icon"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="Edit" @click.stop="openRenameGroup(g)">重命名</el-dropdown-item>
                  <el-dropdown-item :icon="Delete" divided type="danger" @click.stop="deleteGroup(g)">删除分组</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <el-empty v-if="groups.length === 0" description="暂无自定义分组" :image-size="80" />
        </div>
      </aside>

      <section class="main-content" v-loading="loading">
        <div v-if="activeTab === 'all' || activeTab === 'ungrouped'" class="tab-bar">
          <div
            class="tab-item"
            :class="{ active: activeTab === 'all' }"
            @click="switchTab('all')"
          >
            全部
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'ungrouped' }"
            @click="switchTab('ungrouped')"
          >
            未分组
            <el-tag size="small" type="info" effect="plain" style="margin-left:4px">{{ ungroupedCount }}</el-tag>
          </div>
        </div>

        <div v-if="selectMode && items.length > 0" class="select-bar">
          <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll">
            全选
          </el-checkbox>
          <span class="select-hint">已选 {{ selectedIds.length }} 项</span>
        </div>

        <div v-if="items.length > 0" class="fav-grid">
          <div
            v-for="item in items"
            :key="item.productId"
            class="fav-card"
            :class="{
              'off-shelf': item.offShelf,
              'selected': selectedIds.includes(item.productId),
            }"
            draggable="true"
            @dragstart="onDragStart($event, item)"
            @dragend="onDragEnd"
          >
            <div v-if="selectMode" class="select-check" @click.stop="toggleSelect(item.productId)">
              <el-checkbox :model-value="selectedIds.includes(item.productId)" @click.stop />
            </div>

            <div
              v-if="item.priceDownFlag"
              class="price-down-badge"
            >
              降价
            </div>
            <div
              v-if="item.offShelf"
              class="off-shelf-mask"
            >
              <span class="off-shelf-text">已下架</span>
            </div>

            <div class="card-img" @click="goDetail(item.productId)">
              <img
                :src="item.productImage || '/images/default-product.svg'"
                :alt="item.productName"
                @error="$event.target.src = '/images/default-product.svg'"
              />
            </div>

            <div class="card-body">
              <h4 class="card-name" @click="goDetail(item.productId)">{{ item.productName }}</h4>
              <div class="price-row">
                <span class="current-price">¥ {{ item.currentPrice }}</span>
                <span class="favor-price-label">收藏价</span>
                <span class="favor-price">¥ {{ item.favorPrice }}</span>
              </div>
              <div class="change-row">
                <span
                  class="change-value"
                  :class="{
                    down: (item.priceChangePercent || 0) < 0,
                    up: (item.priceChangePercent || 0) > 0,
                  }"
                >
                  <template v-if="(item.priceChangePercent || 0) > 0">
                    ↑ +{{ formatPercent(item.priceChangePercent) }}
                  </template>
                  <template v-else-if="(item.priceChangePercent || 0) < 0">
                    ↓ {{ formatPercent(item.priceChangePercent) }}
                  </template>
                  <template v-else>
                    — 持平
                  </template>
                </span>
                <span class="stock-info" :class="{ 'out': item.stock < 1 }">
                  {{ item.stock < 1 ? '无货' : `库存 ${item.stock}` }}
                </span>
              </div>
              <div v-if="item.hasPriceAlert" class="alert-info">
                <el-icon :size="12" color="#f59e0b"><Bell /></el-icon>
                <span>目标价 ¥{{ item.alertTargetPrice }}</span>
              </div>
              <div class="card-actions">
                <template v-if="!item.offShelf">
                  <el-button size="small" type="primary" :disabled="item.stock < 1" @click="addToCart(item)">
                    加入购物车
                  </el-button>
                </template>
                <template v-else>
                  <el-button size="small" type="primary" plain @click="findSimilar(item)">
                    找相似
                  </el-button>
                </template>
                <el-dropdown trigger="click" @click.stop>
                  <el-button size="small">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :icon="FolderOpened" @click.stop="openMoveToGroup(item)">移至分组</el-dropdown-item>
                      <el-dropdown-item :icon="Bell" @click.stop="openPriceAlert(item)">目标价提醒</el-dropdown-item>
                      <el-dropdown-item :icon="Delete" divided type="danger" @click.stop="removeItem(item)">取消收藏</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-else-if="!loading" description="暂无收藏的商品" :image-size="120">
          <template #description>
            <p>去逛逛发现喜欢的商品吧～</p>
            <el-button type="primary" link @click="$router.push({ name: 'ProductList' })">去商品列表</el-button>
          </template>
        </el-empty>
      </section>
    </div>

    <el-dialog v-model="createGroupVisible" title="新建分组" width="420px">
      <el-form :model="groupForm" label-width="80px">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" maxlength="20" placeholder="如：数码、服饰" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createGroupVisible = false">取消</el-button>
        <el-button type="primary" :loading="groupSubmitting" @click="submitCreateGroup">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameGroupVisible" title="重命名分组" width="420px">
      <el-form :model="groupForm" label-width="80px">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" maxlength="20" placeholder="请输入新名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameGroupVisible = false">取消</el-button>
        <el-button type="primary" :loading="groupSubmitting" @click="submitRenameGroup">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveGroupVisible" title="移至分组" width="420px">
      <el-form label-width="80px">
        <el-form-item label="目标分组">
          <el-select v-model="moveGroupId" placeholder="请选择分组" style="width: 100%">
            <el-option :value="null" label="未分组" />
            <el-option v-for="g in groups" :key="g.id" :value="g.id" :label="g.name" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveGroupVisible = false">取消</el-button>
        <el-button type="primary" :loading="moveSubmitting" @click="submitMoveGroup">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="alertDialogVisible" title="设置目标价提醒" width="420px">
      <el-form :model="alertForm" label-width="100px">
        <el-form-item label="商品">
          <span class="alert-product-name">{{ alertProduct?.productName }}</span>
        </el-form-item>
        <el-form-item label="当前价格">
          <span class="current-price-tag">¥ {{ alertProduct?.currentPrice }}</span>
        </el-form-item>
        <el-form-item label="收藏价格">
          <span>¥ {{ alertProduct?.favorPrice }}</span>
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
          <p class="hint">当商品价格 ≤ 目标价时，将触发提醒</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="alertProduct?.hasPriceAlert" type="danger" @click="cancelAlert">取消提醒</el-button>
        <el-button @click="alertDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="alertSubmitting" @click="submitAlert">确认设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderAdd, Folder, FolderOpened, Document, MoreFilled,
  Edit, Delete, Bell,
} from '@element-plus/icons-vue'
import api from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activeTab = ref('all')
const activeGroupId = ref(null)
const groups = ref([])
const allItems = ref([])
const selectMode = ref(false)
const selectedIds = ref([])

const createGroupVisible = ref(false)
const renameGroupVisible = ref(false)
const moveGroupVisible = ref(false)
const moveGroupId = ref(null)
const moveSingleItem = ref(null)
const moveSubmitting = ref(false)
const groupSubmitting = ref(false)
const groupForm = reactive({ name: '', renameId: null })

const alertDialogVisible = ref(false)
const alertProduct = ref(null)
const alertSubmitting = ref(false)
const alertForm = reactive({ targetPrice: null })

const dragItem = ref(null)
const dragOverGroupId = ref(null)

const items = computed(() => {
  if (activeTab.value === 'all') return allItems.value
  if (activeTab.value === 'ungrouped') return allItems.value.filter((i) => i.groupId == null)
  if (activeTab.value === 'group') return allItems.value.filter((i) => i.groupId === activeGroupId.value)
  return []
})

const totalCount = computed(() => allItems.value.length)
const ungroupedCount = computed(() => allItems.value.filter((i) => i.groupId == null).length)

const isAllSelected = computed(() => items.value.length > 0 && items.value.every((i) => selectedIds.value.includes(i.productId)))
const isIndeterminate = computed(() => {
  const count = items.value.filter((i) => selectedIds.value.includes(i.productId)).length
  return count > 0 && count < items.value.length
})

function formatPercent(v) {
  if (v == null) return '0.00%'
  const n = Number(v)
  return (n >= 0 ? '+' : '') + n.toFixed(2) + '%'
}

async function loadAll() {
  loading.value = true
  try {
    const [gRes, fRes] = await Promise.all([
      api.get('/favorites/groups'),
      api.get('/favorites'),
    ])
    if (gRes.data.code === 200) groups.value = gRes.data.data || []
    if (fRes.data.code === 200) allItems.value = fRes.data.data || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  activeGroupId.value = null
  clearSelection()
}

function switchGroup(gid) {
  activeTab.value = 'group'
  activeGroupId.value = gid
  clearSelection()
}

function clearSelection() {
  selectedIds.value = []
}

function toggleSelect(pid) {
  const idx = selectedIds.value.indexOf(pid)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(pid)
}

function toggleSelectAll(val) {
  if (val) {
    selectedIds.value = items.value.map((i) => i.productId)
  } else {
    selectedIds.value = []
  }
}

function openCreateGroup() {
  groupForm.name = ''
  groupForm.renameId = null
  createGroupVisible.value = true
}

async function submitCreateGroup() {
  if (!groupForm.name || !groupForm.name.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  groupSubmitting.value = true
  try {
    await api.post('/favorites/groups', { name: groupForm.name.trim() })
    ElMessage.success('分组创建成功')
    createGroupVisible.value = false
    await loadAll()
  } catch (e) {
  } finally {
    groupSubmitting.value = false
  }
}

function openRenameGroup(g) {
  groupForm.name = g.name
  groupForm.renameId = g.id
  renameGroupVisible.value = true
}

async function submitRenameGroup() {
  if (!groupForm.name || !groupForm.name.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  groupSubmitting.value = true
  try {
    await api.put(`/favorites/groups/${groupForm.renameId}`, { name: groupForm.name.trim() })
    ElMessage.success('重命名成功')
    renameGroupVisible.value = false
    await loadAll()
  } catch (e) {
  } finally {
    groupSubmitting.value = false
  }
}

async function deleteGroup(g) {
  try {
    await ElMessageBox.confirm(
      `确定删除分组「${g.name}」吗？分组内的商品将移至「未分组」。`,
      '删除分组',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await api.delete(`/favorites/groups/${g.id}`)
    ElMessage.success('分组已删除')
    if (activeTab.value === 'group' && activeGroupId.value === g.id) {
      activeTab.value = 'all'
      activeGroupId.value = null
    }
    await loadAll()
  } catch (e) {
  }
}

function openMoveToGroup(item) {
  moveSingleItem.value = item
  moveGroupId.value = item.groupId
  moveGroupVisible.value = true
}

function openBatchMove() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择商品')
    return
  }
  moveSingleItem.value = null
  moveGroupId.value = null
  moveGroupVisible.value = true
}

async function submitMoveGroup() {
  moveSubmitting.value = true
  try {
    if (moveSingleItem.value) {
      await api.put('/favorites/move', {
        productId: moveSingleItem.value.productId,
        groupId: moveGroupId.value,
      })
      ElMessage.success('已移动至目标分组')
    } else {
      await api.post('/favorites/batch-move', {
        productIds: selectedIds.value,
        groupId: moveGroupId.value,
      })
      ElMessage.success(`已将 ${selectedIds.value.length} 个商品移动`)
      clearSelection()
    }
    moveGroupVisible.value = false
    await loadAll()
  } catch (e) {
  } finally {
    moveSubmitting.value = false
  }
}

async function removeItem(item) {
  try {
    await ElMessageBox.confirm(`确定取消收藏「${item.productName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await api.delete(`/favorites/${item.productId}`)
    ElMessage.success('已取消收藏')
    userStore.favoriteCount = Math.max(0, (userStore.favoriteCount || 0) - 1)
    await loadAll()
  } catch (e) {
  }
}

async function batchRemove() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择商品')
    return
  }
  try {
    await ElMessageBox.confirm(`确定取消收藏选中的 ${selectedIds.value.length} 个商品吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await api.post('/favorites/batch-remove', { productIds: selectedIds.value })
    ElMessage.success('批量取消成功')
    userStore.favoriteCount = Math.max(0, (userStore.favoriteCount || 0) - selectedIds.value.length)
    clearSelection()
    await loadAll()
  } catch (e) {
  }
}

async function addToCart(item) {
  try {
    await api.post('/cart/add', { productId: item.productId, quantity: 1 })
    ElMessage.success('已加入购物车')
    userStore.cartCount = (userStore.cartCount || 0) + 1
  } catch (e) {
  }
}

function findSimilar(item) {
  router.push({ name: 'ProductList', query: { categoryId: item.categoryId } })
}

function goDetail(pid) {
  router.push({ name: 'ProductDetail', params: { id: pid } })
}

function openPriceAlert(item) {
  alertProduct.value = item
  alertForm.targetPrice = item.hasPriceAlert ? item.alertTargetPrice : Number(item.currentPrice) * 0.9
  alertDialogVisible.value = true
}

async function submitAlert() {
  if (!alertForm.targetPrice || alertForm.targetPrice <= 0) {
    ElMessage.warning('请输入有效的目标价格')
    return
  }
  alertSubmitting.value = true
  try {
    await api.post('/favorites/price-alert', {
      productId: alertProduct.value.productId,
      targetPrice: alertForm.targetPrice,
    })
    ElMessage.success('已设置目标价提醒')
    alertDialogVisible.value = false
    await loadAll()
  } catch (e) {
  } finally {
    alertSubmitting.value = false
  }
}

async function cancelAlert() {
  try {
    await ElMessageBox.confirm('确定要取消该商品的价格提醒吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  alertSubmitting.value = true
  try {
    await api.delete(`/favorites/price-alert/${alertProduct.value.productId}`)
    ElMessage.success('已取消价格提醒')
    alertDialogVisible.value = false
    await loadAll()
  } catch (e) {
  } finally {
    alertSubmitting.value = false
  }
}

function onDragStart(e, item) {
  dragItem.value = item
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(item.productId))
}

function onDragEnd() {
  dragItem.value = null
  dragOverGroupId.value = null
}

function onGroupDragOver(gid) {
  dragOverGroupId.value = gid
}

function onGroupDragLeave() {
  dragOverGroupId.value = null
}

async function onGroupDrop(gid) {
  if (!dragItem.value) return
  try {
    await api.put('/favorites/move', {
      productId: dragItem.value.productId,
      groupId: gid,
    })
    ElMessage.success('已移动到目标分组')
    await loadAll()
  } catch (e) {
  } finally {
    dragItem.value = null
    dragOverGroupId.value = null
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.my-favorites {
  padding-bottom: 48px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  letter-spacing: -0.02em;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.sidebar {
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  padding: 12px 0;
  position: sticky;
  top: 88px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.group-list {
  display: flex;
  flex-direction: column;
}

.group-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  transition: background 0.15s, color 0.15s;
  position: relative;
}

.group-item:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.group-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

.group-item.drag-over {
  background: #dbeafe;
  color: var(--color-primary);
  outline: 2px dashed #3b82f6;
  outline-offset: -4px;
}

.group-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-menu {
  opacity: 0;
  transition: opacity 0.15s;
}

.group-item--custom:hover .group-menu {
  opacity: 1;
}

.menu-icon {
  font-size: 14px;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
}

.menu-icon:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--color-text-secondary);
}

.group-divider {
  margin: 8px 16px;
}

.main-content {
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  padding: 20px;
  min-height: 400px;
}

.tab-bar {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 20px;
}

.tab-item {
  padding: 12px 0;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.15s, border-color 0.15s;
  display: inline-flex;
  align-items: center;
}

.tab-item:hover {
  color: var(--color-text);
}

.tab-item.active {
  color: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 600;
}

.select-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
}

.select-hint {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.fav-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #fff;
  position: relative;
  transition: box-shadow 0.2s, border-color 0.2s, transform 0.2s;
  cursor: grab;
}

.fav-card:active {
  cursor: grabbing;
}

.fav-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  border-color: #d1d5db;
  transform: translateY(-2px);
}

.fav-card.selected {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.fav-card.off-shelf {
  opacity: 0.85;
}

.select-check {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  padding: 4px 6px;
  border-radius: 6px;
  border: 1px solid var(--color-border);
}

.price-down-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 5;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 999px;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.3);
}

.off-shelf-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.off-shelf-text {
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 6px 20px;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  letter-spacing: 0.05em;
}

.card-img {
  aspect-ratio: 1;
  background: var(--color-bg);
  overflow: hidden;
  cursor: pointer;
}

.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.fav-card:hover .card-img img {
  transform: scale(1.05);
}

.card-body {
  padding: 12px 14px 14px;
}

.card-name {
  margin: 0 0 8px;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.4;
  height: 2.6em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  cursor: pointer;
}

.card-name:hover {
  color: var(--color-primary);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.current-price {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 1.125rem;
}

.favor-price-label {
  color: var(--color-text-muted);
  font-size: 0.75rem;
  margin-left: 2px;
}

.favor-price {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  text-decoration: line-through;
}

.change-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 0.8125rem;
}

.change-value {
  font-weight: 600;
}

.change-value.up {
  color: #ef4444;
}

.change-value.down {
  color: #10b981;
}

.stock-info {
  color: var(--color-text-secondary);
}

.stock-info.out {
  color: #ef4444;
  font-weight: 600;
}

.alert-info {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 4px;
  margin-bottom: 10px;
  font-size: 0.8125rem;
  color: #92400e;
}

.card-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.alert-product-name {
  color: var(--color-text);
  font-weight: 500;
}

.current-price-tag {
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
