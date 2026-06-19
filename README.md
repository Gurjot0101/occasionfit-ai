<div align="center">

![OccasionFit AI Architecture](assets/owicon.png)
# OccasionFit AI

### *Staring at your wardrobe for 20 minutes? Friends can't agree on what suits you?*
**OccasionFit AI decides in seconds. Upload. Describe. Done.**

**AI-powered outfit recommendations via a planner-based agentic backend**

[![Java](https://img.shields.io/badge/Java_21-Virtual_Threads-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![React Native](https://img.shields.io/badge/React_Native-Expo-blue?logo=react)](https://expo.dev)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-green?logo=mongodb)](https://www.mongodb.com/atlas)
[![GCP](https://img.shields.io/badge/GCP-Cloud_Run-blue?logo=googlecloud)](https://cloud.google.com/run)
[![License](https://img.shields.io/badge/License-Proprietary-red)](#-license)

![OccasionFit Demo](docs/demo.gif)

[Features](#-features) · [Arch Diagram](#arch-diagram) · [Architecture](#-agentic-architecture) · [Tech Stack](#tech-stack) · [Roadmap](#roadmap)

</div>

---

## What is this?

OccasionFit AI is a full-stack mobile app where users upload outfit images, describe occasions, and receive AI-powered styling recommendations through a conversational interface.

The technical centrepiece is a **planner-based agentic backend** — rather than hardcoding tool routing in Java, a single Gemini call decides the full tool execution sequence upfront. The Spring Boot orchestrator is a pure executor with zero routing logic.

---

## 🙏 Try It & Share Feedback

The app is live — install in 30 seconds via EAS.

**[📲 Install on Android](https://expo.dev/artifacts/eas/YaSDCVqQoHONRQZ0pLtHU5SNjkj7zx2SPoMKuCIOkUU.apk)** · *iOS — coming soon*

No Play Store required — installs directly via EAS internal distribution.

Found a bug or have a suggestion? Use the **built-in feedback button** in the app
or drop a message on [LinkedIn](https://linkedin.com/in/gurjot0101).

> Early testers help shape what gets built next — wardrobe memory, weather recommendations, and more.

---

## ✨ Features

### 🔐 Auth
- Google Sign-In → Spring Boot verifies `idToken` with Google OAuth
- JWT Access + Refresh Token rotation
- Tokens stored in device Keychain via `react-native-keychain`
- Redux persists session across app restarts

### 🤖 Agentic Chat
- Planner decides tool sequence in **one LLM call** — no per-step re-decisions
- Streaming AI responses with real-time token-by-token display
- Thread-based context memory with async summarization
- Animated typing indicators and pending states

### 🖼️ Image Capabilities
- Upload 1 or multiple images from gallery
- **Analyze** — Gemini Vision describes outfit and suggests styling
- **Compare** — Upload 2+ outfits, get a side-by-side recommendation
- **Generate** — Text-to-image via Imagen for outfit visualization
- Multi-modal messages (text + images stored per message)

### 🎨 UI
- Light / Dark theme
- Animated RGB gradient borders and text
- Inverted FlatList chat with keyboard-aware input

---

<a id="arch-diagram"></a>
## Architecture Diagram

<p align="center">
  <a href="assets/architecture.png">
    <img src="assets/architecture.jpg" alt="OccasionFit AI Architecture" width="900">
  </a>
</p>
<p align="center"><i>Click to view full resolution</i></p>

---

## 🤖 Agentic Architecture

The core design decision: **Gemini plans, Java executes. Zero routing logic in the orchestrator.**

### Request Flow

```
User Message + Image Count
         │
         ▼
   PlannerService                     ← 1 Gemini call, temperature=0.1
         │
         ▼  {"steps": ["ANALYZE_OUTFIT_IMAGE", "GENERATE_TEXT_RESPONSE"]}
    AgentPlan
         │
         ▼
  AgentOrchestrator                   ← iterates steps, no conditionals
    │         │
    ▼         ▼
  Tool 1    Tool 2                    ← each ToolExecutor is a Spring bean
                                         registered via Map<AgentTool, ToolExecutor>
```

### Why Planner Over Reactive (ReAct)?

| | ReAct | Planner (this project) |
|---|---|---|
| LLM calls per request | 3–4 (one per step) | **1** |
| Execution predictability | Can drift mid-flow | Deterministic |
| Token cost | High — full ctx re-read each step | Low — metadata only to planner |
| Debuggability | Decisions scattered across logs | **Full plan logged upfront** |

### Tool Registry

Tools are auto-registered via Spring's dependency injection — no manual wiring:

```java
// AgentOrchestrator constructor
this.toolRegistry = tools.stream()
    .collect(Collectors.toMap(ToolExecutor::getToolType, t -> t));
```

| Tool | Trigger | Model Used |
|------|---------|------------|
| `DIRECT_REPLY` | Greetings, off-topic, missing image | Gemini text |
| `ANALYZE_OUTFIT_IMAGE` | 1 image uploaded | Gemini Vision |
| `COMPARE_OUTFIT_IMAGES` | 2+ images uploaded | Gemini Vision |
| `GENERATE_OUTFIT_IMAGE` | Generate intent, no image | Imagen |
| `GENERATE_TEXT_RESPONSE` | Final response after analysis | Gemini text |

### Example Plans

```
# 2 images + "which is better for a wedding?"
→ [COMPARE_OUTFIT_IMAGES, GENERATE_TEXT_RESPONSE]

# 2 images + "generate something combining both"
→ [COMPARE_OUTFIT_IMAGES, GENERATE_OUTFIT_IMAGE, GENERATE_TEXT_RESPONSE]

# 1 image + "analyze my outfit"
→ [ANALYZE_OUTFIT_IMAGE, GENERATE_TEXT_RESPONSE]

# "generate a casual summer look"
→ [GENERATE_OUTFIT_IMAGE, GENERATE_TEXT_RESPONSE]

# "hows my dress?" + no image
→ [DIRECT_REPLY]  ← asks user to upload image instead of hallucinating
```

### Key Design Decisions

- **Planner receives metadata, not images** — image count sent as integer to the decision prompt. Raw base64 only reaches vision tool executors. Eliminates unnecessary token cost at the routing layer.
- **`temperature=0.1` for planner** — near-deterministic JSON output. Higher temperature for chat responses where variety is desirable.
- **Failure aborts the plan** — if any tool returns `isSuccess=false`, the orchestrator stops and synthesizes from completed actions rather than continuing with bad state.
- **Context summarized async** — `contextServiceImpl.updateContext()` runs on a virtual thread after response is sent, never blocking the user.

---

<a id="tech-stack"></a>
## 🛠️ Tech Stack

| Layer | Technology | Why |
|-------|------------|-----|
| Mobile | React Native + Expo Router | Cross-platform, EAS cloud builds |
| State | Redux Toolkit | Persistent auth + chat state |
| Auth | Google Sign-In + JWT | Stateless, refresh token rotation |
| Storage | react-native-keychain | Secure token storage |
| Backend | Spring Boot + Java 21 | Virtual threads for high concurrency |
| Database | MongoDB Atlas | Flexible schema for message/thread models |
| AI — Planning & Chat | Google Gemini | Fast, cost-efficient text model |
| AI — Vision | Gemini Vision | Multi-image outfit analysis |
| AI — Image Gen | Google Imagen | High-quality fashion image generation |
| AI — Fallback | OpenAI GPT-4 / DALL-E | Provider redundancy |
| Deployment | GCP Cloud Run | Auto-scaling, CI/CD via Cloud Build |

---

## 📱 Frontend

The mobile app is built with React Native + Expo Router, with Redux Toolkit
managing auth and chat state. Key implementation pieces:

- **Multi-image flows** — `images[]` array support across upload, compare,
  and analyze paths
- **Performance** — inverted `FlatList` with memoized, custom-comparator
  message rendering to avoid re-renders on streaming updates
- **Auth** — Google Sign-In → JWT, tokens in device Keychain, automatic
  refresh via Axios interceptors

**Frontend source is private.** Full architecture breakdown available here →
[`frontend/README.md`](./frontend/README.md), or reach out on
[LinkedIn](https://linkedin.com/in/gurjot0101) for a walkthrough.

---

## 🏗️ Project Structure

```
src/main/java/com/occasionfit/backend/
├── agent/                    ← orchestration layer
│   ├── AgentOrchestrator.java      pure executor — no routing logic
│   ├── AgentContext.java           request state passed between tools
│   ├── AgentPlan.java              List<AgentTool> steps from planner
│   └── tools/
│       ├── ToolExecutor.java       interface — getToolType() + execute()
│       ├── ToolExecutionResult.java
│       └── impl/                   one class per tool
├── ai/                       ← intelligence layer
│   ├── client/
│   │   ├── GeminiClient.java       7 methods: chat, vision, imagen, plan, synthesize
│   │   └── OpenAiClient.java       mirrors GeminiClient — drop-in fallback
│   ├── PlannerService.java         calls generatePlan(), handles fallback
│   └── prompt/
│       ├── PromptBuilder.java      buildPlannerPrompt(), buildSynthesisPrompt()
│       ├── PromptCleaner.java      strips model prefixes (word-boundary safe)
│       └── PromptTemplate.java     shared system prompt
├── service/                  ← business logic only
├── controller/
├── repository/
├── security/                 ← JwtAuthenticationFilter, JwtService, SecurityConfig
├── config/
├── model/
├── dto/
├── scheduler/                ← CleanupScheduler
└── util/                     ← TokenEstimator
```

---

## 🔒 Security

- JWT stateless auth with refresh token rotation
- Google OAuth 2.0 `idToken` verification on every login
- Tokens stored in device Keychain — never in AsyncStorage
- Spring Security filter chain with custom JWT validation
- Secrets managed via GCP Secret Manager in production

---

<a id="roadmap"></a>
## 🛣️ Roadmap

> **Why no LangChain4j, LangGraph4j, or MCP?**
> The agentic architecture here is built from scratch intentionally — to understand
> what these frameworks abstract before using them. LangChain4j, LangGraph4j, and MCP
> are on the roadmap as the next learning milestone.

### In Progress
- [ ] OpenAI GPT-4 / DALL-E fallback activation
- [ ] Thread context UI refresh after async update

### Planned

| Priority | Item |
|----------|------|
| 🔴 High | LangChain4j — provider abstraction + prompt templates |
| 🔴 High | LangGraph4j — dynamic graph-based routing for complex multi-step flows |
| 🟡 Medium | MCP — expose tool executors as MCP server endpoints |
| 🟡 Medium | RAG — vector search over saved wardrobe items |
| 🟢 Low | Cross-message image context (compare current + previous upload) |
| 🟢 Low | Wardrobe management (save + retrieve outfits) |
| 🟢 Low | Weather-based outfit recommendations |
| 🟢 Low | Voice input |

---

## 👨‍💻 Author

**Gurjot Singh** — Senior Software Developer @ Capgemini

AWS Certified Developer · Claude Certified Architect

- GitHub: [@Gurjot0101](https://github.com/Gurjot0101)
- LinkedIn: [linkedin.com/in/gurjot0101](https://linkedin.com/in/gurjot0101)
- Email: gurjot78885@gmail.com

---

## 📄 License

© 2026 Gurjot Singh. All rights reserved.

This repository is shared publicly for portfolio and demonstration purposes
only. No license is granted to copy, modify, distribute, or use this code,
in whole or in part, without explicit written permission from the author.
