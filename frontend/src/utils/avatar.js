// 根据用户名生成唯一颜色
const COLORS = [
  '#f7931e', '#e67e22', '#f7931e', '#d35400', '#f7931e',
  '#07C160', '#1485EE', '#E74C3C', '#9B59B6',
  '#1ABC9C', '#F39C12', '#3498DB', '#E91E63',
  '#FF5722', '#8BC34A', '#FF9800', '#f7931e',
  '#673AB7', '#009688', '#03A9F4'
]

export function getAvatarColor(name) {
  if (!name) return COLORS[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return COLORS[Math.abs(hash) % COLORS.length]
}

export function getInitial(name) {
  if (!name) return '?'
  // 取第一个字符
  const ch = name.charAt(0)
  // 中文取末字
  if (/[一-鿿]/.test(name)) {
    const chinese = name.match(/[一-鿿]/g)
    if (chinese) return chinese[chinese.length - 1]
  }
  return ch.toUpperCase()
}

export function getAvatarStyle(name) {
  return { backgroundColor: getAvatarColor(name) }
}
