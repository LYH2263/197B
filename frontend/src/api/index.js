import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const user = useUserStore()
  if (user.token) config.headers.Authorization = `Bearer ${user.token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    ElMessage.error(msg)
    if (err.response?.status === 401) {
      useUserStore().logout()
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    return Promise.reject(err)
  }
)

export const invoiceApi = {
  create: (data) => api.post('/invoices', data),
  update: (id, data) => api.put(`/invoices/${id}`, data),
  list: () => api.get('/invoices'),
  getDetail: (id) => api.get(`/invoices/${id}`),
  getByOrderId: (orderId) => api.get(`/invoices/order/${orderId}`),

  adminList: (status) => api.get('/admin/invoices', { params: { status } }),
  adminGetDetail: (id) => api.get(`/admin/invoices/${id}`),
  adminApprove: (id, invoiceNumber) => api.post(`/admin/invoices/${id}/approve`, { invoiceNumber }),
  adminReject: (id, rejectReason) => api.post(`/admin/invoices/${id}/reject`, { rejectReason }),
  adminUpdateInvoiceNumber: (id, invoiceNumber) => api.post(`/admin/invoices/${id}/invoice-number`, { invoiceNumber }),
  adminExport: (ids) => api.get('/admin/invoices/export', { params: { ids }, responseType: 'blob' }),
}

export default api
