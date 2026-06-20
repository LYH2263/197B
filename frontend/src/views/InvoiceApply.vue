<template>
  <div class="invoice-apply content-card" v-loading="loading">
    <h3>{{ isEdit ? '修改发票申请' : '申请发票' }}</h3>
    <el-form :model="form" label-width="120px" ref="formRef" :rules="rules">
      <el-form-item label="订单号">
        <span>{{ orderNo }}</span>
      </el-form-item>
      <el-form-item label="发票类型" prop="invoiceType">
        <el-radio-group v-model="form.invoiceType">
          <el-radio value="personal">个人</el-radio>
          <el-radio value="enterprise">企业</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="发票抬头" prop="title">
        <el-input v-model="form.title" placeholder="请输入发票抬头" style="max-width: 400px" />
      </el-form-item>
      <el-form-item
        v-if="form.invoiceType === 'enterprise'"
        label="税号"
        prop="taxNumber"
      >
        <el-input
          v-model="form.taxNumber"
          placeholder="请输入企业税号（15-20位大写字母和数字）"
          style="max-width: 400px"
        />
        <div style="font-size: 12px; color: var(--color-text-muted); margin-top: 4px;">
          企业税号需为15-20位大写字母和数字
        </div>
      </el-form-item>
      <el-form-item v-if="form.invoiceType === 'enterprise'" label="开户行">
        <el-input v-model="form.bankName" placeholder="请输入开户行（可选）" style="max-width: 400px" />
      </el-form-item>
      <el-form-item v-if="form.invoiceType === 'enterprise'" label="银行账号">
        <el-input v-model="form.bankAccount" placeholder="请输入银行账号（可选）" style="max-width: 400px" />
      </el-form-item>
      <el-form-item v-if="form.invoiceType === 'enterprise'" label="企业地址">
        <el-input v-model="form.address" placeholder="请输入企业地址（可选）" style="max-width: 400px" />
      </el-form-item>
      <el-form-item v-if="form.invoiceType === 'enterprise'" label="企业电话">
        <el-input v-model="form.phone" placeholder="请输入企业电话（可选）" style="max-width: 400px" />
      </el-form-item>
      <el-form-item label="接收邮箱" prop="receiveEmail">
        <el-input v-model="form.receiveEmail" placeholder="请输入接收邮箱" style="max-width: 400px" />
      </el-form-item>
      <el-form-item v-if="rejectReason">
        <el-alert :title="'驳回原因：' + rejectReason" type="error" show-icon style="max-width: 600px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ isEdit ? '重新提交' : '提交申请' }}
        </el-button>
        <el-button @click="goBack">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { invoiceApi } from '../api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const orderNo = ref('')
const rejectReason = ref('')

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  orderId: null,
  invoiceType: 'personal',
  title: '',
  taxNumber: '',
  bankName: '',
  bankAccount: '',
  address: '',
  phone: '',
  receiveEmail: '',
})

const rules = {
  invoiceType: [{ required: true, message: '请选择发票类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入发票抬头', trigger: 'blur' }],
  receiveEmail: [
    { required: true, message: '请输入接收邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  taxNumber: [
    { required: true, message: '请输入企业税号', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (form.invoiceType === 'enterprise' && value) {
          if (!/^[A-HJ-NP-RT-UW-Y0-9]{15,20}$/.test(value)) {
            callback(new Error('企业税号格式不正确，应为15-20位大写字母和数字'))
            return
          }
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function loadOrder(orderId) {
  try {
    const res = await fetch(`/api/orders/${orderId}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    const data = await res.json()
    if (data.code === 200) {
      orderNo.value = data.data.orderNo
    }
  } catch (e) {}
}

async function loadInvoice(id) {
  try {
    const res = await invoiceApi.getDetail(id)
    if (res.data.code === 200) {
      const inv = res.data.data
      form.invoiceType = inv.invoiceType
      form.title = inv.title
      form.taxNumber = inv.taxNumber || ''
      form.bankName = inv.bankName || ''
      form.bankAccount = inv.bankAccount || ''
      form.address = inv.address || ''
      form.phone = inv.phone || ''
      form.receiveEmail = inv.receiveEmail
      rejectReason.value = inv.rejectReason || ''
      form.orderId = inv.orderId
      await loadOrder(inv.orderId)
    }
  } catch (e) {}
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        const id = Number(route.params.id)
        const { orderId, ...updateData } = form
        await invoiceApi.update(id, updateData)
        ElMessage.success('修改成功，已重新提交审核')
      } else {
        await invoiceApi.create(form)
        ElMessage.success('申请提交成功')
      }
      router.push('/my-invoices')
    } finally {
      submitting.value = false
    }
  })
}

function goBack() {
  router.back()
}

onMounted(() => {
  if (isEdit.value) {
    loadInvoice(Number(route.params.id))
  } else {
    form.orderId = Number(route.query.orderId)
    loadOrder(form.orderId)
  }
})
</script>

<style scoped>
.invoice-apply {
  max-width: 800px;
  padding: 28px;
}
.invoice-apply h3 {
  margin-bottom: 24px;
}
</style>
