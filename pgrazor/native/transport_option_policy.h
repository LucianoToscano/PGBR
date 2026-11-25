#pragma once
// Transport-fed battle policy freshness.
//
// The combat host re-reads the user's battle/rocket policy from the runtime transport
// on every tick. Two consecutive samples must agree before the values are trusted,
// because the adapter writes them while the tick runs. That check is a RACE, not an
// error: a snapshot sequence that moved between the two reads is the normal case when
// the pilot publishes during a battle transition.
//
// Treating that race as "invalid" and revoking the whole policy zeroed the masks, so
// the executor stopped mid-battle / between battles and only a fresh pilot publish
// brought it back (issue #105 symptom: Auto ATK stops after a battle and needs a
// restart). The same happened when the transport reported a momentarily stale
// snapshot while the game unloaded the battle scene.
//
// Correct behaviour, still fail-closed:
//   * a transient inconsistency is IGNORED for a bounded grace window;
//   * during the grace the previously accepted options stay in force, but they are
//     already bounded by their own short lease (<= 2500 ms), so nothing can run on a
//     stale policy indefinitely;
//   * an inconsistency that persists past the grace revokes the policy;
//   * a definitively malformed value is never tolerated here (the caller keeps its own
//     out-of-range handling and disarms immediately).
//
// Android-free: covered by tools/native_host_tests.py.
#include <cstdint>

namespace pgr_transport_options {

// Long enough to ride out a battle-scene transition, far shorter than a lease.
constexpr uint64_t kGraceMs = 1000;

struct Freshness {
  uint64_t first_bad_ms = 0;  // 0 == the last observation was good
  uint32_t bad_reads = 0;

  static bool stable(bool present, bool fresh, bool same_sequence) {
    return present && fresh && same_sequence;
  }

  // Returns true when the caller must revoke (disarm) the policy now.
  // `now_ms` is a monotonic boot clock; 0 means "no clock", which fails closed.
  bool observe(bool good, uint64_t now_ms) {
    if (good) {
      first_bad_ms = 0;
      bad_reads = 0;
      return false;
    }
    ++bad_reads;
    if (now_ms == 0) return true;  // cannot bound the grace without a clock
    if (first_bad_ms == 0 || now_ms < first_bad_ms) first_bad_ms = now_ms;
    return now_ms - first_bad_ms > kGraceMs;
  }

  bool suspended() const { return first_bad_ms != 0; }

  void reset() {
    first_bad_ms = 0;
    bad_reads = 0;
  }
};

}  // namespace pgr_transport_options
