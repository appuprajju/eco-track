# EcoTrack AI – Personal Carbon Footprint Intelligence Platform

EcoTrack AI is an enterprise-grade, offline-first personal carbon intelligence dashboard designed to empower citizens to calculate, monitor, and intelligently abate their climate footprints. Powered by Jetpack Compose, Room Database, and native client-side Gemini 3.5 AI, it offers rapid visual telemetry, gamified streaks, action challenges, and deep sustainable learning modules.

---

## 🎨 Visual Identity & Spacing Theme
EcoTrack AI utilizes a customized, eye-safe **Forest Emerald & Organic Solar Teal** visual theme with generous adaptive whitespace, consistent Material 3 active navigation bar pills, and responsive Canvas-drawn interactive telemetry. It enforces strict edge-to-edge full screen layouts (`enableEdgeToEdge()`) honoring safe system drawing insets across foldables, tablets, and mobile alike.

---

## 🏗️ Architectural Blueprint (Clean Architecture & MVVM)

The application centers around a strict boundary separation complying with SOLID principles:

```
[ UI Layer (Jetpack Compose) ]
       │            ▲
       ▼  (Events)  │  (Reactive State Flow)
[ State Controller (EcoTrackViewModel) ] Let-Engine
       │            ▲
       ▼            │  (Clean Data Streams)
[ Unified Gateway (EcoTrackRepository) ] Repository Pattern
       │                  │
       ▼ (Offline Data)   ▼ (Generative Predictions)
 [ Room SQLite DB ]     [ Gemini 3.5 API (REST) ]
```

### 1. Data Flow Diagram
```
User Enters Travel/Energy Metrics -> Calculator Composable 
  -> Converts raw entries via EPA Math multipliers -> CarbonLog Data
    -> Injected into local EcoTrack Dao 
      -> Saved Reactively to Room SQLite DB
        -> Emits updated flow streams to Repository State Flow
          -> Triggers ViewModel update 
            -> Real-time telemetry chart updates on dynamic Compose Canvas
```

### 2. User Log Sequence Diagram
```
User (UI)       ViewModel        Repository       Local Room DB       Gemini API
   │                │                │                 │                   │
   ├─► Log Activity─┼───────────────►│                 │                   │
   │   (20km Drive) │                ├─► Write SQLite ─┼──────────────────►│
   │                │                │   Emissions Log │                   │
   │                │                │                 │                   │
   │                │                ├─► Award Points ─┼──────────────────►│
   │                │                │   & Log Streak  │                   │
   │                │                │                 │                   │
   │                │  Fetch Advice  │                 │                   │
   ├─► Click Coach ─┼───────────────►┼────────────────────────────────────►│
   │   "Analyze"    │                │  Prompt real-time carbon context     │
   │                │                │◄───────────────── Result: Action ───┤
   │◄─ Render Tips ─┼◄───────────────┤  tips & roadmap                     │
```

---

## 🔌 Modules & Rich Capabilities

1. **Authentication Gate**: Composed with input verification, rate limiting mock-gates (preventing brute force leaks), and single-source unique local profile configurations.
2. **Carbon Offset Calculator**: Employs real climate math conversions supporting transport fuels (Petrol vs Diesel vs electric), home grid options (Coal vs Solar), and dietary impact (Vegan vs Beef-heavy).
3. **Daily Tracker Ledger**: An offline ledger listing recent recordings, allowing quick deletions, updates, and entries.
4. **Target Goals**: Enables users to set quantitative abating targets (e.g., save 100kg CO₂), drawing customized modern linear progress trackers.
5. **Gamification & Rewards**: Active user levelling based on points, login streaks, and active community challenges ("Zero Vampire Draw", "Cycle Commuter").
6. **Gemini Green Coach**: Client-side Retrofit integrations executing securely using a 60-second connection timeout, pulling customized actionable roadmaps straight from Gemini 3.5 Flash.
7. **Learning Hub & Quiz**: Three standalone topics detailing atmospheric contortion physics, nitrogen runoffs, and vampire watt drafts, awarding immediate points on perfect assessments.

---

## 🔒 Security & Accessibility Compliance

*   **OWASP Mobile Top 10**: Implements robust input validation on emails/passwords, simulates token credential expiration checking, blocks brute force attempts via rate limit counters, and maintains secure storage boundaries.
*   **Accessibility (WCAG 2.2)**: Enforces 48dp absolute minimum clickable touch targets, detailed `contentDescription` vectors on decorative components, dark theme native eye-protection support, and high contrast typography.

---

## ⚙️ Running & Deploying the App

### 1. Requirements
*   Android Studio Ladybug (or newer JVM compilation environments).
*   JDK 17.

### 2. Active Variables setup (.env)
Input your API key securely into Google AI Studio's **Secrets Panel** (which is dynamically exposed to compilation tasks) or configure your `.env` file locally:
```env
GEMINI_API_KEY=YOUR_SECURE_GEMINI_API_KEY
```

### 3. Gradle Tasks
Run the compiling task on your terminal environment:
```bash
# Compile and build the native debug APK
gradle :app:assembleDebug

# Execute all local JVM tests
gradle :app:testDebugUnitTest
```

---

## ⚡ Backend microservice (TypeScript Container)
A containerized Node.js TypeScript REST backend is fully defined in the `/backend` directory.

```bash
# Navigate to the backend target directory
cd backend

# Fire up multi-container orchestrations with docker-compose
docker-compose up --build -d
```
This runs the node API port on `3000`, spinning up an isolated PostgreSQL instance matching the schema database indexes and high performance Redis speed caching layers automatically.
