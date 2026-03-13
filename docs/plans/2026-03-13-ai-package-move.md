# AI Package Move Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rename root package `cn.needy.javaai` to `cn.needy.medibuddy`, and relocate AI-related modules under `cn.needy.medibuddy.ai.*` while preserving existing behavior.

**Architecture:** Move AI packages (assistant/config/controller/websocket/bean/store/tools + related AI entities/services) into `cn.needy.medibuddy.ai` and move non-AI modules into `cn.needy.medibuddy`. Update package declarations and imports consistently across main and test sources, keeping Spring component scanning and bean names unchanged.

**Tech Stack:** Java 17, Spring Boot 3.2.x, LangChain4j, MyBatis-Plus, Redis, MongoDB

---

### Task 1: Inventory and decide AI vs non-AI modules

**Files:**
- Modify: (decision task only; no files)

**Step 1: Confirm AI scope from current tree**
- AI packages to move under `cn.needy.medibuddy.ai`:
  - `src/main/java/cn/needy/javaai/assistant/*`
  - `src/main/java/cn/needy/javaai/config/*`
  - `src/main/java/cn/needy/javaai/controller/*`
  - `src/main/java/cn/needy/javaai/store/*`
  - `src/main/java/cn/needy/javaai/tools/*`
  - `src/main/java/cn/needy/javaai/bean/*` (AI request/response types)
  - `src/main/java/cn/needy/javaai/entity/*` + `mapper/*` + `service/*` + `service/impl/*` (if AI domain)
- Non‑AI packages to move under `cn.needy.medibuddy`:
  - `common/*` (Result, etc.) if shared

**Step 2: Record final scope**
- Make a short checklist in PR notes (not committed) to ensure consistent moves.

**Step 3: Commit**
- (No commit for decision-only task)

---

### Task 2: Move package directories to new namespace

**Files:**
- Create: `src/main/java/cn/needy/medibuddy/**`
- Modify: move files from `src/main/java/cn/needy/javaai/**`

**Step 1: Prepare folders**
- Create directories for:
  - `src/main/java/cn/needy/medibuddy/ai/assistant`
  - `src/main/java/cn/needy/medibuddy/ai/bean`
  - `src/main/java/cn/needy/medibuddy/ai/config`
  - `src/main/java/cn/needy/medibuddy/ai/controller`
  - `src/main/java/cn/needy/medibuddy/ai/store`
  - `src/main/java/cn/needy/medibuddy/ai/tools`
  - plus any AI entity/mapper/service folders you decide in Task 1
  - `src/main/java/cn/needy/medibuddy/common` (if shared)

**Step 2: Move files**
- Move each Java file into its target directory (same relative file name).

**Step 3: Commit**
```bash
git add src/main/java/cn/needy/medibuddy src/main/java/cn/needy/javaai
git commit -m "refactor: relocate medibuddy package layout"
```

---

### Task 3: Update package declarations in moved files

**Files:**
- Modify: all moved Java files under `src/main/java/cn/needy/medibuddy/**`

**Step 1: Update package statements**
- Example:
  - `package cn.needy.javaai.assistant;` → `package cn.needy.medibuddy.ai.assistant;`
  - `package cn.needy.javaai.common;` → `package cn.needy.medibuddy.common;`

**Step 2: Adjust internal imports**
- Replace `cn.needy.javaai.*` with `cn.needy.medibuddy.*` or `cn.needy.medibuddy.ai.*` as applicable.

**Step 3: Commit**
```bash
git add src/main/java/cn/needy/medibuddy
git commit -m "refactor: update package declarations for medibuddy"
```

---

### Task 4: Update non-AI references in main sources

**Files:**
- Modify: `src/main/java/cn/needy/SpringbootStartApp.java`
- Modify: any other source that still imports `cn.needy.javaai.*`

**Step 1: Update application entry package**
- Move `SpringbootStartApp.java` to `src/main/java/cn/needy/medibuddy/SpringbootStartApp.java` and update `package` to `cn.needy.medibuddy`.

**Step 2: Update remaining imports**
- Replace all `cn.needy.javaai.*` imports to the new packages.

**Step 3: Commit**
```bash
git add src/main/java/cn/needy/medibuddy/SpringbootStartApp.java src/main/java/cn/needy/medibuddy
git commit -m "refactor: align main sources with new root package"
```

---

### Task 5: Update test packages and imports

**Files:**
- Modify: `src/test/java/cn/needy/*.java`

**Step 1: Update test package declarations**
- If tests use `package cn.needy;` and reference `SpringbootStartApp`, update to `package cn.needy.medibuddy;` and adjust imports.
- Update any `cn.needy.javaai.*` imports to new packages.

**Step 2: Run a focused test**
Run: `mvn -q -Dtest=AiServiceTest test`
Expected: builds and fails only if environment deps are missing (e.g., external services). If failures are due to missing env, note in summary.

**Step 3: Commit**
```bash
git add src/test/java/cn/needy
git commit -m "test: update packages for medibuddy"
```

---

### Task 6: Clean up old package directories

**Files:**
- Modify: remove empty `src/main/java/cn/needy/javaai` tree

**Step 1: Delete empty directories**
- Ensure no `.java` files remain under `cn/needy/javaai`.

**Step 2: Commit**
```bash
git add src/main/java/cn/needy/javaai
git commit -m "chore: remove legacy javaai packages"
```

---

### Task 7: Sanity check build and summarize

**Files:**
- Modify: none

**Step 1: Run quick compile**
Run: `mvn -q -DskipTests compile`
Expected: compile success.

**Step 2: Summarize changes**
- Provide a short summary of moved packages and any test/build issues.

**Step 3: Commit**
- No commit unless new changes were made.
