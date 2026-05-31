// 时间格式化
const MS_PER_MINUTE = 60000
const MS_PER_HOUR = 3600000
const MS_PER_DAY = 86400000

function isToday(date) {
  const now = new Date()
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
}

function isYesterday(date) {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  return date.getFullYear() === yesterday.getFullYear()
    && date.getMonth() === yesterday.getMonth()
    && date.getDate() === yesterday.getDate()
}

function isThisYear(date) {
  return date.getFullYear() === new Date().getFullYear()
}

function pad(n) { return n < 10 ? '0' + n : n }

export function formatConversationTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isToday(date)) {
    return pad(date.getHours()) + ':' + pad(date.getMinutes())
  }
  if (isYesterday(date)) {
    return '昨天'
  }
  if (isThisYear(date)) {
    return (date.getMonth() + 1) + '/' + date.getDate()
  }
  return date.getFullYear() + '/' + (date.getMonth() + 1) + '/' + date.getDate()
}

export function formatMessageTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return pad(date.getHours()) + ':' + pad(date.getMinutes())
}

export function formatFullTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date

  if (diff < MS_PER_MINUTE) return '刚刚'
  if (diff < MS_PER_HOUR) return Math.floor(diff / MS_PER_MINUTE) + '分钟前'
  if (isToday(date)) {
    return '今天 ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
  }
  if (isYesterday(date)) {
    return '昨天 ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
  }
  if (isThisYear(date)) {
    return (date.getMonth() + 1) + '月' + date.getDate() + '日 ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
  }
  return date.getFullYear() + '年' + (date.getMonth() + 1) + '月' + date.getDate() + '日'
}

export function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}
