<template>
  <div v-loading="loading" class="after-sale-apply">
    <h2 class="page-title">申请售后</h2>

    <template v-if="orderItem">
      <div class="form-card content-card">
        <div class="product-section">
          <el-image
            v-if="orderItem.productImage"
            :src="orderItem.productImage"
            fit="cover"
            class="product-img"
          />
          <div class="product-info">
            <div class="product-name">{{ orderItem.productName }}</div>
            <div class="product-meta">
              单价：¥ {{ orderItem.price }} × {{ orderItem.quantity }}
            </div>
            <div class="product-meta">
              实付金额：<span class="amount">¥ {{ orderItem.totalAmount }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <el-form :model="form" label-width="110px" class="apply-form">
          <el-form-item label="售后类型" required>
            <el-radio-group v-model="form.type">
              <el-radio :value="1">退货退款</el-radio>
              <el-radio :value="2">换货</el-radio>
              <el-radio :value="3">仅退款</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="售后原因" required>
            <el-select v-model="form.reason" placeholder="请选择售后原因" style="width: 320px;">
              <el-option label="商品质量问题" value="商品质量问题" />
              <el-option label="商品与描述不符" value="商品与描述不符" />
              <el-option label="发错货/漏发货" value="发错货/漏发货" />
              <el-option label="尺寸/规格不合适" value="尺寸/规格不合适" />
              <el-option label="不想要了/拍错了" value="不想要了/拍错了" />
              <el-option label="物流问题" value="物流问题" />
              <el-option label="其他原因" value="其他原因" />
            </el-select>
          </el-form-item>

          <el-form-item label="问题描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="请详细描述您遇到的问题，以便我们更快处理（选填）"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="凭证图片">
            <div class="upload-section">
              <el-upload
                list-type="picture-card"
                :auto-upload="false"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                :file-list="fileList"
                :limit="3"
                accept="image/*"
                class="uploader"
              >
                <el-icon><Plus /></el-icon>
                <template #tip>
                  <div class="upload-tip">最多上传 3 张图片，点击上传</div>
                </template>
              </el-upload>
            </div>
          </el-form-item>
        </el-form>

        <div class="actions">
          <el-button @click="$router.back()">返回</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="订单商品不存在或状态不允许售后" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const orderItem = ref(null)
const fileList = ref([])

const form = reactive({
  orderItemId: null,
  type: 1,
  reason: '',
  description: '',
  voucherImages: [],
})

const orderItemId = computed(() => Number(route.params.orderItemId))

function handleFileChange(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    form.voucherImages.push(e.target.result)
  }
  reader.readAsDataURL(file.raw)
}

function handleFileRemove(file, files) {
  const idx = fileList.value.indexOf(file)
  if (idx >= 0) {
    form.voucherImages.splice(idx, 1)
  }
  fileList.value = files
}

async function submit() {
  if (!form.type) {
    ElMessage.warning('请选择售后类型')
    return
  }
  if (!form.reason) {
    ElMessage.warning('请选择售后原因')
    return
  }
  submitting.value = true
  try {
    const res = await api.post('/after-sale', {
      orderItemId: orderItemId.value,
      type: form.type,
      reason: form.reason,
      description: form.description,
      voucherImages: form.voucherImages,
    })
    if (res.data.code === 200) {
      ElMessage.success('售后申请已提交，等待审核')
      router.replace(`/after-sale/${res.data.data.id}`)
    }
  } finally {
    submitting.value = false
  }
}

async function loadOrderItem() {
  loading.value = true
  try {
    const oRes = await api.get('/orders')
    if (oRes.data.code === 200) {
      const orders = oRes.data.data || []
      for (const o of orders) {
        if (o.status !== 3) continue
        const iRes = await api.get(`/orders/${o.id}/items`)
        if (iRes.data.code === 200) {
          const items = iRes.data.data || []
          for (const it of items) {
            if (it.id === orderItemId.value) {
              orderItem.value = it
              form.orderItemId = it.id
              loading.value = false
              return
            }
          }
        }
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadOrderItem)
</script>

<style scoped>
.after-sale-apply {
  padding-bottom: 32px;
}

.page-title {
  margin-bottom: 24px;
}

.form-card {
  max-width: 760px;
  padding: 28px;
}

.product-section {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.product-img {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  background: var(--color-bg-secondary);
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 1.0625rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.product-meta {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  margin-bottom: 4px;
}

.amount {
  color: var(--color-text);
  font-weight: 500;
}

.apply-form {
  margin-top: 8px;
}

.upload-section {
  width: 100%;
}

.uploader {
  :deep(.el-upload--picture-card) {
    width: 100px;
    height: 100px;
  }
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 100px;
    height: 100px;
  }
}

.upload-tip {
  margin-top: 8px;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.actions {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
