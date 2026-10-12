<script>
	import { settleMissedReminder } from './utils/reminder.js'
	import { hasAgreedPrivacy } from './utils/privacy.js'
	import { syncOnResume } from './utils/sync.js'

	export default {
		onLaunch: function() {
			// 启动补齐：若今天的提醒时刻已过且尚未提醒，补发一条本地通知
			try {
				settleMissedReminder()
			} catch (e) {
				// 通知能力缺失时静默跳过，不影响启动
			}
			// 合规兜底：未同意协议时不可停留在任何业务页（深链、被 kill 后恢复等场景）
			this.ensurePrivacyConsent()
			// 离线队列：把上次未同步成功的记录补传
			this.syncPending()
		},
		onShow: function() {
			// 回到前台再检查一次，覆盖「App 在后台跨过提醒时刻」的情况
			try {
				settleMissedReminder()
			} catch (e) {
				// 同上
			}
		},
		onHide: function() {
			// App hide
		},
		methods: {
			// 未同意协议前不发起同步（合规优先）
			syncPending() {
				try {
					if (!hasAgreedPrivacy()) return
					syncOnResume()
				} catch (e) {
					// 同步失败不影响启动，队列保留待下次
				}
			},
			ensurePrivacyConsent() {
				try {
					if (hasAgreedPrivacy()) return
					const pages = getCurrentPages()
					const current = pages.length ? pages[pages.length - 1].route : ''
					if (current === 'pages/auth/auth') return
					setTimeout(function () {
						uni.reLaunch({ url: '/pages/auth/auth' })
					}, 0)
				} catch (e) {
					// 页面栈尚未就绪时忽略，鉴权页自身也会拦截
				}
			}
		}
	}
</script>

<style>
	/* ===== 功过格设计令牌（浅色默认） ===== */
	page {
		--ggg-page-bg: #f8f5ee;
		--ggg-page-bg-outer: #e9e5dd;
		--ggg-surface: #ffffff;
		--ggg-surface-card: rgba(255, 255, 255, 0.75);
		--ggg-surface-card-soft: rgba(255, 255, 255, 0.55);
		--ggg-surface-input: #ffffff;
		--ggg-surface-3: #eeece6;
		--ggg-surface-4: #efede7;
		--ggg-border: #e8e4dc;
		--ggg-border-2: #e3dfd6;
		--ggg-border-3: #e7e1d8;
		--ggg-border-strong: #d5d0c7;
		--ggg-border-ghost: #b8ac98;
		--ggg-text-1: #292a27;
		--ggg-text-2: #77756f;
		--ggg-text-3: #88857f;
		--ggg-text-3b: #8b8982;
		--ggg-text-faint: #99968f;
		--ggg-accent: #367d57;
		--ggg-accent-strong: #3f8b60;
		--ggg-green-fill: #4b956b;
		--ggg-accent-soft: #edf5ef;
		--ggg-accent-soft2: #e7f2e9;
		--ggg-accent-softer: #edf4ed;
		--ggg-tag-positive-text: #477758;
		--ggg-terra: #a45d36;
		--ggg-terra-strong: #b8663e;
		--ggg-terra-deep: #a9533c;
		--ggg-brown: #795f42;
		--ggg-error-text: #9b5937;
		--ggg-terra-soft: #f8ede5;
		--ggg-terra-soft2: #f7eee7;
		--ggg-fs-title: 56rpx;
		--ggg-fs-card: 32rpx;
		--ggg-fs-row: 32rpx;
	}

	/* 页面根：底色与基础字体 */
	page {
		background: var(--ggg-page-bg-outer);
		color: var(--ggg-text-1);
		font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
	}
	/* 基础页面：尽量还原 HTML 原型中的“纸面”质感 */
	.ggg-page {
		min-height: 100vh;
		box-sizing: border-box;
		padding-top: 40rpx;
		padding-top: calc(40rpx + var(--status-bar-height, 0px));
		padding-right: 40rpx;
		padding-bottom: 160rpx;
		padding-left: 40rpx;
		background: var(--ggg-page-bg);
		background-image: radial-gradient(circle at 20% 10%, rgba(174, 145, 98, 0.07), transparent 27%),
			radial-gradient(circle at 90% 80%, rgba(57, 107, 77, 0.045), transparent 30%);
		color: var(--ggg-text-1);
	}

	.ggg-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		min-height: 88rpx;
	}

	.ggg-back {
		width: 88rpx;
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		color: var(--ggg-terra);
		font-size: 44rpx;
		font-weight: 700;
	}

	.ggg-title {
		font-size: 56rpx;
		font-weight: 700;
		letter-spacing: 2rpx;
	}

	.ggg-card {
		background: var(--ggg-surface-card);
		border: 2rpx solid var(--ggg-border-3);
		border-radius: 32rpx;
		padding: 32rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
	}

	.ggg-muted {
		color: var(--ggg-text-2);
	}

	.ggg-small {
		font-size: 24rpx;
	}

	.ggg-btn {
		height: 96rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 32rpx;
		font-size: 28rpx;
		font-weight: 600;
		box-sizing: border-box;
	}

	.ggg-btn-primary {
		background: var(--ggg-accent-strong);
		color: #ffffff;
	}

	.ggg-btn-ghost {
		border: 2rpx solid var(--ggg-border-ghost);
		color: var(--ggg-brown);
		background: transparent;
	}

	.ggg-row {
		display: flex;
		align-items: center;
		min-height: 124rpx;
		border-bottom: 2rpx solid var(--ggg-border);
		padding: 0 32rpx;
	}

	.ggg-row:last-child {
		border-bottom: none;
	}

	.ggg-input {
		height: 96rpx;
		box-sizing: border-box;
		background: var(--ggg-surface);
		border: 2rpx solid var(--ggg-border-2);
		border-radius: 24rpx;
		padding: 0 32rpx;
		font-size: 32rpx;
	}

	.ggg-textarea {
		width: 100%;
		min-height: 240rpx;
		box-sizing: border-box;
		background: var(--ggg-surface);
		border: 2rpx solid var(--ggg-border-2);
		border-radius: 32rpx;
		padding: 28rpx;
		font-size: 32rpx;
	}

	/* 分段控件（趋势/概览、功类/过类、协议切换等） */
	.ggg-segmented {
		display: flex;
		padding: 8rpx;
		background: var(--ggg-surface-3);
		border-radius: 32rpx;
		margin-top: 32rpx;
	}

	.ggg-segmented-item {
		flex: 1;
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 24rpx;
		color: var(--ggg-text-2);
		font-size: 28rpx;
	}

	.ggg-segmented-item.active {
		background: var(--ggg-surface);
		color: var(--ggg-accent);
		font-weight: 600;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}

	/* 标签胶囊 */
	.ggg-tag {
		padding: 8rpx 20rpx;
		border-radius: 20rpx;
		font-size: 22rpx;
	}

	.ggg-tag-positive {
		background: var(--ggg-accent-softer);
		color: var(--ggg-tag-positive-text);
	}

	.ggg-tag-reflect {
		background: var(--ggg-terra-soft2);
		color: var(--ggg-error-text);
	}

	/* 原型中的墨圈 */
	.ggg-ink-ring {
		border: 18rpx solid rgba(40, 43, 38, 0.16);
		box-shadow: inset 4rpx 0 0 rgba(40, 43, 38, 0.13), 0 0 0 6rpx rgba(40, 43, 38, 0.035);
		transform: rotate(-4deg);
		border-radius: 50%;
	}

	/* 深色模式 */
	.theme-dark {
		--ggg-page-bg: #1d1e1b;
		--ggg-page-bg-outer: #161714;
		--ggg-surface: #262822;
		--ggg-surface-card: rgba(36, 38, 34, 0.92);
		--ggg-surface-card-soft: rgba(36, 38, 34, 0.72);
		--ggg-surface-input: #242622;
		--ggg-surface-3: #2c2e29;
		--ggg-surface-4: #2c2e29;
		--ggg-border: #3a3c36;
		--ggg-border-2: #3a3c36;
		--ggg-border-3: #3a3c36;
		--ggg-border-strong: #4a4c45;
		--ggg-border-ghost: #5c5a52;
		--ggg-text-1: #ece9e1;
		--ggg-text-2: #aaa79e;
		--ggg-text-3: #9c998f;
		--ggg-text-3b: #a29f96;
		--ggg-text-faint: #83807a;
		--ggg-accent: #7cc39a;
		--ggg-accent-strong: #5faf83;
		--ggg-green-fill: #4b956b;
		--ggg-accent-soft: #263528;
		--ggg-accent-soft2: #263528;
		--ggg-accent-softer: #262e26;
		--ggg-tag-positive-text: #8fcba6;
		--ggg-terra: #d99a76;
		--ggg-terra-strong: #cd8a63;
		--ggg-terra-deep: #b0603f;
		--ggg-brown: #c9b08e;
		--ggg-error-text: #d79b78;
		--ggg-terra-soft: #3a2c22;
		--ggg-terra-soft2: #3a2c22;
	}

	/* 页面墨色光晕在深色下换一层更淡的 */
	.theme-dark.ggg-page {
		background-image: radial-gradient(circle at 20% 10%, rgba(212, 175, 135, 0.06), transparent 27%),
			radial-gradient(circle at 90% 80%, rgba(90, 130, 104, 0.05), transparent 30%);
	}

	/* ===== 字体大小：只改令牌 ===== */
	.font-small {
		--ggg-fs-title: 48rpx;
		--ggg-fs-card: 28rpx;
		--ggg-fs-row: 28rpx;
	}

	.font-large {
		--ggg-fs-title: 64rpx;
		--ggg-fs-card: 36rpx;
		--ggg-fs-row: 36rpx;
	}

	.ggg-title {
		font-size: var(--ggg-fs-title);
	}

	.ggg-card {
		font-size: var(--ggg-fs-card);
	}

	.ggg-row {
		font-size: var(--ggg-fs-row);
	}
</style>