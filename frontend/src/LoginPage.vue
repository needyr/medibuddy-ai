<script setup lang="ts">
import { computed, ref } from "vue";

interface LoginResult {
  code?: number;
  message?: string;
  data?: {
    userId?: number;
    username?: string;
    token?: string;
    expiresAt?: number;
  };
}

const username = ref("");
const password = ref("");
const loading = ref(false);
const errorText = ref("");

const canSubmit = computed(() => username.value.trim().length > 0 && password.value.length > 0 && !loading.value);

function resetError(): void {
  errorText.value = "";
}

async function submitLogin(): Promise<void> {
  if (!canSubmit.value) {
    return;
  }

  resetError();
  loading.value = true;

  try {
    const response = await fetch("/v1/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: username.value.trim(), password: password.value })
    });

    const payload = (await response.json()) as LoginResult;
    if (!response.ok || (payload.code && payload.code !== 200)) {
      throw new Error(payload.message || `登录失败：${response.status}`);
    }

    const token = payload.data?.token;
    if (!token) {
      throw new Error("登录失败：未返回 token");
    }

    localStorage.setItem("medibuddy:token", token);
    if (payload.data?.expiresAt) {
      localStorage.setItem("medibuddy:expiresAt", String(payload.data.expiresAt));
    }
    if (payload.data?.userId != null) {
      localStorage.setItem("medibuddy:userId", String(payload.data.userId));
    }
    if (payload.data?.username) {
      localStorage.setItem("medibuddy:username", payload.data.username);
    }

    window.location.hash = "#/chat";
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "登录失败，请稍后重试";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page-shell">
    <section class="login-card">
      <header class="login-header">
        <span class="eyebrow">账号登录</span>
        <h1>进入小康医疗助手</h1>
        <p>请使用系统分配的账号登录，登录后可继续问诊与上传资料。</p>
      </header>

      <form class="login-form" @submit.prevent="submitLogin">
        <label class="login-label" for="login-username">用户名</label>
        <input
          id="login-username"
          v-model="username"
          class="login-input"
          type="text"
          autocomplete="username"
          placeholder="请输入用户名"
          @input="resetError"
        />

        <label class="login-label" for="login-password">密码</label>
        <input
          id="login-password"
          v-model="password"
          class="login-input"
          type="password"
          autocomplete="current-password"
          placeholder="请输入密码"
          @input="resetError"
        />

        <p v-if="errorText" class="error-text">{{ errorText }}</p>
        <p v-else class="tip-text">登录后系统会自动保存 token。</p>

        <button class="send-btn login-submit" type="submit" :disabled="!canSubmit">
          {{ loading ? "登录中..." : "登录" }}
        </button>
      </form>
    </section>
  </div>
</template>
