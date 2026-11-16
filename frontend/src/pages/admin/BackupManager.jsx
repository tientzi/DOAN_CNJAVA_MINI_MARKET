import React, { useState, useEffect, useRef } from 'react'
import api from '../../services/api'
import {
  DatabaseBackup, Upload, Download, RotateCcw, Trash2, RefreshCw,
  CheckCircle2, XCircle, Clock, ShieldCheck, HardDrive
} from 'lucide-react'
import './AdminPages.css'

const formatBytes = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(i === 0 ? 0 : 1)} ${sizes[i]}`
}

const STAT_ROWS = [
  { key: 'totalRecords', label: 'Tổng bản ghi (SQL Server)' },
  { key: 'products', label: 'Sản phẩm' },
  { key: 'orders', label: 'Đơn hàng' },
  { key: 'users', label: 'Người dùng' },
  { key: 'coupons', label: 'Mã giảm giá' },
  { key: 'batches', label: 'Lô hàng tồn kho' },
  { key: 'uploadsCount', label: 'Tệp ảnh (uploads/)' }
]

const BackupManager = () => {
  const [status, setStatus] = useState(null)
  const [backups, setBackups] = useState([])
  const [logs, setLogs] = useState([])
  const [progress, setProgress] = useState(null)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState(null)

  const [restoreTarget, setRestoreTarget] = useState(null) // { filename } | { file }
  const [confirmText, setConfirmText] = useState('')

  const fileInputRef = useRef(null)
  const pollRef = useRef(null)

  const showNotice = (type, text) => {
    setNotice({ type, text })
  }

  const fetchAll = async () => {
    try {
      const [statusRes, listRes, logsRes] = await Promise.all([
        api.get('/api/admin/backups/status'),
        api.get('/api/admin/backups'),
        api.get('/api/admin/backups/activity-logs?limit=25')
      ])
      setStatus(statusRes.data)
      setBackups(listRes.data || [])
      setLogs(logsRes.data || [])
    } catch (err) {
      console.error('Lỗi tải dữ liệu sao lưu:', err)
    }
  }

  const fetchProgress = async () => {
    try {
      const res = await api.get('/api/admin/backups/progress')
      if (res.data && res.data.status !== 'idle') {
        setProgress(res.data)
      }
    } catch {
      // ignore transient poll errors
    }
  }

  useEffect(() => {
    fetchAll()
    fetchProgress()
    return () => {
      if (pollRef.current) clearInterval(pollRef.current)
    }
  }, [])

  const startPolling = () => {
    if (pollRef.current) clearInterval(pollRef.current)
    pollRef.current = setInterval(fetchProgress, 250)
  }

  const stopPolling = () => {
    if (pollRef.current) {
      clearInterval(pollRef.current)
      pollRef.current = null
    }
  }

  const handleCreateBackup = async () => {
    setBusy(true)
    setNotice(null)
    startPolling()
    try {
      const res = await api.post('/api/admin/backups/create')
      showNotice('success', `Đã tạo bản sao lưu ${res.data.filename} (${res.data.meta?.stats?.totalRecords ?? 0} bản ghi)`)
      await fetchProgress()
      await fetchAll()
    } catch (err) {
      showNotice('error', err.response?.data?.error || 'Lỗi khi tạo bản sao lưu')
      await fetchProgress()
    } finally {
      stopPolling()
      setBusy(false)
    }
  }

  const handleConfirmRestore = async () => {
    if (confirmText.trim().toUpperCase() !== 'RESTORE' || !restoreTarget) return
    const target = restoreTarget
    setRestoreTarget(null)
    setConfirmText('')
    setBusy(true)
    setNotice(null)
    startPolling()

    try {
      let res
      if (target.file) {
        const formData = new FormData()
        formData.append('file', target.file)
        res = await api.post('/api/admin/backups/upload-restore', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
      } else {
        res = await api.post(`/api/admin/backups/restore/${encodeURIComponent(target.filename)}`)
      }
      showNotice('success', `Phục hồi thành công từ ${res.data.filename}. Đã tự động tạo bản an toàn: ${res.data.safetyBackup}`)
      await fetchProgress()
      await fetchAll()
    } catch (err) {
      showNotice('error', err.response?.data?.error || 'Phục hồi thất bại! Dữ liệu hiện tại được giữ nguyên.')
      await fetchProgress()
      await fetchAll()
    } finally {
      stopPolling()
      setBusy(false)
    }
  }

  const handleDownload = async (filename) => {
    try {
      const res = await api.get(`/api/admin/backups/download/${encodeURIComponent(filename)}`, {
        responseType: 'blob'
      })
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      showNotice('error', 'Không thể tải tệp sao lưu: ' + (err.response?.data?.error || err.message))
    }
  }

  const handleDeleteBackup = async (filename) => {
    if (!window.confirm(`Bạn chắc chắn muốn xóa tệp sao lưu "${filename}"?`)) return
    try {
      await api.delete(`/api/admin/backups/${encodeURIComponent(filename)}`)
      showNotice('success', `Đã xóa tệp sao lưu ${filename}`)
      await fetchAll()
    } catch (err) {
      showNotice('error', err.response?.data?.error || 'Không thể xóa tệp sao lưu')
    }
  }

  const liveStats = status?.liveStats || {}
  const comparison = status?.comparison || {}

  return (
    <div className="admin-crud-page">
      {/* Header */}
      <div className="crud-header" style={{ flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
        <div>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: 0 }}>
            <DatabaseBackup size={24} color="#2563eb" /> Sao lưu & Phục hồi dữ liệu
          </h2>
          <p style={{ color: 'var(--admin-text-secondary)', fontSize: '0.9rem', margin: '4px 0 0 0' }}>
            Sao lưu toàn bộ bảng SQL Server kèm ảnh uploads/ (ZIP + SHA-256) và phục hồi an toàn theo từng bước.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button onClick={fetchAll} disabled={busy} className="btn btn-outline" style={{ padding: '0.5rem 0.9rem', fontSize: '0.88rem' }}>
            <RefreshCw size={15} /> Làm mới
          </button>

          <input
            ref={fileInputRef}
            type="file"
            accept=".zip"
            style={{ display: 'none' }}
            onChange={(e) => {
              const file = e.target.files?.[0]
              e.target.value = ''
              if (file) {
                setConfirmText('')
                setRestoreTarget({ file })
              }
            }}
          />
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={busy}
            className="btn btn-outline"
            style={{ padding: '0.5rem 0.95rem', fontSize: '0.88rem' }}
          >
            <Upload size={15} /> Tải lên ZIP phục hồi
          </button>

          <button
            onClick={handleCreateBackup}
            disabled={busy}
            className="btn btn-primary"
            style={{ padding: '0.5rem 1.1rem', fontSize: '0.88rem' }}
          >
            <DatabaseBackup size={15} /> {busy ? 'Đang xử lý...' : 'Sao lưu ngay'}
          </button>
        </div>
      </div>

      {notice && (
        <div style={{
          padding: '0.8rem 1rem',
          borderRadius: '10px',
          marginBottom: '1rem',
          fontWeight: 600,
          fontSize: '0.9rem',
          background: notice.type === 'error' ? '#fee2e2' : '#dcfce7',
          color: notice.type === 'error' ? '#b91c1c' : '#15803d',
          border: `1px solid ${notice.type === 'error' ? '#fecaca' : '#bbf7d0'}`
        }}>
          {notice.text}
        </div>
      )}

      {/* Progress Card */}
      {progress && progress.status !== 'idle' && (
        <div className="glass" style={{ padding: '1.25rem', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px', flexWrap: 'wrap', gap: '8px' }}>
            <div>
              <strong style={{ fontSize: '1rem' }}>
                {progress.operation === 'restore' ? 'Tiến trình phục hồi dữ liệu' : 'Tiến trình sao lưu dữ liệu'}
                {progress.filename ? ` (${progress.filename})` : ''}
              </strong>
              <div style={{ fontSize: '0.85rem', color: progress.status === 'error' ? '#dc2626' : '#64748b', marginTop: '2px' }}>
                {progress.message}
              </div>
            </div>
            <span style={{
              fontWeight: 700,
              fontSize: '1rem',
              color: progress.status === 'error' ? '#dc2626' : progress.status === 'completed' ? '#15803d' : '#2563eb'
            }}>
              {progress.percent}%
            </span>
          </div>

          <div style={{ width: '100%', height: '8px', background: '#e2e8f0', borderRadius: '999px', overflow: 'hidden', marginBottom: '12px' }}>
            <div style={{
              width: `${progress.percent}%`,
              height: '100%',
              transition: 'width 0.25s ease',
              background: progress.status === 'error' ? '#ef4444' : progress.status === 'completed' ? '#10b981' : '#3b82f6'
            }} />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '8px' }}>
            {(progress.steps || []).map((st, idx) => (
              <div key={idx} style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '7px 10px',
                borderRadius: '8px',
                fontSize: '0.82rem',
                background: st.status === 'done' ? '#f0fdf4' : st.status === 'running' ? '#eff6ff' : st.status === 'error' ? '#fef2f2' : '#f8fafc',
                border: `1px solid ${st.status === 'done' ? '#bbf7d0' : st.status === 'running' ? '#bfdbfe' : st.status === 'error' ? '#fecaca' : '#e2e8f0'}`,
                color: st.status === 'done' ? '#15803d' : st.status === 'running' ? '#1d4ed8' : st.status === 'error' ? '#b91c1c' : '#64748b'
              }}>
                {st.status === 'done' ? <CheckCircle2 size={14} /> : st.status === 'error' ? <XCircle size={14} /> : <Clock size={14} />}
                <span>{idx + 1}. {st.name}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 2-Column Overview */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '16px', marginBottom: '1.25rem' }}>
        {/* Left: System Status */}
        <div className="glass" style={{ padding: '1.25rem' }}>
          <h3 style={{ margin: '0 0 12px 0', fontSize: '1.05rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <HardDrive size={18} color="#2563eb" /> Trạng thái dữ liệu hiện tại
          </h3>
          <div style={{ fontSize: '0.88rem', lineHeight: 1.7, color: '#334155', marginBottom: '12px' }}>
            <div><strong>Cơ sở dữ liệu:</strong> {status?.databaseEngine || 'SQL Server'}</div>
            <div><strong>Định dạng gói:</strong> {status?.backupFormat || 'ZIP (data.json + SHA-256 + uploads/)'}</div>
            <div>
              <strong>Khóa ghi toàn cục:</strong>{' '}
              <span style={{ color: status?.writeLocked ? '#dc2626' : '#15803d', fontWeight: 600 }}>
                {status?.writeLocked ? `Đang khóa (${status.lockReason})` : 'Đang mở (Hoạt động bình thường)'}
              </span>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '8px' }}>
            {STAT_ROWS.map(row => (
              <div key={row.key} style={{ background: '#f8fafc', padding: '8px 12px', borderRadius: '8px', border: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', fontSize: '0.84rem' }}>
                <span style={{ color: '#64748b' }}>{row.label}</span>
                <strong>{liveStats[row.key] ?? 0}</strong>
              </div>
            ))}
          </div>
        </div>

        {/* Right: Restore Comparison */}
        <div className="glass" style={{ padding: '1.25rem' }}>
          <h3 style={{ margin: '0 0 12px 0', fontSize: '1.05rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ShieldCheck size={18} color="#16a34a" /> Đối chiếu số liệu phục hồi gần nhất
          </h3>
          {comparison?.restoredFrom ? (
            <div style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '10px' }}>
              Nguồn phục hồi: <strong>{comparison.restoredFrom}</strong> ({new Date(comparison.updatedAt).toLocaleString('vi-VN')})
            </div>
          ) : (
            <div style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '10px' }}>
              Chưa thực hiện lần phục hồi nào. Bảng sẽ tự đối chiếu số liệu sau khi phục hồi.
            </div>
          )}

          <div style={{ overflowX: 'auto' }}>
            <table className="admin-table" style={{ fontSize: '0.84rem' }}>
              <thead>
                <tr>
                  <th style={{ padding: '8px' }}>Hạng mục</th>
                  <th style={{ padding: '8px', textAlign: 'right' }}>Trước phục hồi</th>
                  <th style={{ padding: '8px', textAlign: 'right' }}>Bản sao lưu</th>
                  <th style={{ padding: '8px', textAlign: 'right' }}>Sau phục hồi</th>
                </tr>
              </thead>
              <tbody>
                {STAT_ROWS.map(row => (
                  <tr key={row.key}>
                    <td style={{ padding: '7px 8px' }}>{row.label}</td>
                    <td style={{ padding: '7px 8px', textAlign: 'right', color: '#64748b' }}>
                      {comparison.beforeRestore ? (comparison.beforeRestore[row.key] ?? 0) : '—'}
                    </td>
                    <td style={{ padding: '7px 8px', textAlign: 'right', color: '#2563eb', fontWeight: 600 }}>
                      {comparison.expectedBackup ? (comparison.expectedBackup[row.key] ?? 0) : '—'}
                    </td>
                    <td style={{ padding: '7px 8px', textAlign: 'right', color: '#15803d', fontWeight: 700 }}>
                      {comparison.afterRestore ? (comparison.afterRestore[row.key] ?? 0) : (liveStats[row.key] ?? 0)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Backup Files List */}
      <div className="admin-table-container glass" style={{ marginBottom: '1.25rem' }}>
        <div style={{ padding: '1rem 1.25rem 0.5rem', fontWeight: 700, fontSize: '1rem' }}>
          Danh sách tệp sao lưu ({backups.length})
        </div>
        <table className="admin-table">
          <thead>
            <tr>
              <th>Tên tệp</th>
              <th>Loại</th>
              <th>Thời gian tạo</th>
              <th>Dung lượng</th>
              <th>Bản ghi / Ảnh</th>
              <th>Checksum SHA-256</th>
              <th style={{ textAlign: 'right' }}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {backups.length === 0 ? (
              <tr>
                <td colSpan="7" style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                  Chưa có bản sao lưu nào trong thư mục backups/. Hãy bấm "Sao lưu ngay" để tạo bản đầu tiên.
                </td>
              </tr>
            ) : (
              backups.map(b => (
                <tr key={b.filename}>
                  <td><strong>{b.filename}</strong></td>
                  <td>
                    <span className={`status-pill ${b.type === 'pre-restore' ? 'warning' : 'active'}`}>
                      {b.type === 'pre-restore' ? 'An toàn pre-restore' : 'Thủ công'}
                    </span>
                  </td>
                  <td style={{ fontSize: '0.85rem' }}>
                    {b.createdAt ? new Date(b.createdAt).toLocaleString('vi-VN') : '—'}
                  </td>
                  <td>{formatBytes(b.sizeBytes)}</td>
                  <td style={{ fontSize: '0.85rem' }}>
                    <strong>{b.stats?.totalRecords ?? '—'}</strong> dòng / <strong>{b.stats?.uploadsCount ?? '—'}</strong> ảnh
                  </td>
                  <td>
                    <code style={{ fontSize: '0.78rem', color: '#64748b' }}>
                      {b.checksum ? `${b.checksum.slice(0, 14)}...` : '—'}
                    </code>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div className="table-actions" style={{ justifyContent: 'flex-end', gap: '6px' }}>
                      <button
                        onClick={() => handleDownload(b.filename)}
                        className="btn btn-outline"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.78rem' }}
                        title="Tải xuống tệp ZIP"
                      >
                        <Download size={13} /> Tải về
                      </button>
                      <button
                        onClick={() => { setConfirmText(''); setRestoreTarget({ filename: b.filename }) }}
                        disabled={busy}
                        className="btn btn-primary"
                        style={{ padding: '0.35rem 0.7rem', fontSize: '0.78rem' }}
                        title="Phục hồi từ tệp sao lưu này"
                      >
                        <RotateCcw size={13} /> Phục hồi
                      </button>
                      <button
                        onClick={() => handleDeleteBackup(b.filename)}
                        disabled={busy}
                        className="action-btn delete"
                        title="Xóa tệp sao lưu"
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
      </div>

      {/* System Activity Logs */}
      <div className="admin-table-container glass">
        <div style={{ padding: '1rem 1.25rem 0.5rem', fontWeight: 700, fontSize: '1rem' }}>
          Nhật ký hoạt động hệ thống gần đây
        </div>
        <table className="admin-table" style={{ fontSize: '0.84rem' }}>
          <thead>
            <tr>
              <th>Thời gian</th>
              <th>Phân hệ</th>
              <th>Hành động</th>
              <th>Chi tiết</th>
              <th>Mức độ</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 ? (
              <tr>
                <td colSpan="5" style={{ textAlign: 'center', padding: '1.5rem', color: '#64748b' }}>
                  Chưa có nhật ký hoạt động nào.
                </td>
              </tr>
            ) : (
              logs.map(entry => (
                <tr key={entry.id}>
                  <td style={{ whiteSpace: 'nowrap', color: '#64748b' }}>
                    {entry.timestamp ? new Date(entry.timestamp).toLocaleString('vi-VN') : '—'}
                  </td>
                  <td><code>{entry.type}</code></td>
                  <td style={{ fontWeight: 600 }}>{entry.action}</td>
                  <td style={{ color: '#475569' }}>{entry.detail}</td>
                  <td>
                    <span className={`status-pill ${entry.status === 'ERROR' ? 'inactive' : entry.status === 'WARN' ? 'warning' : 'active'}`}>
                      {entry.status}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Confirm Restore Modal */}
      {restoreTarget && (
        <div className="admin-form-overlay">
          <div className="admin-popup-form glass" style={{ maxWidth: '480px' }}>
            <h3 style={{ marginTop: 0 }}>Xác nhận phục hồi dữ liệu</h3>
            <p style={{ fontSize: '0.9rem', color: '#475569', lineHeight: 1.5 }}>
              Hệ thống sẽ kiểm tra mã toàn vẹn SHA-256, tự động lưu một bản an toàn <code>pre-restore_*.zip</code> trước khi ghi đè dữ liệu từ:{' '}
              <strong>{restoreTarget.filename || restoreTarget.file?.name}</strong>.
            </p>
            <div className="form-group" style={{ marginTop: '12px' }}>
              <label>Nhập chữ <strong>RESTORE</strong> để xác nhận</label>
              <input
                type="text"
                placeholder="RESTORE"
                value={confirmText}
                onChange={(e) => setConfirmText(e.target.value)}
                autoFocus
              />
            </div>
            <div className="form-actions" style={{ marginTop: '16px' }}>
              <button
                type="button"
                onClick={() => { setRestoreTarget(null); setConfirmText('') }}
                className="btn btn-outline"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                disabled={confirmText.trim().toUpperCase() !== 'RESTORE'}
                onClick={handleConfirmRestore}
                className="btn btn-primary"
              >
                <RotateCcw size={15} /> Tiến hành phục hồi
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default BackupManager
