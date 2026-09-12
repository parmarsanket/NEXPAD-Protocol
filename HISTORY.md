# 📖 Protocol History & Milestone Timeline

This document records the chronological development history of the **NEXPAD Protocol** module, mapping each development branch to its permanent Git Tag.

```
  v0.1        v0.2        v0.3        v0.4        v0.5        v0.6        v0.7        v0.8        v0.9
 (KMP)    ──►(Main)   ──►(Sensor) ──►(AOA IPC)──►(NXPRC)  ──►(Fixes)  ──►(Parity) ──►(Cats)   ──►(Timeline)
```

---

## 📅 Chronological Milestone Index

| Tag | Date & Time | Original Branch | Key Breakthroughs & Architectural Deliverables |
|:---:|:---:|---|---|
| **[`v0.1`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.1)** | `2026-08-15 22:28` | `feature/kmp-protocol-migration` | **KMP Protocol Genesis**: Created shared Kotlin Multiplatform library bridging Android and Windows JVM desktop runtimes with unified binary buffer definitions. |
| **[`v0.2`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.2)** | `2026-08-15 22:33` | `main` | **Production Baseline**: Stabilized 44-byte binary packet serialization and deserialization with strict boundary enforcement. |
| **[`v0.3`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.3)** | `2026-08-24 16:13` | `feature/gravity-sensor-support` | **6-Axis Motion Data Protocol**: Integrated accelerometer, gyroscope steering, and CemuHook DSU motion packet structures. |
| **[`v0.4`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.4)** | `2026-09-04 21:30` | `feature/aoa-bugfixes-v2` | **AOA & Named Pipe IPC**: Formulated IPC message schemas for zero-driver WinUSB driver swapping and AOA accessory handshakes. |
| **[`v0.5`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.5)** | `2026-09-10 01:25` | `feature/nxprc-html-css-engine` | **NXPRC DOM/CSS Compiler**: Multiplatform DOM parser and CSS cascade resolver compiling HTML/CSS buttons into `.nxprc` binary bundles. |
| **[`v0.6`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.6)** | `2026-09-11 02:40` | `fix/nxprc-engine-bugs` | **Advanced Shader Mechanics**: Directional conic sweep start angles (`from 215deg`), elliptical radial aspect ratios, and nested stacking context recursion. |
| **[`v0.7`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.7)** | `2026-09-11 10:56` | `test/nxprc-engine-testing` | **Pixel Parity Verification Suite**: Integrated Headless Chrome pixel-by-pixel differential comparison engine verifying rendering accuracy. |
| **[`v0.8`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.8)** | `2026-09-11 12:15` | `test/nxprc-engine-testing-v2` | **6-Category Gamepad Certification**: Visual audit across ABXY Buttons, D-Pad, Triggers, Bumpers, Joysticks, and System Controls. |
| **[`v0.9`](https://github.com/parmarsanket/NEXPAD-Protocol/releases/tag/v0.9)** | `2026-09-12 09:05` | `test/nxprc-engine-testing-v3` | **Universal Timeline Track Engine & SVG**: Dynamic property timeline tracks, all 150 W3C named colors, and modular SVG geometry parsers. |

---

## 🔍 How to Browse Historical Code
To checkout any historical milestone locally:
```bash
git checkout v0.5
```
Or view the tag directly on GitHub under the **Tags** tab.
