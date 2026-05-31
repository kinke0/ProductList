import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getCurrentUser } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(null)

  async function login(username, password) {
    const res = await loginApi(username, password)
    token.value = res.data.token
    user.value = res.data
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('username', res.data.username)
    localStorage.setItem('userId', res.data.userId)
    localStorage.setItem('roleCode', res.data.roleCode)
    localStorage.setItem('nickname', res.data.nickname)
    return res.data
  }

  async function fetchUser() {
    try {
      const res = await getCurrentUser()
      user.value = res.data
    } catch (e) {
      logout()
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
    localStorage.removeItem('roleCode')
    localStorage.removeItem('nickname')
  }

  function isAdmin() {
    return localStorage.getItem('roleCode') === 'ADMIN'
  }

  function isAdvanced() {
    return localStorage.getItem('roleCode') === 'ADVANCED'
  }

  return { token, user, login, fetchUser, logout, isAdmin, isAdvanced }
})
