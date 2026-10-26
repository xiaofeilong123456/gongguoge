import { request, uploadFile } from './request.js'

export const authApi = {
  sendSmsCode(data) {
    return request({ url: '/auth/sms-code', method: 'POST', data })
  },
  register(data) {
    return request({ url: '/auth/register', method: 'POST', data })
  },
  login(data) {
    return request({ url: '/auth/login', method: 'POST', data })
  },
  logout() {
    return request({ url: '/auth/logout', method: 'POST' })
  },
  changePassword(data) {
    return request({ url: '/auth/password', method: 'PUT', data })
  }
}

export const userApi = {
  getProfile() {
    return request({ url: '/user/profile' })
  },
  updateProfile(data) {
    return request({ url: '/user/profile', method: 'PUT', data })
  },
  // 注销账号：服务端删除账号及全部个人数据，调用后当前 token 立即失效
  deleteAccount() {
    return request({ url: '/user/account', method: 'DELETE' })
  }
}

export const categoryApi = {
  list() {
    return request({ url: '/categories' })
  },
  add(data) {
    return request({ url: '/categories', method: 'POST', data })
  },
  remove(id) {
    return request({ url: '/categories/' + id, method: 'DELETE' })
  }
}

export const recordApi = {
  today() {
    return request({ url: '/records/today' })
  },
  list(params) {
    return request({ url: '/records', data: params || {} })
  },
  add(data) {
    return request({ url: '/records', method: 'POST', data })
  },
  update(id, data) {
    return request({ url: '/records/' + id, method: 'PUT', data })
  },
  remove(id) {
    return request({ url: '/records/' + id, method: 'DELETE' })
  },
  recycle() {
    return request({ url: '/records/recycle' })
  },
  restore(id) {
    return request({ url: '/records/recycle/' + id + '/restore', method: 'POST' })
  },
  permanentDelete(id) {
    return request({ url: '/records/recycle/' + id, method: 'DELETE' })
  }
}

export const statisticsApi = {
  get(params) {
    return request({ url: '/statistics', data: params || {} })
  },
  overview(params) {
    return request({ url: '/statistics/overview', data: params || {} })
  },
  trend(params) {
    return request({ url: '/statistics/trend', data: params || {} })
  },
  category(params) {
    return request({ url: '/statistics/category', data: params || {} })
  }
}

export const dataApi = {
  export() {
    return request({ url: '/data/export' })
  },
  clear() {
    return request({ url: '/data/clear', method: 'DELETE' })
  }
}

export const feedbackApi = {
  submit(data) {
    return request({ url: '/feedback', method: 'POST', data })
  },
  list() {
    return request({ url: '/feedback' })
  }
}

export const syncApi = {
  batch(data) {
    return request({ url: '/sync/batch', method: 'POST', data })
  }
}

export const fileApi = {
  // 上传头像等文件；filePath 来自 uni.chooseImage 的临时路径
  upload(filePath) {
    return uploadFile({ url: '/files/upload', filePath, name: 'file' })
  }
}
