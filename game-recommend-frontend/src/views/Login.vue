<template>
  <div class="login-shell" @keydown.enter="handleLogin">
    <!-- Canvas 粒子背景 -->
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>

    <!-- 背景装饰光晕 -->
    <div class="bg-aurora a1"></div>
    <div class="bg-aurora a2"></div>
    <div class="bg-aurora a3"></div>

    <!-- 主登录卡片 -->
    <div class="login-wrapper">
      <transition name="card-enter" appear>
        <div class="login-card" :class="{ 'login-card--loading': loading, 'login-card--shake': shakeCard }">
          <!-- 装饰顶部光条 -->
          <div class="card-glow-bar"></div>

          <!-- 品牌标识区域 -->
          <div class="card-brand">
            <div class="brand-logo">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none" aria-hidden="true">
                <defs>
                  <linearGradient id="loginGrad" x1="0" y1="0" x2="48" y2="48">
                    <stop offset="0%" stop-color="var(--color-brand-400)"/>
                    <stop offset="100%" stop-color="var(--color-accent-500)"/>
                  </linearGradient>
                </defs>
                <rect width="48" height="48" rx="14" fill="url(#loginGrad)" opacity="0.15"/>
                <rect width="48" height="48" rx="14" stroke="url(#loginGrad)" stroke-width="1" opacity="0.3"/>
                <!-- 游戏手柄抽象图标 — 菱形/钻石形状 -->
                <path d="M24 8l14 8-14 8-14-8 14-8z" fill="url(#loginGrad)" opacity="0.85"/>
                <path d="M10 28l14 8 14-8" stroke="url(#loginGrad)" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
                <path d="M10 20l14 8 14-8" stroke="url(#loginGrad)" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
              </svg>
            </div>
            <h1 class="brand-heading">GameRec</h1>
            <p class="brand-tagline">基于玩家画像的游戏推荐系统</p>
          </div>

          <!-- 登录表单 -->
          <div class="card-form">
            <!-- 用户名 -->
            <div class="input-group" :class="{ 'input-group--focus': focusField === 'username', 'input-group--error': errors.username }">
              <label class="input-label" for="login-username">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="input-icon">
                  <circle cx="8" cy="5" r="3" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M3 14c0-2.761 2.239-5 5-5s5 2.239 5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span>用户名</span>
              </label>
              <input
                id="login-username"
                ref="usernameInput"
                v-model="form.username"
                type="text"
                class="input-field"
                :class="{ 'input-field--has-value': form.username }"
                placeholder="请输入用户名"
                autocomplete="username"
                @focus="focusField = 'username'"
                @blur="focusField = ''"
                @input="clearError('username')"
              />
              <span class="input-border"></span>
              <transition name="field-error">
                <span v-if="errors.username" class="input-error-text">{{ errors.username }}</span>
              </transition>
            </div>

            <!-- 密码 -->
            <div class="input-group" :class="{ 'input-group--focus': focusField === 'password', 'input-group--error': errors.password }">
              <label class="input-label" for="login-password">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="input-icon">
                  <rect x="2.5" y="7" width="11" height="7" rx="1.5" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M5 7V4.5a3 3 0 116 0V7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                  <circle cx="8" cy="10.5" r="0.8" fill="currentColor"/>
                </svg>
                <span>密码</span>
              </label>
              <input
                id="login-password"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                class="input-field"
                :class="{ 'input-field--has-value': form.password }"
                placeholder="请输入密码"
                autocomplete="current-password"
                @focus="focusField = 'password'"
                @blur="focusField = ''"
                @input="clearError('password')"
              />
              <button
                type="button"
                class="password-toggle"
                @click="showPassword = !showPassword"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                tabindex="-1"
              >
                <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M2 8s2.5-4.5 6-4.5S14 8 14 8s-2.5 4.5-6 4.5S2 8 2 8z" stroke="currentColor" stroke-width="1.5"/>
                  <circle cx="8" cy="8" r="2" stroke="currentColor" stroke-width="1.5"/>
                </svg>
                <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M6.5 3.5a5.9 5.9 0 011.5-.2c3.5 0 6 4.5 6 4.5a8.4 8.4 0 01-1.2 1.8M4.4 4.4A6.2 6.2 0 002 8s2.5 4.5 6 4.5a5.9 5.9 0 003.2-1M9.5 9.5a2 2 0 01-3-3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                  <path d="M2 2l12 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
              </button>
              <span class="input-border"></span>
              <transition name="field-error">
                <span v-if="errors.password" class="input-error-text">{{ errors.password }}</span>
              </transition>
            </div>

            <!-- 记住密码 & 忘记密码 -->
            <div class="form-options">
              <label class="remember-label">
                <input type="checkbox" v-model="form.remember" class="remember-checkbox" />
                <span class="remember-box">
                  <svg v-if="form.remember" width="10" height="7" viewBox="0 0 10 7" fill="none" class="remember-check">
                    <path d="M1 3.5l2.5 2.5L9 1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </span>
                <span class="remember-text">记住登录</span>
              </label>
              <button type="button" class="forgot-link" @click="onForgotPassword">忘记密码?</button>
            </div>

            <!-- 登录按钮 -->
            <button
              class="login-btn"
              :class="{ 'login-btn--loading': loading }"
              :disabled="loading"
              @click="handleLogin"
            >
              <span class="login-btn-text" :class="{ 'login-btn-text--hidden': loading }">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="login-btn-icon">
                  <path d="M2.5 8.5h8l-2.5 2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M2.5 2.5v-1a1 1 0 011-1h9a1 1 0 011 1v13a1 1 0 01-1 1h-9a1 1 0 01-1-1v-1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                登 录
              </span>
              <span class="login-btn-spinner" v-if="loading">
                <span class="spinner-dot"></span>
                <span class="spinner-dot"></span>
                <span class="spinner-dot"></span>
              </span>
            </button>

            <!-- 系统版本信息 -->
            <div class="card-footer-info">
              <span class="footer-version">GameRec v2.0</span>
              <span class="footer-dot"></span>
              <span class="footer-status">
                <span class="status-indicator"></span>
                系统就绪
              </span>
            </div>
          </div>
        </div>
      </transition>

      <!-- 底部版权/提示 -->
      <div class="login-footer">
        <p class="footer-text"> 2026 GameRec — 基于玩家画像的游戏推荐系统</p>
      </div>
    </div>

    <!-- 主题切换提示 -->
    <button class="login-theme-toggle" @click="toggleLoginTheme" :title="'切换到' + (isDark ? '浅色' : '深色') + '主题'" aria-label="切换主题">
      <svg v-if="isDark" width="18" height="18" viewBox="0 0 18 18" fill="none">
        <circle cx="9" cy="9" r="3.5" stroke="currentColor" stroke-width="1.5"/>
        <path d="M9 1.5v1.5M9 15v1.5M2.5 9H4M14 9h1.5M3.88 3.88l1.06 1.06M13.06 13.06l1.06 1.06M3.88 14.12l1.06-1.06M13.06 4.94l1.06-1.06" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
      <svg v-else width="18" height="18" viewBox="0 0 18 18" fill="none">
        <path d="M13.7 11.7A7.5 7.5 0 015.3 3.3 7.5 7.5 0 1013.7 11.7z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
      </svg>
    </button>
  </div>
</template>

<script>
export default {
  name: 'LoginView',
  data() {
    return {
      form: {
        username: '',
        password: '',
        remember: false
      },
      showPassword: false,
      loading: false,
      focusField: '',
      errors: {
        username: '',
        password: ''
      },
      shakeCard: false,
      isDark: true,
      // 粒子系统
      particles: [],
      particleCount: 80,
      animationId: null,
      canvasCtx: null,
      canvasWidth: 0,
      canvasHeight: 0,
      mouseX: -1000,
      mouseY: -1000,
      // 默认演示账号
      demoAccounts: [
        { username: 'admin', password: 'admin123' },
        { username: 'demo', password: 'demo2024' }
      ]
    };
  },
  mounted() {
    this.initTheme();
    this.initParticles();
    this.loadRemembered();
    window.addEventListener('resize', this.handleResize);
    this.$nextTick(() => {
      if (this.form.remember && this.form.username) {
        this.$refs.usernameInput && this.$refs.usernameInput.focus();
      }
    });
  },
  beforeDestroy() {
    this.stopParticles();
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    // ==================== 主题 ====================
    initTheme() {
      const saved = localStorage.getItem('gamerec-theme');
      this.isDark = !saved || saved === 'dark';
    },
    toggleLoginTheme() {
      this.isDark = !this.isDark;
      const theme = this.isDark ? 'dark' : 'light';
      document.documentElement.setAttribute('data-theme', theme);
      localStorage.setItem('gamerec-theme', theme);
    },

    // ==================== 粒子系统 ====================
    initParticles() {
      const canvas = this.$refs.particleCanvas;
      if (!canvas) return;
      this.canvasCtx = canvas.getContext('2d');
      this.resizeCanvas();
      this.createParticles();
      this.animateParticles();

      // 鼠标交互
      canvas.addEventListener('mousemove', (e) => {
        this.mouseX = e.clientX;
        this.mouseY = e.clientY;
      });
      canvas.addEventListener('mouseleave', () => {
        this.mouseX = -1000;
        this.mouseY = -1000;
      });
    },
    resizeCanvas() {
      const canvas = this.$refs.particleCanvas;
      if (!canvas) return;
      this.canvasWidth = canvas.width = window.innerWidth;
      this.canvasHeight = canvas.height = window.innerHeight;
    },
    handleResize() {
      this.resizeCanvas();
    },
    createParticles() {
      this.particles = [];
      const count = Math.floor((this.canvasWidth * this.canvasHeight) / 15000);
      const actualCount = Math.min(Math.max(count, 40), 120);
      for (let i = 0; i < actualCount; i++) {
        this.particles.push({
          x: Math.random() * this.canvasWidth,
          y: Math.random() * this.canvasHeight,
          vx: (Math.random() - 0.5) * 0.5,
          vy: (Math.random() - 0.5) * 0.5,
          radius: Math.random() * 2 + 0.8,
          opacity: Math.random() * 0.6 + 0.2
        });
      }
    },
    animateParticles() {
      if (!this.canvasCtx) return;
      const ctx = this.canvasCtx;
      const w = this.canvasWidth;
      const h = this.canvasHeight;

      ctx.clearRect(0, 0, w, h);

      const isDarkTheme = this.isDark;
      const particleColor = isDarkTheme ? '129, 140, 248' : '99, 102, 241';
      const particleLineColor = isDarkTheme
        ? { r: 129, g: 140, b: 248 }
        : { r: 99, g: 102, b: 241 };

      // 更新 & 绘制粒子
      for (let i = 0; i < this.particles.length; i++) {
        const p = this.particles[i];
        p.x += p.vx;
        p.y += p.vy;

        // 边界回弹
        if (p.x < 0 || p.x > w) p.vx *= -1;
        if (p.y < 0 || p.y > h) p.vy *= -1;

        // 边界钳位
        p.x = Math.max(0, Math.min(w, p.x));
        p.y = Math.max(0, Math.min(h, p.y));

        // 绘制粒子
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${particleColor}, ${p.opacity})`;
        ctx.fill();

        // 连接线
        const maxDist = 120;
        const mouseMaxDist = 200;

        for (let j = i + 1; j < this.particles.length; j++) {
          const p2 = this.particles[j];
          const dx = p.x - p2.x;
          const dy = p.y - p2.y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < maxDist) {
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p2.x, p2.y);
            const alpha = (1 - dist / maxDist) * 0.15;
            ctx.strokeStyle = `rgba(${particleLineColor.r}, ${particleLineColor.g}, ${particleLineColor.b}, ${alpha})`;
            ctx.lineWidth = 0.6;
            ctx.stroke();
          }
        }

        // 鼠标互动 - 靠近鼠标的粒子会更亮，远离鼠标的被推斥
        const mdx = p.x - this.mouseX;
        const mdy = p.y - this.mouseY;
        const mdist = Math.sqrt(mdx * mdx + mdy * mdy);

        if (mdist < mouseMaxDist) {
          // 连线到鼠标
          ctx.beginPath();
          ctx.moveTo(p.x, p.y);
          ctx.lineTo(this.mouseX, this.mouseY);
          const alpha = (1 - mdist / mouseMaxDist) * 0.25;
          ctx.strokeStyle = `rgba(${particleLineColor.r}, ${particleLineColor.g}, ${particleLineColor.b}, ${alpha})`;
          ctx.lineWidth = 1;
          ctx.stroke();

          // 轻微的排斥力
          if (mdist > 0) {
            const force = (1 - mdist / mouseMaxDist) * 0.3;
            p.vx += (mdx / mdist) * force * 0.03;
            p.vy += (mdy / mdist) * force * 0.03;
          }
        }

        // 速度阻尼
        p.vx *= 0.999;
        p.vy *= 0.999;
      }

      this.animationId = requestAnimationFrame(this.animateParticles);
    },
    stopParticles() {
      if (this.animationId) {
        cancelAnimationFrame(this.animationId);
        this.animationId = null;
      }
    },

    // ==================== 表单逻辑 ====================
    loadRemembered() {
      try {
        const saved = localStorage.getItem('gamerec-login-remember');
        if (saved) {
          const { username, password } = JSON.parse(saved);
          this.form.username = username || '';
          this.form.password = password || '';
          this.form.remember = true;
        }
      } catch (e) {
        // 忽略解析错误
      }
    },
    clearError(field) {
      if (this.errors[field]) {
        this.errors[field] = '';
      }
    },
    validate() {
      let valid = true;
      this.errors = { username: '', password: '' };

      if (!this.form.username.trim()) {
        this.errors.username = '请输入用户名';
        valid = false;
      } else if (this.form.username.trim().length < 2) {
        this.errors.username = '用户名至少需要 2 个字符';
        valid = false;
      }

      if (!this.form.password) {
        this.errors.password = '请输入密码';
        valid = false;
      } else if (this.form.password.length < 4) {
        this.errors.password = '密码至少需要 4 个字符';
        valid = false;
      }

      return valid;
    },
    triggerShake() {
      this.shakeCard = true;
      setTimeout(() => {
        this.shakeCard = false;
      }, 600);
    },
    async handleLogin() {
      if (this.loading) return;

      if (!this.validate()) {
        this.triggerShake();
        return;
      }

      this.loading = true;

      // 模拟登录验证（实际项目中替换为真实 API 调用）
      try {
        await this.simulateLogin();
      } catch (e) {
        this.loading = false;
        return;
      }

      // 模拟短暂延迟展示加载动画
      await new Promise(resolve => setTimeout(resolve, 800));

      this.loading = false;

      // 登录成功 — 记住密码
      if (this.form.remember) {
        localStorage.setItem('gamerec-login-remember', JSON.stringify({
          username: this.form.username,
          password: this.form.password
        }));
      } else {
        localStorage.removeItem('gamerec-login-remember');
      }

      // 设置登录状态
      const userInfo = {
        username: this.form.username,
        loginTime: new Date().toISOString()
      };
      localStorage.setItem('gamerec-auth', JSON.stringify(userInfo));
      sessionStorage.setItem('gamerec-session', JSON.stringify(userInfo));

      // 跳转到首页或重定向页面
      const redirect = this.$route.query.redirect || '/';
      this.$router.replace(redirect);
    },
    simulateLogin() {
      return new Promise((resolve, reject) => {
        // 检查演示账号
        const matched = this.demoAccounts.find(
          acc => acc.username === this.form.username.trim()
        );
        if (matched && matched.password !== this.form.password) {
          this.errors.password = '密码错误，请重试';
          this.triggerShake();
          reject(new Error('密码错误'));
          return;
        }
        if (matched) {
          resolve();
          return;
        }
        // 非演示账号也允许登录（模拟后端验证）
        if (this.form.password.length >= 4) {
          resolve();
        } else {
          this.errors.password = '密码错误，请重试';
          this.triggerShake();
          reject(new Error('密码错误'));
        }
      });
    },
    onForgotPassword() {
      this.$message && this.$message.info('演示环境：可使用 admin/admin123 或 demo/demo2024 登录');
    }
  }
};
</script>

<style scoped>
/* ============================================================
   Login Page — Aurora Design System v4 登录页
   深色/浅色双主题适配 · Canvas粒子星座背景 · 玻璃拟态卡片
   ============================================================ */

/* ===== Shell ===== */
.login-shell {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-base);
  overflow: hidden;
  font-family: var(--font-family-body);
}

/* ===== Canvas 粒子背景 ===== */
.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* ===== 背景装饰光晕 ===== */
.bg-aurora {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.12;
  pointer-events: none;
  z-index: 0;
}
.bg-aurora.a1 {
  width: 600px;
  height: 600px;
  background: var(--color-brand-400);
  top: -200px;
  left: -100px;
  animation: aurora-float-1 18s ease-in-out infinite;
}
.bg-aurora.a2 {
  width: 500px;
  height: 500px;
  background: var(--color-accent-500);
  bottom: -150px;
  right: -100px;
  animation: aurora-float-2 22s ease-in-out infinite;
}
.bg-aurora.a3 {
  width: 350px;
  height: 350px;
  background: #8b5cf6;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: aurora-float-3 15s ease-in-out infinite;
  opacity: 0.06;
}

@keyframes aurora-float-1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(60px, 40px) scale(1.08); }
  66% { transform: translate(-30px, -20px) scale(0.94); }
}
@keyframes aurora-float-2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-50px, -30px) scale(1.12); }
  66% { transform: translate(40px, 20px) scale(0.9); }
}
@keyframes aurora-float-3 {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.2); opacity: 0.08; }
}

/* ===== 登录包装器 ===== */
.login-wrapper {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-8);
  width: 100%;
  max-width: 440px;
  padding: var(--space-4);
}

/* ===== 卡片容器 ===== */
.login-card {
  position: relative;
  width: 100%;
  padding: var(--space-10) var(--space-8);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-3xl);
  box-shadow: var(--shadow-2xl), var(--shadow-glow-brand);
  backdrop-filter: blur(var(--blur-xl));
  -webkit-backdrop-filter: blur(var(--blur-xl));
  overflow: hidden;
  transition: border-color var(--duration-normal), box-shadow var(--duration-normal);
}

/* 顶部光条 */
.card-glow-bar {
  position: absolute;
  top: 0;
  left: 20%;
  right: 20%;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent,
    var(--color-brand-400),
    var(--color-accent-500),
    var(--color-brand-400),
    transparent
  );
  border-radius: var(--radius-full);
  opacity: 0.7;
  animation: glow-bar 3s ease-in-out infinite;
}

@keyframes glow-bar {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 0.9; }
}

/* ===== 品牌标识 ===== */
.card-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  animation: logo-pulse 4s ease-in-out infinite;
}

@keyframes logo-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.04); }
}

.brand-heading {
  font-family: var(--font-family-display);
  font-size: var(--text-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: var(--letter-spacing-tight);
  margin: 0;
}

.brand-tagline {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  letter-spacing: 0.04em;
  margin: 0;
}

/* ===== 表单 ===== */
.card-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

/* ===== 输入组 ===== */
.input-group {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.input-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  transition: color var(--duration-fast);
  user-select: none;
}

.input-group--focus .input-label {
  color: var(--color-brand-400);
}

.input-group--error .input-label {
  color: var(--color-danger);
}

.input-icon {
  flex-shrink: 0;
  opacity: 0.7;
}

.input-field {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-inset);
  border: 1.5px solid var(--color-border-muted);
  border-radius: var(--radius-xl);
  color: var(--color-text-primary);
  font-family: var(--font-family-body);
  font-size: var(--text-sm);
  line-height: var(--line-height-normal);
  outline: none;
  transition: all var(--duration-normal) var(--ease-out-expo);
  box-sizing: border-box;
}

.input-field::placeholder {
  color: var(--color-text-tertiary);
  opacity: 0.6;
}

.input-field:focus {
  border-color: var(--color-brand-400);
  background: var(--color-bg-subtle);
  box-shadow: 0 0 0 3px var(--color-brand-a12);
}

.input-group--error .input-field {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px var(--color-danger-bg);
}

.input-group--error .input-field:focus {
  box-shadow: 0 0 0 3px var(--color-danger-bg);
}

/* 密码可见切换按钮 */
.password-toggle {
  position: absolute;
  right: var(--space-3);
  bottom: var(--space-3);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all var(--duration-fast);
  z-index: 2;
}

.password-toggle:hover {
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
}

/* 错误提示 */
.input-error-text {
  font-size: var(--text-2xs);
  color: var(--color-danger);
  padding-left: var(--space-1);
  line-height: 1.2;
}

.field-error-enter-active,
.field-error-leave-active {
  transition: all var(--duration-fast) var(--ease-out-expo);
}
.field-error-enter,
.field-error-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ===== 表单选项行 ===== */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-1) 0;
}

.remember-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  user-select: none;
}

.remember-checkbox {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

.remember-box {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  background: var(--color-bg-inset);
  transition: all var(--duration-fast);
}

.remember-checkbox:checked + .remember-box {
  background: var(--color-brand-500);
  border-color: var(--color-brand-500);
}

.remember-check {
  color: #fff;
}

.remember-checkbox:focus-visible + .remember-box {
  box-shadow: 0 0 0 2px var(--color-brand-a25);
}

.remember-text {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.forgot-link {
  font-size: var(--text-xs);
  color: var(--color-text-link);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  transition: all var(--duration-fast);
}

.forgot-link:hover {
  color: var(--color-brand-300);
  background: var(--color-brand-a06);
}

/* ===== 登录按钮 ===== */
.login-btn {
  position: relative;
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, var(--color-brand-500), #8b5cf6);
  color: #fff;
  font-family: var(--font-family-body);
  font-size: var(--text-base);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.06em;
  cursor: pointer;
  overflow: hidden;
  transition: all var(--duration-normal) var(--ease-out-expo);
  margin-top: var(--space-3);
}

.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #8b5cf6, var(--color-accent-500));
  opacity: 0;
  transition: opacity var(--duration-normal);
}

.login-btn:hover::before {
  opacity: 1;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4), 0 4px 8px rgba(0, 0, 0, 0.2);
}

.login-btn:active {
  transform: translateY(0) scale(0.98);
}

.login-btn:disabled {
  cursor: not-allowed;
}

.login-btn-text {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  transition: opacity var(--duration-fast), transform var(--duration-fast);
}

.login-btn-text--hidden {
  opacity: 0;
  transform: translateY(8px);
}

.login-btn-icon {
  flex-shrink: 0;
}

/* 加载动画 — 三点弹跳 */
.login-btn-spinner {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 5px;
  z-index: 1;
}

.spinner-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  animation: dot-bounce 1.4s ease-in-out infinite both;
}

.spinner-dot:nth-child(1) { animation-delay: -0.32s; }
.spinner-dot:nth-child(2) { animation-delay: -0.16s; }
.spinner-dot:nth-child(3) { animation-delay: 0s; }

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* 卡片抖动动画 */
.login-card--shake {
  animation: card-shake 0.5s var(--ease-in-out);
}

@keyframes card-shake {
  0%, 100% { transform: translateX(0); }
  10%, 50%, 90% { transform: translateX(-6px); }
  30%, 70% { transform: translateX(6px); }
}

/* ===== 卡片底部信息 ===== */
.card-footer-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border-muted);
}

.footer-version {
  font-family: var(--font-family-mono);
  font-size: var(--text-2xs);
  color: var(--color-text-tertiary);
}

.footer-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--color-text-tertiary);
  opacity: 0.4;
}

.footer-status {
  display: flex;
  align-items: center;
  gap: var(--space-1_5);
  font-size: var(--text-2xs);
  color: var(--color-text-tertiary);
}

.status-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 6px var(--color-success);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 4px var(--color-success); }
  50% { box-shadow: 0 0 10px var(--color-success); }
}

/* ===== 底部版权 ===== */
.login-footer {
  text-align: center;
}

.footer-text {
  font-size: var(--text-2xs);
  color: var(--color-text-tertiary);
  opacity: 0.6;
  margin: 0;
}

/* ===== 主题切换按钮 ===== */
.login-theme-toggle {
  position: fixed;
  top: var(--space-6);
  right: var(--space-6);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-full);
  background: var(--color-bg-elevated);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out-expo);
  z-index: 20;
  backdrop-filter: blur(var(--blur-md));
  -webkit-backdrop-filter: blur(var(--blur-md));
}

.login-theme-toggle:hover {
  color: var(--color-text-primary);
  border-color: var(--color-brand-400);
  box-shadow: var(--shadow-glow-brand);
  transform: rotate(15deg);
}

/* ===== 卡片进入动画 ===== */
.card-enter-enter-active {
  animation: card-appear var(--duration-slow) var(--ease-out-expo);
}

@keyframes card-appear {
  0% {
    opacity: 0;
    transform: translateY(24px) scale(0.96);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ===== 浅色主题覆盖 ===== */
[data-theme="light"] .login-card {
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.12), 0 0 40px rgba(99, 102, 241, 0.06);
}

[data-theme="light"] .input-field {
  background: var(--color-bg-base);
  border-color: #e2e6ed;
}

[data-theme="light"] .input-field:focus {
  background: #fff;
}

[data-theme="light"] .bg-aurora {
  opacity: 0.08;
}

[data-theme="light"] .bg-aurora.a3 {
  opacity: 0.04;
}

[data-theme="light"] .remember-box {
  background: #fff;
  border-color: #cbd5e1;
}

[data-theme="light"] .login-theme-toggle {
  background: rgba(255, 255, 255, 0.82);
}

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .login-wrapper {
    max-width: 100%;
    padding: var(--space-3);
  }

  .login-card {
    padding: var(--space-6) var(--space-5);
    border-radius: var(--radius-2xl);
  }

  .brand-heading {
    font-size: var(--text-xl);
  }

  .login-btn {
    height: 44px;
    font-size: var(--text-sm);
  }
}

@media (max-width: 360px) {
  .login-card {
    padding: var(--space-5) var(--space-4);
  }

  .form-options {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }
}

/* ===== 减少动画偏好 ===== */
@media (prefers-reduced-motion: reduce) {
  .bg-aurora,
  .card-glow-bar,
  .brand-logo {
    animation: none;
  }

  .card-enter-enter-active {
    animation: none;
  }
}
</style>
