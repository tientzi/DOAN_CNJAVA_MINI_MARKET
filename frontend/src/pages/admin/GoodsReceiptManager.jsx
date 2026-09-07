import React, { useState, useEffect } from 'react'
import api from '../../services/api'
import { Plus, CheckCircle2, Trash2, Box, Eye, FileText, Download, Printer } from 'lucide-react'
import { exportToCSV, printDocument } from '../../utils/exportUtils'
import './AdminPages.css'

const GoodsReceiptManager = () => {
  const [receipts, setReceipts] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  
  // Create Form State
  const [showForm, setShowForm] = useState(false)
  const [supplierId, setSupplierId] = useState('')
  const [note, setNote] = useState('')
  const [items, setItems] = useState([]) // { productId, quantity, importPrice }
  
  // Selected Receipt state
  const [selectedReceipt, setSelectedReceipt] = useState(null)
  
  const [error, setError] = useState('')

  const fetchData = async () => {
    try {
      const recRes = await api.get('/api/admin/goods-receipts')
      const supRes = await api.get('/api/admin/suppliers')
      const prodRes = await api.get('/api/admin/products')
      
      setReceipts(recRes.data.content || recRes.data)
      setSuppliers(supRes.data.filter(s => s.isActive))
      setProducts(prodRes.data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  const handleExportCSV = () => {
    const headers = ['Mã PNK', 'Ngày tạo', 'Nhà cung cấp', 'Người tạo', 'Tổng tiền (VND)', 'Trạng thái', 'Ghi chú']
    const rows = receipts.map(r => [
      `PNK-${r.id}`,
      new Date(r.createdAt).toLocaleString('vi-VN'),
      r.supplierName || '',
      r.createdBy || '',
      r.totalAmount || 0,
      r.status === 'COMPLETED' ? 'Hoàn thành' : r.status === 'CANCELLED' ? 'Đã hủy' : 'Bản nháp',
      r.note || ''
    ])
    exportToCSV(`Danh_Sach_Phieu_Nhap_Kho_MiniMart_${new Date().toISOString().slice(0, 10)}`, headers, rows)
  }

  const handlePrintReceipt = () => {
    if (!selectedReceipt) return
    printDocument(`Phieu_Nhap_Kho_PNK${selectedReceipt.id}_MiniMart`)
  }

  const handleAddItem = () => {
    setItems([...items, { productId: '', quantity: 1, importPrice: 0, batchName: '', expiryDate: '' }])
  }

  const handleRemoveItem = (index) => {
    const newItems = [...items]
    newItems.splice(index, 1)
    setItems(newItems)
  }

  const handleItemChange = (index, field, value) => {
    const newItems = [...items]
    newItems[index][field] = value
    setItems(newItems)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    
    if (!supplierId) return setError('Vui lòng chọn nhà cung cấp')
    if (items.length === 0) return setError('Cần ít nhất 1 sản phẩm')
    
    // Validate items
    for (let i=0; i<items.length; i++) {
      if (!items[i].productId) return setError('Vui lòng chọn sản phẩm cho tất cả các dòng')
      if (items[i].quantity <= 0) return setError('Số lượng phải lớn hơn 0')
      if (items[i].importPrice < 0) return setError('Giá nhập không được âm')
    }

    try {
      await api.post('/api/admin/goods-receipts', {
        supplierId: Number(supplierId),
        note,
        items: items.map(item => ({
          productId: Number(item.productId),
          quantity: Number(item.quantity),
          importPrice: Number(item.importPrice),
          batchName: item.batchName || null,
          expiryDate: item.expiryDate || null
        }))
      })
      fetchData()
      handleCloseForm()
    } catch (err) {
      setError(err.response?.data?.error || 'Lỗi tạo phiếu nhập')
    }
  }

  const handleComplete = async (id) => {
    if (!window.confirm('Sau khi hoàn thành, số lượng tồn kho sẽ được cộng thêm. Bạn có chắc chắn?')) return
    try {
      await api.put(`/api/admin/goods-receipts/${id}/complete`)
      fetchData()
      if (selectedReceipt && selectedReceipt.id === id) setSelectedReceipt(null)
    } catch (err) {
      alert(err.response?.data?.error || 'Không thể hoàn thành')
    }
  }

  const handleCancel = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy phiếu nhập nháp này?')) return
    try {
      await api.put(`/api/admin/goods-receipts/${id}/cancel`)
      fetchData()
      if (selectedReceipt && selectedReceipt.id === id) setSelectedReceipt(null)
    } catch (err) {
      alert(err.response?.data?.error || 'Không thể hủy')
    }
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setSupplierId('')
    setNote('')
    setItems([])
    setError('')
  }

  const getStatusBadge = (status) => {
    if (status === 'COMPLETED') return <span className="badge-success" style={{padding:'4px 8px', borderRadius:'4px', fontSize:'12px'}}>Hoàn thành</span>
    if (status === 'CANCELLED') return <span className="badge-danger" style={{padding:'4px 8px', borderRadius:'4px', fontSize:'12px'}}>Đã hủy</span>
    return <span className="badge-warning" style={{padding:'4px 8px', borderRadius:'4px', fontSize:'12px'}}>Bản nháp</span>
  }

  if (loading) {
    return <div className="loading-state">Đang tải phiếu nhập kho...</div>
  }

  return (
    <div className="admin-crud-page">
      <div className="crud-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2>Quản lý Phiếu nhập kho</h2>
          <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '4px' }}>
            Lập phiếu và nhập chứng từ từ Nhà cung cấp để cập nhật tồn kho chính thức.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button onClick={handleExportCSV} className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <Download size={16} /> Xuất Excel / CSV
          </button>
          <button onClick={() => setShowForm(true)} className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <Plus size={18} /> Tạo phiếu nhập
          </button>
        </div>
      </div>

      {showForm && (
        <div className="admin-form-overlay">
          <div className="admin-popup-form form-large glass" style={{ maxWidth: '800px' }}>
            <h3>Tạo phiếu nhập kho mới</h3>
            {error && <div className="form-error">{error}</div>}

            <form onSubmit={handleSubmit}>
              <div className="form-grid-2">
                <div className="form-group">
                  <label>Nhà cung cấp *</label>
                  <select required value={supplierId} onChange={e => setSupplierId(e.target.value)}>
                    <option value="">-- Chọn NCC --</option>
                    {suppliers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label>Ghi chú</label>
                  <input type="text" value={note} onChange={e => setNote(e.target.value)} />
                </div>
              </div>

              <div className="receipt-items-container margin-top-md" style={{ background: 'rgba(0,0,0,0.02)', padding: '1rem', borderRadius: '8px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <h4>Chi tiết hàng hóa</h4>
                  <button type="button" onClick={handleAddItem} className="btn btn-outline" style={{ padding: '0.25rem 0.5rem', fontSize: '0.875rem' }}>
                    <Plus size={14} /> Thêm dòng
                  </button>
                </div>
                
                {items.length === 0 && <p style={{ fontSize: '0.9rem', color: 'var(--gray-500)' }}>Chưa có sản phẩm nào được chọn.</p>}
                
                {items.map((item, index) => (
                  <div key={index} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1.5fr 1fr 1fr auto', gap: '0.5rem', marginBottom: '0.5rem', alignItems: 'end' }}>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label style={{ fontSize: '0.8rem' }}>Sản phẩm *</label>
                      <select required value={item.productId} onChange={e => handleItemChange(index, 'productId', e.target.value)}>
                        <option value="">-- Chọn sản phẩm --</option>
                        {products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                      </select>
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label style={{ fontSize: '0.8rem' }}>Số lượng *</label>
                      <input type="number" required min="1" value={item.quantity} onChange={e => handleItemChange(index, 'quantity', e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label style={{ fontSize: '0.8rem' }}>Giá nhập (đ) *</label>
                      <input type="number" required min="0" value={item.importPrice} onChange={e => handleItemChange(index, 'importPrice', e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label style={{ fontSize: '0.8rem' }}>Số lô (Tùy chọn)</label>
                      <input type="text" placeholder="VD: L01-2023" value={item.batchName} onChange={e => handleItemChange(index, 'batchName', e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label style={{ fontSize: '0.8rem' }}>HSD (Tùy chọn)</label>
                      <input type="date" value={item.expiryDate} onChange={e => handleItemChange(index, 'expiryDate', e.target.value)} />
                    </div>
                    <button type="button" onClick={() => handleRemoveItem(index)} className="btn btn-outline text-danger" style={{ padding: '0.5rem' }}>
                      <Trash2 size={18} />
                    </button>
                  </div>
                ))}
                
                <div style={{ marginTop: '1rem', textAlign: 'right', fontWeight: 'bold' }}>
                  Tổng cộng: {items.reduce((sum, item) => sum + (Number(item.quantity) * Number(item.importPrice)), 0).toLocaleString()}đ
                </div>
              </div>

              <div className="form-actions margin-top-md">
                <button type="submit" className="btn btn-primary">Lưu nháp</button>
                <button type="button" onClick={handleCloseForm} className="btn btn-outline">Hủy</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {selectedReceipt && (
        <div className="admin-form-overlay receipt-modal-overlay">
          <div className="admin-popup-form form-large glass receipt-modal-content" style={{ maxWidth: '750px' }}>
            {/* Header cho bản in */}
            <div className="print-only">
              <div className="print-doc-header">
                <div>
                  <h1 className="print-brand-title">SIÊU THỊ TIỆN LỢI MINIMART</h1>
                  <p className="print-brand-subtitle">Hotline: 1900 8888 - Địa chỉ: TP. Hồ Chí Minh</p>
                </div>
                <div className="print-doc-meta">
                  <div>Mã chứng từ: <strong>PNK-{selectedReceipt.id}</strong></div>
                  <div>Ngày lập: {new Date(selectedReceipt.createdAt).toLocaleString('vi-VN')}</div>
                </div>
              </div>
              <div className="print-doc-title">
                <h2>PHIẾU NHẬP KHO HÀNG HÓA</h2>
                <p>Nhà cung cấp: <strong>{selectedReceipt.supplierName}</strong> | Trạng thái: {selectedReceipt.status === 'COMPLETED' ? 'Đã hoàn thành nhập kho' : 'Phiếu nháp'}</p>
              </div>
            </div>

            <div className="no-print" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3>Chi tiết Phiếu Nhập #{selectedReceipt.id}</h3>
              {getStatusBadge(selectedReceipt.status)}
            </div>
            
            <div style={{ margin: '1rem 0', padding: '1rem', background: 'rgba(255,255,255,0.5)', borderRadius: '8px' }}>
              <p><strong>Nhà cung cấp:</strong> {selectedReceipt.supplierName}</p>
              <p><strong>Ngày tạo:</strong> {new Date(selectedReceipt.createdAt).toLocaleString('vi-VN')}</p>
              <p><strong>Người tạo:</strong> {selectedReceipt.createdBy || 'Thủ kho'}</p>
              {selectedReceipt.note && <p><strong>Ghi chú:</strong> {selectedReceipt.note}</p>}
            </div>

            <table className="admin-table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Sản phẩm</th>
                  <th>SL</th>
                  <th>Giá nhập</th>
                  <th>Lô / HSD</th>
                  <th>Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                {selectedReceipt.items.map(item => (
                  <tr key={item.id}>
                    <td>{item.sku}</td>
                    <td>{item.productName}</td>
                    <td style={{ textAlign: 'center' }}>{item.quantity}</td>
                    <td style={{ textAlign: 'right' }}>{item.importPrice.toLocaleString()}đ</td>
                    <td>{item.batchName ? `${item.batchName} (${item.expiryDate ? new Date(item.expiryDate).toLocaleDateString('vi-VN') : '-'})` : '-'}</td>
                    <td style={{ textAlign: 'right', fontWeight: 'bold' }}>{(item.quantity * item.importPrice).toLocaleString()}đ</td>
                  </tr>
                ))}
              </tbody>
            </table>
            
            <div style={{ textAlign: 'right', marginTop: '1rem', fontSize: '1.2rem' }}>
              <strong>Tổng tiền: <span className="text-primary">{selectedReceipt.totalAmount.toLocaleString()}đ</span></strong>
            </div>

            {/* Chữ ký khi in */}
            <div className="print-only print-signatures">
              <div className="print-sig-col">
                <strong>Đại diện Giao Hàng (NCC)</strong>
                <span>(Ký và ghi rõ họ tên)</span>
                <div className="print-sig-space"></div>
              </div>
              <div className="print-sig-col">
                <strong>Thủ Kho Nhận Hàng</strong>
                <span>(Ký và ghi rõ họ tên)</span>
                <div className="print-sig-space"></div>
              </div>
              <div className="print-sig-col">
                <strong>Kế Toán / Giám Đốc</strong>
                <span>(Ký và ghi rõ họ tên)</span>
                <div className="print-sig-space"></div>
              </div>
            </div>

            <div className="form-actions margin-top-md no-print" style={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem' }}>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <button type="button" onClick={handlePrintReceipt} className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <Printer size={16} /> In Phiếu Nhập Kho
                </button>
                {selectedReceipt.status === 'DRAFT' && (
                  <>
                    <button onClick={() => handleComplete(selectedReceipt.id)} className="btn btn-primary" style={{ background: 'var(--success)' }}>
                      <CheckCircle2 size={18} /> Hoàn thành & Cộng kho
                    </button>
                    <button onClick={() => handleCancel(selectedReceipt.id)} className="btn btn-outline text-danger">
                      Hủy phiếu
                    </button>
                  </>
                )}
              </div>
              <button onClick={() => setSelectedReceipt(null)} className="btn btn-outline">Đóng</button>
            </div>
          </div>
        </div>
      )}

      <div className="admin-table-container glass">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Ngày tạo</th>
              <th>Nhà cung cấp</th>
              <th>Tổng tiền</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {receipts.map(rec => (
              <tr key={rec.id}>
                <td>#{rec.id}</td>
                <td>{new Date(rec.createdAt).toLocaleDateString('vi-VN')}</td>
                <td><strong>{rec.supplierName}</strong></td>
                <td>{rec.totalAmount.toLocaleString()}đ</td>
                <td>{getStatusBadge(rec.status)}</td>
                <td>
                  <button onClick={() => setSelectedReceipt(rec)} className="btn btn-outline" style={{ padding: '4px 8px', fontSize: '0.85rem' }}>
                    <Eye size={14} style={{ marginRight: '4px' }}/> Xem
                  </button>
                </td>
              </tr>
            ))}
            {receipts.length === 0 && (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '2rem' }}>Chưa có phiếu nhập kho nào</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default GoodsReceiptManager
