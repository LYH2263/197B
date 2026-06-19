import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', component: () => import('../views/Layout.vue'), meta: { requiresAuth: false },
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'products', name: 'ProductList', component: () => import('../views/ProductList.vue') },
      { path: 'products/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue') },
      { path: 'cart', name: 'Cart', component: () => import('../views/Cart.vue'), meta: { requiresAuth: true } },
      { path: 'orders', name: 'OrderList', component: () => import('../views/OrderList.vue'), meta: { requiresAuth: true } },
      { path: 'orders/:id', name: 'OrderDetail', component: () => import('../views/OrderDetail.vue'), meta: { requiresAuth: true } },
      { path: 'after-sale', name: 'AfterSaleList', component: () => import('../views/AfterSaleList.vue'), meta: { requiresAuth: true } },
      { path: 'after-sale/:id', name: 'AfterSaleDetail', component: () => import('../views/AfterSaleDetail.vue'), meta: { requiresAuth: true } },
      { path: 'after-sale/apply/:orderItemId', name: 'AfterSaleApply', component: () => import('../views/AfterSaleApply.vue'), meta: { requiresAuth: true } },
      { path: 'my-reviews', name: 'MyReviews', component: () => import('../views/MyReviews.vue'), meta: { requiresAuth: true } },
      { path: 'coupon-center', name: 'CouponCenter', component: () => import('../views/CouponCenter.vue') },
      { path: 'my-coupons', name: 'MyCoupons', component: () => import('../views/MyCoupons.vue'), meta: { requiresAuth: true } },
      { path: 'points', name: 'PointsCenter', component: () => import('../views/PointsCenter.vue'), meta: { requiresAuth: true } },
      { path: 'points-mall', name: 'PointsMall', component: () => import('../views/PointsMall.vue'), meta: { requiresAuth: true } },
      { path: 'my-exchange', name: 'MyExchange', component: () => import('../views/MyExchange.vue'), meta: { requiresAuth: true } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('../views/AdminUsers.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/reviews', name: 'AdminReviews', component: () => import('../views/AdminReviews.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/after-sale', name: 'AdminAfterSale', component: () => import('../views/AdminAfterSale.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/coupons', name: 'AdminCoupons', component: () => import('../views/AdminCoupons.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/points', name: 'AdminPoints', component: () => import('../views/AdminPoints.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/exchange', name: 'AdminExchange', component: () => import('../views/AdminExchange.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    ],
  },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to, _from, next) => {
  const user = useUserStore()
  if (to.meta.requiresAuth && !user.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAdmin) {
    if (!user.user && user.token) await user.fetchUser()
    if (!user.isAdmin) {
      next({ name: 'Home' })
      return
    }
  }
  next()
})

export default router
