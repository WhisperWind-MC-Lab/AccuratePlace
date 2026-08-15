# Accurate Place

A minimal, server-only implementation of the Easy Place accurate block placement protocol v3 for Minecraft 1.21.1 on NeoForge.

It only decodes placement state encoded in vanilla block interaction packets. Inventory consumption, reach, permissions, collision checks, block updates, and the actual placement remain in Minecraft's normal server-side path.

## Install

Build with Java 21:

```powershell
.\gradlew.bat build
```

Copy `build/libs/accurateplace-0.1.1.jar` to the server's `mods` directory. Clients keep Forgematica and select Easy Place protocol `Version 3` manually. This mod does not advertise itself as Servux and intentionally implements no Servux data-sync channels.

## Safety limits

- Cannot force `powered` or arbitrary `waterlogged` values.
- Cannot request a double slab from one item.
- Applies only while a real server player places a `BlockItem`.
- Bypasses only the protocol-encoded positive X hit offset; vanilla Y/Z hit validation remains active.
- Invalid or unsupported values fall back to vanilla placement state.

The v3 decoding behavior is derived from Servux and Litematica, licensed under LGPL-3.0-or-later.
