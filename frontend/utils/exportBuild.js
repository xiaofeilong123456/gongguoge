// 数据导出内容构造
//
// 与平台无关的纯函数，便于单元测试。
// 背景：后端 /api/data/export 返回的是原始实体，records 里只有 categoryId，
// 没有 categoryName，type 也是数字。这里负责补上可读信息。

// CSV 单元格转义：内容含逗号、引号、换行时必须用双引号包裹，否则会串列
export function csvCell(value) {
  const s = value === undefined || value === null ? '' : String(value)
  return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
}

// categoryId -> 分类名
export function buildCategoryMap(categories) {
  const map = {}
  ;(categories || []).forEach((c) => {
    if (c && c.id !== undefined && c.id !== null) map[c.id] = c.name || ''
  })
  return map
}

// 手机号脱敏：导出文件常被转发/分享，不应带完整手机号
export function maskPhone(phone) {
  const v = String(phone || '')
  return v.length === 11 ? v.slice(0, 3) + '****' + v.slice(7) : v
}

// 导出 JSON：补上分类名与可读的类型文字，让文件本身自解释
export function buildJson(data) {
  const d = data || {}
  const catMap = buildCategoryMap(d.categories)
  const records = (d.records || []).map((r) => Object.assign({}, r, {
    typeText: r.type === 1 ? '功' : '过',
    categoryName: catMap[r.categoryId] || ''
  }))
  const profile = Object.assign({}, d.profile || {})
  // 手机号脱敏：导出文件会被分享出去，避免泄露完整号码
  if (profile.phone) profile.phone = maskPhone(profile.phone)
  return JSON.stringify({
    exportedAt: new Date().toISOString(),
    profile: profile,
    categories: d.categories || [],
    records: records,
    recycle: d.recycle || [],
    feedback: d.feedback || []
  }, null, 2)
}

// 导出 CSV：中文表头、类型转文字、分类名从 categories 解析
export function buildCsv(data) {
  const d = data || {}
  const catMap = buildCategoryMap(d.categories)
  const records = d.records || []
  const header = ['日期', '类型', '分类', '内容'].join(',')
  const rows = records.map((r) => [
    r.recordDate || '',
    r.type === 1 ? '功' : '过',
    catMap[r.categoryId] || '',
    r.content || ''
  ].map(csvCell).join(','))
  // 带 BOM，Excel 打开才不会中文乱码；CRLF 兼容性更好
  return '\ufeff' + [header].concat(rows).join('\r\n')
}
