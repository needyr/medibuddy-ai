<script setup lang="ts">
import DOMPurify from "dompurify";
import { marked } from "marked";
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";

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
  socketConnecting: "连接中",
  socketOffline: "连接断开",
  thinking: "医疗助手思考中",
  typing: "正在整理回答",
  copy: "复制",
  copied: "已复制",
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
const streamingMessageId = ref<number | null>(null);
const errorText = ref("");
const wsConnected = ref(false);
const wsConnecting = ref(false);
const wsError = ref("");
const ws = ref<WebSocket | null>(null);
const typingPlaceholder = "正在整理回答…";
const copiedMessageId = ref<number | null>(null);

const initialMessages = (): ChatMessage[] => [
  { id: 1, role: "assistant", text: copy.welcome, timestamp: formatTime(new Date()) },
  { id: 2, role: "system", text: copy.safety, timestamp: formatTime(new Date()) }
];

const messages = ref<ChatMessage[]>(initialMessages());

const canSend = computed(() => input.value.trim().length > 0 && !loading.value);
const conversationCount = computed(() => messages.value.filter((item) => item.role !== "system").length);
const renderedMessages = computed(() =>
  messages.value.map((message) => ({
    ...message,
    html: renderMarkdown(message.text),
    isTypingPlaceholder: message.role === "assistant" && message.text === typingPlaceholder,
    isStreaming: message.id === streamingMessageId.value,
    canCopy: message.role === "assistant" && message.text !== typingPlaceholder && message.text.trim().length > 0,
    copied: message.id === copiedMessageId.value
  }))
);


const wsEndpointDisplay = computed(() => {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const host = window.location.hostname || "localhost";
  const port = "9000";
  return `${protocol}://${host}:${port}`;
});

const wsStatusText = computed(() => {
  if (wsConnected.value) {
    return copy.online;
  }
  if (wsConnecting.value) {
    return copy.socketConnecting;
  }
  return copy.socketOffline;
});

const tokenKey = "medibuddy:token";

function formatTime(date: Date): string {
  return new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(date);
}

const markdownRenderer = new marked.Renderer();
markdownRenderer.link = (href, title, text) => {
  const safeHref = href ?? "";
  const safeTitle = title ? ` title="${title}"` : "";
  return `<a href="${safeHref}"${safeTitle} target="_blank" rel="noreferrer">${text}</a>`;
};

marked.setOptions({
  renderer: markdownRenderer,
  breaks: true
});

function renderMarkdown(value: string): string {
  const raw = value ?? "";
  const html = marked.parse(raw);
  return DOMPurify.sanitize(html);
}

function scrollToBottom(): void {
  nextTick(() => {
    if (messageViewport.value) {
      messageViewport.value.scrollTop = messageViewport.value.scrollHeight;
    }
  });
}

function pushMessage(role: Role, text: string): number {
  const id = messageSeed.value++;
  messages.value.push({ id, role, text, timestamp: formatTime(new Date()) });
  return id;
}

async function copyMessage(message: ChatMessage): Promise<void> {
  if (!message.text || message.text === typingPlaceholder) {
    return;
  }
  try {
    await navigator.clipboard.writeText(message.text);
  } catch {
    const textarea = document.createElement("textarea");
    textarea.value = message.text;
    textarea.style.position = "fixed";
    textarea.style.opacity = "0";
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    try {
      document.execCommand("copy");
    } finally {
      document.body.removeChild(textarea);
    }
  }
  copiedMessageId.value = message.id;
  window.setTimeout(() => {
    if (copiedMessageId.value === message.id) {
      copiedMessageId.value = null;
    }
  }, 1500);
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

function handleUnauthorized(): void {
  localStorage.removeItem(tokenKey);
  localStorage.removeItem("medibuddy:expiresAt");
  localStorage.removeItem("medibuddy:userId");
  localStorage.removeItem("medibuddy:username");
  errorText.value = "登录已过期，请重新登录。";
  window.location.hash = "#/login";
}

function buildWsUrl(): string {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const host = window.location.hostname || "localhost";
  const port = "9000";
  const token = localStorage.getItem(tokenKey) || "";
  return `${protocol}://${host}:${port}?token=${encodeURIComponent(token)}`;
}

function connectWebSocket(): void {
  if (wsConnecting.value || wsConnected.value) {
    return;
  }
  const token = localStorage.getItem(tokenKey);
  if (!token) {
    handleUnauthorized();
    return;
  }

  wsConnecting.value = true;
  wsError.value = "";

  const socket = new WebSocket(buildWsUrl());
  ws.value = socket;

  socket.onopen = () => {
    wsConnected.value = true;
    wsConnecting.value = false;
  };

  socket.onmessage = (event) => {
    let payload:
      | { type?: string; memoryId?: number; assistantMessage?: string; error?: string }
      | null = null;
    try {
      payload = JSON.parse(event.data);
    } catch {
      payload = null;
    }

    if (!payload) {
      pushMessage("system", "收到无法解析的服务端消息。");
      loading.value = false;
      return;
    }

    const type = payload.type;
    if (type === "start") {
      // start: 新建 assistant 消息
      streamingMessageId.value = pushMessage("assistant", typingPlaceholder);
      scrollToBottom();
      return;
    }

    if (type === "delta") {
      // delta: 追加到最后一条 assistant 消息
      const delta = payload.assistantMessage ?? "";
      if (!delta) {
        return;
      }
      if (streamingMessageId.value !== null) {
        const target = messages.value.find((item) => item.id === streamingMessageId.value);
        if (target && target.role === "assistant") {
          target.text = target.text === typingPlaceholder ? delta : target.text + delta;
          scrollToBottom();
          return;
        }
      }
      for (let i = messages.value.length - 1; i >= 0; i -= 1) {
        if (messages.value[i].role === "assistant") {
          messages.value[i].text += delta;
          scrollToBottom();
          return;
        }
      }
      pushMessage("assistant", delta);
      scrollToBottom();
      return;
    }

    if (type === "end") {
      // end: loading=false
      streamingMessageId.value = null;
      loading.value = false;
      return;
    }

    if (type === "error") {
      // error: 系统消息 + loading=false
      if (streamingMessageId.value !== null) {
        const index = messages.value.findIndex((item) => item.id === streamingMessageId.value);
        if (index >= 0 && messages.value[index].text === typingPlaceholder) {
          messages.value.splice(index, 1);
        }
      }
      pushMessage("system", payload.error || "服务端处理失败。");
      streamingMessageId.value = null;
      loading.value = false;
      return;
    }

    // 兼容旧协议（无 type）
    if (payload.error) {
      pushMessage("system", payload.error);
      streamingMessageId.value = null;
      loading.value = false;
      return;
    }
    if (payload.assistantMessage) {
      pushMessage("assistant", payload.assistantMessage);
      streamingMessageId.value = null;
      loading.value = false;
    }
  };

  socket.onclose = (event) => {
    wsConnected.value = false;
    wsConnecting.value = false;
    if (loading.value) {
      loading.value = false;
    }
    if (event.code === 1008 || event.reason === "Unauthorized") {
      handleUnauthorized();
    }
  };

  socket.onerror = () => {
    wsError.value = "WebSocket 连接失败，请稍后重试。";
    wsConnected.value = false;
    wsConnecting.value = false;
  };
}

async function sendMessage(): Promise<void> {
  const userMessage = input.value.trim();
  if (!userMessage || loading.value) {
    return;
  }

  if (!wsConnected.value) {
    connectWebSocket();
    errorText.value = "连接尚未建立，请稍后重试。";
    return;
  }

  pushMessage("user", userMessage);
  input.value = "";
  errorText.value = "";
  loading.value = true;
  streamingMessageId.value = null;

  try {
    ws.value?.send(JSON.stringify({ memoryId: Number(memoryId.value), userMessage }));
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : "发送失败，请稍后重试。";
    pushMessage("system", "消息发送失败，请稍后重试。");
    streamingMessageId.value = null;
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
  connectWebSocket();
});

onUnmounted(() => {
  ws.value?.close();
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
          <strong>{{ wsEndpointDisplay }}</strong>
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
            {{ loading ? copy.thinking : wsStatusText }}
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
              <button v-if="message.canCopy" class="copy-btn" type="button" @click="copyMessage(message)">
                {{ message.copied ? copy.copied : copy.copy }}
              </button>
            </div>
            <div class="bubble markdown-body">
              <div v-if="message.isTypingPlaceholder" class="typing-bubble">
                <span class="typing-label">{{ copy.typing }}</span>
                <span class="typing-dots">
                  <i></i>
                  <i></i>
                  <i></i>
                </span>
              </div>
              <div v-else class="streaming-text">
                <span v-html="message.html"></span>
                <span v-if="message.isStreaming" class="typing-dots inline-dots">
                  <i></i>
                  <i></i>
                  <i></i>
                </span>
              </div>
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
          <p v-else-if="wsError" class="error-text">{{ wsError }}</p>
          <span v-else class="tip-text">{{ copy.composerHint }}</span>
          <button class="send-btn" type="button" :disabled="!canSend" @click="sendMessage">
            {{ loading ? copy.sending : copy.send }}
          </button>
        </div>
      </footer>
    </main>
  </div>
</template>
