import React from 'react'
import { Link } from 'react-router-dom'

const NotFound = () => {
  return (
    <div style={{
      minHeight: '80vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      textAlign: 'center',
      padding: '2rem'
    }}>
      <h1 style={{ fontSize: '5rem', fontWeight: 800, color: '#ff5722', marginBottom: '0.5rem' }}>404</h1>
      <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.75rem', color: '#1e293b' }}>
        Không tìm thấy trang yêu cầu
      </h2>
      <p style={{ color: '#64748b', maxWidth: '460px', marginBottom: '1.5rem' }}>
        Đường dẫn bạn truy cập không tồn tại, đã được di chuyển hoặc tạm thời không khả dụng.
      </p>
      <div style={{ display: 'flex', gap: '12px' }}>
        <Link to="/" className="btn btn-primary" style={{ padding: '10px 22px', textDecoration: 'none' }}>
          Về trang chủ
        </Link>
        <Link to="/products" className="btn btn-outline" style={{ padding: '10px 22px', textDecoration: 'none' }}>
          Xem sản phẩm
        </Link>
      </div>
    </div>
  )
}

export default NotFound

