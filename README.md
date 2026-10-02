# ⚡ NEXPAD Protocol — Kotlin Multiplatform Engine

[![Platform](https://img.shields.io/badge/Platform-Kotlin%20Multiplatform%20(Android%20%2B%20JVM)-7F52FF.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Packet Size](https://img.shields.io/badge/Payload-44%20Bytes%20Binary%20(Zero--Allocation)-00E5FF.svg)](#-binary-packet-specifications)
[![Throughput](https://img.shields.io/badge/Sampling%20Rate-1000Hz%20(1ms)-3FD25A.svg)](#-key-capabilities)
[![Vector Engine](https://img.shields.io/badge/Vector%20Engine-NXPRC%20120%20FPS-E0A03F.svg)](#-nxprc-vector-runtime--timeline-track-engine)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)
[![Code Standard](https://img.shields.io/badge/Code%20Standard-Zero%20%40Suppress%20Guarantee-purple.svg)](#-developer--contributor-guide)

The **NEXPAD Protocol** module is the universal, zero-allocation binary protocol and vector rendering foundation powering the **NEXPAD** cross-platform ecosystem across Android, Windows Desktop, and embedded devices.

Built as a pure **Kotlin Multiplatform (KMP)** library, it provides mathematically rigorous serialization for 1000Hz controller input packets, bidirectional haptic rumble feedback, and the **NXPRC Vector Runtime Engine** that compiles and animates custom procedural controller skins at 120 FPS.

---

## 🧭 Subsystem Navigation

```
                       ┌────────────────────────┐
                       │   protocol/README.md   │
                       │     (Shared Engine)    │
                       └───────────┬────────────┘
                                   │
          ┌────────────────────────┼────────────────────────┐
          ▼                        ▼                        ▼
     HISTORY.md               ROADMAP.md             ARCHITECTURE.md
   "Where We Came From"     "Where We're Going"       "How It Works"
```

- 📖 **[`HISTORY.md`](./HISTORY.md)** — Chronological record of protocol revisions, OOP refactors, and packet optimizations.
- 🗺️ **[`ROADMAP.md`](./ROADMAP.md)** — Future enhancements including zero-copy Native/C interop, WebAssembly targets, and Bluetooth LE HID packet schemas.
- 🏛️ **[`ARCHITECTURE.md`](./ARCHITECTURE.md)** — Bit-level binary serialization layouts, DOM compiler tokenizers, and 120 FPS timeline track math.
- 📄 **[`NOTICE`](./NOTICE)** — Attributions and standard notices.

---

## 🚀 Key Capabilities

### 1. ⚡ Ultra-Low Latency Binary Protocol (1000Hz)
- Serializes full Xbox 360 controller states into an ultra-compact **44-byte binary payload**.
- Non-allocating byte-buffer mutations guarantee zero garbage collection pressure even when operating at a sustained **1000Hz ($1\,\text{ms}$)** polling rate.
- Bidirectional **8-byte motor feedback** packets (`GamepadFeedback`) transport high-frequency and low-frequency rumble amplitudes from PC games to mobile haptic engines.

### 2. 🎨 NXPRC Vector Runtime & DOM Compiler
- Compiles vector UI elements, HTML/CSS DOM trees, and SVG paths into compact `.nxprc` binary bundles.
- Includes strongly-typed `NamedColor` constants for all **150 standard W3C CSS colors**, eliminating runtime string parsing.
- Dynamic schema validation via `NxprcInputValidator.kt` prevents malformed layouts from crashing the rendering thread.

### 3. ⏱️ Universal Keyframe Timeline Track Engine
- Evaluates user CSS `@keyframes` and SVG transitions across dynamic property tracks:
  - `SCALE`
  - `ROTATION`
  - `OPACITY`
  - `TRANSLATE_X` & `TRANSLATE_Y`
  - `HUE_ROTATE`
- Executes continuous mathematical interpolations at **120 FPS** with deterministic harmonic spring damping.

---

## 📦 Binary Packet Specifications

### 1. Gamepad Input Report (44-Byte Binary Buffer)
The primary packet streamed from NEXPAD Mobile to NEXPAD Desktop:

| Offset (Bytes) | Field Name | Data Type | Value Range / Description |
|---|---|---|---|
| `0..3` | `MAGIC_HEADER` | `UInt32` | Protocol synchronization header (`0x4E585044` = ASCII "NXPD") |
| `4..7` | `SEQUENCE_ID` | `UInt32` | Monotonically increasing sequence number for jitter tracking |
| `8..9` | `DIGITAL_BUTTONS`| `UInt16` | Bitmask for A, B, X, Y, LB, RB, Back, Start, LS Click, RS Click |
| `10` | `DPAD_MASK` | `UInt8` | Bitmask for Up, Down, Left, Right cardinal directions |
| `11` | `TRIGGER_LEFT` | `UInt8` | Left trigger (LT) analog depression ($0\dots 255$) |
| `12` | `TRIGGER_RIGHT`| `UInt8` | Right trigger (RT) analog depression ($0\dots 255$) |
| `13..14`| `THUMB_LX` | `Int16` | Left stick X-axis ($-32768\dots 32767$) |
| `15..16`| `THUMB_LY` | `Int16` | Left stick Y-axis ($-32768\dots 32767$) |
| `17..18`| `THUMB_RX` | `Int16` | Right stick X-axis ($-32768\dots 32767$) |
| `19..20`| `THUMB_RY` | `Int16` | Right stick Y-axis ($-32768\dots 32767$) |
| `21..32`| `GYRO_DATA` | `Float32[3]` | 3-axis angular velocity in radians/sec ($X, Y, Z$) |
| `33..43`| `ACCEL_DATA` | `Float32[3]` | 3-axis linear acceleration in $m/s^2$ ($X, Y, Z$) |

### 2. Gamepad Feedback Report (8-Byte Haptic Packet)
The feedback packet transmitted from NEXPAD Desktop back to NEXPAD Mobile:

| Offset (Bytes) | Field Name | Data Type | Description |
|---|---|---|---|
| `0..3` | `FEEDBACK_MAGIC`| `UInt32` | Feedback sync header (`0x4E584642` = ASCII "NXFB") |
| `4` | `LEFT_MOTOR` | `UInt8` | Low-frequency large rumble motor speed ($0\dots 255$) |
| `5` | `RIGHT_MOTOR`| `UInt8` | High-frequency small rumble motor speed ($0\dots 255$) |
| `6..7` | `FLAGS_RESERVED`| `UInt16` | LED status indicator and extension flags |

---

## 🛠️ Developer & Contributor Guide

### Library Architecture
```
protocol/src/
├── commonMain/kotlin/com/sanket/tools/nexpad/
│   ├── category/       # CategoryManager, CategoryModels (D-Pad, ABXY, Triggers, Sticks)
│   ├── model/          # GamepadInput, GamepadFeedback, NexpadKeys
│   ├── nxprc/          # NXPRC document models, binary codec, DOM packager, validator
│   │   └── engine/     # Keyframe timeline tracks, token evaluators, color space maps
│   └── protocol/       # NexpadProtocol packet parser and zero-copy byte buffers
└── jvmTest/kotlin/com/sanket/tools/nexpad/
    └── ...             # Automated unit tests and packet round-trip benchmarks
```

### Strict Quality Standards
- **Zero Suppression Rule**: Strictly **0 `@Suppress`** and **0 `@SuppressLint`** across all commonMain and jvmTest sources.
- **Pure Multiplatform**: Zero platform-specific imports in `commonMain`.

### Building and Testing

```powershell
# Clone repository
git clone https://github.com/parmarsanket/nexpad.git
cd nexpad/protocol

# Compile JVM targets
.\gradlew.bat compileKotlinJvm

# Run automated packet tests and benchmarks
.\gradlew.bat jvmTest

# Publish library to local Maven repository (~/.m2/repository)
.\gradlew.bat publishToMavenLocal
```

---

## 📄 License & Attribution

This library is licensed under the **Apache License, Version 2.0**.
- See the full [LICENSE](./LICENSE) file for legal terms.
- Third-party references and notices are detailed in the [NOTICE](./NOTICE) file.

Copyright © 2026 **Sanket Parmar**. All rights reserved.
