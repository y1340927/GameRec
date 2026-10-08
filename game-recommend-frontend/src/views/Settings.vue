<template>
  <div class="page-settings">
    <page-header title="系统设置" description="AI 接口配置 · 由用户自行管理 API Key" />

    <!-- AI 设置 -->
    <panel title="AI 接口设置">
      <div class="settings-intro">
        <span class="settings-intro-icon" aria-hidden="true">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 1.5l.94 2.06L11 4.5l-2.06.94L8 7.5l-.94-2.06L5 4.5l2.06-.94L8 1.5z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
            <circle cx="12.5" cy="10.5" r="1.8" stroke="currentColor" stroke-width="1.2"/>
            <path d="M13.8 11.8L15.5 13.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
        </span>
        <div class="settings-intro-text">
          <p class="settings-intro-title">API Key 隐私保护</p>
          <p class="settings-intro-desc">
            本项目源码<b>不包含任何真实密钥</b>。请在此页面填写你自己的 AI 服务 API Key，
            配置将加密保存在本地数据库（<code>ai_settings</code> 表），不会出现在代码或配置文件中。
          </p>
        </div>
        <div class="settings-status" :class="configured ? 'is-configured' : 'is-empty'">
          <span class="status-dot"></span>
          <span>{{ configured ? '已配置 API Key' : '未配置 API Key' }}</span>
        </div>
      </div>

      <div class="form-grid">
        <!-- 启用开关 -->
        <div class="form-group form-group--full">
          <div class="form-group-row">
            <div>
              <label class="form-label">启用 AI 助手</label>
              <p class="form-hint">关闭后 AI 接口将返回友好提示，系统其它功能不受影响</p>
            </div>
            <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
          </div>
        </div>

        <!-- API Key -->
        <div class="form-group form-group--full">
          <label for="ai-api-key" class="form-label">API Key</label>
          <div class="api-key-row">
            <input
              id="ai-api-key"
              v-model="form.apiKey"
              type="password"
              class="form-input"
              :placeholder="configured ? apiKeyMasked + '（留空则保持不变）' : '请粘贴你的 AI 服务 API Key'"
              autocomplete="off"
            />
            <button class="btn btn-ghost" @click="toggleKeyVisible" :title="keyVisible ? '隐藏' : '显示'">
              <svg v-if="keyVisible" width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M1.5 8s2.5-4.5 6.5-4.5 6.5 4.5 6.5 4.5-2.5 4.5-6.5 4.5S1.5 8 1.5 8z" stroke="currentColor" stroke-width="1.3"/>
                <path d="M8 10.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z" stroke="currentColor" stroke-width="1.3"/>
              </svg>
              <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M1.5 8s2.5-4.5 6.5-4.5 6.5 4.5 6.5 4.5-2.5 4.5-6.5 4.5S1.5 8 1.5 8z" stroke="currentColor" stroke-width="1.3"/>
                <path d="M4 12L12 4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <p class="form-hint">
            {{ configured ? '已保存密钥：' + apiKeyMasked + '。如需更换请输入新 Key；留空保存将保留原 Key。' : 'Key 只保存在本机数据库，可安全分享源码。' }}
          </p>
        </div>

        <!-- 模型名称 -->
        <div class="form-group">
          <label for="ai-model" class="form-label">模型名称</label>
          <input id="ai-model" v-model="form.model" type="text" class="form-input" placeholder="glm-4-flash-250414" />
          <p class="form-hint">如智谱 GLM-4-Flash-250414（免费）、GLM-4.5、DeepSeek 等</p>
        </div>

        <!-- 接口地址 -->
        <div class="form-group">
          <label for="ai-base-url" class="form-label">接口地址（Base URL）</label>
          <input id="ai-base-url" v-model="form.baseUrl" type="text" class="form-input" placeholder="https://open.bigmodel.cn/api/paas/v4" />
          <p class="form-hint">兼容 OpenAI 格式的 /chat/completions 接口</p>
        </div>

        <!-- Max Tokens -->
        <div class="form-group">
          <label for="ai-max-tokens" class="form-label">最大 Token 数</label>
          <el-input-number id="ai-max-tokens" v-model="form.maxTokens" :min="128" :max="4096" :step="128" controls-position="right" />
        </div>

        <!-- 温度 -->
        <div class="form-group">
          <label for="ai-temperature" class="form-label">温度（Temperature）</label>
          <el-input-number id="ai-temperature" v-model="form.temperature" :min="0" :max="1" :step="0.1" controls-position="right" />
          <p class="form-hint">越低回答越稳定，建议 0.3</p>
        </div>

        <!-- 超时 -->
        <div class="form-group">
          <label for="ai-timeout" class="form-label">超时时间（秒）</label>
          <el-input-number id="ai-timeout" v-model="form.timeoutSeconds" :min="15" :max="300" :step="5" controls-position="right" />
        </div>
      </div>

      <div class="form-actions">
        <button class="btn btn-primary" @click="saveSettings" :disabled="saving">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M2.5 8.5l3.5 3.5 7.5-8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          {{ saving ? '保存中...' : '保存设置' }}
        </button>
        <button class="btn btn-ghost" @click="testConnection" :disabled="testing || !configured">
          <span class="test-spinner" v-if="testing" aria-hidden="true"></span>
          <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M8 2v2.5M8 11.5V14M2.93 2.93l1.77 1.77M11.3 11.3l1.77 1.77M2 8h2.5M11.5 8H14" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
            <circle cx="8" cy="8" r="2.6" stroke="currentColor" stroke-width="1.4"/>
          </svg>
          {{ testing ? '测试中...' : '测试连接' }}
        </button>
        <span v-if="testResult" class="test-result" :class="testOk ? 'test-result--ok' : 'test-result--fail'">
          {{ testResult }}
        </span>
      </div>
    </panel>

    <!-- 安全说明 -->
    <panel title="安全与使用说明">
      <ul class="guide-list">
        <li>1. 本项目源码已移除所有硬编码密钥，可在 GitHub / Gitee / CSDN 等平台安全开源。</li>
        <li>2. API Key 保存于本地数据库 <code>ai_settings</code> 表，仅本机后端可读取，前端接口只返回脱敏值（如 <code>abc****wxyz</code>）。</li>
        <li>3. 推荐使用智谱 BigModel 免费模型：GLM-4-Flash-250414（128K 上下文，速度快）。</li>
        <li>4. 如需更换其它服务商（DeepSeek / OpenAI 兼容接口），修改「接口地址」与「模型名称」即可。</li>
        <li>5. 若密钥泄露，请立即到服务商控制台吊销并重新生成。</li>
      </ul>
    </panel>
  </div>
</template>

<script>
import axios from 'axios';
import PageHeader from '@/components/PageHeader.vue';
import Panel from '@/components/Panel.vue';

export default {
  name: 'Settings',
  components: { PageHeader, Panel },
  data() {
    return {
      loading: true,
      saving: false,
      testing: false,
      configured: false,
      apiKeyMasked: '',
      keyVisible: false,
      testResult: '',
      testOk: false,
      form: {
        enabled: true,
        apiKey: '',
        model: 'glm-4-flash-250414',
        baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
        maxTokens: 1024,
        temperature: 0.3,
        timeoutSeconds: 90
      }
    };
  },
  mounted() {
    this.loadSettings();
  },
  methods: {
    async loadSettings() {
      this.loading = true;
      try {
        const res = await axios.get('/ai/settings', { timeout: 8000 });
        const d = res.data && res.data.data;
        if (d) {
          this.configured = !!d.configured;
          this.apiKeyMasked = d.apiKeyMasked || '';
          this.form.enabled = !!d.enabled;
          this.form.model = d.model || this.form.model;
          this.form.baseUrl = d.baseUrl || this.form.baseUrl;
          this.form.maxTokens = d.maxTokens || 1024;
          this.form.temperature = d.temperature != null ? d.temperature : 0.3;
          this.form.timeoutSeconds = d.timeoutSeconds || 90;
        }
      } catch (e) {
        this.$message.error('无法加载设置，请确认后端服务已启动');
      } finally {
        this.loading = false;
      }
    },
    toggleKeyVisible() {
      this.keyVisible = !this.keyVisible;
      const input = document.getElementById('ai-api-key');
      if (input) input.type = this.keyVisible ? 'text' : 'password';
    },
    async saveSettings() {
      this.saving = true;
      try {
        // 安全约定：输入框为空或与脱敏值相同 → 传空字符串，后端保留原 Key
        const submittedKey = this.form.apiKey.trim();
        const payload = {
          enabled: this.form.enabled ? 1 : 0,
          apiKey: submittedKey === '' || (this.apiKeyMasked && submittedKey === this.apiKeyMasked) ? '' : submittedKey,
          model: this.form.model.trim(),
          baseUrl: this.form.baseUrl.trim(),
          maxTokens: this.form.maxTokens,
          temperature: this.form.temperature,
          timeoutSeconds: this.form.timeoutSeconds
        };
        const res = await axios.post('/ai/settings', payload, { timeout: 15000 });
        const d = res.data && res.data.data;
        if (res.data && res.data.code === 200) {
          this.configured = !!d.configured;
          this.apiKeyMasked = d.apiKeyMasked || '';
          this.form.apiKey = '';
          this.$message.success('AI 设置已保存，立即生效（无需重启服务）');
        } else {
          this.$message.error((res.data && res.data.message) || '保存失败');
        }
      } catch (e) {
        this.$message.error('保存失败，请检查后端服务');
      } finally {
        this.saving = false;
      }
    },
    async testConnection() {
      this.testing = true;
      this.testResult = '';
      try {
        const res = await axios.post('/ai/test', null, { timeout: 120000 });
        const d = res.data && res.data.data;
        if (d) {
          this.testOk = !!d.success;
          this.testResult = d.success
            ? `连接成功（${d.costMs}ms，模型：${d.model}）`
            : (d.message || '连接失败');
        } else {
          this.testOk = false;
          this.testResult = '测试无响应';
        }
      } catch (e) {
        this.testOk = false;
        this.testResult = '测试请求异常：' + (e.message || '网络错误');
      } finally {
        this.testing = false;
      }
    }
  }
};
</script>

<style scoped>
.page-settings { max-width: var(--content-max-width); margin: 0 auto; }

/* ===== 顶部说明条 ===== */
.settings-intro {
  display: flex; align-items: center; gap: var(--space-4);
  padding: var(--space-5); margin: var(--space-5) var(--space-5) 0;
  background: var(--color-bg-subtle); border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-xl);
}
.settings-intro-icon {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; border-radius: var(--radius-lg); flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-brand-a15), var(--color-accent-a10));
  color: var(--color-brand-400);
}
.settings-intro-text { flex: 1; min-width: 0; }
.settings-intro-title { font-size: var(--text-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: var(--space-1); }
.settings-intro-desc { font-size: var(--text-xs); color: var(--color-text-secondary); line-height: 1.7; }
.settings-intro-desc code, .guide-list code {
  background: var(--color-bg-inset); padding: 1px 6px; border-radius: var(--radius-sm);
  font-family: var(--font-family-mono); font-size: 11px; color: var(--color-brand-400);
}
.settings-status {
  display: inline-flex; align-items: center; gap: var(--space-2); flex-shrink: 0;
  padding: var(--space-2) var(--space-3); border-radius: var(--radius-full);
  font-size: var(--text-xs); font-weight: var(--font-weight-medium);
}
.settings-status.is-configured { background: var(--color-success-bg); color: var(--color-success); border: 1px solid var(--color-success-border); }
.settings-status.is-empty { background: var(--color-warm-a12); color: var(--color-warm-500); border: 1px solid var(--color-warm-a25); }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }

/* ===== 表单 ===== */
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-4); padding: var(--space-5); }
.form-group { display: flex; flex-direction: column; gap: var(--space-2); min-width: 0; }
.form-group--full { grid-column: 1 / -1; }
.form-group-row { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.form-label { font-size: var(--text-xs); color: var(--color-text-secondary); font-weight: var(--font-weight-medium); }
.form-hint { font-size: 11px; color: var(--color-text-tertiary); line-height: 1.6; margin: 0; }
.form-input {
  padding: 10px 12px; border: 1px solid var(--color-border-default); border-radius: var(--radius-lg);
  background: var(--color-bg-subtle); color: var(--color-text-primary); font-size: var(--text-sm);
  transition: border-color var(--duration-fast), box-shadow var(--duration-fast);
  width: 100%; box-sizing: border-box;
}
.form-input:focus { outline: none; border-color: var(--color-brand-500); box-shadow: 0 0 0 3px var(--color-brand-a15); }
.form-input::placeholder { color: var(--color-text-tertiary); }

.api-key-row { display: flex; gap: var(--space-2); align-items: stretch; }
.api-key-row .form-input { flex: 1; font-family: var(--font-family-mono); letter-spacing: 0.5px; }

.form-actions {
  display: flex; align-items: center; gap: var(--space-3); flex-wrap: wrap;
  padding: 0 var(--space-5) var(--space-5);
}

.btn { display: inline-flex; align-items: center; gap: var(--space-2); padding: 10px 16px; border: none; border-radius: var(--radius-lg); font-size: var(--text-sm); font-weight: var(--font-weight-medium); cursor: pointer; transition: all var(--duration-fast); white-space: nowrap; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: var(--color-brand-600); color: #fff; }
.btn-primary:hover:not(:disabled) { background: var(--color-brand-500); }
.btn-ghost { background: var(--color-bg-subtle); color: var(--color-text-secondary); border: 1px solid var(--color-border-default); }
.btn-ghost:hover:not(:disabled) { color: var(--color-text-primary); border-color: var(--color-border-strong); }

.test-spinner { width: 12px; height: 12px; border: 2px solid var(--color-border-default); border-top-color: var(--color-brand-500); border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }

.test-result { font-size: var(--text-xs); font-weight: var(--font-weight-medium); }
.test-result--ok { color: var(--color-success); }
.test-result--fail { color: var(--color-danger); }

/* ===== 安全说明 ===== */
.guide-list { list-style: none; margin: 0; padding: var(--space-5); display: flex; flex-direction: column; gap: var(--space-3); }
.guide-list li { font-size: var(--text-xs); color: var(--color-text-secondary); line-height: 1.8; }

@media (max-width: 768px) {
  .form-grid { grid-template-columns: 1fr; }
  .settings-intro { flex-wrap: wrap; }
  .settings-status { width: 100%; justify-content: center; }
  .form-actions .btn { flex: 1; justify-content: center; }
  .test-result { width: 100%; }
}
</style>
