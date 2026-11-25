// Host-only fixture for the transport policy freshness guard (issue #105 class).
#include "transport_option_policy.h"

#include <cstdint>
#include <cstdio>

namespace {
int failures = 0;
void check(bool ok, const char *what) {
    if (!ok) {
        std::printf("FAIL %s\n", what);
        ++failures;
    }
}
using pgr_transport_options::Freshness;
using pgr_transport_options::kGraceMs;

void a_good_read_keeps_the_policy() {
    Freshness f;
    check(!f.observe(true, 1000), "fresh sample keeps the policy");
    check(!f.suspended(), "fresh sample clears the grace");
    check(f.bad_reads == 0, "fresh sample resets the counter");
}

void a_single_race_does_not_revoke() {
    Freshness f;
    check(!f.observe(true, 1000), "start good");
    // The pilot published between the two reads: sequence moved.
    check(!f.observe(false, 1001), "one racing read is tolerated");
    check(f.suspended(), "the race is recorded");
    check(!f.observe(true, 1002), "recovery keeps the policy");
    check(!f.suspended(), "recovery clears the grace");
}

void a_short_outage_is_tolerated_then_repaired() {
    Freshness f;
    check(!f.observe(true, 5000), "start good");
    for (uint64_t t = 5001; t <= 5000 + kGraceMs; ++t)
        check(!f.observe(false, t), "inside the grace the policy survives");
    check(!f.observe(true, 5000 + kGraceMs + 1), "recovery after the grace");
}

void a_persistent_outage_revokes() {
    Freshness f;
    check(!f.observe(true, 9000), "start good");
    check(!f.observe(false, 9001), "first bad read");
    check(!f.observe(false, 9001 + kGraceMs), "still inside the grace boundary");
    check(f.observe(false, 9002 + kGraceMs), "past the grace the policy is revoked");
    check(f.suspended(), "still recorded as suspended");
    check(!f.observe(true, 9003 + kGraceMs), "a later good read stops revoking");
}

void no_clock_fails_closed() {
    Freshness f;
    check(f.observe(false, 0), "a bad read without a clock revokes immediately");
}

void recovery_after_revoke_restarts_cleanly() {
    Freshness f;
    f.observe(false, 1);
    check(f.observe(false, 1 + kGraceMs + 1), "revoked once the grace expires");
    check(!f.observe(true, 2 + kGraceMs), "next good read clears everything");
    check(f.first_bad_ms == 0 && f.bad_reads == 0, "state fully reset");
    check(!f.observe(false, 3 + kGraceMs), "a new race starts a new grace");
}

void sample_agreement_rule() {
    check(Freshness::stable(true, true, true), "agreement requires present+fresh+same seq");
    check(Freshness::stable(false, true, true) == false, "missing sample is not agreement");
    check(Freshness::stable(true, false, true) == false, "stale sample is not agreement");
    check(Freshness::stable(true, true, false) == false, "moved sequence is not agreement");
}
}  // namespace

int main() {
    a_good_read_keeps_the_policy();
    a_single_race_does_not_revoke();
    a_short_outage_is_tolerated_then_repaired();
    a_persistent_outage_revokes();
    no_clock_fails_closed();
    recovery_after_revoke_restarts_cleanly();
    sample_agreement_rule();
    if (failures) {
        std::printf("%d failure(s)\n", failures);
        return 1;
    }
    std::puts("PASS transport option policy: races tolerated inside a bounded grace, "
              "persistent staleness disarms, recovery restarts cleanly");
    return 0;
}
