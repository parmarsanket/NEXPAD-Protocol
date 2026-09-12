# 🏛️ Protocol Architecture Blueprint

A deep technical blueprint of the binary wire formats, DOM parser, CSS cascade resolver, and timeline animation system in the **NEXPAD Protocol**.

---

## 1. Binary Wire Formats

### A. Gamepad Input Packet (44 Bytes, Big-Endian)
Transmitted from Android to PC at $1000\,\text{Hz}$:

```
Offset  Size (Bytes)  Type      Field Name        Description
0x00    4             Int32     Magic             0x4E585044 ("NXPD")
0x04    2             Int16     SequenceNumber    Cyclic packet counter (0..65535)
0x06    2             UInt16    ButtonsBitmask    Bitmask of 16 digital buttons (A, B, X, Y, etc.)
0x08    4             Float32   LeftStickX        Normalized deflection (-1.0 to 1.0)
0x0C    4             Float32   LeftStickY        Normalized deflection (-1.0 to 1.0)
0x10    4             Float32   RightStickX       Normalized deflection (-1.0 to 1.0)
0x14    4             Float32   RightStickY       Normalized deflection (-1.0 to 1.0)
0x18    4             Float32   LeftTrigger       Analog pull depth (0.0 to 1.0)
0x1C    4             Float32   RightTrigger      Analog pull depth (0.0 to 1.0)
0x20    4             Float32   GyroPitch         Angular velocity / orientation
0x24    4             Float32   GyroRoll          Angular velocity / orientation
0x28    4             Float32   GyroYaw           Angular velocity / orientation
```

### B. Gamepad Feedback Packet (8 Bytes)
Transmitted from PC to Android to drive tactile rumble motors:

```
Offset  Size (Bytes)  Type      Field Name        Description
0x00    4             Int32     Magic             0x4642434B ("FBCK")
0x04    1             UInt8     LeftMotorSpeed    Low-frequency heavy rumble (0..255)
0x05    1             UInt8     RightMotorSpeed   High-frequency light rumble (0..255)
0x06    2             UInt16    Reserved          Padding / future LED state
```

---

## 2. NXPRC Vector Pipeline

```
 [HTML / CSS / SVG]
         │
         ▼
 ┌───────────────────────────────────────┐
 │ HtmlDomParser.kt                      │
 │ - Strips scripts/unsafe attributes    │
 │ - Builds DomNode hierarchy            │
 └──────────────────┬────────────────────┘
                    │
                    ▼
 ┌───────────────────────────────────────┐
 │ CssTokenizer.kt & CssSelectorParser.kt│
 │ - Tokenizes rules, media, keyframes   │
 │ - Computes W3C specificity            │
 └──────────────────┬────────────────────┘
                    │
                    ▼
 ┌───────────────────────────────────────┐
 │ CssCascadeResolver.kt                 │
 │ - Resolves inheritance & CSS variables│
 │ - Matches pseudo-classes (:active)    │
 └──────────────────┬────────────────────┘
                    │
                    ▼
 ┌───────────────────────────────────────┐
 │ SvgGeometryParser.kt                  │
 │ - Converts <line>, <circle>, <poly>   │
 │   into unified SVG vector paths       │
 └──────────────────┬────────────────────┘
                    │
                    ▼
 ┌───────────────────────────────────────┐
 │ NxprcCompiler.kt                      │
 │ - Compiles to CanvasLayer AST         │
 │ - Encodes timeline tracks & physics   │
 └──────────────────┬────────────────────┘
                    │
                    ▼
           [.nxprc Binary Bundle]
```

---

## 3. Universal Keyframe Timeline Track Engine

Any arbitrary user `@keyframes` block is compiled into mathematical property tracks:

$$\text{Value}(t) = \text{Value}_A + (\text{Value}_B - \text{Value}_A) \times \text{Easing}\left(\frac{t - f_A}{f_B - f_A}\right)$$

Active tracks (`SCALE`, `ROTATION`, `OPACITY`, `TRANSLATE_X`, `TRANSLATE_Y`, `HUE_ROTATE`) are sampled simultaneously at $120\,\text{FPS}$ by the Compose Canvas renderer with zero runtime allocations.
