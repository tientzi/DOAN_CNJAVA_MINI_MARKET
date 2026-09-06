import React, { useState, useEffect } from 'react'
import api from '../../services/api'
import { ClipboardList, ArrowUpRight, ArrowDownRight, RefreshCw, AlertCircle } from 'lucide-react'
import './AdminPages.css'

const InventoryLedgerManager = () => {
  const [ledgers, setLedgers] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const fetchLedgers = async (pageNumber = 0) => {
    setLoading(true)
    try {
      const response = await api.get(`/api/admin/inventory-ledger?page=${pageNumber}&size=20`)
      setLedgers(response.data.content)
      setTotalPages(response.data.totalPages)
    } catch (err) {
      console.error('Không thể lấy lịch sử kho', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchLedgers(page)
  }, [page])

  const getChangeTypeIcon = (type) => {
    switch (type) {
      case 'IMPORT': return <ArrowUpRight size={16} className="text-success" />
      case 'SELL': return <ArrowDownRight size={16} className="text-danger" />
      case 'RETURN': return <ArrowUpRight size={16} className="text-warning" />
      case 'DAMAGE': return <ArrowDownRight size={16} className="text-danger" />
      case 'MANUAL_ADJUST': return <RefreshCw size={16} className="text-info" />
      default: return <AlertCircle size={16} />
    }
  }

  const getChangeTypeLabel = (type) => {
    switch (type) {
      case 'IMPORT': return 'Nhập kho'
      case 'SELL': return 'Bán hàng'
      case 'RETURN': return 'Khách hoàn trả'
      case 'DAMAGE': return 'Báo hỏng'
      case 'MANUAL_ADJUST': return 'Điều chỉnh thủ công'
      default: return type
    }
  }

  return (
    <div className="admin-crud-page">
      <div className="crud-header">
        <h2><ClipboardList size={24} style={{ marginRight: '8px', verticalAlign: 'middle' }}/> Lịch sử Kho (Inventory Ledger)</h2>
        <button onClick={() => fetchLedgers(page)} className="btn btn-outline">
          <RefreshCw size={16} /> Làm mới
        </button>
      </div>

      <div className="admin-table-container glass">
        {loading ? (
          <div className="loading-state">Đang tải dữ liệu...</div>
        ) : (
          <>
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Thời gian</th>
                  <th>Sản phẩm</th>
                  <th>Loại biến động</th>
                  <th>Thay đổi</th>
                  <th>Tồn trước</th>
                  <th>Tồn sau</th>
                  <th>Ghi chú</th>
                  <th>Người thực hiện</th>
                </tr>
              </thead>
              <tbody>
                {ledgers.length > 0 ? (
                  ledgers.map(l => (
                    <tr key={l.id}>
                      <td>#{l.id}</td>
                      <td>{new Date(l.createdAt).toLocaleString('vi-VN')}</td>
                      <td><strong>{l.productName}</strong></td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          {getChangeTypeIcon(l.changeType)}
                          {getChangeTypeLabel(l.changeType)}
                        </div>
                      </td>
                      <td>
                        <strong className={l.quantityChange > 0 ? 'text-success' : l.quantityChange < 0 ? 'text-danger' : ''}>
                          {l.quantityChange > 0 ? `+${l.quantityChange}` : l.quantityChange}
                        </strong>
                      </td>
                      <td>{l.previousStock}</td>
                      <td><strong>{l.newStock}</strong></td>
                      <td style={{ maxWidth: '200px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={l.note}>
                        {l.note}
                      </td>
                      <td>{l.createdBy}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="9" style={{ textAlign: 'center', padding: '2rem' }}>Không có dữ liệu lịch sử kho.</td>
                  </tr>
                )}
              </tbody>
            </table>

            {/* Phân trang */}
            {totalPages > 1 && (
              <div className="pagination" style={{ display: 'flex', justifyContent: 'center', gap: '1rem', marginTop: '1rem', padding: '1rem' }}>
                <button 
                  className="btn btn-outline" 
                  disabled={page === 0} 
                  onClick={() => setPage(p => p - 1)}
                >
                  Trang trước
                </button>
                <span style={{ alignSelf: 'center' }}>Trang {page + 1} / {totalPages}</span>
                <button 
                  className="btn btn-outline" 
                  disabled={page >= totalPages - 1} 
                  onClick={() => setPage(p => p + 1)}
                >
                  Trang sau
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

export default InventoryLedgerManager
