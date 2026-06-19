<template>
  <div class="points-mall">
    <div class="points-header content-card">
      <div class="points-info">
        <div class="points-label">当前积分</div>
        <div class="points-value">
          <span class="points-number">{{ userPoints }}</span>
          <span class="points-unit">积分</span>
        </div>
      </div>
      <div class="points-tip">使用积分兑换心仪好物</div>
    </div>

    <h2 class="page-title">积分商品</h2>
    <div v-loading="loading" class="grid">
      <div
        v-for="product in products"
        :key="product.id"
        class="product-card"
        @click="openExchangeDialog(product)"
      >
        <div class="product-img">
          <img
            :src="product.image || '/images/default-product.svg'"
            alt=""
            @error="$event.target.src = '/images/default-product.svg'"
          />
        </div>
        <div class="product-info">
          <div class="product-name">{{ product.name }}</div>
          <div class="product-stock">库存：{{ product.stock }}</div>
          <div class="product-points">
            <span class="points-num">{{ product.points }}</span>
            <span class="points-text">积分</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && products.length === 0" description="暂无商品" />

    <el-dialog
      v-model="dialogVisible"
      title="兑换商品"
      width="480px"
      :close-on-click-modal="false"
    >
      <div v-if="currentProduct" class="exchange-dialog">
        <div class="exchange-product">
          <div class="exchange-img">
            <img
              :src="currentProduct.image || '/images/default-product.svg'"
              alt=""
              @error="$event.target.src = '/images/default-product.svg'"
            />
          </div>
          <div class="exchange-info">
            <div class="exchange-name">{{ currentProduct.name }}</div>
            <div class="exchange-points">
              所需积分：<span class="highlight">{{ currentProduct.points }}</span>
            </div>
            <div class="exchange-my-points">
              当前积分：<span :class="userPoints >= currentProduct.points ? 'sufficient' : 'insufficient'">{{ userPoints }}</span>
            </div>
            <div v-if="userPoints < currentProduct.points" class="insufficient-tip">
              积分不足，无法兑换
            </div>
          </div>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="80px"
          class="exchange-form"
        >
          <el-form-item label="收货人" prop="receiverName">
            <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
          </el-form-item>
          <el-form-item label="电话" prop="receiverPhone">
            <el-input v-model="form.receiverPhone" placeholder="请输入手机号码" maxlength="11" />
          </el-form-item>
          <el-form-item label="地址" prop="receiverAddress">
            <el-input
              v-model="form.receiverAddress"
              type="textarea"
              :rows="3"
              placeholder="请输入详细地址"
              maxlength="200"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!currentProduct || userPoints < currentProduct.points"
          @click="submitExchange"
        >
          确认兑换
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const loading = ref(true)
const products = ref([])
const userPoints = ref(0)
const dialogVisible = ref(false)
const currentProduct = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
})

const rules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' },
  ],
  receiverPhone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
  receiverAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
  ],
}

async function load() {
  loading.value = true
  try {
    const [productsRes, userRes] = await Promise.all([
      api.get('/exchange/products'),
      userStore.user ? Promise.resolve({ data: { code: 200, data: userStore.user } }) : userStore.fetchUser().then(() => ({ data: { code: 200, data: userStore.user } })),
    ])
    if (productsRes.data.code === 200) {
      products.value = productsRes.data.data || []
    }
    if (userRes.data.code === 200 && userRes.data.data) {
      userPoints.value = userRes.data.data.points || 0
    }
  } finally {
    loading.value = false
  }
}

function openExchangeDialog(product) {
  if (product.stock <= 0) {
    ElMessage.warning('该商品库存不足')
    return
  }
  currentProduct.value = product
  form.receiverName = ''
  form.receiverPhone = ''
  form.receiverAddress = ''
  dialogVisible.value = true
}

async function submitExchange() {
  if (!formRef.value || !currentProduct.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const res = await api.post('/exchange/orders', {
      pointsProductId: currentProduct.value.id,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
    })
    if (res.data.code === 200) {
      ElMessage.success('兑换成功')
      dialogVisible.value = false
      load()
    }
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.points-mall {
  padding-bottom: 32px;
}

.points-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff7875 100%);
  color: #fff;
  border-radius: var(--radius-md);
}

.points-info {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.points-label {
  font-size: 1rem;
  opacity: 0.9;
}

.points-value {
  display: flex;
  align-items: baseline;
}

.points-number {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1;
}

.points-unit {
  font-size: 1rem;
  margin-left: 4px;
  opacity: 0.9;
}

.points-tip {
  font-size: 0.875rem;
  opacity: 0.85;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--color-text);
}

.grid {
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
  height: 200px;
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

.product-stock {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-top: 6px;
}

.product-points {
  display: flex;
  align-items: baseline;
  margin-top: 10px;
}

.points-num {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-primary);
}

.points-text {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-left: 4px;
}

.exchange-dialog {
  padding: 8px 0;
}

.exchange-product {
  display: flex;
  gap: 16px;
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--color-border);
}

.exchange-img {
  width: 100px;
  height: 100px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
}

.exchange-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.exchange-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.exchange-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
}

.exchange-points {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.exchange-points .highlight {
  color: var(--color-primary);
  font-weight: 600;
}

.exchange-my-points {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.exchange-my-points .sufficient {
  color: var(--color-success, #52c41a);
  font-weight: 600;
}

.exchange-my-points .insufficient {
  color: var(--color-danger, #f5222d);
  font-weight: 600;
}

.insufficient-tip {
  font-size: 0.8125rem;
  color: var(--color-danger, #f5222d);
  margin-top: 4px;
}

.exchange-form {
  margin-top: 0;
}
</style>
