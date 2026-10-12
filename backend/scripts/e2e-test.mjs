// 功过格 · 后端端到端接口测试
//
// 用法:
//   node scripts/e2e-test.mjs
//   API_BASE=http://127.0.0.1:8080/api node scripts/e2e-test.mjs
//
// 依赖 Node 18+（内置 fetch），无需第三方包。
// 前置: MySQL 与 Redis 已启动、后端已运行、建议 ALIYUN_SMS_ENABLED=false（验证码固定 123456）。

const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api'
const MOCK_CODE = '123456'
// 测试脚本注册的账号昵称，作为 clean-test-data.mjs 的唯一识别依据
const TEST_NICKNAME = '测试账号'

const results = []
let token = ''

function rec(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  -> ' + detail : ''}`)
}

async function call(method, path, body, opts = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token && !opts.noAuth) headers['Authorization'] = 'Bearer ' + token
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { json = { raw: text.slice(0, 200) } }
  return { status: res.status, body: json }
}

// 唯一手机号，避免与既有数据冲突；不同场景用不同号码以保证互不干扰
const stamp = String(Date.now()).slice(-8)
const phone = '139' + stamp
const phoneScene = '138' + stamp
const phoneReplay = '137' + stamp
console.log('API:', BASE)
console.log('主流程手机号:', phone, ' 场景隔离:', phoneScene, ' 防重放:', phoneReplay, '\n')

try {
  // 1. 场景隔离：register 场景发的码，不能用于 login
  await call('POST', '/auth/sms-code', { phone: phoneScene, scene: 'register' })
  const crossScene = await call('POST', '/auth/login', { phone: phoneScene, code: MOCK_CODE, nickname: TEST_NICKNAME })
  rec('场景隔离：register 的码不能用于 login', crossScene.body.code !== 200,
    `code=${crossScene.body.code} msg=${crossScene.body.message || ''}`)

  // 2. 发送登录验证码
  const sms = await call('POST', '/auth/sms-code', { phone, scene: 'login' })
  rec('POST /auth/sms-code 发送验证码', sms.status === 200 && sms.body.code === 200,
    `status=${sms.status} code=${sms.body.code} msg=${sms.body.message || ''}`)

  // 3. 限流：1 分钟内重复发码应被拒
  const rateLimit = await call('POST', '/auth/sms-code', { phone, scene: 'login' })
  rec('限流：1 分钟内重复发码被拒', rateLimit.body.code !== 200,
    `code=${rateLimit.body.code} msg=${rateLimit.body.message || ''}`)

  // 4. 登录（未注册自动注册）。显式昵称便于 clean-test-data.mjs 安全识别本脚本产生的账号
  const login = await call('POST', '/auth/login', { phone, code: MOCK_CODE, nickname: TEST_NICKNAME })
  const okLogin = login.status === 200 && login.body.code === 200 && login.body.data && login.body.data.token
  rec('POST /auth/login 登录（mock 验证码）', !!okLogin,
    okLogin ? `user=${login.body.data.userInfo && login.body.data.userInfo.nickname}` : JSON.stringify(login.body).slice(0, 160))
  if (okLogin) token = login.body.data.token

  // 5. 鉴权：无 token 应拒绝
  const noAuth = await call('GET', '/records/today', undefined, { noAuth: true })
  rec('鉴权：无 token 访问受保护接口被拒', noAuth.status === 401 || noAuth.body.code === 401,
    `status=${noAuth.status} code=${noAuth.body.code}`)

  // 6. 用户资料
  const prof = await call('GET', '/user/profile')
  rec('GET /user/profile 获取资料', prof.status === 200 && prof.body.code === 200,
    `phone=${prof.body.data && prof.body.data.phone}`)

  // 7. 分类列表
  const cats = await call('GET', '/categories')
  const catList = cats.body.data || []
  const posCat = catList.find(c => c.type === 1)
  const refCat = catList.find(c => c.type === 2)
  rec('GET /categories 分类列表', cats.status === 200 && catList.length >= 10,
    `共 ${catList.length} 个，功类=${catList.filter(c => c.type === 1).length} 过类=${catList.filter(c => c.type === 2).length}`)

  // 8. 新增「功」记录
  const add1 = await call('POST', '/records', { type: 1, categoryId: posCat ? posCat.id : 1, content: '端到端测试：坚持阅读20分钟' })
  const rec1 = add1.body.data
  rec('POST /records 新增功记录', add1.status === 200 && add1.body.code === 200,
    `id=${rec1 && rec1.id} type=${rec1 && rec1.type}`)

  // 9. 新增「过」记录
  const add2 = await call('POST', '/records', { type: 2, categoryId: refCat ? refCat.id : 6, content: '端到端测试：对家人语气急躁' })
  rec('POST /records 新增过记录', add2.status === 200 && add2.body.code === 200,
    `id=${add2.body.data && add2.body.data.id}`)

  // 10. 幂等：同 clientId 重复提交只写一条，且第二次返回同一条
  const cid = 'e2e-' + Date.now()
  const idem1 = await call('POST', '/records', { type: 1, categoryId: posCat ? posCat.id : 1, content: '幂等测试', clientId: cid })
  const idem2 = await call('POST', '/records', { type: 1, categoryId: posCat ? posCat.id : 1, content: '幂等测试', clientId: cid })
  const allRecs = await call('GET', '/records')
  const idemCount = (allRecs.body.data || []).filter(r => r.content === '幂等测试').length
  const sameId = idem1.body.data && idem2.body.data && idem1.body.data.id === idem2.body.data.id
  rec('幂等：相同 clientId 重复提交返回同一条', idem1.body.code === 200 && idem2.body.code === 200 && sameId && idemCount === 1,
    `首次id=${idem1.body.data && idem1.body.data.id} 重复code=${idem2.body.code} 库中条数=${idemCount}`)

  // 11. 验证码防重放
  await call('POST', '/auth/sms-code', { phone: phoneReplay, scene: 'login' })
  const first = await call('POST', '/auth/login', { phone: phoneReplay, code: MOCK_CODE, nickname: TEST_NICKNAME })
  const replay = await call('POST', '/auth/login', { phone: phoneReplay, code: MOCK_CODE, nickname: TEST_NICKNAME })
  rec('验证码防重放：同一码不能用两次', first.body.code === 200 && replay.body.code !== 200,
    `首次code=${first.body.code} 重放code=${replay.body.code}`)

  // 12. 今日概览
  const today = await call('GET', '/records/today')
  rec('GET /records/today 今日记录', today.status === 200 && today.body.code === 200, `数据已返回`)

  // 13. 更新记录
  let updOk = false, updDetail = '无记录可更新'
  if (rec1 && rec1.id) {
    const upd = await call('PUT', '/records/' + rec1.id, { type: 1, categoryId: posCat ? posCat.id : 1, content: '端到端测试：已修改的内容' })
    updOk = upd.status === 200 && upd.body.code === 200
    updDetail = `status=${upd.status} code=${upd.body.code}`
  }
  rec('PUT /records/{id} 更新记录', updOk, updDetail)

  // 14. 删除 -> 回收站 -> 恢复
  let recycleOk = false, recycleDetail = '无记录'
  if (rec1 && rec1.id) {
    await call('DELETE', '/records/' + rec1.id)
    const recy = await call('GET', '/records/recycle')
    const inRecycle = (recy.body.data || []).some(r => r.id === rec1.id)
    const restore = await call('POST', '/records/recycle/' + rec1.id + '/restore')
    const after = await call('GET', '/records')
    const back = (after.body.data || []).some(r => r.id === rec1.id)
    recycleOk = inRecycle && restore.body.code === 200 && back
    recycleDetail = `进入回收站=${inRecycle} 恢复code=${restore.body.code} 恢复后存在=${back}`
  }
  rec('删除 -> 回收站 -> 恢复 闭环', recycleOk, recycleDetail)

  // 15. 彻底删除
  let permOk = false, permDetail = '无记录'
  if (add2.body.data && add2.body.data.id) {
    const pid = add2.body.data.id
    await call('DELETE', '/records/' + pid)
    const perm = await call('DELETE', '/records/recycle/' + pid)
    const recy = await call('GET', '/records/recycle')
    const gone = !(recy.body.data || []).some(r => r.id === pid)
    permOk = perm.status === 200 && gone
    permDetail = `code=${perm.body.code} 已不存在=${gone}`
  }
  rec('回收站彻底删除', permOk, permDetail)

  // 16. 统计
  const st = await call('GET', '/statistics?range=7')
  const ov = await call('GET', '/statistics/overview')
  const tr = await call('GET', '/statistics/trend?range=7')
  const ct = await call('GET', '/statistics/category')
  rec('GET /statistics 汇总', st.status === 200 && st.body.code === 200, `code=${st.body.code}`)
  rec('GET /statistics/overview', ov.status === 200 && ov.body.code === 200,
    `total=${ov.body.data && ov.body.data.total} positive=${ov.body.data && ov.body.data.positive} reflect=${ov.body.data && ov.body.data.reflect} days=${ov.body.data && ov.body.data.recordDays}`)
  rec('GET /statistics/trend', tr.status === 200 && tr.body.code === 200, `点数=${(tr.body.data || []).length}`)
  rec('GET /statistics/category', ct.status === 200 && ct.body.code === 200, `分组数=${(ct.body.data || []).length}`)

  // 17. 分类自定义增删
  const addCat = await call('POST', '/categories', { type: 1, name: '端到端测试分类' })
  const newCatId = addCat.body.data && (addCat.body.data.id || addCat.body.data)
  const delCat = newCatId ? await call('DELETE', '/categories/' + newCatId) : { status: 0, body: {} }
  rec('POST/DELETE /categories 自定义分类增删', addCat.body.code === 200 && delCat.body.code === 200,
    `新增=${addCat.body.code} 删除=${delCat.body.code}`)

  // 18. 意见反馈
  const fb = await call('POST', '/feedback', { content: '端到端测试反馈内容' })
  rec('POST /feedback 提交反馈', fb.status === 200 && fb.body.code === 200, `code=${fb.body.code}`)

  // 19. 数据导出
  const exp = await call('GET', '/data/export')
  rec('GET /data/export 数据导出', exp.status === 200 && exp.body.code === 200, `data类型=${typeof exp.body.data}`)

  // 20. 批量同步（离线队列提交路径）
  const syncCid = 'e2e-sync-' + Date.now()
  const sync = await call('POST', '/sync/batch', {
    items: [{
      clientId: syncCid,
      op: 'create',
      type: 1,
      categoryName: '学习',
      content: '端到端测试：同步记录',
      recordDate: new Date().toISOString().slice(0, 10)
    }]
  })
  const syncApplied = (sync.body.data && sync.body.data.applied) || []
  const syncSkipped = (sync.body.data && sync.body.data.skipped) || []
  const synced = (await call('GET', '/records')).body.data || []
  const syncedOk = synced.some((r) => r.clientId === syncCid && r.categoryName === '学习')
  rec('POST /sync/batch 批量同步（create + 按名称解析分类）',
    sync.status === 200 && sync.body.code === 200 && syncApplied.length === 1 && syncApplied[0].op === 'create' && syncSkipped.length === 0 && syncedOk,
    `applied=${syncApplied.length} skipped=${syncSkipped.length} 已入库=${syncedOk} categoryName=${syncApplied[0] && syncApplied[0].serverId ? 'ok' : '-'}`)

  // 20b. 幂等：同 clientId 重复同步不产生第二条
  await call('POST', '/sync/batch', { items: [{ clientId: syncCid, op: 'create', type: 1, categoryName: '学习', content: '端到端测试：同步记录', recordDate: new Date().toISOString().slice(0, 10) }] })
  const dupCount = ((await call('GET', '/records')).body.data || []).filter((r) => r.clientId === syncCid).length
  rec('批量同步幂等：重复提交不产生重复记录', dupCount === 1, `库中条数=${dupCount}`)

  // 20c. update / delete 操作
  const updSync = await call('POST', '/sync/batch', { items: [{ clientId: syncCid, op: 'update', type: 2, categoryName: '自律', content: '端到端测试：同步后修改', recordDate: new Date().toISOString().slice(0, 10) }] })
  const afterUpd = ((await call('GET', '/records')).body.data || []).find((r) => r.clientId === syncCid)
  rec('批量同步 update 生效', (updSync.body.data.applied || []).length === 1 && afterUpd && afterUpd.content === '端到端测试：同步后修改',
    afterUpd ? `content=${afterUpd.content}` : '未找到')

  const delSync = await call('POST', '/sync/batch', { items: [{ clientId: syncCid, op: 'delete' }] })
  const gone = !((await call('GET', '/records')).body.data || []).some((r) => r.clientId === syncCid)
  const inBin = ((await call('GET', '/records/recycle')).body.data || []).some((r) => r.clientId === syncCid)
  rec('批量同步 delete 进回收站', (delSync.body.data.applied || []).length === 1 && gone && inBin, `已移出=${gone} 在回收站=${inBin}`)

  const purgeSync = await call('POST', '/sync/batch', { items: [{ clientId: syncCid, op: 'purge' }] })
  const goneForever = !((await call('GET', '/records/recycle')).body.data || []).some((r) => r.clientId === syncCid)
  rec('批量同步 purge 彻底删除', (purgeSync.body.data.applied || []).length === 1 && goneForever, `已彻底删除=${goneForever}`)

  // 20d. 未知 op 应被跳过且不中断批次
  const badOp = await call('POST', '/sync/batch', { items: [{ clientId: 'bogus-' + Date.now(), op: 'bogus' }] })
  rec('批量同步未知 op 被跳过', badOp.body.code === 200 && (badOp.body.data.skipped || []).length === 1,
    `skipped=${(badOp.body.data.skipped || []).length}`)

  // 21. 退出登录
  const out = await call('POST', '/auth/logout')
  rec('POST /auth/logout 退出登录', out.status === 200 && out.body.code === 200, `code=${out.body.code}`)

  // 22. 退出后 token 状态（记录事实，不虚构预期）
  // 当前设计：JWT 纯无状态，logout 仅返回 200，不吊销已签发的 token。
  const afterOut = await call('GET', '/user/profile')
  const revoked = afterOut.status === 401 || afterOut.body.code === 401
  rec('退出后 token 仍可用（当前为设计取舍，非缺陷）', !revoked,
    revoked ? '服务端已吊销' : '未吊销：服务端不存登录态，logout 不加入黑名单')
} catch (e) {
  console.error('\n执行中断:', e && e.message)
  console.error('请确认后端已启动、MySQL 与 Redis 均可用。')
}

const pass = results.filter(r => r.ok).length
console.log(`\n===== 结果: ${pass}/${results.length} 通过 =====`)
if (pass !== results.length) {
  console.log('失败项:')
  results.filter(r => !r.ok).forEach(r => console.log('  -', r.name, '::', r.detail))
}
process.exit(pass === results.length ? 0 : 1)
