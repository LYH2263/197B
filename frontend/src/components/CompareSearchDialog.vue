<template>
  <el-dialog
    v-model="visible"
    title="添加商品到对比栏"
    width="720px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="search-bar">
      <el-select
        v-model="categoryId"
        placeholder="全部分类"
        clearable
        class="search-select"
        @change="load"
      >
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称"
        clearable
        class="search-input"
        @keyup.enter="load"
      >
        <template #append>
          <el-button :icon="Search" @click="load" />
        </template>
      </el-input>
    </div>
    <div v-loading="loading" class="results">
      <div v-if="products.length === 0 && !loading" class="empty">
        <el-empty description="未找到商品" />
      </div>
      <div v-for="p in products" :key="p.id" class="result-item">
        <div class="result-img" @click="goDetail(p.id)">
          <img
            :src="p.mainImage || '/images/default-product.svg'"
            alt=""
            @error="$event.target.src = '/images/default-product.svg'"
          />
        </div>
        <div class="result-info" @click="goDetail(p.id)">
          <div class="result-name">{{ p.name }}</div>
          <div class="result-subtitle">{{ p.subtitle }}</div>
          <div class="result-price">¥ {{ p.price }}</div>
        </div>
        <div class="result-action">
          <el-button
            v-if="compareStore.isInCompare(p.id)"
            type="success"
            disabled
          >
            已添加
          </el-button>
          <el-button
            v-else
            type="primary"
            :disabled="compareStore.isFull"
            @click="handleAdd(p)"
          >
            添加对比
          </el-button>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button
        type="primary"
        :disabled="compareStore.count === 0"
        @click="goCompare"
      >
        去对比 ({{ compareStore.count }})
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '../api'
import { useCompareStore } from '../stores/compare'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'added'])

const compareStore = useCompareStore()
const router = useRouter()
const visible = ref(props.modelValue)
const loading = ref(false)
const categories = ref([])
const products = ref([])
const categoryId = ref(null)
const keyword = ref('')

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      loadCategories()
      load()
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

onMounted(() => {
  if (visible.value) {
    loadCategories()
    load()
  }
})

async function loadCategories() {
  if (categories.value.length > 0) return
  try {
    const res = await api.get('/categories')
    if (res.data.code === 200) categories.value = res.data.data || []
  } catch {}
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (categoryId.value) params.categoryId = categoryId.value
    if (keyword.value) params.keyword = keyword.value
    const res = await api.get('/products', { params })
    products.value = res.data.code === 200 ? (res.data.data || []) : []
  } finally {
    loading.value = false
  }
}

async function handleAdd(product) {
  const result = await compareStore.addProduct(product)
  if (result.success) {
    ElMessage.success(result.message)
    emit('added', product)
  } else {
    ElMessage.warning(result.message)
  }
}

function goDetail(id) {
  visible.value = false
  router.push(`/products/${id}`)
}

function goCompare() {
  visible.value = false
  router.push('/compare')
}

function handleClose() {
  visible.value = false
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-select {
  width: 180px;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
}

.results {
  max-height: 480px;
  overflow-y: auto;
  padding-right: 8px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  background: var(--color-surface);
  transition: all var(--transition);
}

.result-item:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-sm);
}

.result-img {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
}

.result-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.result-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.result-name {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-subtitle {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-price {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-primary);
  margin-top: 6px;
}

.result-action {
  flex-shrink: 0;
}

.empty {
  padding: 40px 0;
}
</style>
