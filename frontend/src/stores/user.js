import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(null)
  const cartCount = ref(0)
  const favoriteCount = ref(0)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  function setToken(t) {
    token.value = t
    if (t) localStorage.setItem('token', t)
    else localStorage.removeItem('token')
  }

  async function fetchUser() {
    if (!token.value) return
    try {
      const res = await api.get('/auth/me')
      if (res.data.code === 200) user.value = res.data.data
      else setToken('')
    } catch {
      setToken('')
    }
  }

  async function fetchFavoriteCount() {
    if (!token.value) {
      favoriteCount.value = 0
      return
    }
    try {
      const res = await api.get('/favorites/count')
      if (res.data.code === 200) favoriteCount.value = res.data.data?.count ?? 0
    } catch {
      favoriteCount.value = 0
    }
  }

  function logout() {
    setToken('')
    user.value = null
    cartCount.value = 0
    favoriteCount.value = 0
  }

  return { token, user, cartCount, favoriteCount, isLoggedIn, isAdmin, setToken, fetchUser, fetchFavoriteCount, logout }
})
