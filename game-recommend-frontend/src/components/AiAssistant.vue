<template>
  <div
    class="ai-assistant-panel"
    :class="{ 'ai-floating': mode === 'floating', 'ai-embedded': mode === 'embedded' }"
    :style="mergedStyle"
  >
    <!-- 顶部标题栏 -->
    <div class="ai-header">
      <div class="ai-title">
        <span class="ai-icon" aria-hidden="true">
          <svg class="ai-icon-svg" width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M12 2l1.85 4.6L19 7.5l-3.7 3.3.95 5L12 13.6 7.75 15.8l.95-5L5 7.5l5.15-.9L12 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" fill="currentColor" fill-opacity="0.18"/>
            <circle cx="18" cy="5" r="1" fill="currentColor"/>
            <circle cx="5" cy="18" r="0.8" fill="currentColor"/>
            <circle cx="19" cy="17" r="0.6" fill="currentColor"/>
          </svg>
        </span>
        <div class="ai-title-text">
          <span class="ai-name">GameRec AI 助手</span>
          <span class="ai-name-sub">基于系统内部数据 · 不编造</span>
        </div>
        <span class="ai-model" v-if="modelName">{{ modelName }}</span>
        <el-tag v-if="!available" type="info" size="mini" effect="plain">未配置</el-tag>
        <el-tag v-else type="success" size="mini" effect="plain">在线</el-tag>
      </div>
      <div class="ai-actions">
        <button
          v-if="messages.length > 1"
          class="ai-icon-btn"
          @click="clearChat"
          title="清空对话"
        >
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M2 4h10M4 4V2.5C4 1.95 4.45 1.5 5 1.5h4c.55 0 1 .45 1 1V4M5.5 6.5v4M8.5 6.5v4M3 4l.5 7.5c0 .55.45 1 1 1h5c.55 0 1-.45 1-1L11 4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
        </button>
        <button
          v-if="mode === 'floating'"
          class="ai-icon-btn"
          @click="$emit('close')"
          title="关闭"
        >
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M3 3l8 8M11 3l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        </button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="ai-messages" ref="messagesRef">
      <div v-for="m in displayedMessages" :key="m._key" :class="['ai-msg', `ai-msg-${m.role}`]">
        <div class="ai-avatar">
          <span v-if="m.role === 'user'">我</span>
          <span v-else>AI</span>
        </div>
        <div class="ai-bubble">
          <!-- 关键：缓存 renderMd 结果，避免每次响应式更新都重跑正则 -->
          <!-- 关键修复A: 增加 v-if/v-else 回退，_html 为空时直接显示文本（修复初始气泡空白） -->
          <div v-if="m.role === 'assistant' && m._html" class="ai-content" v-html="m._html"></div>
          <div v-else-if="m.role === 'assistant' && m.content" class="ai-content">{{ m.content }}</div>
          <div v-else class="ai-content">{{ m.content }}</div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="ai-msg ai-msg-assistant">
        <div class="ai-avatar">AI</div>
        <div class="ai-bubble">
          <div class="ai-loading">
            <span></span><span></span><span></span>
            <em>AI 正在思考中…</em>
          </div>
        </div>
      </div>

      <!-- 错误/提示 -->
      <div v-if="errorText" class="ai-error">{{ errorText }}</div>
    </div>

    <!-- 快捷问题（自动轮换） -->
    <div v-if="showQuick && messages.length <= 1" class="ai-quick">
      <div class="ai-quick-label">
        <span class="ai-quick-dot"></span>
        <span>试试问我</span>
        <span class="ai-quick-counter">{{ currentQIndex + 1 }}/{{ quickQuestionsList.length }}</span>
      </div>
      <div class="ai-quick-list">
        <button
          v-for="(q, idx) in visibleQuestions"
          :key="q + '-' + idx"
          class="ai-quick-tag"
          :class="{ 'ai-quick-tag--primary': idx === 0 }"
          @click="sendMessage(q)"
        >
          <svg v-if="idx === 0" class="ai-quick-icon" width="11" height="11" viewBox="0 0 12 12" fill="none" aria-hidden="true">
            <path d="M6 1v3M6 8v3M1 6h3M8 6h3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
            <circle cx="6" cy="6" r="1.6" fill="currentColor"/>
          </svg>
          <span>{{ q }}</span>
        </button>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="ai-input">
      <el-input
        v-model="input"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        :placeholder="inputPlaceholder"
        :disabled="loading"
        @keydown.enter.exact.prevent="onEnter"
        @compositionstart="composing = true"
        @compositionend="composing = false"
      />
      <button
        class="ai-send-btn"
        :class="{ 'is-loading': loading, 'is-disabled': !input.trim() || sending }"
        :disabled="loading || !input.trim() || sending"
        @click="sendCurrent"
        :title="loading ? '正在发送...' : '发送 (Enter)'"
      >
        <span v-if="!loading">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M2 8h10M8 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span v-else class="ai-send-spinner"></span>
      </button>
      <!-- 取消按钮：仅在 loading 时显示，避免长时间卡死无法终止 -->
      <button
        v-if="loading"
        class="ai-cancel-btn"
        @click="cancelRequest(false)"
        title="停止等待 AI 回复"
      >
        <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><rect x="2" y="2" width="8" height="8" rx="1" fill="currentColor"/></svg>
      </button>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

// 极简 Markdown 渲染（粗体/行内代码/代码块/段落/列表）
// 关键：使用 (.*?) 非贪婪 + [\s\S] 匹配换行，避免正则回溯
function escapeHtml(s) {
  if (s == null) return ''
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
function renderMd(text) {
  if (!text) return ''
  let s = escapeHtml(text)
  // 代码块（必须先处理，避免里面被行内规则再次匹配）
  s = s.replace(/```([\s\S]*?)```/g, function (_, code) {
    return '<pre><code>' + code.trim() + '</code></pre>'
  })
  // 行内代码
  s = s.replace(/`([^`\n]+)`/g, '<code>$1</code>')
  // 粗体
  s = s.replace(/\*\*([^*\n]{1,500})\*\*/g, '<strong>$1</strong>')
  // 段落与列表
  const paras = s.split(/\n{2,}/)
  return paras.map(function (p) {
    if (/^\s*[-*]\s+/m.test(p)) {
      const items = p.split(/\n/).filter(function (l) { return /^\s*[-*]\s+/.test(l) })
        .map(function (l) { return '<li>' + l.replace(/^\s*[-*]\s+/, '').replace(/\n/g, '<br>') + '</li>' })
        .join('')
      return '<ul>' + items + '</ul>'
    }
    return '<p>' + p.replace(/\n/g, '<br>') + '</p>'
  }).join('')
}

// 通用问题库（按场景分组）
const GENERAL_QUESTIONS = [
  '系统里有多少款游戏？最受欢迎的是哪款？',
  '帮我在游戏库里推荐 3 款适合放松的休闲游戏',
  '我想玩剧情深刻的 RPG，系统里有哪些值得推荐？',
  '当前游戏库中好评率最高的游戏类型是什么？',
  '系统里免费游戏占比多少？质量如何？',
  '给玩家 #1 画像做一段分析总结',
  '最近 1 年内发布的热门游戏有哪些？',
  '帮我推荐几款 2023-2026 年高评价的动作游戏'
]

// 游戏详情页问题库（针对具体游戏）
const GAME_QUESTIONS = [
  '用一段话介绍一下这款游戏的核心玩法',
  '这款游戏适合什么样的人群？',
  '这款游戏值不值得现在入手？',
  '和同类型游戏相比，这款有什么亮点？',
  '系统里玩家对它的评价怎么样？',
  '总结这款游戏的优点和不足',
  '我应该先看哪些标签/分类来了解它？',
  '简明说说这款游戏的发行商和开发商'
]

// 玩家画像页问题库
const USER_QUESTIONS = [
  '这个玩家是什么类型？有什么特点？',
  '这个玩家活跃度如何？需要召回吗？',
  '他/她对什么类型的游戏最感兴趣？',
  '总结这个玩家的运营价值',
  '给这个玩家推荐 3 款合适的游戏',
  '这个玩家的消费习惯怎么样？',
  '他的 RFM 三维表现如何解读？',
  '分析这个玩家的行为偏好'
]

// 系统全局问题库（用于浮动 AI 助手）
const SYSTEM_QUESTIONS = [
  '系统里有多少款游戏？最受欢迎的是哪款？',
  '系统当前有多少玩家？平均评分多少？',
  '帮我在游戏库里推荐 3 款适合放松的休闲游戏',
  '当前游戏库中好评率最高的游戏类型是什么？',
  '系统里免费游戏占比多少？质量如何？',
  '最近 1 年内发布的热门游戏有哪些？',
  '系统里最受欢迎的 3 款动作游戏是哪些？',
  '帮我推荐几款 2023-2026 年高评价的游戏'
]

// 关键修复1: 加大请求超时到 90 秒，匹配智谱 BigModel 慢时段
const REQUEST_TIMEOUT_MS = 90000
// 关键修复2: 硬上限消息数（避免长对话内存爆炸）
const HARD_MESSAGE_LIMIT = 50

export default {
  name: 'AiAssistant',
  inheritAttrs: false,
  props: {
    /** 显示模式：floating 浮窗 / embedded 内嵌 */
    mode: { type: String, default: 'embedded' },
    /** 预设系统提示词（覆盖默认） */
    systemPrompt: { type: String, default: '' },
    /** 是否显示快捷问题 */
    showQuick: { type: Boolean, default: true },
    /** 场景：general / game / user / system */
    context: { type: String, default: 'general' },
    /** 场景 ID（gameId 或 userId） */
    contextId: { type: [Number, String], default: null },
    /** 自定义问题列表（覆盖默认） */
    quickQuestions: { type: Array, default: null },
    /** 自定义欢迎语 */
    welcomeMessage: { type: String, default: '' },
    /** 自动轮换间隔（毫秒），0 = 禁用 */
    rotateInterval: { type: Number, default: 5000 },
    /** 自定义高度（覆盖默认 min-height） */
    height: { type: String, default: '' }
  },
  data() {
    return {
      input: '',
      // 关键修复B: 用 _buildMsg 工厂预渲染初始消息的 _html，避免初始气泡空白（截图3）
      // 关键修复C: 启动时直接传入已渲染的 _html，跳过 mounted 中的二次替换
      // 关键修复D: 用 +1 占位让 _msgKeySeq 从 1 开始递增
      messages: (function () {
        const initContent = '你好！我是 GameRec AI 助手，所有回答都基于系统内部数据。'
        return [{
          _key: 0,
          role: 'assistant',
          content: initContent,
          _html: renderMd(initContent)
        }]
      })(),
      loading: false,
      sending: false,
      errorText: '',
      available: false,
      modelName: 'glm-4-flash-250414',
      currentQIndex: 0,
      rotateTimer: null,
      composing: false,
      cancelTokenSource: null,
      // 关键修复4: 用单调递增 key 替代数组下标，确保 v-for diff 正确
      _msgKeySeq: 1,
      MAX_MESSAGES: 40,
      MAX_HISTORY_ROUNDS: 6,
      // 关键修复L: welcomeMessage 应用标志，避免父组件异步数据加载时多次替换消息
      // （例如 GameDetail 嵌入模式下 game 异步加载完成后 welcomeMessage 变化）
      _welcomeApplied: false
    }
  },
  computed: {
    effectiveQuestions() {
      if (this.quickQuestions && this.quickQuestions.length > 0) return this.quickQuestions
      if (this.context === 'game') return GAME_QUESTIONS
      if (this.context === 'user') return USER_QUESTIONS
      if (this.context === 'system') return SYSTEM_QUESTIONS
      return GENERAL_QUESTIONS
    },
    quickQuestionsList() { return this.effectiveQuestions },
    visibleQuestions() {
      const list = this.effectiveQuestions
      const out = []
      for (let i = 0; i < Math.min(4, list.length); i++) {
        out.push(list[(this.currentQIndex + i) % list.length])
      }
      return out
    },
    inputPlaceholder() {
      if (this.context === 'game') return '问关于这款游戏的问题（Enter 发送）'
      if (this.context === 'user') return '问关于这个玩家的问题（Enter 发送）'
      if (this.context === 'system') return '问关于系统数据/游戏库的问题（Enter 发送）'
      return '输入你的问题（Enter 发送，Shift+Enter 换行）'
    },
    // 关键修复5: 计算属性返回已渲染的 messages，避免模板内调用函数
    displayedMessages() {
      return this.messages
    },
    // 关键修复6: 合并父组件传入的 style 与默认高度，防止高度塌缩
    // 关键修复I: 浮窗模式必须 height: 100% 撑满 .ai-floating-panel (680px)，
    // 否则消息列表区域只占自然高度，下面露出大片空白和底层内容
    mergedStyle() {
      const userStyle = (this.$attrs && this.$attrs.style) || {}
      let baseStyle
      if (this.mode === 'floating') {
        // 浮窗模式：固定 height: 100%，由父 .ai-floating-panel 提供 680px
        baseStyle = { height: '100%', minHeight: '480px' }
      } else if (this.height) {
        // 嵌入模式：使用 prop height 或默认 560px
        baseStyle = { minHeight: this.height, height: this.height }
      } else {
        baseStyle = { minHeight: '560px' }
      }
      return Object.assign({}, baseStyle, userStyle)
    }
  },
  watch: {
    // 关键修复7: 当父组件异步更新 welcomeMessage 时，同步刷新首条消息
    welcomeMessage: {
      handler(newVal) {
        if (!newVal) return
        // 仅当 messages 仍是初始的「一条欢迎语」时同步
        if (this.messages.length === 1 && this.messages[0].content !== newVal) {
          // 关键修复E: 增加 _welcomeApplied 标志避免父组件异步更新（如 game 数据加载）
          // 导致 welcome 消息在短时间内被多次替换，造成"闪动"
          if (this._welcomeApplied) return
          this._welcomeApplied = true
          this.messages = [this._buildMsg('assistant', newVal)]
        }
      }
    },
    quickQuestionsList() {
      this.currentQIndex = 0
    }
    // 关键修复8: 移除 deep:true 的 messages 监听器（改用 messagesChanged 显式调用）
  },
  mounted() {
    this.checkStatus()
    // 关键修复F: 把 welcomeMessage 同步延迟到 mounted 后第一次 $nextTick，
    // 避免父组件 game 数据异步加载过程中的多次替换（导致内容闪动）
    this.$nextTick(() => {
      // 关键修复G: 仅当父组件传入的 welcomeMessage 与默认 fallback 不同时才应用
      // 默认 fallback 字符串："对话已清空..." 和 "你好！我是 GameRec AI 助手..." 一致即可
      const initContent = this.messages[0] && this.messages[0].content
      // 仅当父组件提供了非空的、且与默认内容不同的 welcomeMessage 时才替换
      if (this.welcomeMessage && this.welcomeMessage !== initContent) {
        this._welcomeApplied = true
        this.messages = [this._buildMsg('assistant', this.welcomeMessage)]
      } else {
        // 关键修复H: 即使不替换，也标记为已应用，避免后续 watcher 再次触发
        this._welcomeApplied = true
      }
    })
    if (this.$refs.messagesRef) {
      this.$nextTick(this.scrollToBottom)
    }
    this.startRotate()
  },
  beforeUnmount() {
    this.stopRotate()
    this.cancelTokenSource = null
    // 关键修复9: 静默取消未完成的 axios 请求
    if (this._activeController) {
      try { this._activeController.abort() } catch (e) { /* noop */ }
      this._activeController = null
    }
  },
  methods: {
    /**
     * 构造一条标准消息对象（带唯一 key 和预渲染 HTML）
     * 关键：把 renderMd 放在数据写入时执行一次，而不是每次重渲染都执行
     */
    _buildMsg(role, content) {
      const msg = {
        _key: this._msgKeySeq++,
        role,
        content
      }
      if (role === 'assistant') {
        msg._html = renderMd(content)
      }
      return msg
    },
    async checkStatus() {
      try {
        const res = await axios.get('/ai/status', { timeout: 8000 })
        if (res && res.data && res.data.data) {
          this.available = !!res.data.data.available
          if (res.data.data.model) this.modelName = res.data.data.model
        }
      } catch (e) {
        this.available = false
      }
    },
    startRotate() {
      if (this.rotateInterval <= 0) return
      this.stopRotate()
      this.rotateTimer = setInterval(() => {
        const len = this.effectiveQuestions.length
        if (len > 0) this.currentQIndex = (this.currentQIndex + 1) % len
      }, this.rotateInterval)
    },
    stopRotate() {
      if (this.rotateTimer) {
        clearInterval(this.rotateTimer)
        this.rotateTimer = null
      }
    },
    pickEndpoint() {
      if (this.context === 'game' && this.contextId) return { url: '/ai/game-context', extra: { gameId: Number(this.contextId) } }
      if (this.context === 'user' && this.contextId) return { url: '/ai/user-context', extra: { userId: Number(this.contextId) } }
      if (this.context === 'system') return { url: '/ai/system-stats', extra: {} }
      return { url: '/ai/chat', extra: {} }
    },
    /**
     * 关键修复10: sendMessage 全链路修复
     * - sending 锁防止重入
     * - AbortController 替代 CancelToken（更现代）
     * - 90s 超时
     * - finally 块兜底重置所有状态
     */
    async sendMessage(text) {
      if (!text || !text.trim()) return
      if (this.sending) return
      this.sending = true
      this.stopRotate()
      this.errorText = ''

      const userText = text.trim()
      this.messages.push(this._buildMsg('user', userText))
      this.input = ''
      this.loading = true
      this.$nextTick(this.scrollToBottom)

      // 关键修复11: 超过硬上限时主动截断（防止 watch deep 性能塌方）
      this._trimIfNeeded()

      // 用 AbortController 替代 axios CancelToken
      if (typeof AbortController !== 'undefined') {
        this._activeController = new AbortController()
      }

      try {
        const history = this.buildSafeHistory()
        const { url, extra } = this.pickEndpoint()
        const payload = {
          message: userText,
          history,
          systemPrompt: this.systemPrompt || undefined,
          ...extra
        }

        const config = {
          timeout: REQUEST_TIMEOUT_MS
        }
        if (this._activeController) {
          config.signal = this._activeController.signal
        }

        const res = await axios.post(url, payload, config)
        const reply = (res && res.data && res.data.data && res.data.data.reply) || '（无回复）'
        this.messages.push(this._buildMsg('assistant', reply))
        this._trimIfNeeded()
      } catch (e) {
        let msg
        if (axios.isCancel(e) || (e && e.name === 'CanceledError') || (e && e.name === 'AbortError')) {
          msg = '已取消本次请求。'
        } else if (e && (e.code === 'ECONNABORTED' || /timeout/i.test(e.message || ''))) {
          msg = 'AI 服务响应超时（90秒），请稍后重试。智谱 BigModel 在某些时段可能较慢。\n\n建议：1) 缩短问题 2) 清空对话后重试 3) 稍等几分钟再试'
        } else if (e && e.code === 'ERR_NETWORK') {
          msg = '网络连接失败，请检查后端服务是否正常运行。'
        } else if (e && !e.response) {
          msg = '无法连接后端服务：' + (e.message || '网络错误')
        } else {
          msg = (e && e.response && e.response.data && e.response.data.message) || (e && e.message) || '未知错误'
        }
        this.errorText = '⚠️ ' + msg
        this.messages.push(this._buildMsg('assistant', '⚠️ ' + msg + '\n\n（提示：点击右上角 🗑 可清空对话重试）'))
      } finally {
        // 关键修复12: finally 块保证所有状态必被重置（即使抛错也不卡死）
        this.loading = false
        this.sending = false
        this._activeController = null
        this.$nextTick(this.scrollToBottom)
        this.startRotate()
      }
    },
    /**
     * 显式截断消息列表：保留第一条欢迎语 + 最近 N-1 条消息
     * 使用 splice 原地修改，保留消息对象的 _key，避免 Vue 重建 DOM 触发动画闪烁
     */
    _trimIfNeeded() {
      if (this.messages.length <= this.MAX_MESSAGES) return
      const removeCount = this.messages.length - this.MAX_MESSAGES
      if (removeCount > 0) {
        this.messages.splice(1, removeCount)
      }
    },
    /**
     * 构建安全的 history：跳过首条欢迎语，按 user/assistant 配对，限制轮数
     */
    buildSafeHistory() {
      const all = this.messages.slice(1)
      const maxCount = this.MAX_HISTORY_ROUNDS * 2
      const sliced = all.slice(-maxCount)
      return sliced
        .filter(m => m && m.content && m.content.trim()
          && !(typeof m.content === 'string' && m.content.startsWith('⚠️'))
          && m.role !== 'system')
        .map(m => ({ role: m.role, content: m.content }))
    },
    /**
     * 关键修复14: 取消正在进行的请求
     * @param {boolean} silent - true 时不追加提示消息
     */
    cancelRequest(silent) {
      if (this._activeController) {
        try { this._activeController.abort() } catch (e) { /* noop */ }
        this._activeController = null
      }
      if (!silent) {
        this.errorText = ''
        this.messages.push(this._buildMsg('assistant', '⏹️ 已取消本次请求。'))
        this._trimIfNeeded()
      }
    },
    sendCurrent() {
      this.sendMessage(this.input)
    },
    onEnter() {
      if (this.composing) return
      this.sendCurrent()
    },
    clearChat() {
      // 静默取消未完成请求
      if (this._activeController) {
        try { this._activeController.abort() } catch (e) { /* noop */ }
        this._activeController = null
      }
      const welcome = this.welcomeMessage || '对话已清空。有什么想了解的？'
      this.messages = [this._buildMsg('assistant', welcome)]
      this.errorText = ''
      this.loading = false
      this.sending = false
      this.startRotate()
    },
    scrollToBottom() {
      const el = this.$refs.messagesRef
      if (el) el.scrollTop = el.scrollHeight
    }
  }
}
</script>

<style scoped>
.ai-assistant-panel {
  display: flex;
  flex-direction: column;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-2xl);
  overflow: hidden;
  /* 关键修复15: 用 min-height 兜底，防止父容器高度塌缩导致组件消失 */
  min-height: 480px;
  font-family: var(--font-family-body);
  font-weight: var(--font-weight-normal);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
  box-shadow: var(--shadow-2xl);
}
.ai-assistant-panel.ai-floating {
  box-shadow: var(--shadow-2xl), 0 0 0 1px var(--color-border-muted);
}
.ai-assistant-panel.ai-embedded {
  height: 100%;
}

/* ===== Header ===== */
.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--color-border-muted);
  background: var(--color-bg-elevated);
  flex-shrink: 0;
  gap: 8px;
}
.ai-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  flex: 1;
  min-width: 0;
}
.ai-icon {
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-brand-a15), var(--color-accent-a10));
  position: relative;
  color: var(--color-brand-400);
}
.ai-icon-svg {
  display: block;
  flex-shrink: 0;
}
.ai-title-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.ai-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
  white-space: nowrap;
  letter-spacing: 0.2px;
}
.ai-name-sub {
  font-size: 10px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
  letter-spacing: 0.3px;
}
.ai-model {
  font-size: 10px;
  color: var(--color-text-secondary);
  font-family: var(--font-family-mono);
  background: var(--color-bg-subtle);
  padding: 2px 7px;
  border-radius: var(--radius-md);
  font-weight: 500;
  border: 1px solid var(--color-border-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.ai-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.ai-icon-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border-muted);
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
}
.ai-icon-btn:hover {
  background: var(--color-bg-overlay);
  color: var(--color-text-primary);
  border-color: var(--color-border-strong);
}

/* ===== Status Tags ===== */
.ai-title >>> .el-tag--success {
  background-color: var(--color-success-bg) !important;
  color: var(--color-success) !important;
  border-color: var(--color-success-border) !important;
  font-weight: 600 !important;
  flex-shrink: 0;
  transition: opacity 0.3s ease;
}
.ai-title >>> .el-tag--info {
  background-color: var(--color-bg-subtle) !important;
  color: var(--color-text-primary) !important;
  border-color: var(--color-border-default) !important;
  font-weight: 600 !important;
  flex-shrink: 0;
  transition: opacity 0.3s ease;
}

/* ===== Messages ===== */
.ai-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  background: var(--color-bg-base);
  scroll-behavior: smooth;
}
.ai-messages::-webkit-scrollbar { width: 6px; }
.ai-messages::-webkit-scrollbar-thumb { background: var(--color-border-default); border-radius: 10px; }

.ai-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: flex-start;
}
.ai-msg-user { flex-direction: row-reverse; }

.ai-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  color: #fff;
  box-shadow: var(--shadow-sm);
}
.ai-msg-assistant .ai-avatar {
  background: linear-gradient(135deg, var(--color-brand-500) 0%, #8b5cf6 50%, #a855f7 100%);
}
.ai-msg-user .ai-avatar {
  background: linear-gradient(135deg, var(--color-warm-500) 0%, var(--color-danger) 100%);
}

.ai-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 14px;
  line-height: 1.6;
  font-size: 13.5px;
  word-break: break-word;
  font-weight: 500;
}
.ai-msg-assistant .ai-bubble {
  background: var(--color-bg-elevated);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border-muted);
  border-top-left-radius: 4px;
  box-shadow: var(--shadow-xs);
}
.ai-msg-user .ai-bubble {
  background: linear-gradient(135deg, var(--color-brand-600) 0%, var(--color-brand-500) 100%);
  color: #ffffff;
  border-top-right-radius: 4px;
  font-weight: 500;
  box-shadow: 0 2px 8px var(--color-brand-a25);
}

.ai-content :deep(p) { margin: 0 0 8px 0; }
.ai-content :deep(p:last-child) { margin-bottom: 0; }
.ai-content :deep(strong) { font-weight: 700; color: inherit; }
.ai-content :deep(code) {
  background: var(--color-bg-subtle);
  color: var(--color-text-primary);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-size: 12.5px;
  font-family: var(--font-family-mono);
}
.ai-content :deep(pre) {
  background: var(--color-bg-inset);
  color: var(--color-text-primary);
  padding: 10px 14px;
  border-radius: var(--radius-lg);
  overflow-x: auto;
  font-size: 12.5px;
  font-family: var(--font-family-mono);
  margin: 6px 0;
}
.ai-content :deep(ul), .ai-content :deep(ol) { margin: 6px 0; padding-left: 22px; }
.ai-content :deep(li) { margin: 3px 0; }

/* ===== Loading ===== */
.ai-loading {
  display: flex;
  gap: 5px;
  padding: 4px 0;
  align-items: center;
}
.ai-loading span {
  width: 7px;
  height: 7px;
  background: var(--color-brand-500);
  border-radius: 50%;
  animation: ai-bounce 1.4s infinite ease-in-out both;
}
.ai-loading span:nth-child(1) { animation-delay: -0.32s; }
.ai-loading span:nth-child(2) { animation-delay: -0.16s; }
.ai-loading em {
  font-style: normal;
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-left: 6px;
}
@keyframes ai-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ===== Error ===== */
.ai-error {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger);
  padding: 10px 14px;
  border-radius: var(--radius-lg);
  font-size: 13px;
  margin: 10px 0;
  font-weight: 500;
}

/* ===== Quick Questions (轮换) ===== */
.ai-quick {
  padding: 10px 16px 12px;
  background: var(--color-bg-elevated);
  border-top: 1px solid var(--color-border-muted);
  flex-shrink: 0;
}
.ai-quick-label {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-bottom: 8px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 6px;
}
.ai-quick-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-brand-500), var(--color-accent-500));
  box-shadow: 0 0 6px var(--color-brand-a50);
}
.ai-quick-counter {
  margin-left: auto;
  font-family: var(--font-family-mono);
  font-size: 10px;
  color: var(--color-text-tertiary);
  background: var(--color-bg-subtle);
  padding: 1px 6px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-muted);
}
.ai-quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.ai-quick-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: inherit;
}
.ai-quick-tag:hover {
  background: var(--color-bg-overlay);
  color: var(--color-text-primary);
  border-color: var(--color-border-strong);
  transform: translateY(-1px);
}
.ai-quick-tag--primary {
  background: linear-gradient(135deg, var(--color-brand-a15), var(--color-accent-a10)) !important;
  color: var(--color-brand-400) !important;
  border-color: var(--color-brand-a25) !important;
  font-weight: 600 !important;
}
.ai-quick-tag--primary:hover {
  background: linear-gradient(135deg, var(--color-brand-500), var(--color-accent-500)) !important;
  color: #fff !important;
  border-color: transparent !important;
  box-shadow: 0 4px 12px var(--color-brand-a35);
}
.ai-quick-icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}

/* ===== Input ===== */
.ai-input {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-muted);
  background: var(--color-bg-elevated);
  align-items: flex-end;
  flex-shrink: 0;
}
.ai-input >>> .el-textarea {
  flex: 1;
}
.ai-input >>> .el-textarea__inner {
  resize: none;
  border-radius: var(--radius-lg);
  font-size: 13.5px;
  font-family: var(--font-family-body);
  background: var(--color-bg-base);
  color: var(--color-text-primary);
  border-color: var(--color-border-default);
  transition: border-color 0.15s, box-shadow 0.15s;
  padding: 9px 12px;
  min-height: 38px !important;
  line-height: 1.5;
}
.ai-input >>> .el-textarea__inner:focus {
  border-color: var(--color-brand-500);
  box-shadow: 0 0 0 3px var(--color-brand-a15);
}
.ai-input >>> .el-textarea__inner::placeholder {
  color: var(--color-text-tertiary);
  font-size: 13px;
}
.ai-input >>> .el-textarea__inner:hover {
  border-color: var(--color-border-strong);
}
.ai-send-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-brand-500) 0%, #8b5cf6 100%);
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 4px 12px var(--color-brand-a25);
  flex-shrink: 0;
}
.ai-send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--color-brand-600) 0%, #7c3aed 100%);
  box-shadow: 0 6px 16px var(--color-brand-a35);
  transform: translateY(-1px);
}
.ai-send-btn:active:not(:disabled) { transform: translateY(0); }
.ai-send-btn:disabled, .ai-send-btn.is-disabled {
  background: var(--color-bg-subtle) !important;
  color: var(--color-text-tertiary) !important;
  box-shadow: none !important;
  cursor: not-allowed;
  opacity: 0.7;
}
.ai-send-btn.is-loading {
  cursor: wait;
}
.ai-send-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: ai-spin 0.8s linear infinite;
}
@keyframes ai-spin { to { transform: rotate(360deg); } }

.ai-cancel-btn {
  width: 28px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border-muted);
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
  border-radius: var(--radius-lg);
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s;
}
.ai-cancel-btn:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border-color: var(--color-danger-border);
}

/* ===== 浅色主题微调 ===== */
:root[data-theme="light"] .ai-bubble {
  font-weight: 500;
  line-height: 1.65;
}
:root[data-theme="light"] .ai-content {
  letter-spacing: 0.01em;
}
:root[data-theme="light"] .ai-content :deep(code) {
  background: var(--color-brand-a08);
  color: var(--color-brand-700);
}
:root[data-theme="light"] .ai-quick-tag--primary {
  background: linear-gradient(135deg, var(--color-brand-500), var(--color-accent-500)) !important;
  color: #fff !important;
  border-color: transparent !important;
}

/* ===== 深色主题增强对比度 ===== */
:root[data-theme="dark"] .ai-send-btn {
  box-shadow: 0 4px 14px var(--color-brand-a50);
}
</style>
