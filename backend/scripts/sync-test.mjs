// 功过格 · 离线同步专项测试
//
// 用法:
//   node scripts/sync-test.mjs
//   API_BASE=http://127.0.0.1:8080/api node scripts/sync-test.mjs
//
// 只依赖 Node 18+（内置 fetch）。前置与 e2e-test.mjs 相同：
// MySQL 与 Redis 已启动、后端已运行、ALIYUN_SMS_ENABLED=false。

const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api'
const CODE = '123456'
// 测试脚本注册的账号昵称，作为 clean-test-data.mjs 的唯一识别依据
const TEST_NICKNAME = '测试账号'

let token = ''
let pass = 0
let fail = 0
function t(name, ok, detail) {
  if (ok) { pass++; console.log('PASS  ' + name) }
  else { fail++; console.log('FAIL  ' + name + (detail ? '  -> ' + detail : '')) }
}
async function call(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = 'Bearer ' + token
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
const sync = (items) => call('POST', '/sync/batch', { items })
const allRecords = async () => (await call('GET', '/records')).body.data || []
const recycle = async () => (await call('GET', '/records/recycle')).body.data || []

console.log('API:', BASE)
const phone = '136' + String(Date.now()).slice(-8)
await call('POST', '/auth/sms-code', { phone, scene: 'login' })
const login = await call('POST', '/auth/login', { phone, code: CODE, nickname: TEST_NICKNAME })
token = login.body.data && login.body.data.token
if (!token) {
  console.log('登录失败，无法继续:', JSON.stringify(login.body))
  process.exit(1)
}
console.log('测试手机号:', phone, '\n')

const cid = 'sync_test_' + Date.now()
const today = new Date().toISOString().slice(0, 10)

// 1. create：用 categoryName 回退解析分类（模拟离线时拿不到 categoryId）
const r1 = await sync([{ clientId: cid, op: 'create', type: 1, categoryName: '学习', content: '同步测试新增', recordDate: today }])
const a1 = (r1.body.data && r1.body.data.applied) || []
t('sync create 成功应用', r1.body.code === 200 && a1.length === 1 && a1[0].op === 'create',
  JSON.stringify(r1.body.data).slice(0, 200))
const created = (await allRecords()).find((x) => x.content === '同步测试新增')
t('create 后服务端存在该记录', !!created, created ? 'id=' + created.id : '未找到')
t('create 时按 categoryName 正确解析分类', created && created.categoryName === '学习',
  created ? 'categoryName=' + created.categoryName : '-')

// 2. create 幂等
const cid2 = 'sync_idem_' + Date.now()
await sync([{ clientId: cid2, op: 'create', type: 1, categoryName: '习惯', content: '同步测试幂等', recordDate: today }])
await sync([{ clientId: cid2, op: 'create', type: 1, categoryName: '习惯', content: '同步测试幂等', recordDate: today }])
const idemList = (await allRecords()).filter((x) => x.content === '同步测试幂等')
t('create 幂等：重复同步只有一条', idemList.length === 1, '条数=' + idemList.length)

// 3. update
const r3 = await sync([{ clientId: cid, op: 'update', type: 2, categoryName: '自律', content: '同步测试修改后', recordDate: today }])
const updated = (await allRecords()).find((x) => x.clientId === cid)
t('sync update 成功应用', (r3.body.data.applied || []).length === 1, JSON.stringify(r3.body.data).slice(0, 200))
t('update 后内容与类型已变更', updated && updated.content === '同步测试修改后' && updated.type === 2,
  updated ? 'content=' + updated.content + ' type=' + updated.type : '未找到')

// 4. update 对不存在的 clientId：应按新增处理
const cid3 = 'sync_upd_new_' + Date.now()
const r4 = await sync([{ clientId: cid3, op: 'update', type: 1, categoryName: '健康', content: '同步测试更新即新增', recordDate: today }])
const upNew = (await allRecords()).find((x) => x.clientId === cid3)
t('update 不存在时回退为新增', (r4.body.data.applied || []).length === 1 && !!upNew, upNew ? 'id=' + upNew.id : '未创建')

// 5. delete 进回收站
const r5 = await sync([{ clientId: cid, op: 'delete' }])
const gone = !(await allRecords()).some((x) => x.clientId === cid)
const inRecycle = (await recycle()).some((x) => x.clientId === cid)
t('sync delete 成功应用', (r5.body.data.applied || []).length === 1, JSON.stringify(r5.body.data).slice(0, 200))
t('delete 后不在正常列表且在回收站', gone && inRecycle, 'gone=' + gone + ' inRecycle=' + inRecycle)

// 6. delete 幂等
const r6 = await sync([{ clientId: cid, op: 'delete' }])
t('delete 幂等：重复删除仍返回成功', (r6.body.data.applied || []).length === 1, JSON.stringify(r6.body.data).slice(0, 200))

// 7. purge 彻底删除
const r7 = await sync([{ clientId: cid, op: 'purge' }])
const stillInRecycle = (await recycle()).some((x) => x.clientId === cid)
t('sync purge 成功应用', (r7.body.data.applied || []).length === 1, JSON.stringify(r7.body.data).slice(0, 200))
t('purge 后记录彻底消失', !stillInRecycle, '仍在回收站=' + stillInRecycle)

// 8. 未知 op 被跳过且不中断批次
const r8 = await sync([{ clientId: 'x_' + Date.now(), op: 'bogus', type: 1, content: 'x', recordDate: today }])
const skipped = (r8.body.data && r8.body.data.skipped) || []
t('未知 op 被跳过且不中断批次', r8.body.code === 200 && skipped.length === 1 && skipped[0].reason,
  JSON.stringify(r8.body.data).slice(0, 220))

// 9. 混合批次
const base = 'sync_mix_' + Date.now()
const cA = base + '_a', cB = base + '_b', cC = base + '_c'
await sync([
  { clientId: cA, op: 'create', type: 1, categoryName: '学习', content: '同步测试混合A', recordDate: today },
  { clientId: cB, op: 'create', type: 2, categoryName: '习惯', content: '同步测试混合B', recordDate: today }
])
const r9 = await sync([
  { clientId: cA, op: 'update', type: 1, categoryName: '学习', content: '同步测试混合A改', recordDate: today },
  { clientId: cB, op: 'delete' },
  { clientId: cC, op: 'create', type: 1, categoryName: '人际', content: '同步测试混合C', recordDate: today }
])
const list9 = await allRecords()
const aOk = list9.some((x) => x.clientId === cA && x.content === '同步测试混合A改')
const bGone = !list9.some((x) => x.clientId === cB) && (await recycle()).some((x) => x.clientId === cB)
const cOk = list9.some((x) => x.clientId === cC)
t('混合批次(update+delete+create) 全部正确处理',
  (r9.body.data.applied || []).length === 3 && aOk && bGone && cOk,
  'applied=' + (r9.body.data.applied || []).length + ' A改=' + aOk + ' B删除=' + bGone + ' C新增=' + cOk)

// 10. 缺 clientId 属请求格式错误
const r10 = await sync([{ op: 'create', type: 1, categoryName: '学习', content: '无clientId', recordDate: today }])
t('缺 clientId 的批次被拒绝（格式错误）', r10.body.code === 400,
  'code=' + r10.body.code + ' msg=' + r10.body.message)

// 11. create 缺 content：跳过并给出原因
const r11 = await sync([{ clientId: 'nocontent_' + Date.now(), op: 'create', type: 1, categoryName: '学习', recordDate: today }])
const sk11 = (r11.body.data && r11.body.data.skipped) || []
t('create 缺 content 被跳过并给出原因', sk11.length === 1 && /内容/.test(sk11[0].reason || ''),
  JSON.stringify(r11.body.data).slice(0, 220))

// 12. 离线新增拿不到分类时，服务端应回退到预设分类而不是拒绝
const cidOffline = 'sync_offline_' + Date.now()
const r12 = await sync([{ clientId: cidOffline, op: 'create', type: 1, content: '离线无分类', recordDate: today }])
const offlineRec = (await allRecords()).find((x) => x.clientId === cidOffline)
t('离线新增无分类时回退到预设分类（不再卡队列）',
  (r12.body.data.applied || []).length === 1 && !!offlineRec,
  'applied=' + (r12.body.data.applied || []).length + ' skipped=' + (r12.body.data.skipped || []).length +
  ' categoryName=' + (offlineRec && offlineRec.categoryName))

// 13. 删除老记录（无 clientId）时可用 serverId 定位
const legacyAdd = await call('POST', '/records', { type: 1, categoryId: 1, content: '老记录serverId删除', recordDate: today })
const legacyId = legacyAdd.body.data.id
const r13 = await sync([{ clientId: 'legacy_' + Date.now(), op: 'delete', serverId: legacyId }])
const legacyGone = !(await allRecords()).some((x) => x.id === legacyId)
const legacyInBin = (await recycle()).some((x) => x.id === legacyId)
t('serverId 可定位并删除老记录', (r13.body.data.applied || []).length === 1 && legacyGone && legacyInBin,
  'applied=' + (r13.body.data.applied || []).length + ' 移出列表=' + legacyGone + ' 进回收站=' + legacyInBin)

// 14. 前端兜底的合成 clientId（s_ 前缀）不得假成功
const r14 = await sync([{ clientId: 's_99999999', op: 'delete' }])
const sk14 = (r14.body.data && r14.body.data.skipped) || []
t('伪造的 s_ 前缀 clientId 被拒绝（不假成功）',
  sk14.length === 1 && (r14.body.data.applied || []).length === 0,
  JSON.stringify(r14.body.data).slice(0, 220))

// 15. 完全无定位信息的删除应被拒绝
const r15 = await sync([{ clientId: '', op: 'delete' }])
t('缺 clientId 的删除请求被参数校验拒绝', r15.body.code === 400, 'code=' + r15.body.code)

console.log('\n===== 结果: ' + pass + '/' + (pass + fail) + ' 通过 =====')
if (fail) {
  console.log('存在失败项')
}
process.exit(fail === 0 ? 0 : 1)
