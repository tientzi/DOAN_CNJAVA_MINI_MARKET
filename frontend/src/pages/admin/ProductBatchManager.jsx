import React, { useState, useEffect } from 'react'
import api from '../../services/api'
import { Calendar, AlertTriangle, Percent, Tag, Check, X, ArrowRight } from 'lucide-react'
import './AdminPages.css'

const ProductBatchManager = () => {
  const [batches, setBatches] = useState([])
  const [expiringBatches, setExpiringBatches] = useState([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('EXPIRING') // EXPIRING, ALL

  // Modal / Sale configuration
  const [selectedBatch, setSelectedBatch] = useState(null)
  const [discountPercent, setDiscountPercent] = useState('')
  const [customPrice, setCustomPrice] = useState('')
  const [saleMode, setSaleMode] = useState('PERCENT') // 'PERCENT' or 'PRICE'
  const [applying, setApplying] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')

  const fetchBatches = async () => {
    setLoading(true)
    try {
      const [allRes, expRes] = await Promise.all([
        api.get('/api/admin/batches'),
        api.get('/api/admin/batches/expiring?days=30')
      ])
      setBatches(allRes.data)
      setExpiringBatches(expRes.data)
    } catch (err) {
      console.error('Lỗi khi tải thông tin lô hàng', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchBatches()
  }, [])

  const openSaleModal = (batch) => {
    setSelectedBatch(batch)
    setDiscountPercent('')
    setCustomPrice('')
    setSaleMode('PERCENT')
    setSuccessMessage('')
  }

  const closeSaleModal = () => {
    setSelectedBatch(null)
    setDiscountPercent('')
    setCustomPrice('')
  }

  // Tính trước giá sau giảm
  const calculatePreviewPrice = (originalPrice) => {
    if (!originalPrice) return 0
    if (saleMode === 'PERCENT') {
      const pct = parseFloat(discountPercent)
      if (isNaN(pct) || pct <= 0 || pct >= 100) return originalPrice
      return Math.round(originalPrice * (100 - pct) / 100)
    } else {
      const price = parseFloat(customPrice)
      if (isNaN(price) || price <= 0 || price >= originalPrice) return originalPrice
      return price
    }
  }

  const handleApplySale = async (e) => {
    e.preventDefault()
    if (!selectedBatch) return

    const payload = {}
    if (saleMode === 'PERCENT') {
      const pct = parseInt(discountPercent, 10)
      if (!pct || pct <= 0 || pct >= 100) {
        alert('Vui lòng nhập mức % giảm giá hợp lệ từ 1% đến 99%')
        return
      }
      payload.discountPercentage = pct
    } else {
      const price = parseFloat(customPrice)
      if (!price || price <= 0) {
        alert('Vui lòng nhập mức giá sale hợp lệ lớn hơn 0')
        return
      }
      payload.customSalePrice = price
    }

    setApplying(true)
    try {
      await api.post(`/api/admin/batches/${selectedBatch.productId}/clearance-sale`, payload)
      setSuccessMessage(`✅ Đã áp dụng giảm giá cho sản phẩm ${selectedBatch.productName} thành công!`)
      setTimeout(() => {
        closeSaleModal()
        fetchBatches()
      }, 1200)
    } catch (err) {
      alert(err.response?.data?.error || 'Lỗi khi áp dụng giá khuyến mãi')
    } finally {
      setApplying(false)
    }
  }

  const isExpired = (dateString) => new Date(dateString) < new Date()

  const getStatusBadge = (dateString) => {
    if (isExpired(dateString)) {
      return <span className="badge-danger" style={{ padding: '4px 8px', borderRadius: '4px', fontSize: '12px', background: '#fee2e2', color: '#dc2626' }}>Đã hết hạn</span>
    }
    const daysLeft = Math.ceil((new Date(dateString) - new Date()) / (1000 * 60 * 60 * 24))
    return (
      <span className="badge-warning" style={{ padding: '4px 8px', borderRadius: '4px', fontSize: '12px', background: '#fef3c7', color: '#d97706' }}>
        Còn {daysLeft} ngày
      </span>
    )
  }

  if (loading) return <div className="loading-state">Đang tải thông tin lô hàng...</div>

  const displayList = activeTab === 'ALL' ? batches : expiringBatches

  return (
    <div className="admin-crud-page">
      <div className="crud-header">
        <div>
          <h2>Quản lý Lô & Hạn sử dụng (Sale Cận Date)</h2>
          <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '4px' }}>
            Theo dõi hạn sử dụng các lô hàng và chủ động áp dụng mức giảm giá tùy ý để xả hàng cận date.
          </p>
        </div>
      </div>

      <div className="admin-tabs margin-bottom-md" style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid #ddd', paddingBottom: '0.5rem' }}>
        <button 
          className={`btn ${activeTab === 'EXPIRING' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('EXPIRING')}
          style={{ position: 'relative' }}
        >
          <AlertTriangle size={16} style={{ marginRight: '6px' }} />
          Cảnh báo cận Date (30 ngày)
          {expiringBatches.length > 0 && (
            <span style={{ position: 'absolute', top: '-8px', right: '-8px', background: '#dc2626', color: 'white', borderRadius: '50%', width: '22px', height: '22px', fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              {expiringBatches.length}
            </span>
          )}
        </button>
        <button 
          className={`btn ${activeTab === 'ALL' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('ALL')}
        >
          Tất cả lô hàng ({batches.length})
        </button>
      </div>

      <div className="admin-table-container glass">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID Lô</th>
              <th>Mã SKU</th>
              <th>Tên sản phẩm</th>
              <th>Tên Lô</th>
              <th>Số lượng còn</th>
              <th>Hạn sử dụng</th>
              <th>% Đang Sale</th>
              <th>Trạng thái</th>
              <th>Thao tác Sale</th>
            </tr>
          </thead>
          <tbody>
            {displayList.map(b => (
              <tr key={b.id}>
                <td>#{b.id}</td>
                <td><code>{b.productSku}</code></td>
                <td><strong>{b.productName}</strong></td>
                <td>{b.batchName || 'Lô mặc định'}</td>
                <td><strong style={{ color: '#0284c7' }}>{b.quantity}</strong></td>
                <td>
                  <Calendar size={14} style={{ marginRight: '4px', verticalAlign: 'middle', color: isExpired(b.expiryDate) ? 'red' : 'inherit' }} />
                  <span style={{ color: isExpired(b.expiryDate) ? 'red' : 'inherit', fontWeight: isExpired(b.expiryDate) ? 'bold' : 'normal' }}>
                    {new Date(b.expiryDate).toLocaleDateString('vi-VN')}
                  </span>
                </td>
                <td>
                  {b.discountPercentage && b.discountPercentage > 0 ? (
                    <span 
                      style={{ 
                        background: '#fee2e2', 
                        color: '#dc2626', 
                        padding: '4px 10px', 
                        borderRadius: '20px', 
                        fontWeight: 800, 
                        fontSize: '0.85rem',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '2px',
                        border: '1px solid #fecaca',
                        cursor: 'help'
                      }}
                      title={b.originalPrice && b.salePrice ? `Giá gốc: ${b.originalPrice.toLocaleString()}đ ➔ Giá sale: ${b.salePrice.toLocaleString()}đ` : 'Đang xả kho giảm giá'}
                    >
                      -{b.discountPercentage}%
                    </span>
                  ) : (
                    <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>—</span>
                  )}
                </td>
                <td>{getStatusBadge(b.expiryDate)}</td>
                <td>
                  <button 
                    onClick={() => openSaleModal(b)} 
                    className="btn btn-primary" 
                    style={{ padding: '5px 10px', fontSize: '0.85rem', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                  >
                    <Percent size={14} /> Thiết lập Sale
                  </button>
                </td>
              </tr>
            ))}
            {displayList.length === 0 && (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                  {activeTab === 'EXPIRING' ? 'Không có lô hàng nào sắp hết hạn trong 30 ngày tới.' : 'Chưa có lô hàng nào.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* MODAL THIẾT LẬP MỨC SALE TÙY Ý CÓ PREVIEW */}
      {selectedBatch && (
        <div className="admin-form-overlay" onClick={(e) => e.target === e.currentTarget && closeSaleModal()}>
          <form onSubmit={handleApplySale} className="admin-popup-form glass" style={{ maxWidth: '480px', width: '90%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Tag size={20} color="#d97706" /> Thiết lập giá Sale xả hàng
              </h3>
              <button type="button" onClick={closeSaleModal} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <p style={{ margin: '0 0 1rem 0', fontSize: '0.9rem', color: '#475569' }}>
              Sản phẩm: <strong>{selectedBatch.productName}</strong> ({selectedBatch.productSku})
            </p>

            {/* Chọn chế độ nhập */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '1rem' }}>
              <button
                type="button"
                className={`btn ${saleMode === 'PERCENT' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setSaleMode('PERCENT')}
                style={{ flex: 1, padding: '6px' }}
              >
                Giảm theo %
              </button>
              <button
                type="button"
                className={`btn ${saleMode === 'PRICE' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setSaleMode('PRICE')}
                style={{ flex: 1, padding: '6px' }}
              >
                Nhập giá Sale trực tiếp
              </button>
            </div>

            {saleMode === 'PERCENT' ? (
              <div className="form-group">
                <label>Nhập mức giảm giá mong muốn (%):</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <input
                    type="number"
                    min="1"
                    max="99"
                    required
                    placeholder="Nhập % (Ví dụ: 15, 30, 50...)"
                    value={discountPercent}
                    onChange={(e) => setDiscountPercent(e.target.value)}
                    autoFocus
                    style={{ flex: 1 }}
                  />
                  <span style={{ fontWeight: 'bold', fontSize: '1.1rem' }}>%</span>
                </div>
                {/* Gợi ý mức giảm nhanh */}
                <div style={{ display: 'flex', gap: '6px', marginTop: '6px' }}>
                  {[10, 20, 30, 50].map(p => (
                    <button
                      key={p}
                      type="button"
                      onClick={() => setDiscountPercent(p.toString())}
                      style={{ padding: '3px 8px', fontSize: '0.8rem', borderRadius: '4px', border: '1px solid #cbd5e1', background: '#f8fafc', cursor: 'pointer' }}
                    >
                      -{p}%
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="form-group">
                <label>Nhập giá bán Sale trực tiếp (VNĐ):</label>
                <input
                  type="number"
                  min="1000"
                  step="500"
                  required
                  placeholder="Nhập giá mới..."
                  value={customPrice}
                  onChange={(e) => setCustomPrice(e.target.value)}
                  autoFocus
                />
              </div>
            )}

            {/* BẢNG PREVIEW GIÁ MỚI */}
            <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '8px', padding: '0.85rem', marginBottom: '1.2rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                <span style={{ fontSize: '0.85rem', color: '#166534' }}>Mức điều chỉnh:</span>
                <strong style={{ color: '#16a34a' }}>
                  {saleMode === 'PERCENT' ? `Giảm ${discountPercent || 0}%` : `Định giá trực tiếp`}
                </strong>
              </div>
              <p style={{ margin: 0, fontSize: '0.85rem', color: '#15803d' }}>
                ✨ Giá sale mới sẽ được cập nhật ngay vào cơ sở dữ liệu và hiển thị trực tiếp ra trang chủ cửa hàng.
              </p>
            </div>

            {successMessage && (
              <div style={{ color: '#16a34a', fontWeight: 'bold', marginBottom: '1rem', textAlign: 'center' }}>
                {successMessage}
              </div>
            )}

            <div className="form-actions" style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <button type="button" onClick={closeSaleModal} className="btn btn-outline" disabled={applying}>
                Hủy bỏ
              </button>
              <button type="submit" className="btn btn-primary" disabled={applying}>
                {applying ? 'Đang lưu...' : 'Xác nhận áp dụng Sale'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}

export default ProductBatchManager
