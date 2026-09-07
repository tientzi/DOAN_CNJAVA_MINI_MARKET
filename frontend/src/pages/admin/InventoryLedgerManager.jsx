import React, { useState, useEffect } from 'react'
import api from '../../services/api'
import { ClipboardList, ArrowUpRight, ArrowDownRight, RefreshCw, AlertCircle, Download, Printer } from 'lucide-react'
import { exportToCSV, printDocument } from '../../utils/exportUtils'
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
      setLedgers(response.data.content || response.data)
      setTotalPages(response.data.totalPages || 1)
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

  const handleExportCSV = () => {
    const headers = ['Mã GD', 'Thời gian', 'Sản phẩm', 'Loại biến động', 'Thay đổi', 'Tồn trước', 'Tồn sau', 'Ghi chú', 'Người thực hiện']
    const rows = ledgers.map(l => [
      `#${l.id}`,
      new Date(l.createdAt).toLocaleString('vi-VN'),
      l.productName || '',
      getChangeTypeLabel(l.changeType),
      l.quantityChange > 0 ? `+${l.quantityChange}` : l.quantityChange,
      l.previousStock || 0,
      l.newStock || 0,
      l.note || '',
      l.createdBy || ''
    ])
    exportToCSV(`So_Cai_Kho_MiniMart_${new Date().toISOString().slice(0, 10)}`, headers, rows)
  }

  const handlePrint = () => {
    printDocument('Sổ Cái Biến Động Kho - Siêu thị MiniMart')
  }

  return (
    <div className="admin-crud-page">
      {/* Header cho bản in */}
      <div className="print-only">
        <div className="print-doc-header">
          <div>
            <h1 className="print-brand-title">SIÊU THỊ TIỆN LỢI MINIMART</h1>
            <p className="print-brand-subtitle">Hotline: 1900 8888 - Địa chỉ: TP. Hồ Chí Minh</p>
          </div>
          <div className="print-doc-meta">
            <div>Ngày in: {new Date().toLocaleString('vi-VN')}</div>
            <div>Báo cáo: Sổ cái biến động kho</div>
          </div>
        </div>
        <div className="print-doc-title">
          <h2>SỔ CÁI BIẾN ĐỘNG KHO HÀNG (INVENTORY LEDGER)</h2>
          <p>Trang {page + 1} / {totalPages || 1}</p>
        </div>
      </div>

      <div className="crud-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <h2><ClipboardList size={24} style={{ marginRight: '8px', verticalAlign: 'middle' }}/> Lịch sử Kho (Inventory Ledger)</h2>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button onClick={handleExportCSV} className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <Download size={16} /> Xuất Excel / CSV
          </button>
          <button onClick={handlePrint} className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <Printer size={16} /> In Sổ Cái Kho
          </button>
          <button onClick={() => fetchLedgers(page)} className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <RefreshCw size={16} /> Làm mới
          </button>
        </div>
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
          <strong>Kế toán trưởng</strong>
          <span>(Ký và ghi rõ họ tên)</span>
          <div className="print-sig-space"></div>
        </div>
      </div>
    </div>
  )
}

export default InventoryLedgerManager
