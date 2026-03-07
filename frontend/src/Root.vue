<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import App from "./App.vue";
import UploadPage from "./UploadPage.vue";

const currentHash = ref(window.location.hash || "#/chat");

const currentView = computed(() => currentHash.value === "#/upload" ? UploadPage : App);

function syncHash(): void {
  if (!window.location.hash) {
    window.location.hash = "#/chat";
  }
  currentHash.value = window.location.hash || "#/chat";
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
