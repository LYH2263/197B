import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import api from '../api'
import { useUserStore } from './user'

const STORAGE_KEY = 'compare_list'
const MAX_ITEMS = 4

function loadLocal() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function saveLocal(list) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list || []))
  } catch {}
}

export function parseProductParams(product) {
  const params = {}
  if (!product) return params

  if (product.productParam && typeof product.productParam === 'object') {
    Object.assign(params, product.productParam)
  }

  if (product.detail) {
    try {
      const parsed = JSON.parse(product.detail)
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        if (parsed.params && typeof parsed.params === 'object') {
          Object.assign(params, parsed.params)
        } else {
          Object.assign(params, parsed)
        }
      }
    } catch {
      if (typeof product.detail === 'string') {
        const lines = product.detail.split(/[<br>|\n]/).filter(l => l.trim())
        lines.forEach(line => {
          const match = line.match(/^([^:：]+)[:：]\s*(.+)$/)
          if (match) {
            params[match[1].trim()] = match[2].trim()
          }
        })
      }
    }
  }

  return params
}

export const useCompareStore = defineStore('compare', () => {
  const userStore = useUserStore()
  const items = ref(loadLocal())

  const count = computed(() => items.value.length)
  const isFull = computed(() => items.value.length >= MAX_ITEMS)
  const maxItems = computed(() => MAX_ITEMS)
  const isLoggedIn = computed(() => userStore.isLoggedIn)

  function isInCompare(productId) {
    return items.value.some(i => Number(i.id) === Number(productId))
  }

  function getProductById(productId) {
    return items.value.find(i => Number(i.id) === Number(productId)) || null
  }

  async function addProduct(product) {
    if (!product || product.id == null) return { success: false, message: '商品信息无效' }
    if (isInCompare(product.id)) return { success: false, message: '该商品已在对比栏' }
    if (isFull.value) return { success: false, message: `对比栏最多添加 ${MAX_ITEMS} 个商品` }

    let productData = product
    if (!product.name || !product.price) {
      try {
        const res = await api.get(`/products/${product.id}`)
        if (res.data.code === 200 && res.data.data) {
          productData = res.data.data
        } else {
          return { success: false, message: '获取商品信息失败' }
        }
      } catch {
        return { success: false, message: '获取商品信息失败' }
      }
    }

    const newItem = {
      id: Number(productData.id),
      name: productData.name,
      subtitle: productData.subtitle || '',
      mainImage: productData.mainImage || '',
      price: productData.price,
      stock: productData.stock ?? 0,
      categoryId: productData.categoryId || null,
      categoryName: productData.categoryName || '',
      avgRating: productData.avgRating ?? null,
      salesCount: productData.salesCount ?? 0,
      detail: productData.detail || '',
      productParam: productData.productParam || null,
      addedAt: new Date().toISOString(),
    }

    items.value.push(newItem)
    saveLocal(items.value)
    return { success: true, message: '已加入对比栏' }
  }

  function removeProduct(productId) {
    const idx = items.value.findIndex(i => Number(i.id) === Number(productId))
    if (idx > -1) {
      items.value.splice(idx, 1)
      saveLocal(items.value)
      return true
    }
    return false
  }

  function clearAll() {
    items.value = []
    saveLocal(items.value)
  }

  async function refreshProducts() {
    if (items.value.length === 0) return
    const promises = items.value.map(async (item, index) => {
      try {
        const res = await api.get(`/products/${item.id}`)
        if (res.data.code === 200 && res.data.data) {
          const p = res.data.data
          items.value[index] = {
            ...items.value[index],
            name: p.name,
            subtitle: p.subtitle || '',
            mainImage: p.mainImage || '',
            price: p.price,
            stock: p.stock ?? 0,
            categoryId: p.categoryId || null,
            detail: p.detail || '',
          }
        }
      } catch {}
    })
    await Promise.all(promises)
    saveLocal(items.value)
  }

  function getSlot(index) {
    return items.value[index] || null
  }

  watch(
    () => items.value,
    (val) => {
      saveLocal(val)
    },
    { deep: true }
  )

  return {
    items,
    count,
    isFull,
    maxItems,
    isLoggedIn,
    isInCompare,
    getProductById,
    addProduct,
    removeProduct,
    clearAll,
    refreshProducts,
    getSlot,
  }
})
