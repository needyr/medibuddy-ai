<script setup lang="ts">
import { computed, ref } from "vue";

interface UploadResult {
  code?: number;
  message?: string;
  data?: string;
}

const supportedExtensions = [
  ".txt",
  ".md",
  ".markdown",
  ".csv",
  ".json",
  ".xml",
  ".html",
  ".htm",
  ".yaml",
  ".yml",
  ".properties",
  ".log",
  ".java",
  ".sql"
] as const;

const accept = supportedExtensions.join(",");
const selectedFile = ref<File | null>(null);
const uploading = ref(false);
const dragActive = ref(false);
const errorText = ref("");
const successText = ref("");
const fileInput = ref<HTMLInputElement | null>(null);

const supportText = computed(() => supportedExtensions.join("、"));
const selectedFileName = computed(() => selectedFile.value?.name ?? "未选择文件");

function goBack(): void {
  window.location.hash = "#/chat";
}

function openFilePicker(): void {
  fileInput.value?.click();
}

function resetNotice(): void {
  errorText.value = "";
  successText.value = "";
}

function isSupportedFile(file: File): boolean {
  const fileName = file.name.toLowerCase();
  const extensionMatched = supportedExtensions.some((extension) => fileName.endsWith(extension));
  const mimeType = file.type.toLowerCase();
  const mimeMatched = mimeType.startsWith("text/") || ["application/json", "application/xml"].includes(mimeType);
  return extensionMatched || mimeMatched;
}

function chooseFile(file: File | null): void {
  resetNotice();
  if (!file) {
    selectedFile.value = null;
    return;
  }

  if (!isSupportedFile(file)) {
    selectedFile.value = null;
    errorText.value = `当前仅支持 TextDocumentParser 可直接读取的文本类文件：${supportText.value}`;
    return;
  }

  selectedFile.value = file;
}

function onFileChange(event: Event): void {
  const input = event.target as HTMLInputElement;
  chooseFile(input.files?.[0] ?? null);
}

function onDrop(event: DragEvent): void {
  event.preventDefault();
  dragActive.value = false;
  chooseFile(event.dataTransfer?.files?.[0] ?? null);
}

async function uploadFile(): Promise<void> {
  if (!selectedFile.value || uploading.value) {
    return;
  }

  resetNotice();
  uploading.value = true;

  try {
    const formData = new FormData();
    formData.append("file", selectedFile.value);

    const response = await fetch("/v1/medibuddy/upload", {
      method: "POST",
      body: formData
    });

    if (!response.ok) {
      throw new Error(`上传失败：${response.status}`);
    }

    const result = (await response.json()) as UploadResult;
    if (result.code && result.code !== 200) {
      throw new Error(result.message || "上传失败，请稍后重试");
    }

    successText.value = result.data || result.message || "文件上传成功";
    selectedFile.value = null;
    if (fileInput.value) {
      fileInput.value.value = "";
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "上传失败，请稍后重试";
  } finally {
    uploading.value = false;
  }
}
</script>

<template>
  <div class="upload-page-shell">
    <section class="upload-page-card">
      <header class="upload-page-header">
        <div>
          <span class="eyebrow">文件上传</span>
          <h1>上传知识文件</h1>
          <p>支持拖拽上传，文件会提交到 `/v1/medibuddy/upload` 并交给后端 `TextDocumentParser` 解析。</p>
        </div>
        <button class="ghost-btn" type="button" @click="goBack">返回会话</button>
      </header>

      <section
        class="upload-dropzone"
        :class="{ active: dragActive }"
        @click="openFilePicker"
        @dragenter.prevent="dragActive = true"
        @dragover.prevent="dragActive = true"
        @dragleave.prevent="dragActive = false"
        @drop="onDrop"
      >
        <input
          ref="fileInput"
          class="upload-input"
          type="file"
          :accept="accept"
          @change="onFileChange"
        />
        <div class="upload-dropzone-icon">↑</div>
        <h2>拖动文件到这里，或点击选择文件</h2>
        <p>仅支持文本类文件：{{ supportText }}</p>
        <span class="upload-file-name">当前文件：{{ selectedFileName }}</span>
      </section>

      <section class="upload-tips-card">
        <h3>上传限制</h3>
        <ul>
          <li>不支持 PDF、Word、Excel、图片等二进制格式。</li>
          <li>建议优先上传 `.txt`、`.md`、`.csv`、`.json` 等纯文本内容。</li>
        </ul>
      </section>

      <footer class="upload-actions">
        <div class="upload-status">
          <p v-if="errorText" class="error-text">{{ errorText }}</p>
          <p v-else-if="successText" class="upload-success">{{ successText }}</p>
          <p v-else class="tip-text">选择一个文本文件后点击“开始上传”即可。</p>
        </div>
        <button class="send-btn" type="button" :disabled="!selectedFile || uploading" @click="uploadFile">
          {{ uploading ? "上传中..." : "开始上传" }}
        </button>
      </footer>
    </section>
  </div>
</template>
