import React, { useState, useEffect } from 'react'
import api from '../../services/api'
import {
  Layers, RefreshCw, Play, Pause, AlertTriangle, RotateCcw,
  Trash2, Search, CheckCircle2, XCircle, Clock, X
} from 'lucide-react'
import './AdminPages.css'

const QueueManager = () => {
  const [summary, setSummary] = useState({
    streamLength: 0,
    deadLetterLength: 0,
    retryLength: 0,
    isPaused: false,
    status: 'RUNNING',
    dbPendingCount: 0,
    dbProcessingCount: 0,
    waitingPaymentCount: 0,
    dbSucceededCount: 0,
    dbFailedCount: 0,
    totalJobs: 0
  })
  const [jobs, setJobs] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(true)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [notice, setNotice] = useState(null)

  const showNotice = (type, text) => {
    setNotice({ type, text })
    setTimeout(() => setNotice(null), 4000)
  }

  const fetchSummary = async () => {
    try {
      const res = await api.get('/api/admin/queue/summary')
      setSummary(res.data)
    } catch (err) {
      console.warn('Lỗi tải thông số hàng đợi:', err)
    }
  }

  const fetchJobs = async (page = 0) => {
    try {
      const res = await api.get('/api/admin/queue/jobs', {
        params: {
          status: statusFilter,
          keyword: searchTerm.trim(),
          page,
          size: 15
        }
      })
      setJobs(res.data.content || [])
      setTotalPages(res.data.totalPages || 0)
      setCurrentPage(res.data.number || 0)
    } catch (err) {
      console.error('Lỗi tải danh sách Job:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchSummary()
    fetchJobs(0)
  }, [statusFilter])

  useEffect(() => {
    if (!autoRefresh) return
    const timer = setInterval(() => {
      fetchSummary()
      fetchJobs(currentPage)
    }, 3000)
    return () => clearInterval(timer)
  }, [autoRefresh, currentPage, statusFilter, searchTerm])

  const handleTogglePause = async () => {
    try {
      const res = await api.post('/api/admin/queue/toggle-pause')
      setSummary(prev => ({ ...prev, isPaused: res.data.isPaused, status: res.data.status }))
      showNotice(res.data.isPaused ? 'warn' : 'success', res.data.message)
      fetchSummary()
      fetchJobs(currentPage)
    } catch (err) {
      showNotice('error', err.response?.data?.error || 'Không thể thay đổi trạng thái Worker')
    }
  }

  const handleSimulateFailure = async () => {
    try {
      const res = await api.post('/api/admin/queue/simulate-failure')
      showNotice('warn', res.data.message)
      fetchSummary()
      fetchJobs(0)
    } catch (err) {
      showNotice('error', err.response?.data?.error || 'Không thể tạo Job lỗi thử nghiệm')
    }
  }

  const handleRetryJob = async (jobId) => {
    try {
      const res = await api.post(`/api/admin/queue/jobs/${jobId}/retry`)
      showNotice('success', res.data.message)
      setTimeout(() => {
        fetchSummary()
        fetchJobs(currentPage)
      }, 450)
    } catch (err) {
      showNotice('error', err.response?.data?.error || 'Không thể thử lại Job này')
    }
  }

  const handleDeleteJob = async (jobId) => {
    if (!window.confirm(`Xóa bản ghi Job ${jobId} khỏi danh sách theo dõi?`)) return
    try {
      await api.delete(`/api/admin/queue/jobs/${jobId}`)
      showNotice('success', `Đã xóa Job ${jobId}`)
      fetchSummary()
      fetchJobs(currentPage)
    } catch (err) {
      showNotice('error', err.response?.data?.error || 'Không thể xóa Job')
    }
  }

  const renderStatusPill = (status) => {
    switch (status) {
      case 'SUCCEEDED':
        return (
          <span className="status-pill active" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle2 size={13} /> Thành công
          </span>
        )
      case 'WAITING_PAYMENT':
        return (
          <span className="status-pill" style={{ background: '#e0f2fe', color: '#0369a1', borderColor: '#bae6fd', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={13} /> Chờ duyệt TT
          </span>
        )
      case 'PENDING':
      case 'PROCESSING':
      case 'RETRYING':
        return (
          <span className="status-pill warning" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={13} /> {status === 'PROCESSING' ? 'Đang xử lý' : status === 'RETRYING' ? 'Đang thử lại' : 'Đang chờ'}
          </span>
        )
      case 'FAILED':
      default:
        return (
          <span className="status-pill inactive" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <XCircle size={13} /> Thất bại / DLQ
          </span>
        )
    }
  }

  const renderResolutionLabel = (tier) => {
    switch (tier) {
      case 'VIP_TIER':
        return <span style={{ fontSize: '0.78rem', fontWeight: 600, color: '#7c3aed', background: '#f3e8ff', padding: '2px 8px', borderRadius: '6px' }}>Ưu tiên hạng VIP</span>
      case 'FAIR_LOTTERY':
        return <span style={{ fontSize: '0.78rem', fontWeight: 600, color: '#0369a1', background: '#e0f2fe', padding: '2px 8px', borderRadius: '6px' }}>Bốc thăm công bằng</span>
      default:
        return <span style={{ fontSize: '0.78rem', fontWeight: 600, color: '#475569', background: '#f1f5f9', padding: '2px 8px', borderRadius: '6px' }}>FIFO</span>
    }
  }

  return (
    <div className="admin-crud-page">
      {/* Header */}
      <div className="crud-header" style={{ flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
        <div>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: 0 }}>
            <Layers size={24} color="#2563eb" /> Hàng đợi xử lý đơn hàng
          </h2>
          <p style={{ color: 'var(--admin-text-secondary)', fontSize: '0.9rem', margin: '4px 0 0 0' }}>
            Giám sát Worker đa luồng, phân xử tranh chấp tồn kho 3 cấp độ và xử lý lại đơn lỗi (DLQ).
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
          <label style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '0.85rem',
            color: '#475569',
            background: '#fff',
            border: '1px solid var(--admin-border)',
            padding: '0.45rem 0.75rem',
            borderRadius: '8px',
            cursor: 'pointer'
          }}>
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            Tự động làm mới (3s)
          </label>

          <button
            onClick={() => { fetchSummary(); fetchJobs(currentPage) }}
            className="btn btn-outline"
            style={{ padding: '0.5rem 0.9rem', fontSize: '0.88rem' }}
          >
            <RefreshCw size={15} /> Làm mới
          </button>

          <button
            onClick={handleTogglePause}
            className="btn"
            style={{
              padding: '0.5rem 1rem',
              fontSize: '0.88rem',
              background: summary.isPaused ? '#16a34a' : '#d97706',
              color: '#fff',
              border: 'none'
            }}
          >
            {summary.isPaused ? <><Play size={15} /> Tiếp tục Worker</> : <><Pause size={15} /> Tạm dừng Worker</>}
          </button>

          <button
            onClick={handleSimulateFailure}
            className="btn btn-outline"
            style={{
              padding: '0.5rem 0.9rem',
              fontSize: '0.88rem',
              color: '#dc2626',
              borderColor: '#fca5a5'
            }}
          >
            <AlertTriangle size={15} /> Tạo Job lỗi DLQ
          </button>
        </div>
      </div>

      {notice && (
        <div style={{
          padding: '0.75rem 1rem',
          borderRadius: '10px',
          marginBottom: '1rem',
          fontWeight: 600,
          fontSize: '0.9rem',
          background: notice.type === 'error' ? '#fee2e2' : notice.type === 'warn' ? '#fef3c7' : '#dcfce7',
          color: notice.type === 'error' ? '#b91c1c' : notice.type === 'warn' ? '#b45309' : '#15803d',
          border: `1px solid ${notice.type === 'error' ? '#fecaca' : notice.type === 'warn' ? '#fde68a' : '#bbf7d0'}`
        }}>
          {notice.text}
        </div>
      )}

      {/* Summary Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))',
        gap: '14px',
        marginBottom: '1.25rem'
      }}>
        <div className="glass" style={{ padding: '1rem 1.25rem', borderLeft: `4px solid ${summary.isPaused ? '#d97706' : '#16a34a'}` }}>
          <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>TRẠNG THÁI WORKER</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, marginTop: '4px', color: summary.isPaused ? '#b45309' : '#15803d' }}>
            {summary.isPaused ? 'Đang tạm dừng' : 'Đang hoạt động'}
          </div>
          <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
            Trong hàng đợi: <strong>{summary.streamLength}</strong>
          </div>
        </div>

        <div className="glass" style={{ padding: '1rem 1.25rem', borderLeft: '4px solid #f59e0b' }}>
          <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>CHỜ XỬ LÝ (PENDING)</div>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '4px', color: '#b45309' }}>
            {summary.dbPendingCount}
          </div>
          <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
            Đang xử lý: <strong>{summary.dbProcessingCount}</strong>
          </div>
        </div>

        <div className="glass" style={{ padding: '1rem 1.25rem', borderLeft: '4px solid #0284c7' }}>
          <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>CHỜ DUYỆT THANH TOÁN</div>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '4px', color: '#0369a1' }}>
            {summary.waitingPaymentCount}
          </div>
          <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
            Đơn QR đã giữ kho
          </div>
        </div>

        <div className="glass" style={{ padding: '1rem 1.25rem', borderLeft: '4px solid #10b981' }}>
          <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>HOÀN TẤT (SUCCEEDED)</div>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '4px', color: '#15803d' }}>
            {summary.dbSucceededCount}
          </div>
          <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
            Tổng số Job: <strong>{summary.totalJobs}</strong>
          </div>
        </div>

        <div className="glass" style={{ padding: '1rem 1.25rem', borderLeft: '4px solid #ef4444' }}>
          <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>LỖI / DLQ (FAILED)</div>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '4px', color: '#dc2626' }}>
            {summary.dbFailedCount}
          </div>
          <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
            Đã thử lại: <strong>{summary.retryLength}</strong>
          </div>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '12px',
        alignItems: 'center',
        justifyContent: 'space-between',
        background: '#fff',
        padding: '12px 16px',
        borderRadius: '12px',
        border: '1px solid var(--admin-border)',
        marginBottom: '1rem'
      }}>
        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
          {[
            { key: 'ALL', label: 'Tất cả' },
            { key: 'PENDING', label: 'Chờ xử lý' },
            { key: 'WAITING_PAYMENT', label: 'Chờ duyệt TT' },
            { key: 'SUCCEEDED', label: 'Thành công' },
            { key: 'FAILED', label: 'Thất bại / DLQ' }
          ].map(tab => (
            <button
              key={tab.key}
              onClick={() => setStatusFilter(tab.key)}
              className={`btn ${statusFilter === tab.key ? 'btn-primary' : 'btn-outline'}`}
              style={{ padding: '0.4rem 0.85rem', fontSize: '0.84rem' }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <form
          onSubmit={(e) => { e.preventDefault(); fetchJobs(0) }}
          style={{ display: 'flex', gap: '8px', alignItems: 'center', flex: '1', maxWidth: '360px', minWidth: '220px' }}
        >
          <div style={{ position: 'relative', flex: 1 }}>
            <Search size={16} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
            <input
              type="text"
              placeholder="Tìm mã Job, mã đơn, khách hàng..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{
                width: '100%',
                padding: '0.45rem 2rem 0.45rem 2.1rem',
                borderRadius: '8px',
                border: '1px solid var(--admin-border)',
                fontSize: '0.88rem'
              }}
            />
            {searchTerm && (
              <button
                type="button"
                onClick={() => { setSearchTerm(''); setTimeout(() => fetchJobs(0), 0) }}
                style={{ position: 'absolute', right: '8px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
              >
                <X size={14} />
              </button>
            )}
          </div>
          <button type="submit" className="btn btn-outline" style={{ padding: '0.45rem 0.8rem', fontSize: '0.85rem' }}>
            Tìm
          </button>
        </form>
      </div>

      {/* Table */}
      <div className="admin-table-container glass">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Mã Job</th>
              <th>Đơn hàng</th>
              <th>Khách hàng</th>
              <th>Thanh toán</th>
              <th>Phân xử</th>
              <th>Trạng thái</th>
              <th>Tiến độ</th>
              <th>Ghi chú / Lỗi</th>
              <th style={{ textAlign: 'right' }}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="9" style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                  Đang tải danh sách hàng đợi...
                </td>
              </tr>
            ) : jobs.length === 0 ? (
              <tr>
                <td colSpan="9" style={{ textAlign: 'center', padding: '2.5rem', color: '#64748b' }}>
                  Chưa có tác vụ nào trong hàng đợi phù hợp với bộ lọc.
                </td>
              </tr>
            ) : (
              jobs.map(job => (
                <tr key={job.jobId}>
                  <td>
                    <code style={{ fontWeight: 700, color: '#1e293b' }}>{job.jobId}</code>
                    {job.retryCount > 0 && (
                      <div style={{ fontSize: '0.75rem', color: '#7c3aed', fontWeight: 600 }}>
                        Retry #{job.retryCount}
                      </div>
                    )}
                  </td>
                  <td>
                    {job.orderId ? <strong>#{job.orderId}</strong> : <span style={{ color: '#94a3b8' }}>Thử nghiệm</span>}
                    <div style={{ fontSize: '0.8rem', color: '#64748b' }}>
                      {job.amount ? `${Number(job.amount).toLocaleString()}đ` : '—'}
                    </div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{job.customerName || '—'}</div>
                    <div style={{ fontSize: '0.76rem', color: '#64748b' }}>
                      Hạng: {job.membershipTier || 'BRONZE'}
                    </div>
                  </td>
                  <td style={{ fontSize: '0.85rem' }}>
                    {job.paymentMethod === 'COD' ? 'COD' : job.paymentMethod || '—'}
                  </td>
                  <td>{renderResolutionLabel(job.resolutionTier)}</td>
                  <td>{renderStatusPill(job.status)}</td>
                  <td style={{ minWidth: '95px' }}>
                    <div style={{ fontSize: '0.78rem', fontWeight: 600, marginBottom: '3px' }}>{job.progress}%</div>
                    <div style={{ width: '100%', height: '6px', background: '#e2e8f0', borderRadius: '999px', overflow: 'hidden' }}>
                      <div style={{
                        width: `${job.progress}%`,
                        height: '100%',
                        background: job.status === 'FAILED' ? '#ef4444' : job.status === 'SUCCEEDED' ? '#10b981' : '#3b82f6'
                      }} />
                    </div>
                  </td>
                  <td style={{ maxWidth: '250px', fontSize: '0.82rem', color: job.errorMessage ? '#dc2626' : '#64748b' }}>
                    {job.errorMessage || (job.createdAt ? new Date(job.createdAt).toLocaleString('vi-VN') : '—')}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div className="table-actions" style={{ justifyContent: 'flex-end', gap: '6px' }}>
                      {job.status === 'FAILED' && (
                        <button
                          onClick={() => handleRetryJob(job.jobId)}
                          className="btn btn-primary"
                          style={{ padding: '0.35rem 0.7rem', fontSize: '0.78rem' }}
                          title="Thử xử lý lại đơn hàng này"
                        >
                          <RotateCcw size={13} /> Thử lại
                        </button>
                      )}
                      <button
                        onClick={() => handleDeleteJob(job.jobId)}
                        className="action-btn delete"
                        title="Xóa khỏi lịch sử hàng đợi"
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 16px', borderTop: '1px solid var(--admin-border)' }}>
            <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
              Trang <strong>{currentPage + 1}</strong> / {totalPages}
            </span>
            <div style={{ display: 'flex', gap: '6px' }}>
              <button
                disabled={currentPage === 0}
                onClick={() => fetchJobs(currentPage - 1)}
                className="btn btn-outline"
                style={{ padding: '0.35rem 0.75rem', fontSize: '0.82rem' }}
              >
                Trước
              </button>
              <button
                disabled={currentPage + 1 >= totalPages}
                onClick={() => fetchJobs(currentPage + 1)}
                className="btn btn-outline"
                style={{ padding: '0.35rem 0.75rem', fontSize: '0.82rem' }}
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default QueueManager
