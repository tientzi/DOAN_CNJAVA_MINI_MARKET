import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../../services/api'
import { PackageCheck, AlertTriangle, FilePlus2, Search, ArrowUpRight, ShieldCheck, Download, Printer } from 'lucide-react'
import { exportToCSV, printDocument } from '../../utils/exportUtils'
import { matchesRelative } from '../../utils/searchUtils'
import './AdminPages.css'

const InventoryManager = () => {
  const [inventory, setInventory] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterType, setFilterType] = useState('ALL') // ALL, LOW_STOCK, OUT_OF_STOCK

  const fetchInventory = async () => {
    try {
      const response = await api.get('/api/admin/inventory')
      setInventory(response.data)
    } catch (err) {
      console.error('Lỗi khi tải thông tin tồn kho', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchInventory()
  }, [])

  const handleExportCSV = () => {
    const headers = ['Mã SP (ID)', 'Mã SKU', 'Tên sản phẩm', 'Danh mục', 'Tồn hiện tại', 'Tồn tối thiểu', 'Vị trí kệ', 'Trạng thái tồn kho']
    const rows = filteredList.map(item => {
      let status = 'Đủ hàng'
      if (item.currentStock <= 0) status = 'Hết hàng'
      else if (item.currentStock < item.minimumStock) status = 'Sắp hết hàng'
      return [
        item.product?.id || '',
        item.product?.sku || '',
        item.product?.name || '',
        item.product?.category?.name || '',
        item.currentStock || 0,
        item.minimumStock || 0,
        item.location || '',
        status
      ]
    })
    exportToCSV(`Bao_Cao_Ton_Kho_MiniMart_${new Date().toISOString().slice(0, 10)}`, headers, rows)
  }

  const handlePrint = () => {
    printDocument('Báo cáo Tồn kho - Siêu thị MiniMart')
  }

  if (loading) {
    return <div className="loading-state">Đang tải thông tin kho...</div>
  }

  // Lọc dữ liệu
  const filteredList = inventory.filter(item => {
    const matchSearch = matchesRelative([
      item.product?.name,
      item.product?.sku,
      item.product?.barcode,
      item.location
    ], searchTerm)

    if (!matchSearch) return false

    if (filterType === 'OUT_OF_STOCK') return item.currentStock <= 0
    if (filterType === 'LOW_STOCK') return item.currentStock > 0 && item.currentStock < item.minimumStock
    return true
  })

  // Thống kê nhanh
  const totalItems = inventory.reduce((sum, item) => sum + item.currentStock, 0)
  const lowStockCount = inventory.filter(item => item.currentStock > 0 && item.currentStock < item.minimumStock).length
  const outOfStockCount = inventory.filter(item => item.currentStock <= 0).length

  return (
    <div className="admin-crud-page">
      {/* Header chỉ hiển thị khi in */}
      <div className="print-only">
        <div className="print-doc-header">
          <div>
            <h1 className="print-brand-title">SIÊU THỊ TIỆN LỢI MINIMART</h1>
            <p className="print-brand-subtitle">Hotline: 1900 8888 - Địa chỉ: TP. Hồ Chí Minh</p>
          </div>
          <div className="print-doc-meta">
            <div>Ngày in: {new Date().toLocaleString('vi-VN')}</div>
            <div>Báo cáo: Tồn kho & Định mức dự trữ</div>
          </div>
        </div>
        <div className="print-doc-title">
          <h2>BÁO CÁO TỒN KHO HÀNG HÓA</h2>
          <p>Tổng số mặt hàng: {filteredList.length} | Tổng lượng hàng tồn: {totalItems.toLocaleString()} đơn vị</p>
        </div>
      </div>

      <div className="crud-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2>Quản lý Tồn kho (Chỉ xem)</h2>
          <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '4px' }}>
            🔒 Quy trình chuẩn siêu thị: Số lượng tồn chỉ tăng thông qua Phiếu Nhập Kho để đảm bảo tính minh bạch.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button onClick={handleExportCSV} className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <Download size={16} /> Xuất Excel / CSV
          </button>
          <button onClick={handlePrint} className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <Printer size={16} /> In Báo Cáo Tồn Kho
          </button>
          <Link to="/admin/goods-receipts" className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
            <FilePlus2 size={18} /> Tạo phiếu nhập kho
          </Link>
        </div>
      </div>

      {/* Thẻ thống kê */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
        <div className="card glass" style={{ padding: '1rem', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ background: '#e0f2fe', color: '#0284c7', padding: '12px', borderRadius: '8px' }}>
            <PackageCheck size={24} />
          </div>
          <div>
            <span style={{ fontSize: '0.85rem', color: '#64748b' }}>Tổng số lượng tồn</span>
            <h3 style={{ margin: 0, fontSize: '1.4rem' }}>{totalItems.toLocaleString()}</h3>
          </div>
        </div>

        <div className="card glass" style={{ padding: '1rem', display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }} onClick={() => setFilterType('LOW_STOCK')}>
          <div style={{ background: '#fef3c7', color: '#d97706', padding: '12px', borderRadius: '8px' }}>
            <AlertTriangle size={24} />
          </div>
          <div>
            <span style={{ fontSize: '0.85rem', color: '#64748b' }}>Sắp hết hàng (&lt; Tối thiểu)</span>
            <h3 style={{ margin: 0, fontSize: '1.4rem', color: '#d97706' }}>{lowStockCount}</h3>
          </div>
        </div>

        <div className="card glass" style={{ padding: '1rem', display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }} onClick={() => setFilterType('OUT_OF_STOCK')}>
          <div style={{ background: '#fee2e2', color: '#dc2626', padding: '12px', borderRadius: '8px' }}>
            <AlertTriangle size={24} />
          </div>
          <div>
            <span style={{ fontSize: '0.85rem', color: '#64748b' }}>Đã hết hàng (Tồn = 0)</span>
            <h3 style={{ margin: 0, fontSize: '1.4rem', color: '#dc2626' }}>{outOfStockCount}</h3>
          </div>
        </div>
      </div>

      {/* Thanh công cụ tìm kiếm và lọc */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', background: '#fff', border: '1px solid #cbd5e1', borderRadius: '8px', padding: '0.4rem 0.8rem', width: '320px' }}>
          <Search size={16} style={{ color: '#94a3b8', marginRight: '8px' }} />
          <input 
            type="text" 
            placeholder="Tìm theo tên sản phẩm, SKU, kệ..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ border: 'none', outline: 'none', width: '100%', fontSize: '0.9rem' }}
          />
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button 
            className={`btn ${filterType === 'ALL' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setFilterType('ALL')}
            style={{ padding: '6px 12px', fontSize: '0.85rem' }}
          >
            Tất cả ({inventory.length})
          </button>
          <button 
            className={`btn ${filterType === 'LOW_STOCK' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setFilterType('LOW_STOCK')}
            style={{ padding: '6px 12px', fontSize: '0.85rem' }}
          >
            Cần nhập hàng ({lowStockCount})
          </button>
          <button 
            className={`btn ${filterType === 'OUT_OF_STOCK' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setFilterType('OUT_OF_STOCK')}
            style={{ padding: '6px 12px', fontSize: '0.85rem' }}
          >
            Hết hàng ({outOfStockCount})
          </button>
        </div>
      </div>

      {/* Bảng tồn kho - CHỈ XEM (Read-only) */}
      <div className="admin-table-container glass">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Mã SKU</th>
              <th>Tên sản phẩm</th>
              <th>Vị trí kệ</th>
              <th>Tồn kho hiện tại</th>
              <th>Ngưỡng tối thiểu</th>
              <th>Trạng thái kho</th>
              <th>Lần cập nhật cuối</th>
            </tr>
          </thead>
          <tbody>
            {filteredList.map(item => {
              const isOutOfStock = item.currentStock <= 0
              const isLowStock = !isOutOfStock && item.currentStock < item.minimumStock

              return (
                <tr key={item.id}>
                  <td>#{item.id}</td>
                  <td><code>{item.product?.sku || '—'}</code></td>
                  <td>
                    <strong>{item.product?.name || `Product ID: ${item.productId}`}</strong>
                    {item.product?.unit && <small style={{ color: '#64748b', display: 'block' }}>Đơn vị: {item.product.unit}</small>}
                  </td>
                  <td>{item.location || '—'}</td>
                  <td>
                    <span style={{ 
                      fontWeight: 'bold', 
                      fontSize: '1.05rem',
                      color: isOutOfStock ? '#dc2626' : isLowStock ? '#d97706' : '#16a34a' 
                    }}>
                      {item.currentStock}
                    </span>
                  </td>
                  <td>{item.minimumStock}</td>
                  <td>
                    {isOutOfStock ? (
                      <span className="status-pill danger" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <AlertTriangle size={12} /> Hết hàng
                      </span>
                    ) : isLowStock ? (
                      <span className="status-pill warning" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: '#fef3c7', color: '#d97706' }}>
                        <AlertTriangle size={12} /> Sắp hết hàng
                      </span>
                    ) : (
                      <span className="status-pill active" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <ShieldCheck size={12} /> An toàn
                      </span>
                    )}
                  </td>
                  <td>
                    {item.lastUpdated ? new Date(item.lastUpdated).toLocaleString('vi-VN') : '—'}
                  </td>
                </tr>
              )
            })}
            {filteredList.length === 0 && (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                  Không tìm thấy thông tin tồn kho phù hợp.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Chữ ký khi in */}
      <div className="print-only print-signatures">
        <div className="print-sig-col">
          <strong>Người lập biểu</strong>
          <span>(Ký và ghi rõ họ tên)</span>
          <div className="print-sig-space"></div>
        </div>
        <div className="print-sig-col">
          <strong>Thủ kho</strong>
          <span>(Ký và ghi rõ họ tên)</span>
          <div className="print-sig-space"></div>
        </div>
        <div className="print-sig-col">
          <strong>Ban Giám Đốc</strong>
          <span>(Ký và ghi rõ họ tên)</span>
          <div className="print-sig-space"></div>
        </div>
      </div>
    </div>
  )
}

export default InventoryManager
