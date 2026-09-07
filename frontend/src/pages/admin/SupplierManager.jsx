import React, { useState, useEffect } from 'react'
import api from '../../services/api'
import { Plus, Edit2, Trash2, ShieldCheck, ShieldAlert, Search, X } from 'lucide-react'
import { matchesRelative } from '../../utils/searchUtils'
import './AdminPages.css'

const SupplierManager = () => {
  const [suppliers, setSuppliers] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')

  // Form states
  const [showForm, setShowForm] = useState(false)
  const [editId, setEditId] = useState(null)
  
  const [name, setName] = useState('')
  const [contactName, setContactName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [address, setAddress] = useState('')
  const [isActive, setIsActive] = useState(true)
  const [error, setError] = useState('')

  const fetchSuppliers = async () => {
    try {
      const response = await api.get('/api/admin/suppliers')
      setSuppliers(response.data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchSuppliers()
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    
    if (!name.trim()) {
      setError('Tên nhà cung cấp không được để trống')
      return
    }

    const payload = { name, contactName, phone, email, address, isActive }

    try {
      if (editId) {
        await api.put(`/api/admin/suppliers/${editId}`, payload)
      } else {
        await api.post('/api/admin/suppliers', payload)
      }
      fetchSuppliers()
      handleCloseForm()
    } catch (err) {
      setError(err.response?.data?.error || 'Lỗi khi lưu nhà cung cấp')
    }
  }

  const handleEdit = (sup) => {
    setEditId(sup.id)
    setName(sup.name)
    setContactName(sup.contactName || '')
    setPhone(sup.phone || '')
    setEmail(sup.email || '')
    setAddress(sup.address || '')
    setIsActive(sup.isActive)
    setShowForm(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa nhà cung cấp này?')) return
    try {
      await api.delete(`/api/admin/suppliers/${id}`)
      fetchSuppliers()
    } catch (err) {
      alert(err.response?.data?.error || 'Không thể xóa nhà cung cấp')
    }
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditId(null)
    setName('')
    setContactName('')
    setPhone('')
    setEmail('')
    setAddress('')
    setIsActive(true)
    setError('')
  }

  if (loading) {
    return <div className="loading-state">Đang tải nhà cung cấp...</div>
  }

  return (
    <div className="admin-crud-page">
      <div className="crud-header">
        <h2>Quản lý Nhà cung cấp</h2>
        <button onClick={() => setShowForm(true)} className="btn btn-primary">
          <Plus size={18} /> Thêm nhà cung cấp
        </button>
      </div>

      {showForm && (
        <div className="admin-form-overlay">
          <form onSubmit={handleSubmit} className="admin-popup-form form-large glass">
            <h3>{editId ? 'Cập nhật nhà cung cấp' : 'Thêm nhà cung cấp mới'}</h3>
            {error && <div className="form-error">{error}</div>}

            <div className="form-grid-2">
              <div className="form-group">
                <label>Tên nhà cung cấp *</label>
                <input type="text" required value={name} onChange={(e) => setName(e.target.value)} />
              </div>
              <div className="form-group">
                <label>Người liên hệ</label>
                <input type="text" value={contactName} onChange={(e) => setContactName(e.target.value)} />
              </div>
              <div className="form-group">
                <label>Số điện thoại</label>
                <input type="text" value={phone} onChange={(e) => setPhone(e.target.value)} />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
            </div>
            
            <div className="form-group margin-top-sm">
              <label>Địa chỉ</label>
              <textarea rows="2" value={address} onChange={(e) => setAddress(e.target.value)}></textarea>
            </div>

            <div className="form-checkbox margin-top-sm">
              <label>
                <input type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)} />
                <span> Hoạt động</span>
              </label>
            </div>

            <div className="form-actions margin-top-md">
              <button type="submit" className="btn btn-primary">Lưu lại</button>
              <button type="button" onClick={handleCloseForm} className="btn btn-outline">Hủy bỏ</button>
            </div>
          </form>
        </div>
      )}

      {/* Thanh tìm kiếm tương đối */}
      <div style={{
        display: 'flex',
        gap: '12px',
        alignItems: 'center',
        flexWrap: 'wrap',
        marginBottom: '16px',
        background: '#ffffff',
        padding: '12px 16px',
        borderRadius: '12px',
        border: '1px solid #e2e8f0',
        boxShadow: '0 2px 4px rgba(0,0,0,0.02)'
      }}>
        <div style={{ position: 'relative', flex: 1, minWidth: '240px' }}>
          <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
          <input
            type="text"
            placeholder="Tìm kiếm nhà cung cấp (gõ không dấu: vietgap, acecook, sdt, dia chi...)"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              width: '100%',
              padding: '9px 36px 9px 38px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '0.92rem',
              outline: 'none'
            }}
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
            >
              <X size={16} />
            </button>
          )}
        </div>

        {searchTerm && (
          <button
            onClick={() => setSearchTerm('')}
            className="btn btn-outline"
            style={{ padding: '8px 14px', fontSize: '0.85rem' }}
          >
            Đặt lại
          </button>
        )}

        <span style={{ fontSize: '0.85rem', color: '#64748b', marginLeft: 'auto' }}>
          Hiển thị: <strong>{suppliers.filter(s => matchesRelative([s.name, s.contactName, s.phone, s.email, s.address], searchTerm)).length}</strong> / {suppliers.length} NCC
        </span>
      </div>

      <div className="admin-table-container glass">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên NCC</th>
              <th>Người liên hệ</th>
              <th>Điện thoại</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {suppliers.filter(s => matchesRelative([s.name, s.contactName, s.phone, s.email, s.address], searchTerm)).length > 0 ? (
              suppliers.filter(s => matchesRelative([s.name, s.contactName, s.phone, s.email, s.address], searchTerm)).map(sup => (
                <tr key={sup.id}>
                  <td>#{sup.id}</td>
                  <td><strong>{sup.name}</strong></td>
                  <td>{sup.contactName}</td>
                  <td>{sup.phone}</td>
                  <td>
                    <span className={`status-pill ${sup.isActive ? 'active' : 'inactive'}`}>
                      {sup.isActive ? <ShieldCheck size={12} /> : <ShieldAlert size={12} />}
                      {sup.isActive ? 'Hoạt động' : 'Tạm dừng'}
                    </span>
                  </td>
                  <td>
                    <div className="table-actions">
                      <button onClick={() => handleEdit(sup)} className="action-btn edit"><Edit2 size={16} /></button>
                      <button onClick={() => handleDelete(sup.id)} className="action-btn delete"><Trash2 size={16} /></button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '2.5rem', color: '#64748b' }}>
                  Không tìm thấy nhà cung cấp nào phù hợp với từ khóa "{searchTerm}".
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default SupplierManager
