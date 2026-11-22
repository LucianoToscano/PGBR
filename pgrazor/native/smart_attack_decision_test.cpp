#include "smart_attack_decision.h"
#include <cassert>
#include <cmath>
#include <cstdio>
#include <limits>
using namespace pgr_smart_attack;
static bool near(double a,double b) { return std::abs(a-b)<1e-9; }
int main() {
    Move moves[] = {{101, 11, Kind::CHARGED, 80, 40, Availability::READY},
                    {102, 1, Kind::CHARGED, 80, 40, Availability::READY},
                    {103, 10, Kind::FAST, 5, 8, Availability::READY}};
    Snapshot s; s.enabled=true; s.active_id=1; s.opponent_id=2; s.active_types[0]=11;
    s.opponent_types[0]=10; s.energy=100; s.energy_cap=100; s.stab=1.2; s.moves=moves; s.count=3;
    auto d=decide(s);
    assert(d.status==Status::RECOMMENDED && d.ideal.id==101 && d.recommended.id==101);
    assert(near(d.recommended.effectiveness,1.6) && near(d.recommended.stab,1.2)); // super > neutral, STAB
    assert(near(d.recommended.adjusted_power,80*1.6*1.2));
    s.opponent_types[0]=11; d=decide(s);
    assert(d.recommended.id==102 && near(d.ideal.effectiveness,1)); // neutral > resisted
    s.opponent_types[0]=12; s.opponent_types[1]=16; d=decide(s);
    assert(d.recommended.id==102); // water double resisted, normal neutral
    moves[1].availability=Availability::NOT_READY; d=decide(s);
    assert(d.ideal.id==102 && d.recommended.id==101 && d.reason==Reason::FALLBACK_READY);
    assert(near(d.recommended.effectiveness,0.390625));
    s.opponent_types[0]=5; s.opponent_types[1]=6; moves[1].availability=Availability::READY;
    d=decide(s); assert(d.recommended.id==101 && near(d.recommended.effectiveness,2.56));
    s.energy=0; d=decide(s);
    assert(d.ideal.id==101 && d.ideal.blocked==Blocked::INSUFFICIENT_ENERGY && d.recommended.id==103);
    moves[2].availability=Availability::NOT_READY; d=decide(s);
    assert(d.status==Status::NO_READY_MOVE && d.recommended.id==0);
    s.energy=100; moves[0].availability=Availability::UNKNOWN; d=decide(s);
    assert(d.ideal.blocked==Blocked::UNKNOWN && d.recommended.id==102);
    moves[0].availability=Availability::READY; moves[1].availability=Availability::NOT_READY;
    d=decide(s); assert(d.recommended.id==101); // just one available
    s.opponent_id=3; s.opponent_types[0]=11; s.opponent_types[1]=0;
    moves[1].availability=Availability::READY; d=decide(s); assert(d.recommended.id==102); // new opponent, no cache
    s.opponent_types[0]=0; d=decide(s); assert(d.status==Status::INVALID_DATA && !d.recommended.id);
    s.enabled=false; d=decide(s); assert(d.status==Status::DISABLED && !d.recommended.id);
    s.enabled=true; s.opponent_types[0]=10;
    moves[0].power=std::numeric_limits<double>::quiet_NaN(); assert(decide(s).status==Status::INVALID_DATA);
    moves[0].power=80; moves[1].id=101; assert(decide(s).status==Status::INVALID_DATA);
    moves[1].id=102; s.active_types[1]=11; assert(decide(s).status==Status::INVALID_DATA);
    s.active_types[1]=0; s.count=0; assert(decide(s).status==Status::INVALID_DATA);
    // Same effectiveness: compare adjusted power / energy, including STAB.
    s.count=2; s.opponent_types[0]=1; moves[1].type=10; moves[1].power=90;
    moves[0].power=80; moves[0].energy=40; moves[1].energy=40;
    d=decide(s); assert(d.recommended.id==101); // 96 with STAB > 90
    moves[1].energy=30; d=decide(s); assert(d.recommended.id==102);
    // Single fast-only snapshot is a supported fallback.
    s.moves=&moves[2]; s.count=1; moves[2].availability=Availability::READY;
    d=decide(s); assert(d.ideal.id==103 && d.recommended.id==103);
    std::puts("SmartAttackDecisionTest passed");
}
