# 📖 Protocol Development History & Branch Timeline

This document records the chronological development history of the **NEXPAD Protocol** module across its development branches.

```
   Branch 01      Branch 02      Branch 03      Branch 04      Branch 05      Branch 06      Branch 07      Branch 08      Branch 09
  (KMP Genesis)──► (Main)   ──► (Sensors)  ──► (AOA IPC)  ──► (NXPRC DOM)──► (Fixes)    ──► (Parity)   ──► (Categories)──► (Timeline)
```

---

## 📅 Chronological Branch Milestone Index

| # | Date & Time | Branch Name | Key Breakthroughs & Architectural Deliverables |
|:---:|:---:|---|---|
| **01** | `2026-08-15 22:28` | [`feature/kmp-protocol-migration`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/feature/kmp-protocol-migration) | **KMP Protocol Genesis**: Created shared Kotlin Multiplatform library bridging Android and Windows JVM desktop runtimes with unified binary buffer definitions. |
| **02** | `2026-08-15 22:33` | [`main`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/main) | **Production Baseline**: Stabilized 44-byte binary packet serialization and deserialization with strict boundary enforcement. |
| **03** | `2026-08-24 16:13` | [`feature/gravity-sensor-support`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/feature/gravity-sensor-support) | **6-Axis Motion Data Protocol**: Integrated accelerometer, gyroscope steering, and CemuHook DSU motion packet structures. |
| **04** | `2026-09-04 21:30` | [`feature/aoa-bugfixes-v2`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/feature/aoa-bugfixes-v2) | **AOA & Named Pipe IPC**: Formulated IPC message schemas for zero-driver WinUSB driver swapping and AOA accessory handshakes. |
| **05** | `2026-09-10 01:25` | [`feature/nxprc-html-css-engine`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/feature/nxprc-html-css-engine) | **NXPRC DOM/CSS Compiler**: Multiplatform DOM parser and CSS cascade resolver compiling HTML/CSS buttons into `.nxprc` binary bundles. |
| **06** | `2026-09-11 02:40` | [`fix/nxprc-engine-bugs`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/fix/nxprc-engine-bugs) | **Advanced Shader Mechanics**: Directional conic sweep start angles (`from 215deg`), elliptical radial aspect ratios, and nested stacking context recursion. |
| **07** | `2026-09-11 10:56` | [`test/nxprc-engine-testing`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/test/nxprc-engine-testing) | **Pixel Parity Verification Suite**: Integrated Headless Chrome pixel-by-pixel differential comparison engine verifying rendering accuracy. |
| **08** | `2026-09-11 12:15` | [`test/nxprc-engine-testing-v2`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/test/nxprc-engine-testing-v2) | **6-Category Gamepad Certification**: Visual audit across ABXY Buttons, D-Pad, Triggers, Bumpers, Joysticks, and System Controls. |
| **09** | `2026-09-12 09:05` | [`test/nxprc-engine-testing-v3`](https://github.com/parmarsanket/NEXPAD-Protocol/tree/test/nxprc-engine-testing-v3) | **Universal Timeline Track Engine & SVG**: Dynamic property timeline tracks, all 150 W3C named colors, and modular SVG geometry parsers. |

---

## 🔍 How to Browse Historical Code
To checkout any historical branch locally:
```bash
git checkout feature/nxprc-html-css-engine
```
Or view all branches on GitHub: [github.com/parmarsanket/NEXPAD-Protocol/branches](https://github.com/parmarsanket/NEXPAD-Protocol/branches).
