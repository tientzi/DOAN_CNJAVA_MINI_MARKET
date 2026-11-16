import React, { createContext, useState, useEffect, useContext } from 'react'
import api from '../services/api'
import { AuthContext } from './AuthContext'

export const CartContext = createContext()

export const CartProvider = ({ children }) => {
  const [cart, setCart] = useState({ items: [] })
  const [loading, setLoading] = useState(false)
  const [selectedProductIds, setSelectedProductIds] = useState([])
  const { isAuthenticated } = useContext(AuthContext)

  const syncSelectedWithCart = (items) => {
    const validIds = (items || []).map(i => i.productId)
    setSelectedProductIds(prev => {
      if (!prev || prev.length === 0) return validIds
      const filtered = prev.filter(id => validIds.includes(id))
      return filtered.length > 0 ? filtered : validIds
    })
  }

  const fetchCart = async () => {
    if (!isAuthenticated) return
    setLoading(true)
    try {
      const response = await api.get('/api/cart')
      setCart(response.data)
      syncSelectedWithCart(response.data?.items)
    } catch (error) {
      console.error('Không thể lấy giỏ hàng', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (isAuthenticated) {
      fetchCart()
    } else {
      setCart({ items: [] })
    }
  }, [isAuthenticated])

  const addToCart = async (productId, quantity) => {
    try {
      const response = await api.post('/api/cart/add', { productId, quantity })
      setCart(response.data)
      return response.data
    } catch (error) {
      throw error.response?.data?.error || 'Không thể thêm sản phẩm vào giỏ hàng'
    }
  }

  const updateCartItem = async (productId, quantity) => {
    try {
      const response = await api.put('/api/cart/update', { productId, quantity })
      setCart(response.data)
      return response.data
    } catch (error) {
      throw error.response?.data?.error || 'Không thể cập nhật số lượng'
    }
  }

  const removeCartItem = async (productId) => {
    try {
      const response = await api.delete(`/api/cart/remove/${productId}`)
      setCart(response.data)
      return response.data
    } catch (error) {
      throw error.response?.data?.error || 'Không thể xóa sản phẩm khỏi giỏ hàng'
    }
  }

  const clearCart = () => {
    setCart({ items: [] })
    setSelectedProductIds([])
  }

  // Icon giỏ hàng hiển thị số lượng từng loại sản phẩm trong giỏ (VD: SP1 có 5 món -> hiển thị 1)
  const totalItemsCount = cart.items ? cart.items.length : 0

  const computeSubtotal = (itemsList) => (itemsList || []).reduce((sum, item) => {
    if (item.hasMultiBatch && item.saleBatchQuantity > 0 && item.quantity > item.saleBatchQuantity) {
      const salePart = item.saleBatchQuantity * item.saleBatchPrice
      const regularPart = (item.quantity - item.saleBatchQuantity) * item.productPrice
      return sum + salePart + regularPart
    }
    const price = (item.hasMultiBatch && item.saleBatchPrice != null && item.quantity <= item.saleBatchQuantity)
      ? item.saleBatchPrice
      : (item.productSalePrice != null ? item.productSalePrice : item.productPrice)
    return sum + price * item.quantity
  }, 0)

  const cartSubtotal = computeSubtotal(cart.items)

  return (
    <CartContext.Provider value={{
      cart, loading, fetchCart, addToCart, updateCartItem, removeCartItem, clearCart,
      totalItemsCount, cartSubtotal, computeSubtotal,
      selectedProductIds, setSelectedProductIds
    }}>
      {children}
    </CartContext.Provider>
  )
}
