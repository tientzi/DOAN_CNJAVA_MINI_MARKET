import React, { createContext, useState, useEffect } from 'react'
import api from '../services/api'

export const AuthContext = createContext()

const syncJwtCookie = (tokenValue) => {
  try {
    if (tokenValue) {
      document.cookie = `jwt_token=${tokenValue}; path=/; max-age=86400; SameSite=Lax`
    } else {
      document.cookie = 'jwt_token=; path=/; max-age=0; SameSite=Lax'
    }
  } catch (e) {
    // ignore cookie errors
  }
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    if (storedToken && storedUser) {
      setToken(storedToken)
      setUser(JSON.parse(storedUser))
      syncJwtCookie(storedToken)
    } else {
      syncJwtCookie(null)
    }
    setLoading(false)
  }, [])

  const login = async (usernameOrEmail, password, rememberMe) => {
    try {
      const response = await api.post('/api/auth/login', { usernameOrEmail, password, rememberMe })
      const { accessToken, role, id, username, email } = response.data
      
      const userData = { id, username, email, role }
      
      setToken(accessToken)
      setUser(userData)
      
      localStorage.setItem('token', accessToken)
      localStorage.setItem('user', JSON.stringify(userData))
      syncJwtCookie(accessToken)
      
      return userData
    } catch (error) {
      throw error.response?.data?.error || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.'
    }
  }

  /**
   * Đăng nhập bằng token từ OAuth2 callback (Google/Facebook)
   */
  const loginWithToken = (tokenData) => {
    const { token: accessToken, id, username, email, role, avatarUrl } = tokenData
    const userData = { 
      id: parseInt(id), 
      username, 
      email, 
      role,
      avatarUrl: avatarUrl || null
    }
    
    setToken(accessToken)
    setUser(userData)
    
    localStorage.setItem('token', accessToken)
    localStorage.setItem('user', JSON.stringify(userData))
    syncJwtCookie(accessToken)
    
    return userData
  }

  const register = async (username, email, password, fullName, phone) => {
    try {
      await api.post('/api/auth/register', { username, email, password, fullName, phone })
    } catch (error) {
      const errData = error.response?.data
      if (errData && typeof errData === 'object') {
        const messages = Object.values(errData).join(', ')
        throw messages || 'Đăng ký thất bại'
      }
      throw error.response?.data?.error || 'Đăng ký thất bại'
    }
  }

  const logout = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    syncJwtCookie(null)
  }

  const forgotPassword = async (email, newPassword) => {
    try {
      await api.post('/api/auth/forgot-password', { email, newPassword })
    } catch (error) {
      throw error.response?.data?.error || 'Không thể đặt lại mật khẩu'
    }
  }

  const updateUser = (updatedFields) => {
    setUser(prev => {
      if (!prev) return null
      const newUser = { ...prev, ...updatedFields }
      localStorage.setItem('user', JSON.stringify(newUser))
      return newUser
    })
  }

  const isAdmin = user?.role === 'ROLE_ADMIN'
  const isShipper = user?.role === 'ROLE_SHIPPER'

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: !!token, isAdmin, isShipper, loading, login, loginWithToken, register, logout, forgotPassword, updateUser }}>
      {children}
    </AuthContext.Provider>
  )
}
