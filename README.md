# ⚡ NEXPAD Protocol (Kotlin Multiplatform Engine)

The universal, zero-allocation protocol and vector rendering foundation powering the **NEXPAD** cross-platform ecosystem across Android, Windows Desktop, and embedded devices.

```
                    ┌───────────────┐
                    │   README.md   │
                    │ "What is it?" │
                    └───────┬───────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
     HISTORY.md         ROADMAP.md       ARCHITECTURE.md
     "Where we         "Where we're       "How it
      came from"          going"           works"
          │
          ▼
      Git Tags
   (v0.1 ──► v0.9)
```

---

## 🧭 Documentation Pillars

Explore the complete design and evolution of the NEXPAD Protocol:

- 📖 **[`HISTORY.md`](./HISTORY.md)** — **Where We Came From**: Chronological timeline of all releases from `v0.1` (KMP Genesis) to `v0.9` (Universal Timeline Track Engine).
- 🗺️ **[`ROADMAP.md`](./ROADMAP.md)** — **Where We're Going**: Upcoming milestones, zero-copy packet pipelines, and community standards.
- 🏛️ **[`ARCHITECTURE.md`](./ARCHITECTURE.md)** — **How It Works**: Deep technical blueprints, binary schemas, DOM compiler, and GPU matrix shaders.

---

## 🚀 Core Capabilities

1. **Ultra-Low Latency Binary Protocol**:
   - 44-byte raw binary buffer encoding for full Xbox 360 controller states ($1000\,\text{Hz}$ capable).
   - 8-byte bidirectional motor speed vibration packets (`GamepadFeedback`).
2. **NXPRC Vector Runtime Engine**:
   - Compiles HTML/CSS DOM and SVG vector paths into compact `.nxprc` binary bundles.
   - All 150 standard W3C CSS colors represented as strongly-typed `NamedColor` constants.
3. **Universal Keyframe Timeline Track Engine**:
   - Evaluates arbitrary user CSS `@keyframes` and SVG animations via dynamic property tracks (`SCALE`, `ROTATION`, `OPACITY`, `TRANSLATE`, `HUE_ROTATE`) at $120\,\text{FPS}$ with zero per-frame memory allocation.
4. **Zero-Driver USB Support**:
   - Defines Android Open Accessory (AOA) protocol handshakes and named pipe IPC structures for programmatic WinUSB swapping.

---

## 🛠️ Build & Verification

```bash
# Compile and run unit test suites
./gradlew compileKotlinJvm jvmTest

# Publish protocol artifact to local Maven cache
./gradlew publishToMavenLocal
```
