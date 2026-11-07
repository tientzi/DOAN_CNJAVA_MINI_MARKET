import React, { useEffect, useRef, useState } from 'react'

const ThymeleafEmbed = ({ src, minHeight = 760 }) => {
  const iframeRef = useRef(null)
  const [height, setHeight] = useState(minHeight)

  // Đảm bảo cookie jwt_token luôn được đồng bộ từ localStorage trước khi tải trang Thymeleaf
  const token = localStorage.getItem('token') || ''
  if (token) {
    try {
      document.cookie = `jwt_token=${token}; path=/; max-age=86400; SameSite=Lax`
    } catch (e) {
      // ignore
    }
  }

  useEffect(() => {
    const handleMessage = (event) => {
      if (!event.data || typeof event.data !== 'object') return
      if (event.data.type === 'THYMELEAF_EMBED_RESIZE' && typeof event.data.height === 'number') {
        setHeight(Math.max(minHeight, event.data.height))
      }
    }

    window.addEventListener('message', handleMessage)
    return () => window.removeEventListener('message', handleMessage)
  }, [minHeight])

  const handleLoad = () => {
    try {
      const doc = iframeRef.current?.contentWindow?.document
      if (doc && doc.body) {
        const contentHeight = Math.max(
          doc.body.scrollHeight || 0,
          doc.documentElement?.offsetHeight || 0
        )
        if (contentHeight > 0) {
          setHeight(Math.max(minHeight, contentHeight + 28))
        }
      }
    } catch (e) {
      // Cross-origin fallback handled by postMessage
    }
  }

  return (
    <div style={{ width: '100%', overflow: 'hidden' }}>
      <iframe
        ref={iframeRef}
        src={src}
        title="MiniMart View"
        onLoad={handleLoad}
        style={{
          width: '100%',
          height: `${height}px`,
          border: 'none',
          display: 'block',
          backgroundColor: 'transparent',
          overflow: 'hidden'
        }}
      />
    </div>
  )
}

export default ThymeleafEmbed
