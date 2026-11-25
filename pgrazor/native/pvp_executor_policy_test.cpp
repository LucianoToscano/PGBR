// Host-only fixture for the shared battle executor suspend/resume policy (issue #105).
//
// The regression this guards: a transient inability to verify the battle scope used to
// destroy the verified executor, so Auto ATK stopped after a battle and only a restart
// brought it back. Suspension must withhold every action while never leaking one.
#include "pvp_executor_policy.h"

#include <cstdio>

namespace {
int failures = 0;
void check(bool ok, const char *what) {
    if (!ok) {
        std::printf("FAIL %s\n", what);
        ++failures;
    }
}
using pgr_pvp_exec::Action;
using pgr_pvp_exec::Classifier;
using pgr_pvp_exec::decide;

void healthy_battle_keeps_acting() {
    const Action a = decide(false, true, true, Classifier::CLEAR);
    check(a == Action::KEEP, "verified scope + clear classifier keeps acting");
    check(pgr_pvp_exec::actions_allowed(a), "only KEEP allows an invocation");
}

void stopping_or_failed_is_authoritative() {
    check(decide(true, true, true, Classifier::CLEAR) == Action::LEAVE,
          "host stop/failure leaves");
    check(decide(true, true, false, Classifier::UNREADABLE) == Action::LEAVE,
          "a terminal host never suspends");
    check(!pgr_pvp_exec::actions_allowed(decide(true, true, true, Classifier::CLEAR)),
          "a terminal host never acts");
}

void expired_scene_is_authoritative() {
    check(decide(false, false, true, Classifier::CLEAR) == Action::LEAVE,
          "an expired typed scene leaves");
    check(!pgr_pvp_exec::actions_allowed(decide(false, false, true, Classifier::CLEAR)),
          "an expired scene never acts");
}

void unreadable_classifier_suspends_instead_of_dying() {
    const Action a = decide(false, true, true, Classifier::UNREADABLE);
    check(a == Action::SUSPEND, "an unreadable PvE classifier suspends, not leaves");
    check(!pgr_pvp_exec::actions_allowed(a), "suspension never invokes the game");
    // The same battle resumes without a new Hub/Preparation/VNext sequence.
    check(decide(false, true, true, Classifier::CLEAR) == Action::KEEP,
          "the same scope resumes when the classifier is readable again");
}

void a_real_pve_scope_still_leaves() {
    const Action a = decide(false, true, true, Classifier::PVE_ACTIVE);
    check(a == Action::LEAVE, "raid/gym/Max scope always leaves");
    check(!pgr_pvp_exec::actions_allowed(a), "a PvE scope is never acted in");
}

void toggling_the_assistant_off_suspends_and_rearms() {
    check(decide(false, true, false, Classifier::CLEAR) == Action::SUSPEND,
          "turning the assistant off suspends the same battle");
    check(decide(false, true, true, Classifier::CLEAR) == Action::KEEP,
          "turning it back on re-arms without a new battle entry");
}

void suspension_outranks_nothing_else() {
    // Even with the user policy live, unreadable data means "do nothing".
    check(pgr_pvp_exec::actions_allowed(
              decide(false, true, true, Classifier::UNREADABLE)) == false,
          "policy live + unreadable classifier still withholds actions");
}

void leave_reasons_are_sanitized() {
    check(decide(false, true, true, Classifier::PVE_ACTIVE) == Action::LEAVE, "pve leaves");
    const char *reason = pgr_pvp_exec::leave_reason(false, true, Classifier::PVE_ACTIVE);
    check(reason != nullptr, "a leave reason exists");
    for (const char *p = reason; p && *p; ++p)
        check(*p == '-' || (*p >= 'a' && *p <= 'z'), "reason is a lowercase diagnostic tag");
}
}  // namespace

int main() {
    healthy_battle_keeps_acting();
    stopping_or_failed_is_authoritative();
    expired_scene_is_authoritative();
    unreadable_classifier_suspends_instead_of_dying();
    a_real_pve_scope_still_leaves();
    toggling_the_assistant_off_suspends_and_rearms();
    suspension_outranks_nothing_else();
    leave_reasons_are_sanitized();
    if (failures) {
        std::printf("%d failure(s)\n", failures);
        return 1;
    }
    std::puts("PASS PvP executor policy: authoritative ends leave, transient gaps "
              "suspend, only KEEP invokes the game");
    return 0;
}
