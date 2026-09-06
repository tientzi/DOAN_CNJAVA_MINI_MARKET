import React, { useState, useEffect, useContext } from 'react'
import { useNavigate } from 'react-router-dom'
import { CartContext } from '../contexts/CartContext'
import { AuthContext } from '../contexts/AuthContext'
import api from '../services/api'
import { MapPin, Plus, CheckCircle, Wallet, Tag, QrCode, X, Copy, Check, Smartphone, Banknote, CreditCard } from 'lucide-react'
import './Checkout.css'

const DEFAULT_METHODS = [
  {
    id: 1,
    methodCode: 'COD',
    methodName: 'Thanh toán khi nhận hàng (COD)',
    description: 'Thanh toán tiền mặt cho shipper khi nhận được hàng.',
    isEnabled: true,
    icon: 'Banknote'
  },
  {
    id: 2,
    methodCode: 'BANK_TRANSFER',
    methodName: 'Chuyển khoản Ngân hàng (VietQR)',
    description: 'Quét mã VietQR chuyển khoản nhanh 24/7. Miễn phí giao dịch.',
    bankName: 'MB Bank (Ngân hàng Quân Đội)',
    accountNumber: '0336174371',
    accountHolder: 'NGUYEN TIEN TAI',
    transferSyntax: 'MINIMART {MA_DON}',
    qrCodeUrl: '/uploads/vietqr.png',
    isEnabled: true,
    icon: 'QrCode'
  },
  {
    id: 3,
    methodCode: 'MOMO',
    methodName: 'Ví điện tử MoMo',
    description: 'Quét mã QR MoMo để thanh toán. Nhanh chóng và an toàn.',
    bankName: 'Ví điện tử MoMo',
    accountNumber: '0336174371',
    accountHolder: 'NGUYEN TIEN TAI',
    transferSyntax: 'MINIMART {MA_DON}',
    qrCodeUrl: '/uploads/momo_qr.png',
    isEnabled: true,
    icon: 'Smartphone'
  }
]

const Checkout = () => {
  const { cart, cartSubtotal, clearCart } = useContext(CartContext)
  const { user } = useContext(AuthContext)
  const navigate = useNavigate()

  // Addresses
  const [addresses, setAddresses] = useState([])
  const [selectedAddressId, setSelectedAddressId] = useState(null)
  
  // New address form
  const [showAddForm, setShowAddForm] = useState(false)
  const [receiverName, setReceiverName] = useState('')
  const [receiverPhone, setReceiverPhone] = useState('')
  const [province, setProvince] = useState('')
  const [district, setDistrict] = useState('')
  const [ward, setWard] = useState('')
  const [detailAddress, setDetailAddress] = useState('')
  const [isDefault, setIsDefault] = useState(false)
  
  // Coupon
  const [couponCode, setCouponCode] = useState('')
  const [appliedCoupon, setAppliedCoupon] = useState('')
  const [discountAmount, setDiscountAmount] = useState(0)
  const [couponError, setCouponError] = useState('')
  const [couponSuccess, setCouponSuccess] = useState('')

  // Payment methods
  const [paymentMethods, setPaymentMethods] = useState(DEFAULT_METHODS)
  const [paymentMethod, setPaymentMethod] = useState('COD')
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // QR Modal
  const [showQRModal, setShowQRModal] = useState(false)
  const [activeQRConfig, setActiveQRConfig] = useState(null)
  const [qrOrderInfo, setQrOrderInfo] = useState('')
  const [copiedSTK, setCopiedSTK] = useState(false)
  const [qrConfirming, setQrConfirming] = useState(false)

  const fetchAddresses = async () => {
    try {
      const response = await api.get('/api/addresses')
      setAddresses(response.data)
      const defaultAddr = response.data.find(addr => addr.isDefault)
      if (defaultAddr) {
        setSelectedAddressId(defaultAddr.id)
      } else if (response.data.length > 0) {
        setSelectedAddressId(response.data[0].id)
      }
    } catch (err) {
      console.error('Không thể lấy địa chỉ nhận hàng', err)
    }
  }

  const fetchPaymentMethods = async () => {
    try {
      const response = await api.get('/api/public/payment-methods')
      if (response.data && response.data.length > 0) {
        const activeOnes = response.data.filter(m => m.isEnabled)
        setPaymentMethods(activeOnes)
        if (activeOnes.length > 0) {
          setPaymentMethod(activeOnes[0].methodCode)
        }
      }
    } catch (err) {
      console.warn('Dùng cấu hình thanh toán mặc định', err)
    }
  }

  useEffect(() => {
    if (cart.items.length === 0) {
      navigate('/cart')
      return
    }
    fetchAddresses()
    fetchPaymentMethods()
  }, [])

  const handleAddAddress = async (e) => {
    e.preventDefault()
    setError('')
    if (!/^\d{10}$/.test(receiverPhone)) {
      setError('Số điện thoại phải có đúng 10 chữ số')
      return
    }
    try {
      const response = await api.post('/api/addresses', {
        receiverName, receiverPhone, province, district, ward, detailAddress, isDefault
      })
      setAddresses(prev => [...prev, response.data])
      setSelectedAddressId(response.data.id)
      setShowAddForm(false)
      setReceiverName(''); setReceiverPhone(''); setProvince('')
      setDistrict(''); setWard(''); setDetailAddress(''); setIsDefault(false)
    } catch (err) {
      setError(err.response?.data?.error || 'Không thể thêm địa chỉ mới.')
    }
  }

  const handleApplyCoupon = async (e) => {
    e.preventDefault()
    setCouponError(''); setCouponSuccess(''); setDiscountAmount(0); setAppliedCoupon('')
    if (!couponCode.trim()) return
    try {
      const response = await api.post('/api/public/coupons/apply', { code: couponCode, amount: cartSubtotal })
      setDiscountAmount(response.data.discountAmount)
      setAppliedCoupon(response.data.code)
      setCouponSuccess(`Áp dụng thành công! Giảm ${response.data.discountAmount.toLocaleString()}đ`)
    } catch (err) {
      setCouponError(err.response?.data?.error || 'Mã giảm giá không hợp lệ')
    }
  }

  const submitOrder = async () => {
    const addr = addresses.find(a => a.id === selectedAddressId)
    const fullAddrString = `${addr.detailAddress}, ${addr.ward}, ${addr.district}, ${addr.province}`
    setLoading(true)
    try {
      await api.post('/api/orders', {
        shippingName: addr.receiverName,
        shippingPhone: addr.receiverPhone,
        shippingAddress: fullAddrString,
        paymentMethod,
        couponCode: appliedCoupon || null,
        note
      })
      clearCart()
      navigate('/orders?success=true')
    } catch (err) {
      setError(err.response?.data?.error || 'Đã xảy ra lỗi khi tạo đơn hàng.')
      setShowQRModal(false)
    } finally {
      setLoading(false)
      setQrConfirming(false)
    }
  }

  // Calculate Tier Discount
  let tierDiscount = 0
  let tierPercentage = 0
  let tierName = ''
  if (user?.membershipTier === 'SILVER') {
    tierPercentage = 2
    tierName = 'Bạc'
  } else if (user?.membershipTier === 'GOLD') {
    tierPercentage = 5
    tierName = 'Vàng'
  } else if (user?.membershipTier === 'DIAMOND') {
    tierPercentage = 8
    tierName = 'Kim Cương'
  }
  tierDiscount = (cartSubtotal * tierPercentage) / 100
  const finalTotal = Math.max(0, cartSubtotal - discountAmount - tierDiscount)

  const handlePlaceOrder = async () => {
    setError('')
    if (!selectedAddressId) {
      setError('Vui lòng chọn địa chỉ giao hàng')
      return
    }

    const currentMethod = paymentMethods.find(m => m.methodCode === paymentMethod)
    if (paymentMethod !== 'COD' && currentMethod) {
      const randCode = Date.now().toString().slice(-6)
      const transferSyntax = currentMethod.transferSyntax 
        ? currentMethod.transferSyntax.replace('{MA_DON}', randCode)
        : `MINIMART DH${randCode}`
      setQrOrderInfo(transferSyntax)
      setActiveQRConfig(currentMethod)
      setShowQRModal(true)
    } else {
      await submitOrder()
    }
  }

  const handleQRConfirm = async () => {
    setQrConfirming(true)
    await submitOrder()
  }

  const handleCopySTK = () => {
    if (activeQRConfig?.accountNumber) {
      navigator.clipboard.writeText(activeQRConfig.accountNumber)
      setCopiedSTK(true)
      setTimeout(() => setCopiedSTK(false), 2000)
    }
  }

  const getMethodIcon = (code) => {
    switch (code) {
      case 'COD':
        return <Banknote size={20} className="payment-icon" />
      case 'MOMO':
        return <Smartphone size={20} className="payment-icon" />
      case 'BANK_TRANSFER':
      default:
        return <QrCode size={20} className="payment-icon" />
    }
  }

  return (
    <div className="checkout-page">
      <h1 className="page-title">Thanh toán đơn hàng</h1>

      {error && <div className="checkout-general-error">{error}</div>}

      <div className="checkout-grid">
        {/* Left Column */}
        <div className="checkout-steps">
          {/* Bước 1: Địa chỉ giao hàng */}
          <div className="checkout-step-card glass">
            <div className="step-card-header">
              <h3><MapPin size={22} className="step-icon" /> 1. Địa chỉ giao hàng</h3>
              {!showAddForm && (
                <button onClick={() => setShowAddForm(true)} className="btn btn-outline add-addr-btn">
                  <Plus size={16} /> Thêm địa chỉ mới
                </button>
              )}
            </div>

            {showAddForm ? (
              <form onSubmit={handleAddAddress} className="add-address-form">
                <h4>Nhập thông tin địa chỉ mới</h4>
                <div className="form-grid-addr">
                  <div className="form-group">
                    <label>Họ tên người nhận</label>
                    <input type="text" required value={receiverName} onChange={(e) => setReceiverName(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>Số điện thoại</label>
                    <input type="text" required value={receiverPhone} onChange={(e) => setReceiverPhone(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>Tỉnh / Thành phố</label>
                    <input type="text" required value={province} onChange={(e) => setProvince(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>Quận / Huyện (VD: Quận Tân Phú, Quận Tân Bình, Quận 12...)</label>
                    <input type="text" required value={district} onChange={(e) => setDistrict(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>Phường / Xã</label>
                    <input type="text" required value={ward} onChange={(e) => setWard(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>Địa chỉ chi tiết (Số nhà, tên đường...)</label>
                    <input type="text" required value={detailAddress} onChange={(e) => setDetailAddress(e.target.value)} />
                  </div>
                </div>
                <div className="form-checkbox-addr">
                  <label>
                    <input type="checkbox" checked={isDefault} onChange={(e) => setIsDefault(e.target.checked)} />
                    <span> Đặt làm địa chỉ mặc định</span>
                  </label>
                </div>
                <div className="form-addr-actions">
                  <button type="submit" className="btn btn-primary">Lưu địa chỉ</button>
                  <button type="button" onClick={() => setShowAddForm(false)} className="btn btn-outline">Hủy bỏ</button>
                </div>
              </form>
            ) : (
              <div className="addresses-list">
                {addresses.length > 0 ? (
                  addresses.map(addr => (
                    <div
                      key={addr.id}
                      className={`address-item-card ${selectedAddressId === addr.id ? 'selected' : ''}`}
                      onClick={() => setSelectedAddressId(addr.id)}
                    >
                      <div className="addr-check">
                        <CheckCircle size={20} className="check-icon" />
                      </div>
                      <div className="addr-info">
                        <div className="addr-meta">
                          <strong>{addr.receiverName}</strong>
                          <span className="addr-phone">{addr.receiverPhone}</span>
                          {addr.isDefault && <span className="default-tag">Mặc định</span>}
                        </div>
                        <p>{`${addr.detailAddress}, ${addr.ward}, ${addr.district}, ${addr.province}`}</p>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className="no-address-warn">Bạn chưa cấu hình địa chỉ nào. Hãy bấm "Thêm địa chỉ mới" để tiến hành đặt hàng!</p>
                )}
              </div>
            )}
          </div>

          {/* Bước 2: Phương thức thanh toán */}
          <div className="checkout-step-card glass">
            <h3><Wallet size={22} className="step-icon" /> 2. Phương thức thanh toán</h3>
            <div className="payment-options">
              {paymentMethods.map(method => (
                <label key={method.methodCode} className={`payment-option-card ${paymentMethod === method.methodCode ? 'selected' : ''}`}>
                  <input
                    type="radio"
                    name="payment"
                    checked={paymentMethod === method.methodCode}
                    onChange={() => setPaymentMethod(method.methodCode)}
                  />
                  <div className="payment-desc">
                    <strong>
                      {getMethodIcon(method.methodCode)}
                      <span style={{ marginLeft: '8px' }}>{method.methodName}</span>
                    </strong>
                    <p>{method.description}</p>
                  </div>
                </label>
              ))}
            </div>

            {/* Preview QR khi chọn chuyển khoản / QR */}
            {paymentMethod !== 'COD' && (
              <div className="qr-preview-hint">
                <QrCode size={16} />
                <span>Mã QR thanh toán chính thức sẽ hiển thị ngay khi bạn bấm xác nhận đặt hàng</span>
              </div>
            )}
          </div>

          {/* Ghi chú đơn hàng */}
          <div className="checkout-step-card glass">
            <h3>Ghi chú cho shipper</h3>
            <textarea
              rows="3"
              placeholder="Nhập ghi chú giao hàng (ví dụ: giao giờ hành chính, gọi trước khi đến...)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="note-textarea"
            ></textarea>
          </div>
        </div>

        {/* Right Column: Order Summary & Coupon */}
        <div className="checkout-summary-col">
          {/* Coupon Card */}
          <div className="checkout-summary-card glass margin-bottom-summary">
            <h3><Tag size={20} /> Mã giảm giá (Coupon)</h3>
            <form onSubmit={handleApplyCoupon} className="coupon-form">
              <input
                type="text"
                placeholder="Nhập mã coupon..."
                value={couponCode}
                onChange={(e) => setCouponCode(e.target.value)}
                disabled={!!appliedCoupon}
              />
              {appliedCoupon ? (
                <button type="button" onClick={() => { setAppliedCoupon(''); setDiscountAmount(0); setCouponSuccess(''); setCouponCode('') }} className="btn btn-outline remove-coupon-btn">
                  Xóa
                </button>
              ) : (
                <button type="submit" className="btn btn-primary">Áp dụng</button>
              )}
            </form>
            {couponSuccess && <span className="coupon-alert success-text">{couponSuccess}</span>}
            {couponError && <span className="coupon-alert error-text">{couponError}</span>}
          </div>

          {/* Summary Card */}
          <div className="checkout-summary-card glass">
            <h3>Chi tiết đơn hàng</h3>
            <div className="order-items-preview">
              {cart.items.map(item => {
                const price = item.productSalePrice != null ? item.productSalePrice : item.productPrice
                return (
                  <div key={item.id} className="preview-item-row">
                    <span className="preview-name">{item.productName} <strong>x{item.quantity}</strong></span>
                    <span className="preview-price">{(price * item.quantity).toLocaleString()}đ</span>
                  </div>
                )
              })}
            </div>

            <div className="summary-details-box">
              <div className="summary-row">
                <span>Tổng tiền hàng</span>
                <span>{cartSubtotal.toLocaleString()}đ</span>
              </div>
              {discountAmount > 0 && (
                <div className="summary-row">
                  <span>Mã giảm giá Voucher {appliedCoupon ? `(${appliedCoupon})` : ''}</span>
                  <span className="discount-value-summary">-{discountAmount.toLocaleString()}đ</span>
                </div>
              )}
              {tierDiscount > 0 && (
                <div className="summary-row">
                  <span>Ưu đãi thành viên {tierName} (-{tierPercentage}%)</span>
                  <span className="discount-value-summary" style={{ color: '#16a34a', fontWeight: 700 }}>
                    -{tierDiscount.toLocaleString()}đ
                  </span>
                </div>
              )}
              <div className="summary-row">
                <span>Phí vận chuyển</span>
                {user?.membershipTier === 'DIAMOND' ? (
                  <span className="shipping-free" style={{ color: '#0284c7', fontWeight: 700 }}>Miễn phí 100% (Đặc quyền Kim Cương)</span>
                ) : user?.membershipTier === 'GOLD' ? (
                  <span className="shipping-free" style={{ color: '#d97706', fontWeight: 700 }}>Giảm 30% phí ship (Đặc quyền Vàng)</span>
                ) : (
                  <span className="shipping-free">Miễn phí giao hàng</span>
                )}
              </div>
              <div className="summary-total-row">
                <span>Tổng thanh toán</span>
                <strong>{finalTotal.toLocaleString()}đ</strong>
              </div>
            </div>

            <button
              onClick={handlePlaceOrder}
              disabled={loading || addresses.length === 0}
              className="btn btn-primary place-order-btn"
            >
              {loading ? 'Đang xử lý đặt hàng...' : 'Xác nhận đặt hàng'}
            </button>
          </div>
        </div>
      </div>

      {/* ====== QR PAYMENT MODAL ====== */}
      {showQRModal && activeQRConfig && (
        <div className="qr-modal-overlay" onClick={(e) => e.target === e.currentTarget && setShowQRModal(false)}>
          <div className="qr-modal">
            {/* Header */}
            <div className="qr-modal-header">
              <div className="qr-modal-title">
                <QrCode size={22} />
                <span>Thanh toán {activeQRConfig.methodName}</span>
              </div>
              <button className="qr-close-btn" onClick={() => setShowQRModal(false)}>
                <X size={20} />
              </button>
            </div>

            {/* QR Body */}
            <div className="qr-modal-body">
              {/* Amount badge */}
              <div className="qr-amount-badge">
                <span className="qr-amount-label">Số tiền cần chuyển</span>
                <span className="qr-amount-value">{finalTotal.toLocaleString()}<small>đ</small></span>
              </div>

              {/* QR Image */}
              <div className="qr-image-wrapper">
                <img
                  src={activeQRConfig.qrCodeUrl || '/uploads/vietqr.png'}
                  alt={`Mã QR ${activeQRConfig.methodName}`}
                  className="qr-image"
                  onError={(e) => { 
                    e.target.onerror = null;
                    const quickQR = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(
                      `${activeQRConfig.accountNumber || '0336174371'}|${activeQRConfig.accountHolder || 'NGUYEN TIEN TAI'}|${finalTotal}|${qrOrderInfo}`
                    )}`;
                    e.target.src = quickQR;
                  }}
                />
              </div>

              {/* Bank Info */}
              <div className="qr-bank-info">
                <div className="qr-bank-row">
                  <span className="qr-bank-label">Ngân hàng / Ví</span>
                  <span className="qr-bank-value">{activeQRConfig.bankName || 'Ngân hàng'}</span>
                </div>
                <div className="qr-bank-row">
                  <span className="qr-bank-label">Số tài khoản / SĐT</span>
                  <div className="qr-stk-row">
                    <span className="qr-bank-value qr-stk">{activeQRConfig.accountNumber}</span>
                    <button className="qr-copy-btn" onClick={handleCopySTK}>
                      {copiedSTK ? <Check size={14} /> : <Copy size={14} />}
                      {copiedSTK ? 'Đã chép' : 'Sao chép'}
                    </button>
                  </div>
                </div>
                <div className="qr-bank-row">
                  <span className="qr-bank-label">Chủ tài khoản</span>
                  <span className="qr-bank-value">{activeQRConfig.accountHolder}</span>
                </div>
                <div className="qr-bank-row">
                  <span className="qr-bank-label">Nội dung chuyển khoản</span>
                  <span className="qr-bank-value qr-note">{qrOrderInfo}</span>
                </div>
              </div>

              <p className="qr-instruction">
                📱 Mở App Ngân hàng hoặc Ví điện tử → Quét mã QR hoặc Chuyển tiền với nội dung trên, sau đó bấm <strong>Tôi đã chuyển khoản thành công</strong> bên dưới.
              </p>
            </div>

            {/* Footer actions */}
            <div className="qr-modal-footer">
              <button className="btn btn-outline" onClick={() => setShowQRModal(false)}>
                Quay lại
              </button>
              <button
                className="btn btn-primary qr-confirm-btn"
                onClick={handleQRConfirm}
                disabled={qrConfirming}
              >
                {qrConfirming ? 'Đang xử lý...' : '✅ Tôi đã chuyển khoản thành công'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Checkout
