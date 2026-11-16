import React, { useContext, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CartContext } from '../contexts/CartContext'
import { Trash2, ShoppingBag, ArrowRight, Info, Tag } from 'lucide-react'
import './Cart.css'

const Cart = () => {
  const {
    cart,
    computeSubtotal,
    updateCartItem,
    removeCartItem,
    loading,
    selectedProductIds,
    setSelectedProductIds
  } = useContext(CartContext)
  const [errorMap, setErrorMap] = useState({})
  const navigate = useNavigate()

  const allProductIds = (cart.items || []).map(i => i.productId)
  const effectiveSelectedIds = selectedProductIds.filter(id => allProductIds.includes(id))
  const isAllSelected = allProductIds.length > 0 && effectiveSelectedIds.length === allProductIds.length

  const toggleSelectAll = () => {
    if (isAllSelected) {
      setSelectedProductIds([])
    } else {
      setSelectedProductIds(allProductIds)
    }
  }

  const toggleSelectItem = (productId) => {
    setSelectedProductIds(prev =>
      prev.includes(productId) ? prev.filter(id => id !== productId) : [...prev, productId]
    )
  }

  const selectedItems = (cart.items || []).filter(i => effectiveSelectedIds.includes(i.productId))
  const selectedSubtotal = computeSubtotal ? computeSubtotal(selectedItems) : 0

  const handleQtyChange = async (productId, newQty) => {
    const parsedQty = Number(newQty)
    if (!Number.isFinite(parsedQty) || parsedQty < 1) return

    setErrorMap(prev => ({ ...prev, [productId]: '' }))
    try {
      await updateCartItem(productId, parsedQty)
    } catch (err) {
      setErrorMap(prev => ({ ...prev, [productId]: err }))
    }
  }

  const handleRemove = async (productId) => {
    try {
      await removeCartItem(productId)
    } catch (err) {
      console.error(err)
    }
  }

  if (loading && cart.items.length === 0) {
    return <div className="loading-state">Đang tải giỏ hàng...</div>
  }

  return (
    <div className="cart-page">
      <h1 className="page-title"><ShoppingBag size={28} /> Giỏ hàng của bạn</h1>

      {cart.items && cart.items.length > 0 ? (
        <div className="cart-grid">
          {/* List Items */}
          <div className="cart-items-list glass">
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '10px 16px',
              borderBottom: '1px solid #e2e8f0',
              marginBottom: '6px'
            }}>
              <label style={{ display: 'inline-flex', alignItems: 'center', gap: '10px', cursor: 'pointer', fontWeight: 700, fontSize: '0.94rem', color: '#0f172a' }}>
                <input
                  type="checkbox"
                  checked={isAllSelected}
                  onChange={toggleSelectAll}
                  style={{ width: '18px', height: '18px', cursor: 'pointer', accentColor: 'var(--primary)' }}
                />
                <span>Chọn tất cả ({cart.items.length} sản phẩm)</span>
              </label>
              <span style={{ fontSize: '0.84rem', color: '#64748b' }}>
                Đã chọn <strong>{selectedItems.length}</strong> sản phẩm
              </span>
            </div>

            {cart.items.map(item => {
              const isChecked = effectiveSelectedIds.includes(item.productId)
              const isMultiBatchSplit = item.hasMultiBatch && item.saleBatchQuantity > 0 && item.quantity > item.saleBatchQuantity
              const saleQty = item.saleBatchQuantity || 0
              const normalQty = item.quantity - saleQty
              const salePrice = item.saleBatchPrice || 0
              const normalPrice = item.productPrice || 0

              let itemTotal = 0
              if (isMultiBatchSplit) {
                itemTotal = (saleQty * salePrice) + (normalQty * normalPrice)
              } else if (item.hasMultiBatch && item.saleBatchPrice != null && item.quantity <= item.saleBatchQuantity) {
                itemTotal = item.saleBatchPrice * item.quantity
              } else {
                const effectivePrice = item.productSalePrice != null ? item.productSalePrice : item.productPrice
                itemTotal = effectivePrice * item.quantity
              }

              return (
                <div
                  key={item.id}
                  className="cart-item-row"
                  style={{
                    gap: '12px',
                    padding: '1rem 8px',
                    background: isChecked ? 'transparent' : '#f8fafc',
                    opacity: isChecked ? 1 : 0.72
                  }}
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => toggleSelectItem(item.productId)}
                    style={{ width: '18px', height: '18px', flexShrink: 0, cursor: 'pointer', accentColor: 'var(--primary)' }}
                    title="Chọn sản phẩm để thanh toán"
                  />
                  <div className="cart-item-img" style={{ flexShrink: 0 }}>
                    <img src={item.productMainImage} alt={item.productName} onError={(e) => { e.target.src = 'https://images.unsplash.com/photo-1542838132-92c53300491e?q=80&w=150&auto=format&fit=crop' }} />
                  </div>
                  
                  <div className="cart-item-details" style={{ flex: '1 1 180px', minWidth: 0 }}>
                    <Link to={`/products/${item.productId}`} className="item-name">{item.productName}</Link>
                    <span className="item-stock-warning" style={{ display: 'block', marginTop: '2px' }}>Kho còn: {item.maxStock} sản phẩm</span>

                    {/* THÔNG BÁO THÔNG MINH KHI ĐƠN HÀNG TÁCH 2 LÔ (LÔ SALE + LÔ MỚI) */}
                    {isMultiBatchSplit && (
                      <div style={{ 
                        background: '#fffbeb', 
                        border: '1px solid #fde68a', 
                        borderRadius: '8px', 
                        padding: '8px 10px', 
                        marginTop: '8px', 
                        fontSize: '0.82rem', 
                        color: '#92400e',
                        lineHeight: 1.4
                      }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '5px', fontWeight: 700, marginBottom: '2px' }}>
                          <Info size={15} color="#d97706" /> Lưu ý giá theo lô:
                        </div>
                        <span>
                          Sản phẩm chỉ còn <strong>{saleQty}</strong> món thuộc lô cận date giá ưu đãi <strong>{salePrice.toLocaleString()}đ</strong>{item.saleBatchExpiryDate ? ` (HSD: ${item.saleBatchExpiryDate})` : ''}. 
                          <strong> {normalQty}</strong> món còn lại tính theo giá tiêu chuẩn <strong>{normalPrice.toLocaleString()}đ</strong> của lô mới.
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="cart-item-price" style={{ minWidth: '130px' }}>
                    {isMultiBatchSplit ? (
                      <div style={{ fontSize: '0.85rem' }}>
                        <div style={{ color: '#d97706', fontWeight: 700 }}>
                          {saleQty} x {salePrice.toLocaleString()}đ <small style={{ color: '#b45309' }}>(Sale)</small>
                        </div>
                        <div style={{ color: '#334155', fontWeight: 600, marginTop: '2px' }}>
                          {normalQty} x {normalPrice.toLocaleString()}đ <small style={{ color: '#64748b' }}>(Gốc)</small>
                        </div>
                      </div>
                    ) : item.hasMultiBatch && item.saleBatchPrice != null && item.quantity <= item.saleBatchQuantity ? (
                      <>
                        <span className="current-price" style={{ color: '#ea580c' }}>{item.saleBatchPrice.toLocaleString()}đ</span>
                        <span className="old-price">{item.productPrice.toLocaleString()}đ</span>
                        <small style={{ display: 'block', color: '#ea580c', fontSize: '0.75rem', fontWeight: 600 }}>⚡ Xả kho cận date</small>
                      </>
                    ) : item.productSalePrice != null ? (
                      <>
                        <span className="current-price">{item.productSalePrice.toLocaleString()}đ</span>
                        <span className="old-price">{item.productPrice.toLocaleString()}đ</span>
                      </>
                    ) : (
                      <span className="current-price">{item.productPrice.toLocaleString()}đ</span>
                    )}
                  </div>

                  <div className="cart-item-qty">
                    <div className="quantity-selector qty-cart">
                      <button onClick={() => handleQtyChange(item.productId, item.quantity - 1)}>-</button>
                      <input
                        type="number"
                        min="1"
                        value={item.quantity}
                        onChange={(e) => {
                          const val = parseInt(e.target.value, 10)
                          if (!isNaN(val) && val >= 1) {
                            handleQtyChange(item.productId, val)
                          }
                        }}
                        style={{
                          width: '46px',
                          textAlign: 'center',
                          border: 'none',
                          background: 'transparent',
                          fontWeight: 600,
                          fontSize: '0.95rem',
                          outline: 'none'
                        }}
                      />
                      <button onClick={() => handleQtyChange(item.productId, item.quantity + 1)}>+</button>
                    </div>
                    {item.quantity > item.maxStock && (
                      <span style={{ fontSize: '0.72rem', color: '#d97706', fontWeight: 600, textAlign: 'center' }}>
                        Vượt tồn kho ({item.maxStock}) - Sẽ kiểm tra ở hàng đợi
                      </span>
                    )}
                    {errorMap[item.productId] && <span className="item-qty-error">{errorMap[item.productId]}</span>}
                  </div>

                  <div className="cart-item-total">
                    <strong>{itemTotal.toLocaleString()}đ</strong>
                  </div>

                  <button className="delete-item-btn" onClick={() => handleRemove(item.productId)} title="Xóa khỏi giỏ hàng">
                    <Trash2 size={18} />
                  </button>
                </div>
              )
            })}
          </div>

          {/* Checkout Summary Card */}
          <div className="cart-summary-card glass">
            <h3>Tóm tắt đơn hàng</h3>
            
            <div className="summary-row">
              <span>Tạm tính ({selectedItems.length} sản phẩm đã chọn)</span>
              <span>{selectedSubtotal.toLocaleString()}đ</span>
            </div>

            <div className="summary-row">
              <span>Phí vận chuyển</span>
              <span className="shipping-free">Miễn phí</span>
            </div>

            <div className="summary-total-row">
              <span>Tổng thanh toán</span>
              <strong>{selectedSubtotal.toLocaleString()}đ</strong>
            </div>

            <button
              onClick={() => navigate('/checkout')}
              disabled={selectedItems.length === 0}
              className="btn btn-primary checkout-btn"
              style={{ opacity: selectedItems.length === 0 ? 0.55 : 1, cursor: selectedItems.length === 0 ? 'not-allowed' : 'pointer' }}
            >
              Tiến hành thanh toán ({selectedItems.length}) <ArrowRight size={18} />
            </button>

            <Link to="/products" className="continue-shopping">Tiếp tục mua sắm</Link>
          </div>
        </div>
      ) : (
        <div className="empty-cart-state glass">
          <ShoppingBag size={64} className="empty-cart-icon" />
          <h3>Giỏ hàng của bạn đang trống!</h3>
          <p>Hãy thêm các sản phẩm tươi sạch và tiêu dùng thiết yếu từ siêu thị MiniMart vào giỏ hàng.</p>
          <Link to="/products" className="btn btn-primary">Mua sắm ngay</Link>
        </div>
      )}
    </div>
  )
}

export default Cart
