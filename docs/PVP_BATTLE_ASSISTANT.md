# PGBR PvP Battle Assistant — implementation boundary

**Status (2026-11-21): policy and settings implemented; no verified in-game PvP adapter.**

The official PGSharp documentation describes *Raid & Gym Battle Assistant*, *Team Rocket Battle Assistant* and *PvP Rank*; it does **not** document an independent GO Battle League combat executor. Strings `rocket.fast`, `rocket.charge`, `rocket.shield` and `battleAssistant` found in the supplied PGSharp package do not prove an interoperable PvP interface.

## Existing code
- `Control.pvpBattleOptions()`: independent persisted 3-bit mask (fast, charged, shield), default zero.
- `Control.pvpBattleAllowed()`: fails closed until a verified, version-specific PvP runtime marks readiness and the host is live.
- `PvpBattleController`: pure policy that outputs action IDs **only** from fresh observations; it does not touch the game.
- UI lives under Settings > Batalhas > PvP. Do not silently reuse Rocket/raid/gym hooks or masks.

## Required adapter before enabling PvP controls
1. Verify the supported installed game build and that each symbol/method and live object belongs to a **PvP** match rather than Rocket or PvE.
2. Observe a nonzero per-match identity, battle active/ended state, input availability, charged readiness, shield prompt/availability and next-Pokémon prompt on the verified Unity owner thread.
3. Provide monotonically timed observations, with a maximum 350 ms observation age, to `PvpBattleController.decide`. Reset on background, match end, loss of lease, version mismatch or unexpected UI.
4. Only dispatch the returned action to a validated, modal-specific UI input. Ensure shield and swap actions are one-shot per prompt; do not send unbounded taps.
5. Add a **versioned** PvP-specific ABI; update both `NativeRuntimeTransport.java` and `runtime/native-transport.js` with tests, instead of editing only `runtime/translated.js` (native-transport-test enforces parity).
6. Set `Control.setPvpBattleReady(true,...)` only after end-to-end verification on device; on any uncertain hook, fail closed and present the reason to the user.

## Tests
`PvpBattleControllerTest` covers scope isolation, fast attack throttle, one charged action per readiness cycle, one shield/swap per prompt, input readiness, stale samples, module readiness and match lifecycle. No integration test for real taps is possible without the target live game version and an instrumented test session. Do not claim PvP auto-combat works until such an end-to-end test passes.
