<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import App from "./App.vue";
import LoginPage from "./LoginPage.vue";
import UploadPage from "./UploadPage.vue";

const tokenKey = "medibuddy:token";
const expiresAtKey = "medibuddy:expiresAt";
const currentHash = ref(window.location.hash || "#/chat");

const currentView = computed(() => {
  if (currentHash.value === "#/login") {
    return LoginPage;
  }
  if (currentHash.value === "#/upload") {
    return UploadPage;
  }
  return App;
});

function clearAuth(): void {
  localStorage.removeItem(tokenKey);
  localStorage.removeItem(expiresAtKey);
  localStorage.removeItem("medibuddy:userId");
  localStorage.removeItem("medibuddy:username");
}

function hasValidToken(): boolean {
  const token = localStorage.getItem(tokenKey);
  if (!token) {
    return false;
  }
  const expiresAtRaw = localStorage.getItem(expiresAtKey);
  if (expiresAtRaw) {
    const expiresAt = Number(expiresAtRaw);
    if (Number.isFinite(expiresAt) && expiresAt > 0 && Date.now() > expiresAt) {
      clearAuth();
      return false;
    }
  }
  return true;
}

function syncHash(): void {
  if (!window.location.hash) {
    window.location.hash = "#/chat";
  }
  const hash = window.location.hash || "#/chat";
  const authed = hasValidToken();
  if (hash !== "#/login" && !authed) {
    window.location.hash = "#/login";
    currentHash.value = "#/login";
    return;
  }
  if (hash === "#/login" && authed) {
    window.location.hash = "#/chat";
    currentHash.value = "#/chat";
    return;
  }
  currentHash.value = hash;
}

onMounted(() => {
  syncHash();
  window.addEventListener("hashchange", syncHash);
});

onUnmounted(() => {
  window.removeEventListener("hashchange", syncHash);
});
</script>

<template>
  <component :is="currentView" />
</template>
