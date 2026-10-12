// 功过格 · 清理测试产生的数据
//
// 用法:
//   node scripts/clean-test-data.mjs            # 预览，需确认
//   node scripts/clean-test-data.mjs --yes      # 跳过确认
//   node scripts/clean-test-data.mjs --phone 13912345678,13800000000 --yes   # 只删指定手机号
//
// 安全设计（重要）：
//   早期版本靠「昵称为默认值 + 手机号 13x 开头」猜测测试数据，
//   曾误删真实用户账号。现在只删**测试脚本自己显式标记**的数据：
//     1. 昵称恰好等于 TEST_NICKNAME 的账号（e2e-test.mjs / sync-test.mjs 会这样注册）；
//     2. 这些账号名下的功过记录；
//     3. 内容以「端到端测试」或「同步测试」开头的反馈。
//   你手工注册的账号（昵称「功过格用户」）永远不会被这个脚本删除。
//
// 通过 mysql 命令行客户端执行，无需 Node 第三方依赖。

import { spawnSync } from 'node:child_process'
import { existsSync, readdirSync } from 'node:fs'
import path from 'node:path'
import readline from 'node:readline'

// 测试脚本注册时使用的昵称，作为唯一删除依据
const TEST_NICKNAME = '测试账号'

const cfg = {
  host: process.env.MYSQL_HOST || '127.0.0.1',
  port: process.env.MYSQL_PORT || '3306',
  user: process.env.MYSQL_USER || 'root',
  password: process.env.MYSQL_PASSWORD || process.env.MYSQL_PWD || '',
  database: process.env.MYSQL_DATABASE || 'gongguoge'
}
const autoYes = process.argv.includes('--yes')
const phoneArgIdx = process.argv.indexOf('--phone')
const explicitPhones = phoneArgIdx >= 0 && process.argv[phoneArgIdx + 1]
  ? process.argv[phoneArgIdx + 1].split(',').map((s) => s.trim()).filter(Boolean)
  : []

function findMysql() {
  if (process.env.MYSQL_BIN && existsSync(process.env.MYSQL_BIN)) return process.env.MYSQL_BIN
  const candidates = [
    'D:\\software\\111\\bin\\mysql.exe',
    'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe',
    'C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysql.exe'
  ]
  for (const c of candidates) {
    if (existsSync(c)) return c
  }
  try {
    for (const d of readdirSync('D:\\software')) {
      const p = path.join('D:\\software', d, 'bin', 'mysql.exe')
      if (existsSync(p)) return p
    }
  } catch (e) {
    // 忽略
  }
  const which = spawnSync('where', ['mysql'], { encoding: 'utf8' })
  if (which.status === 0) return which.stdout.split(/\r?\n/)[0].trim()
  return null
}

const mysqlBin = findMysql()
if (!mysqlBin) {
  console.error('未找到 mysql 客户端。请设置环境变量 MYSQL_BIN 指向 mysql.exe。')
  process.exit(1)
}
console.log('mysql 客户端:', mysqlBin)
console.log('目标数据库:', cfg.user + '@' + cfg.host + ':' + cfg.port + '/' + cfg.database + '\n')

function q(sql) {
  const args = [
    '-h' + cfg.host,
    '-P' + cfg.port,
    '-u' + cfg.user,
    '--default-character-set=utf8mb4',
    '-N',
    '-B',
    '-e',
    sql
  ]
  if (cfg.password) args.splice(3, 0, '-p' + cfg.password)
  const r = spawnSync(mysqlBin, args, { encoding: 'utf8' })
  if (r.status !== 0) throw new Error((r.stderr || '').trim() || 'mysql 执行失败')
  return (r.stdout || '').trim()
}

const DB = cfg.database
// 删除依据：昵称精确匹配，或调用方显式指定的手机号
const conds = ["nickname = '" + TEST_NICKNAME + "'"]
if (explicitPhones.length) {
  conds.push("phone IN (" + explicitPhones.map((p) => "'" + p.replace(/'/g, '') + "'").join(',') + ")")
}
const USER_COND = '(' + conds.join(' OR ') + ')'

let counts
try {
  counts = {
    users: q('SELECT COUNT(*) FROM ' + DB + '.t_user WHERE ' + USER_COND + ';'),
    records: q('SELECT COUNT(*) FROM ' + DB + '.t_merit_record WHERE user_id IN (SELECT id FROM ' + DB + '.t_user WHERE ' + USER_COND + ');'),
    feedback: q("SELECT COUNT(*) FROM " + DB + ".t_feedback WHERE content LIKE '端到端测试%' OR content LIKE '同步测试%';"),
    categories: q("SELECT COUNT(*) FROM " + DB + ".t_category WHERE name = '端到端测试分类';")
  }
} catch (e) {
  console.error('查询失败:', e.message)
  process.exit(1)
}

console.log('删除依据: 昵称 = "' + TEST_NICKNAME + '"' + (explicitPhones.length ? ' 或 指定手机号 ' + explicitPhones.join(', ') : ''))
console.log('\n将清理以下数据:')
console.log('  测试账号      : ' + counts.users + ' 个')
console.log('  账号名下记录  : ' + counts.records + ' 条')
console.log('  测试反馈      : ' + counts.feedback + ' 条')
console.log('  测试分类      : ' + counts.categories + ' 个')

const keep = q('SELECT COUNT(*) FROM ' + DB + '.t_user WHERE NOT ' + USER_COND + ';')
console.log('\n将保留账号: ' + keep + ' 个（含你手工注册的账号，昵称为「功过格用户」）')

const total = Number(counts.users) + Number(counts.records) + Number(counts.feedback) + Number(counts.categories)
if (total === 0) {
  console.log('\n没有需要清理的数据。')
  process.exit(0)
}

if (!autoYes) {
  if (!process.stdin.isTTY) {
    console.log('\n非交互环境（stdin 不是终端），未确认，已取消。')
    console.log('确认清理请加 --yes 参数。')
    process.exit(0)
  }
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout })
  const answer = await new Promise(function (resolve) {
    rl.question('\n确认删除? 输入 yes 继续: ', resolve)
  })
  rl.close()
  if (String(answer).trim().toLowerCase() !== 'yes') {
    console.log('已取消。')
    process.exit(0)
  }
}

try {
  q('DELETE FROM ' + DB + '.t_merit_record WHERE user_id IN (SELECT id FROM ' + DB + '.t_user WHERE ' + USER_COND + ');')
  q("DELETE FROM " + DB + ".t_feedback WHERE content LIKE '端到端测试%' OR content LIKE '同步测试%';")
  q("DELETE FROM " + DB + ".t_category WHERE name = '端到端测试分类';")
  q('DELETE FROM ' + DB + '.t_user WHERE ' + USER_COND + ';')
  console.log('\n清理完成。')
} catch (e) {
  console.error('\n清理失败:', e.message)
  process.exit(1)
}
