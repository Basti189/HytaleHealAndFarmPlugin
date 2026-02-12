# TestPlugin – Sleep Heal & Infinite Soil Water

A small **Hytale server test plugin** demonstrating basic ECS usage.  
It automatically heals players after sleeping and keeps farmland permanently watered.

> This project is meant for **learning and experimentation**, not balanced gameplay.

---

## Features

### 🛌 Sleep Heal
- Detects when a player wakes up (`PlayerSleep.MorningWakeUp`).
- If the player's health is **below maximum**, it is restored to 100%.
- Optional chat or notification message can be displayed.

### 🌱 Infinite Soil Water
- Every `TilledSoilBlock` is automatically set to a very distant `wateredUntil` time.
- Prevents farmland from drying out under normal gameplay conditions.
- Does **not** directly modify blocks — only the soil component.

---

## How It Works

### Sleep Healing
- Uses a `RefChangeSystem` on `PlayerSomnolence`.
- Triggers on `onComponentSet`.
- Checks if the new sleep state is `MorningWakeUp`.
- Reads the player’s `EntityStatMap`.
- Calls `maximizeStatValue(Health)` only if current HP < max HP.

### Infinite Soil Water
- Uses `onEntityAdded` and optionally `onComponentSet` for `TilledSoilBlock`.
- Sets `wateredUntil` to `now + 10 years`.
- Avoids `Instant.MAX` to prevent overflow/serialization issues.
- Writes the updated component back using `commandBuffer.putComponent(...)`.

---

## Installation

1. Build the plugin JAR.
2. Place the JAR into the server’s `Mods/` directory.
3. Start or restart the server.

No client-side mod required.

---

## Configuration

Currently none.  
This is a minimal proof-of-concept plugin.

---

## Notes

- Intended for **development and testing**.
- Infinite watering can heavily affect game balance.
- Chat debug messages can be removed or replaced with notifications.
- API names may change between Hytale preview builds.

---

## Compatibility

- Tested with early Hytale ECS server builds.
- Future API updates may require adjustments.

---

## License

MIT (or choose your own)

---

**Goal:**  
Provide a minimal example of reacting to ECS component changes and modifying gameplay mechanics server-side.


