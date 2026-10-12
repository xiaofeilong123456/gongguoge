<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">分类管理</view>
      <view class="ggg-back"><GIcon name="tag" :size="30" color="var(--ggg-accent)" /></view>
    </view>

    <view class="type-switch">
      <view class="type-item" :class="{ active: type === 1 }" @click="type = 1">功类</view>
      <view class="type-item" :class="{ active: type === 2 }" @click="type = 2">过类</view>
    </view>

    <view class="ggg-card list-card">
      <view v-for="item in filteredCategories" :key="item.id" class="category-row">
        <view class="category-mark" :class="type === 1 ? 'positive' : 'reflect'"><GIcon :name="type === 1 ? 'check' : 'restart'" :size="24" :color="type === 1 ? 'var(--ggg-accent)' : 'var(--ggg-terra)'" /></view>
        <view class="category-main">
          <view class="category-name">{{ item.name }}</view>
          <view class="category-count">{{ item.count }} 条记录</view>
        </view>
        <view v-if="item.system" class="system-tag">预设</view>
        <view v-else class="delete-btn" @click="removeCategory(item.id)"><GIcon name="trash" :size="26" color="var(--ggg-terra)" /></view>
      </view>
      <view v-if="!filteredCategories.length" class="empty">暂无分类</view>
    </view>

    <view class="ggg-card add-card">
      <view class="add-title">新增{{ type === 1 ? '功类' : '过类' }}分类</view>
      <view class="add-row">
        <input class="ggg-input add-input" v-model="newName" placeholder="分类名称" />
        <view class="add-btn" @click="addCategory"><GIcon name="plus" :size="26" color="#ffffff" /> 添加</view>
      </view>
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { categoryApi } from '../../utils/api.js'

export default {
  components: { GIcon },
  data() {
    return {
      type: 1,
      newName: '',
      categories: []
    }
  },
  computed: {
    filteredCategories() {
      return this.categories.filter(item => item.type === this.type)
    }
  },
  onShow() {
    this.loadCategories()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    loadCategories() {
      categoryApi.list().then((list) => {
        this.categories = list || []
      }).catch(() => {
        this.categories = []
      })
    },
    addCategory() {
      const name = this.newName.trim()
      if (!name) return
      categoryApi.add({
        type: this.type,
        name
      }).then((res) => {
        this.categories.push(res)
        this.newName = ''
      }).catch((err) => {
        uni.showToast({ title: (err && err.message) || '添加失败', icon: 'none' })
      })
    },
    removeCategory(id) {
      uni.showModal({
        title: '确认删除',
        content: '确认删除该自定义分类？',
        success: (res) => {
          if (!res.confirm) return
          categoryApi.remove(id).then(() => {
            this.categories = this.categories.filter(item => item.id !== id)
          }).catch((err) => {
            uni.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.type-switch {
  display: flex;
  padding: 8rpx;
  background: var(--ggg-surface-3);
  border-radius: 32rpx;
  margin-top: 32rpx;
}

.type-item {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  color: var(--ggg-text-2);
  font-size: 28rpx;
}

.type-item.active {
  background: var(--ggg-surface);
  color: var(--ggg-accent);
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.list-card {
  margin-top: 24rpx;
  padding: 0;
  overflow: hidden;
}

.category-row {
  display: flex;
  align-items: center;
  min-height: 124rpx;
  padding: 0 32rpx;
  border-bottom: 2rpx solid var(--ggg-border);
}

.category-row:last-child {
  border-bottom: none;
}

.category-mark {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.category-mark.positive {
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
}

.category-mark.reflect {
  background: var(--ggg-terra-soft);
  color: var(--ggg-terra);
}

.category-main {
  flex: 1;
  min-width: 0;
  margin-left: 24rpx;
}

.category-name {
  font-size: 28rpx;
  font-weight: 600;
}

.category-count {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--ggg-text-3);
}

.system-tag {
  padding: 8rpx 16rpx;
  border-radius: 16rpx;
  background: var(--ggg-surface-4);
  color: var(--ggg-text-2);
  font-size: 22rpx;
}

.delete-btn {
  color: var(--ggg-terra);
  font-size: 26rpx;
  display: flex;
  align-items: center;
}

.empty {
  padding: 48rpx;
  text-align: center;
  color: var(--ggg-text-3);
}

.add-card {
  margin-top: 24rpx;
}

.add-title {
  font-weight: 600;
}

.add-row {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.add-input {
  flex: 1;
  min-width: 0;
}

.add-btn {
  height: 88rpx;
  padding: 0 32rpx;
  border-radius: 20rpx;
  background: var(--ggg-accent-strong);
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8rpx;
}
</style>
