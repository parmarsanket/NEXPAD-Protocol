# 🗺️ Protocol Roadmap & Future Architecture

This document details upcoming architectural tracks for the **NEXPAD Protocol** module.

---

## 🎯 Current Status: `v0.9` (Universal Timeline Track Engine)
- Complete KMP binary protocol (44-byte input, 8-byte rumble feedback).
- Universal keyframe timeline interpolation running at $120\,\text{FPS}$.
- Modular SVG vector geometry parser & all 150 W3C named colors.

---

## 🔮 Planned Milestones

### Milestone `v1.0`: Zero-Copy FlatBuffers & Direct Memory Transport
- [ ] Migrate packet buffers to Zero-Copy FlatBuffers / Unsafe ByteBuffers for direct memory blitting.
- [ ] Direct kernel packet piping for low-overhead Windows driver consumption.
- [ ] End-to-end cryptographic checksum verification for untrusted network transports.

### Milestone `v1.1`: Bluetooth LE HID Specifier
- [ ] Standardized Bluetooth LE GATT service definitions for direct controller pairing without desktop server.
- [ ] Multi-controller identification protocol (supporting up to 4 simultaneous virtual controllers).

### Milestone `v1.2`: WebRTC & Cloud Relay Protocol
- [ ] Remote gaming relay protocol with STUN/TURN traversal.
- [ ] Adaptive Bitrate (ABR) packet throttling based on dynamic network jitter and RTT.

### Milestone `v1.3`: Community Vector Component Exchange
- [ ] Cryptographic signing for third-party `.nxprc` packages.
- [ ] Dynamic component manifest schema for the NEXPAD community skin repository.
