import React, { useState, useEffect, useContext } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import api from '../services/api'
import { CartContext } from '../contexts/CartContext'
import { ClipboardList, ShoppingBag, CreditCard, RotateCcw, QrCode, X, Copy, Check, Clock } from 'lucide-react'
import './OrderHistory.css'
import './Checkout.css'

const OrderHistory = () => {
  const [orders, setOrders] = useState([])
  const [paymentMethods, setPaymentMethods] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { fetchCart } = useContext(CartContext)

  // Modal Thanh toán lại cho đơn UNPAID
  const [retryOrder, setRetryOrder] = useState(null)
  const [retryQRConfig, setRetryQRConfig] = useState(null)
  const [retryConfirming, setRetryConfirming] = useState(false)
  const [retryCountdown, setRetryCountdown] = useState(10)
  const [copiedSTK, setCopiedSTK] = useState(false)
  const [reorderingId, setReorderingId] = useState(null)

  const fetchOrders = async () => {
    try {
      const response = await api.get('/api/orders/my-orders')
      setOrders(response.data)
    } catch (err) {
      console.error('Không thể lấy lịch sử đơn hàng', err)
      setError('Đã xảy ra lỗi khi tải lịch sử đơn hàng.')
    } finally {
      setLoading(false)
    }
  }

  const fetchPaymentMethods = async () => {
    try {
      const res = await api.get('/api/public/payment-methods')
      setPaymentMethods(res.data || [])
    } catch {
      // ignore
    }
  }

  useEffect(() => {
    fetchOrders()
    fetchPaymentMethods()
    if (searchParams.get('success')) {
      setSuccess('Đặt hàng thành công! Cảm ơn bạn đã ủng hộ MiniMart.')
      setTimeout(() => {
        setSuccess('')
      }, 4000)
    } else if (searchParams.get('unpaid')) {
      setError('Quá 10 giây chưa xác nhận chuyển khoản. Đơn hàng đã được lưu vào Lịch sử với trạng thái Chưa thanh toán — bạn có thể bấm "Thanh toán lại" hoặc "Đặt hàng lại".')
    }
  }, [searchParams])

  // Tự động làm mới ngầm mỗi 5 giây nếu đang có đơn chờ duyệt/hàng đợi (không hiện đếm ngược 30s)
  useEffect(() => {
    const hasPending = orders.some(o =>
      o.status === 'CHO_TIEP_NHAN' || (o.status === 'CHO_XAC_NHAN' && o.paymentStatus === 'PENDING')
    )
    if (!hasPending) return
    const timer = setInterval(fetchOrders, 5000)
    return () => clearInterval(timer)
  }, [orders])

  // Đếm ngược 10 giây trong Modal Thanh toán lại
  useEffect(() => {
    if (!retryOrder || retryConfirming) return
    if (retryCountdown <= 0) {
      setRetryOrder(null)
      setError('Đã hết 10 giây xác nhận chuyển khoản. Đơn hàng vẫn được giữ ở trạng thái Chưa thanh toán.')
      return
    }
    const timer = setTimeout(() => setRetryCountdown(c => c - 1), 1000)
    return () => clearTimeout(timer)
  }, [retryOrder, retryCountdown, retryConfirming])

  const handleOpenRetryPayment = (order) => {
    setError('')
    setSuccess('')
    const matched = paymentMethods.find(m =>
      (m.methodCode || m.methodKey || m.code) === order.paymentMethod
    ) || {
      methodName: order.paymentMethod === 'MOMO' ? 'Ví MoMo' : 'Chuyển khoản Ngân hàng (VietQR)',
      bankName: order.paymentMethod === 'MOMO' ? 'MoMo' : 'MBBank',
      accountNumber: '0336174371',
      accountHolder: 'NGUYEN TIEN TAI',
      qrCodeUrl: '/uploads/vietqr.png'
    }
    setRetryQRConfig(matched)
    setRetryCountdown(10)
    setRetryOrder(order)
  }

  const handleConfirmRetryPayment = async () => {
    if (!retryOrder) return
    setRetryConfirming(true)
    setError('')
    try {
      await api.post(`/api/orders/${retryOrder.id}/retry-payment`)
      setRetryOrder(null)
      setSuccess(`Đã xác nhận thanh toán cho đơn #OD${retryOrder.id}! Đơn hàng đang chờ Admin kiểm tra và duyệt.`)
      await fetchOrders()
    } catch (err) {
      setError(err.response?.data?.error || 'Không thể xác nhận thanh toán lại')
      setRetryOrder(null)
    } finally {
      setRetryConfirming(false)
    }
  }

  const handleReorder = async (order) => {
    setError('')
    setSuccess('')
    setReorderingId(order.id)
    try {
      await api.post(`/api/orders/${order.id}/reorder`)
      await fetchCart()
      navigate('/cart')
    } catch (err) {
      setError(err.response?.data?.error || 'Không thể đưa sản phẩm vào giỏ hàng để đặt lại')
    } finally {
      setReorderingId(null)
    }
  }

  const handleCancelOrder = async (orderId) => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy đơn hàng này không?')) return

    setError('')
    setSuccess('')
    try {
      await api.put(`/api/orders/${orderId}/cancel`)
      setSuccess('Đã hủy đơn hàng thành công và hoàn trả tồn kho.')
      fetchOrders()
    } catch (err) {
      setError(err.response?.data?.error || 'Không thể hủy đơn hàng')
    }
  }

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'CHO_TIEP_NHAN': return 'badge-warning'
      case 'CHO_XAC_NHAN': return 'badge-warning'
      case 'DA_XAC_NHAN': return 'badge-info'
      case 'DANG_GIAO': return 'badge-primary'
      case 'HOAN_THANH': return 'badge-success'
      case 'TU_CHOI':
      case 'HUY': return 'badge-danger'
      default: return ''
    }
  }

  const getStatusText = (status) => {
    switch (status) {
      case 'CHO_TIEP_NHAN': return 'Đang chờ hàng đợi'
      case 'CHO_XAC_NHAN': return 'Chờ xác nhận'
      case 'DA_XAC_NHAN': return 'Đã xác nhận'
      case 'DA_NHAN_DON': return 'Shipper nhận đơn'
      case 'DANG_GIAO': return 'Đang giao hàng'
      case 'DA_GIAO': return 'Đã giao hàng'
      case 'HOAN_THANH': return 'Hoàn thành'
      case 'TU_CHOI': return 'Từ chối (Thiếu kho)'
      case 'HUY': return 'Đã hủy'
      default: return status
    }
  }

  if (loading) {
    return <div className="loading-state">Đang tải lịch sử đơn hàng...</div>
  }

  return (
    <div className="orders-page">
      <h1 className="page-title"><ClipboardList size={28} /> Lịch sử đơn hàng</h1>

      {success && <div className="order-alert success-alert">{success}</div>}
      {error && <div className="order-alert error-alert">{error}</div>}

      {orders.length > 0 ? (
        <div className="orders-list-container">
          {orders.map(order => {
            const isUnpaid = order.paymentStatus === 'UNPAID' && (order.status === 'CHO_XAC_NHAN' || order.status === 'CHO_TIEP_NHAN')
            const canReorder = isUnpaid || order.status === 'HUY' || order.status === 'TU_CHOI' || order.status === 'HOAN_THANH'

            return (
              <div key={order.id} className="order-card-item glass">
                <div className="order-item-header">
                  <div className="order-meta-info">
                    <span>Mã đơn: <strong>#OD{order.id}</strong></span>
                    <span className="dot">•</span>
                    <span>Ngày đặt: {new Date(order.createdAt).toLocaleString('vi-VN')}</span>
                  </div>
                  <span className={`status-badge ${getStatusBadgeClass(order.status)}`}>
                    {getStatusText(order.status)}
                  </span>
                </div>

                <div className="order-item-products">
                  {order.items.map(item => (
                    <div key={item.id} className="item-prod-row">
                      <img src={item.productImage} alt={item.productName} className="item-prod-img" onError={(e) => { e.target.src = 'https://images.unsplash.com/photo-1542838132-92c53300491e?q=80&w=150&auto=format&fit=crop' }} />
                      <div className="item-prod-details">
                        <h4>{item.productName}</h4>
                        <span>Số lượng: {item.quantity}</span>
                      </div>
                      <span className="item-prod-price">{item.price.toLocaleString()}đ</span>
                    </div>
                  ))}
                </div>

                <div className="order-item-footer">
                  <div className="order-shipping-details">
                    <p>Người nhận: <strong>{order.shippingName}</strong> ({order.shippingPhone})</p>
                    <p>Địa chỉ: {order.shippingAddress}</p>
                    {order.note && <p>Ghi chú: <em>"{order.note}"</em></p>}
                  </div>

                  <div className="order-payment-box">
                    <div className="payment-summary-row">
                      <span>Tổng tiền hàng:</span>
                      <span>{order.totalAmount.toLocaleString()}đ</span>
                    </div>
                    {order.discountAmount > 0 && (
                      <div className="payment-summary-row text-discount-row">
                        <span>Mã giảm giá:</span>
                        <span>-{order.discountAmount.toLocaleString()}đ</span>
                      </div>
                    )}
                    <div className="payment-summary-row final-row">
                      <span>Tổng thanh toán:</span>
                      <strong>{order.finalAmount.toLocaleString()}đ</strong>
                    </div>
                    <span className="payment-method-badge">{order.paymentMethod === 'COD' ? 'Thanh toán COD' : 'Chuyển khoản'}</span>

                    {order.paymentMethod !== 'COD' && order.paymentMethod !== 'TIEN_MAT' && (
                      <span style={{
                        display: 'inline-block',
                        marginTop: '6px',
                        padding: '4px 10px',
                        borderRadius: '6px',
                        fontSize: '0.8rem',
                        fontWeight: 700,
                        background: order.paymentStatus === 'COMPLETED' ? '#dcfce7'
                          : order.paymentStatus === 'UNPAID' ? '#ffedd5'
                          : order.paymentStatus === 'FAILED' ? '#fee2e2'
                          : '#fef3c7',
                        color: order.paymentStatus === 'COMPLETED' ? '#15803d'
                          : order.paymentStatus === 'UNPAID' ? '#c2410c'
                          : order.paymentStatus === 'FAILED' ? '#b91c1c'
                          : '#b45309'
                      }}>
                        {order.paymentStatus === 'COMPLETED' ? 'Đã thanh toán thành công'
                          : order.paymentStatus === 'UNPAID' ? 'Chưa thanh toán'
                          : order.paymentStatus === 'FAILED' ? 'Thanh toán không thành công'
                          : 'Chờ Admin xác nhận'}
                      </span>
                    )}

                    {order.deliveryNote && (order.status === 'HUY' || order.status === 'TU_CHOI') && (
                      <div style={{ fontSize: '0.78rem', color: '#dc2626', marginTop: '4px', fontStyle: 'italic' }}>
                        {order.deliveryNote}
                      </div>
                    )}

                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '8px', justifyContent: 'flex-end' }}>
                      {isUnpaid && (
                        <button
                          onClick={() => handleOpenRetryPayment(order)}
                          className="btn btn-primary"
                          style={{ padding: '0.5rem 0.9rem', fontSize: '0.85rem', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                        >
                          <CreditCard size={15} /> Thanh toán lại
                        </button>
                      )}

                      {canReorder && (
                        <button
                          onClick={() => handleReorder(order)}
                          disabled={reorderingId === order.id}
                          className="btn btn-outline"
                          style={{ padding: '0.5rem 0.9rem', fontSize: '0.85rem', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                        >
                          <RotateCcw size={15} /> {reorderingId === order.id ? 'Đang chuyển...' : 'Đặt hàng lại'}
                        </button>
                      )}

                      {(order.status === 'CHO_TIEP_NHAN' || order.status === 'CHO_XAC_NHAN' || order.status === 'DA_XAC_NHAN' || order.status === 'DA_NHAN_DON') && (
                        <button
                          onClick={() => handleCancelOrder(order.id)}
                          className="btn btn-outline cancel-order-btn"
                        >
                          Hủy đơn hàng
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      ) : (
        <div className="empty-orders-state glass">
          <ShoppingBag size={64} className="empty-icon" />
          <h3>Bạn chưa đặt đơn hàng nào!</h3>
          <p>Hãy chọn những thực phẩm tươi sạch và tạo đơn hàng đầu tiên của bạn.</p>
          <a href="/products" className="btn btn-primary">Mua sắm ngay</a>
        </div>
      )}

      {/* Modal Thanh toán lại cho đơn Chưa thanh toán */}
      {retryOrder && retryQRConfig && (
        <div className="qr-modal-overlay" onClick={(e) => e.target === e.currentTarget && setRetryOrder(null)}>
          <div className="qr-modal">
            <div className="qr-modal-header">
              <div className="qr-modal-title">
                <QrCode size={22} />
                <span>Thanh toán lại đơn #OD{retryOrder.id}</span>
              </div>
              <button className="qr-close-btn" onClick={() => setRetryOrder(null)}>
                <X size={20} />
              </button>
            </div>

            <div className="qr-modal-body">
              <div className="qr-amount-badge">
                <span className="qr-amount-label">Số tiền cần chuyển</span>
                <span className="qr-amount-value">{retryOrder.finalAmount.toLocaleString()}<small>đ</small></span>
              </div>

              <div className="qr-image-wrapper">
                <img
                  src={retryQRConfig.qrCodeUrl || '/uploads/vietqr.png'}
                  alt="Mã QR thanh toán"
                  className="qr-image"
                  onError={(e) => {
                    e.target.onerror = null
                    e.target.src = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(
                      `${retryQRConfig.accountNumber || '0336174371'}|${retryQRConfig.accountHolder || 'NGUYEN TIEN TAI'}|${retryOrder.finalAmount}|MINIMART DH${retryOrder.id}`
                    )}`
                  }}
                />
              </div>

              <div className="qr-bank-info">
                <div className="qr-bank-row">
                  <span className="qr-bank-label">Ngân hàng / Ví</span>
                  <span className="qr-bank-value">{retryQRConfig.bankName || 'Ngân hàng'}</span>
                </div>
                <div className="qr-bank-row">
                  <span className="qr-bank-label">Số tài khoản / SĐT</span>
                  <div className="qr-stk-row">
                    <span className="qr-bank-value qr-stk">{retryQRConfig.accountNumber}</span>
                    <button
                      className="qr-copy-btn"
                      onClick={() => {
                        navigator.clipboard.writeText(retryQRConfig.accountNumber || '')
                        setCopiedSTK(true)
                        setTimeout(() => setCopiedSTK(false), 2000)
                      }}
                    >
                      {copiedSTK ? <Check size={14} /> : <Copy size={14} />}
                      {copiedSTK ? 'Đã chép' : 'Sao chép'}
                    </button>
                  </div>
                </div>
                <div className="qr-bank-row">
                  <span className="qr-bank-label">Chủ tài khoản</span>
                  <span className="qr-bank-value">{retryQRConfig.accountHolder}</span>
                </div>
                <div className="qr-bank-row">
                  <span className="qr-bank-label">Nội dung chuyển khoản</span>
                  <span className="qr-bank-value qr-note">MINIMART DH{retryOrder.id}</span>
                </div>
              </div>

              <div style={{
                background: retryCountdown <= 3 ? '#fee2e2' : '#fff7ed',
                border: `1px solid ${retryCountdown <= 3 ? '#fca5a5' : '#fdba74'}`,
                color: retryCountdown <= 3 ? '#b91c1c' : '#c2410c',
                borderRadius: '10px',
                padding: '10px 14px',
                marginBottom: '12px',
                textAlign: 'center',
                fontWeight: 700,
                fontSize: '0.92rem'
              }}>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <Clock size={16} /> Vui lòng xác nhận đã chuyển khoản trong: <span style={{ fontSize: '1.15rem' }}>{retryCountdown}s</span>
                </span>
              </div>
            </div>

            <div className="qr-modal-footer">
              <button className="btn btn-outline" onClick={() => setRetryOrder(null)}>
                Đóng
              </button>
              <button
                className="btn btn-primary qr-confirm-btn"
                onClick={handleConfirmRetryPayment}
                disabled={retryConfirming}
              >
                {retryConfirming ? 'Đang xử lý...' : 'Tôi đã chuyển khoản thành công'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default OrderHistory
