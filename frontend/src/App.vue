<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";

type Role = "assistant" | "user" | "system";

interface ChatMessage {
  id: number;
  role: Role;
  text: string;
  timestamp: string;
}

const copy = {
  appName: "小康医疗助手",
  assistantName: "小康助手",
  userName: "患者",
  systemName: "系统提醒",
  title: "智能初诊与就医咨询",
  eyebrow: "门诊对话界面",
  heroCopy: "面向门诊咨询场景设计的对话界面，适合快速收集症状、整理问题，并辅助判断挂号方向。",
  currentSession: "当前会话",
  newSession: "新建问诊",
  clearSession: "清空会话",
  messageCount: "消息数",
  endpoint: "服务端接口",
  quickAsk: "快捷提问",
  quickAskHint: "适合首轮问诊",
  online: "服务在线",
  thinking: "医疗助手思考中",
  typing: "正在整理回答",
  composerLabel: "描述您的问题",
  composerPlaceholder: "例如：我头痛三天，伴随低烧和喉咙痛，应该先做哪些检查？支持简单 Markdown。",
  composerHint: "按 Enter 发送，Shift + Enter 换行，支持简单 Markdown 显示",
  send: "发送咨询",
  sending: "发送中...",
  welcome: "您好，我是小康医疗助手。您可以描述症状、检查结果，或直接询问挂号与就诊建议。",
  safety: "提示：AI 回答仅供参考，紧急情况请及时前往医院或拨打急救电话。",
  resetWelcome: "已为您开启新的问诊会话。请告诉我目前最困扰您的症状或问题。",
  resetSafety: "提示：如存在胸痛、呼吸困难、意识异常等急症，请立即线下就医。",
  fallback: "当前服务暂时不可用，请确认后端已启动，并检查模型、MongoDB 和 MySQL 连接状态。\n\n- 如需继续排查，请把最新报错贴给我。\n- 如需正式问诊，请稍后再试。"
} as const;

const suggestions = [
  "我发烧两天了，还伴有咳嗽，应该挂什么科？",
  "体检报告显示转氨酶偏高，要注意什么？",
  "最近总是胸闷，但不确定是不是心脏问题。",
  "帮我整理一下明天去医院要准备的问题。"
] as const;

const messageViewport = ref<HTMLElement | null>(null);
const messageSeed = ref(3);
const memoryId = ref(String(Date.now()));
const input = ref("");
const loading = ref(false);
const errorText = ref("");

const initialMessages = (): ChatMessage[] => [
  { id: 1, role: "assistant", text: copy.welcome, timestamp: formatTime(new Date()) },
  { id: 2, role: "system", text: copy.safety, timestamp: formatTime(new Date()) }
];

const messages = ref<ChatMessage[]>(initialMessages());

const canSend = computed(() => input.value.trim().length > 0 && !loading.value);
const conversationCount = computed(() => messages.value.filter((item) => item.role !== "system").length);
const renderedMessages = computed(() => messages.value.map((message) => ({ ...message, html: renderMarkdown(message.text) })));

function formatTime(date: Date): string {
  return new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(date);
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function renderInlineMarkdown(value: string): string {
  return value
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*]+)\*/g, "<em>$1</em>")
    .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>');
}

function renderMarkdown(value: string): string {
  const escaped = escapeHtml(value).replace(/\r\n/g, "\n");
  const lines = escaped.split("\n");
  const parts: string[] = [];
  let inList = false;

  for (const rawLine of lines) {
    const line = rawLine.trimEnd();
    const listMatch = line.match(/^[-*]\s+(.*)$/);

    if (listMatch) {
      if (!inList) {
        parts.push("<ul>");
        inList = true;
      }
      parts.push(`<li>${renderInlineMarkdown(listMatch[1])}</li>`);
      continue;
    }

    if (inList) {
      parts.push("</ul>");
      inList = false;
    }

    if (!line.trim()) {
      parts.push("<p></p>");
      continue;
    }

    if (line.startsWith("### ")) {
      parts.push(`<h3>${renderInlineMarkdown(line.slice(4))}</h3>`);
      continue;
    }

    if (line.startsWith("## ")) {
      parts.push(`<h2>${renderInlineMarkdown(line.slice(3))}</h2>`);
      continue;
    }

    if (line.startsWith("# ")) {
      parts.push(`<h1>${renderInlineMarkdown(line.slice(2))}</h1>`);
      continue;
    }

    parts.push(`<p>${renderInlineMarkdown(line)}</p>`);
  }

  if (inList) {
    parts.push("</ul>");
  }

  return parts.join("");
}

function pushMessage(role: Role, text: string): void {
  messages.value.push({ id: messageSeed.value++, role, text, timestamp: formatTime(new Date()) });
}

function useSuggestion(text: string): void {
  input.value = text;
}

function resetConversation(): void {
  memoryId.value = String(Date.now());
  input.value = "";
  errorText.value = "";
  messages.value = [
    { id: 1, role: "assistant", text: copy.resetWelcome, timestamp: formatTime(new Date()) },
    { id: 2, role: "system", text: copy.resetSafety, timestamp: formatTime(new Date()) }
  ];
  messageSeed.value = 3;
}

function clearConversation(): void {
  input.value = "";
  errorText.value = "";
  messages.value = initialMessages();
  messageSeed.value = 3;
}

function goToUploadPage(): void {
  window.location.hash = "#/upload";
}

async function sendMessage(): Promise<void> {
  const userMessage = input.value.trim();
  if (!userMessage || loading.value) {
    return;
  }

  pushMessage("user", userMessage);
  input.value = "";
  errorText.value = "";
  loading.value = true;

  try {
    const response = await fetch("/v1/medibuddy/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ memoryId: Number(memoryId.value), userMessage })
    });

    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`);
    }

    const text = await response.text();
    pushMessage("assistant", text || "当前未返回内容，请稍后重试。");
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "请求失败，请稍后重试。";
    pushMessage("assistant", copy.fallback);
  } finally {
    loading.value = false;
  }
}

watch(
  () => [messages.value.length, loading.value],
  async () => {
    await nextTick();
    if (messageViewport.value) {
      messageViewport.value.scrollTop = messageViewport.value.scrollHeight;
    }
  }
);

onMounted(() => {
  if (messageViewport.value) {
    messageViewport.value.scrollTop = messageViewport.value.scrollHeight;
  }
});
</script>

<template>
  <div class="app-shell">
    <aside class="hero-panel">
      <div class="hero-badge">Hospital AI Desk</div>
      <h1>{{ copy.appName }}</h1>
      <p class="hero-copy">{{ copy.heroCopy }}</p>

      <div class="status-card">
        <div>
          <span class="label">{{ copy.currentSession }}</span>
          <strong>{{ memoryId }}</strong>
        </div>
        <button class="ghost-btn" type="button" @click="resetConversation">{{ copy.newSession }}</button>
      </div>

      <div class="stats-grid">
        <article>
          <span>{{ copy.messageCount }}</span>
          <strong>{{ conversationCount }}</strong>
        </article>
        <article>
          <span>{{ copy.endpoint }}</span>
          <strong>/v1/medibuddy/chat</strong>
        </article>
      </div>

      <section class="suggestion-panel">
        <div class="panel-head">
          <h2>{{ copy.quickAsk }}</h2>
          <span>{{ copy.quickAskHint }}</span>
        </div>
        <button
          v-for="suggestion in suggestions"
          :key="suggestion"
          class="suggestion-item"
          type="button"
          @click="useSuggestion(suggestion)"
        >
          {{ suggestion }}
        </button>
      </section>
    </aside>

    <main class="chat-panel">
      <header class="chat-header">
        <div>
          <span class="eyebrow">{{ copy.eyebrow }}</span>
          <h2>{{ copy.title }}</h2>
        </div>
        <div class="header-actions">
          <button class="ghost-btn ghost-btn-light" type="button" @click="clearConversation">{{ copy.clearSession }}</button>
          <button class="ghost-btn ghost-btn-light" type="button" @click="goToUploadPage">上传文件</button>
          <div class="header-state" :class="{ busy: loading }">
            <span class="state-dot"></span>
            {{ loading ? copy.thinking : copy.online }}
          </div>
        </div>
      </header>

      <section ref="messageViewport" class="message-list">
        <article v-for="message in renderedMessages" :key="message.id" class="message-row" :data-role="message.role">
          <div class="avatar">
            <span v-if="message.role === 'assistant'">医</span>
            <span v-else-if="message.role === 'user'">我</span>
            <span v-else>提</span>
          </div>
          <div class="bubble-wrap">
            <div class="meta">
              <strong>{{ message.role === "assistant" ? copy.assistantName : message.role === "user" ? copy.userName : copy.systemName }}</strong>
              <time>{{ message.timestamp }}</time>
            </div>
            <div class="bubble markdown-body" v-html="message.html"></div>
          </div>
        </article>

        <article v-if="loading" class="message-row typing-row" data-role="assistant">
          <div class="avatar"><span>医</span></div>
          <div class="bubble-wrap">
            <div class="meta">
              <strong>{{ copy.assistantName }}</strong>
              <time>{{ formatTime(new Date()) }}</time>
            </div>
            <div class="bubble typing-bubble">
              <span class="typing-label">{{ copy.typing }}</span>
              <span class="typing-dots">
                <i></i>
                <i></i>
                <i></i>
              </span>
            </div>
          </div>
        </article>
      </section>

      <footer class="composer">
        <label class="composer-label" for="message-input">{{ copy.composerLabel }}</label>
        <textarea
          id="message-input"
          v-model="input"
          class="composer-input"
          rows="4"
          :placeholder="copy.composerPlaceholder"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="composer-actions">
          <p v-if="errorText" class="error-text">{{ errorText }}</p>
          <span v-else class="tip-text">{{ copy.composerHint }}</span>
          <button class="send-btn" type="button" :disabled="!canSend" @click="sendMessage">
            {{ loading ? copy.sending : copy.send }}
          </button>
        </div>
      </footer>
    </main>
  </div>
</template>
