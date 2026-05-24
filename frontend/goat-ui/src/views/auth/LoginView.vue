<template>
  <div class="login-shell">
    <div class="tech-grid"></div>
    <div class="signal-line signal-line--one"></div>
    <div class="signal-line signal-line--two"></div>

    <main class="login-stage">
      <section class="brand-panel">
        <div class="brand-mark">
          <span>TC</span>
          <i></i>
        </div>
        <p class="eyebrow">Goat Cloud AI Platform</p>
        <h1>统一权限与 AI 中台</h1>
        <p class="brand-desc">
          聚合用户、角色、组织、菜单、数据权限与 AI 助手、知识库、问数和智能体编排能力，
          构建企业级智能应用底座。
        </p>

        <div class="platform-strip">
          <span>RBAC 权限</span>
          <span>数据权限</span>
          <span>AI 原生</span>
        </div>
      </section>

      <section class="login-card">
        <div class="login-card__header">
          <span>欢迎回来</span>
          <h2>登录控制台</h2>
          <p>请输入账号密码进入 Goat Cloud</p>
        </div>

        <el-form :model="form" class="login-form" @submit.prevent="handleLogin">
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="用户名">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" size="large" placeholder="密码" show-password>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="remember">记住登录状态</el-checkbox>
            <span>安全会话由 Redis 托管</span>
          </div>

          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
            进入系统
          </el-button>

          <div class="login-tip">默认账号：admin / Admin@123456</div>
        </el-form>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const remember = ref(true)

const form = reactive({
  username: 'admin',
  password: 'Admin@123456',
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: 32px;
  background:
    radial-gradient(circle at 18% 16%, rgba(36, 190, 255, 0.25), transparent 28%),
    radial-gradient(circle at 82% 18%, rgba(63, 214, 160, 0.18), transparent 26%),
    linear-gradient(135deg, #07111f 0%, #0b2037 48%, #102a3c 100%);
}

.tech-grid {
  position: absolute;
  inset: 0;
  opacity: 0.36;
  background-image:
    linear-gradient(rgba(97, 202, 255, 0.16) 1px, transparent 1px),
    linear-gradient(90deg, rgba(97, 202, 255, 0.16) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(to bottom, transparent, #000 15%, #000 78%, transparent);
}

.signal-line {
  position: absolute;
  height: 1px;
  width: 46vw;
  background: linear-gradient(90deg, transparent, rgba(63, 214, 160, 0.82), transparent);
  filter: drop-shadow(0 0 12px rgba(63, 214, 160, 0.65));
  animation: scan 5.6s ease-in-out infinite;
}

.signal-line--one {
  top: 18%;
  left: 4%;
}

.signal-line--two {
  right: 4%;
  bottom: 22%;
  animation-delay: 1.8s;
}

.login-stage {
  position: relative;
  z-index: 1;
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) 420px;
  gap: 28px;
  align-items: stretch;
}

.brand-panel,
.login-card {
  border: 1px solid rgba(169, 222, 255, 0.22);
  background: rgba(8, 24, 42, 0.72);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(18px);
}

.brand-panel {
  min-height: 560px;
  padding: 54px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-mark {
  width: 68px;
  height: 68px;
  position: relative;
  display: grid;
  place-items: center;
  margin-bottom: 28px;
  color: #dff8ff;
  font-weight: 800;
  letter-spacing: 1px;
  border: 1px solid rgba(91, 205, 255, 0.56);
  background: linear-gradient(145deg, rgba(28, 135, 207, 0.22), rgba(62, 226, 170, 0.14));
}

.brand-mark i {
  position: absolute;
  inset: -7px;
  border: 1px solid rgba(63, 214, 160, 0.32);
  transform: rotate(8deg);
}

.eyebrow {
  margin: 0 0 14px;
  color: #55d6ff;
  font-size: 13px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.brand-panel h1 {
  margin: 0;
  color: #f5fbff;
  font-size: 44px;
  line-height: 1.16;
  font-weight: 700;
}

.brand-desc {
  max-width: 620px;
  margin: 22px 0 36px;
  color: rgba(233, 247, 255, 0.78);
  font-size: 16px;
  line-height: 1.9;
}

.platform-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.platform-strip span {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  color: rgba(240, 250, 255, 0.88);
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(181, 225, 255, 0.14);
  font-size: 13px;
}

.login-card {
  padding: 42px 38px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: rgba(247, 251, 255, 0.94);
}

.login-card__header {
  margin-bottom: 26px;
}

.login-card__header span {
  color: #1489c9;
  font-size: 13px;
  font-weight: 600;
}

.login-card__header h2 {
  margin: 8px 0 8px;
  color: #102033;
  font-size: 30px;
  line-height: 1.2;
}

.login-card__header p {
  margin: 0;
  color: #6d7c8f;
  font-size: 14px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dce8f2 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1b9fe4 inset, 0 0 0 3px rgba(27, 159, 228, 0.12);
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 2px 0 18px;
  color: #75869a;
  font-size: 13px;
}

.login-button {
  width: 100%;
  min-height: 46px;
  border: 0;
  border-radius: 4px;
  font-weight: 600;
  background: linear-gradient(90deg, #0989d8, #13b7c9 55%, #32c48d);
  box-shadow: 0 12px 26px rgba(9, 137, 216, 0.24);
}

.login-tip {
  margin-top: 18px;
  padding: 10px 12px;
  color: #64748b;
  text-align: center;
  font-size: 13px;
  background: #edf6fb;
  border: 1px solid #d7e9f5;
}

@keyframes scan {
  0%,
  100% {
    transform: translateX(-18px);
    opacity: 0.35;
  }

  50% {
    transform: translateX(18px);
    opacity: 1;
  }
}

@media (max-width: 980px) {
  .login-stage {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: auto;
    padding: 38px;
  }
}

@media (max-width: 640px) {
  .login-shell {
    padding: 16px;
  }

  .brand-panel {
    padding: 28px;
  }

  .brand-panel h1 {
    font-size: 32px;
  }

  .login-card {
    padding: 30px 24px;
  }

  .login-options {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
