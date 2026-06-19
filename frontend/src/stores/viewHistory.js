import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api'
import { useUserStore } from './user'

const LOCAL_KEY = 'view_history_local'
const PRIVACY_KEY = 'view_history_privacy'
const DEDUP_MINUTES = 30

function loadLocal() {
  try {
    const raw = localStorage.getItem(LOCAL_KEY)
    if (!raw) return []
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function saveLocal(list) {
  try {
    localStorage.setItem(LOCAL_KEY, JSON.stringify(list || []))
  } catch {}
}

function getPrivacy() {
  try {
    return sessionStorage.getItem(PRIVACY_KEY) === '1'
  } catch {
    return false
  }
}

function setPrivacy(val) {
  try {
    if (val) sessionStorage.setItem(PRIVACY_KEY, '1')
    else sessionStorage.removeItem(PRIVACY_KEY)
  } catch {}
}

function parseLocalDT(s) {
  if (!s) return null
  const d = new Date(s)
  return isNaN(d.getTime()) ? null : d
}

export const useViewHistoryStore = defineStore('viewHistory', () => {
  const userStore = useUserStore()
  const localList = ref(loadLocal())
  const privacyMode = ref(getPrivacy())

  const isLoggedIn = computed(() => userStore.isLoggedIn)

  function togglePrivacy() {
    privacyMode.value = !privacyMode.value
    setPrivacy(privacyMode.value)
  }

  function setPrivacyMode(val) {
    privacyMode.value = !!val
    setPrivacy(privacyMode.value)
  }

  async function addRecord(productId, viewedPrice, productInfo) {
    if (privacyMode.value) return

    const now = new Date()
    const nowISO = now.toISOString()

    if (isLoggedIn.value) {
      try {
        await api.post('/view-history/add', { productId })
      } catch {}
      return
    }

    const list = localList.value.slice()
    let updated = false
    for (let i = 0; i < list.length; i++) {
      if (Number(list[i].productId) === Number(productId)) {
        const lastView = parseLocalDT(list[i].viewedAt)
        if (lastView) {
          const diffMs = now.getTime() - lastView.getTime()
          const diffMin = diffMs / (1000 * 60)
          if (diffMin < DEDUP_MINUTES) {
            list[i].viewedAt = nowISO
            list[i].viewedPrice = viewedPrice
            if (productInfo) {
              list[i].productName = productInfo.name
              list[i].productImage = productInfo.mainImage
              list[i].categoryId = productInfo.categoryId
            }
            updated = true
            break
          }
        }
      }
    }

    if (!updated) {
      const item = {
        id: `local_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
        productId: Number(productId),
        viewedPrice,
        viewedAt: nowISO,
      }
      if (productInfo) {
        item.productName = productInfo.name
        item.productImage = productInfo.mainImage
        item.categoryId = productInfo.categoryId
        item.status = productInfo.status
        item.stock = productInfo.stock
      }
      list.unshift(item)
    }

    list.sort((a, b) => {
      const ta = parseLocalDT(a.viewedAt)?.getTime() || 0
      const tb = parseLocalDT(b.viewedAt)?.getTime() || 0
      return tb - ta
    })

    if (list.length > 500) list.length = 500

    localList.value = list
    saveLocal(list)
  }

  function removeLocalById(id) {
    const list = localList.value.filter(i => String(i.id) !== String(id))
    localList.value = list
    saveLocal(list)
  }

  function removeLocalBatch(ids) {
    const set = new Set(ids.map(String))
    const list = localList.value.filter(i => !set.has(String(i.id)))
    localList.value = list
    saveLocal(list)
  }

  function clearLocal() {
    localList.value = []
    saveLocal([])
  }

  function getLocalRecent(limit = 6) {
    return localList.value.slice(0, limit)
  }

  function getLocalWithPage(page = 1, size = 10) {
    const start = (page - 1) * size
    const list = localList.value.slice(start, start + size)
    return { list, total: localList.value.length, page, size }
  }

  async function mergeToAccount() {
    if (!isLoggedIn.value) return { mergedCount: 0 }
    if (localList.value.length === 0) return { mergedCount: 0 }

    const items = localList.value.map(i => ({
      productId: Number(i.productId),
      viewedPrice: i.viewedPrice,
      viewedAt: i.viewedAt,
    }))

    try {
      const res = await api.post('/view-history/merge', { items })
      if (res.data.code === 200) {
        clearLocal()
        return res.data.data || { mergedCount: 0 }
      }
    } catch {}
    return { mergedCount: 0 }
  }

  function getLocalItemsForMerge() {
    return localList.value.slice()
  }

  function hasLocalItems() {
    return localList.value.length > 0
  }

  return {
    privacyMode,
    localList,
    isLoggedIn,
    togglePrivacy,
    setPrivacyMode,
    addRecord,
    removeLocalById,
    removeLocalBatch,
    clearLocal,
    getLocalRecent,
    getLocalWithPage,
    mergeToAccount,
    getLocalItemsForMerge,
    hasLocalItems,
  }
})
