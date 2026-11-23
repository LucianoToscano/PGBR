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
    Snapshot s; s.enabled=true; s.mode=Mode::PVP; s.active_id=1; s.opponent_id=2; s.active_types[0]=11;
    s.opponent_types[0]=10; s.energy=100; s.energy_cap=100; s.stab=1.2; s.moves=moves; s.count=3;
    auto d=decide(s);
    assert(d.status==Status::RECOMMENDED && d.ideal.id==101 && d.recommended.id==101);
    // Real battle Pokemon identifiers are unsigned 64-bit, including the high bit.
    s.active_id=UINT64_C(0xfedcba9876543210); s.opponent_id=UINT64_C(0x8000000000000001);
    assert(decide(s).status==Status::RECOMMENDED);
    s.active_id=0; assert(decide(s).status==Status::INVALID_DATA);
    s.active_id=UINT64_C(0xfedcba9876543210); s.opponent_id=0;
    assert(decide(s).status==Status::INVALID_DATA);
    s.opponent_id=UINT64_C(0x8000000000000001);
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
    // PvP prioritizes adjusted power per energy, including STAB.
    s.count=2; s.opponent_types[0]=1; moves[1].type=10; moves[1].power=90;
    moves[0].power=80; moves[0].energy=40; moves[1].energy=40;
    d=decide(s); assert(d.recommended.id==101); // 96 with STAB > 90
    moves[1].energy=30; d=decide(s); assert(d.recommended.id==102);
    // Single fast-only snapshot is a supported fallback.
    s.moves=&moves[2]; s.count=1; moves[2].availability=Availability::READY;
    d=decide(s); assert(d.ideal.id==103 && d.recommended.id==103);
    // A weak super-effective hit must not outrank a much stronger neutral hit.
    Move compare[] = {{201,11,Kind::CHARGED,20,35,Availability::READY,2},
                      {202,1,Kind::CHARGED,100,60,Availability::READY,2},
                      {203,10,Kind::FAST,8,10,Availability::READY,1}};
    s.moves=compare; s.count=3; s.active_types[0]=10; s.opponent_types[0]=10;
    s.energy=100; s.mode=Mode::PVP;
    d=decide(s); assert(d.recommended.id==202 && near(d.ideal.adjusted_power,100));
    assert(near(20*1.6,32)); // super effectiveness alone no longer wins
    compare[1].availability=Availability::NOT_READY; d=decide(s);
    assert(d.ideal.id==202 && d.recommended.id==201 && d.reason==Reason::FALLBACK_READY);
    compare[1].availability=Availability::UNKNOWN; d=decide(s); assert(d.recommended.id==201);
    compare[1].availability=Availability::READY; s.energy=40; d=decide(s);
    assert(d.ideal.id==202 && d.ideal.blocked==Blocked::INSUFFICIENT_ENERGY && d.recommended.id==201);
    s.energy=100;
    // PvP cost changes ordering even if adjusted power is equal.
    compare[0].power=62.5; compare[0].energy=35; compare[1].power=100; compare[1].energy=60;
    d=decide(s); assert(d.recommended.id==201 && near(d.recommended.score,100.0/35));
    // Full tie is independent of input order; the lowest ID wins.
    compare[0]={201,1,Kind::CHARGED,100,60,Availability::READY,2};
    compare[1]={202,1,Kind::CHARGED,100,60,Availability::READY,2};
    d=decide(s); assert(d.recommended.id==201);
    Move tmp=compare[0]; compare[0]=compare[1]; compare[1]=tmp;
    d=decide(s); assert(d.recommended.id==201);
    // STAB changes PvP ranking, despite a lower base power.
    tmp=compare[0]; compare[0]=compare[1]; compare[1]=tmp; compare[0].energy=35;
    compare[0].type=10; compare[0].power=90; compare[1].power=100;
    s.opponent_types[0]=1; d=decide(s); assert(d.recommended.id==201 && near(d.recommended.adjusted_power,108));
    // Weak super-effective charged move also loses to stronger neutral in PvE with equal costs/durations.
    compare[0]={201,11,Kind::CHARGED,20,40,Availability::READY,2};
    compare[1]={202,1,Kind::CHARGED,100,40,Availability::READY,2};
    compare[2]={203,2,Kind::FAST,8,10,Availability::READY,1};
    s.active_types[0]=10; s.opponent_types[0]=10; s.mode=Mode::PVE;
    d=decide(s); assert(d.recommended.id==202 && near(d.recommended.score,(100+4*8.0)/6));
    compare[0].availability=Availability::NOT_READY;compare[1].availability=Availability::NOT_READY;
    compare[2].availability=Availability::NOT_READY;
    assert(decide(s).status==Status::NO_READY_MOVE);
    compare[2].availability=Availability::READY; assert(decide(s).recommended.id==203);
    // PvE compares sustained cycle efficiency, not PvP immediate power or efficacy tier.
    compare[0]={201,11,Kind::CHARGED,80,35,Availability::READY,1};
    compare[1]={202,1,Kind::CHARGED,100,80,Availability::READY,3};
    compare[2]={203,2,Kind::FAST,8,10,Availability::READY,1};
    s.active_types[0]=1; s.opponent_types[0]=10; s.mode=Mode::PVP;
    d=decide(s); assert(d.recommended.id==201); // 128/35 vs 120/80
    s.mode=Mode::PVE; d=decide(s);
    assert(d.recommended.id==201 && near(d.recommended.score,(128+4*8.0)/5));
    compare[0].power=40; d=decide(s); assert(d.recommended.id==201); // 19.2 vs 16.727...
    compare[0].energy=80; d=decide(s); assert(d.recommended.id==202); // longer charge cycle
    // Same data, different policy: high DPE but slow vs lower DPE but fast.
    compare[0]={201,1,Kind::CHARGED,50,35,Availability::READY,20};
    compare[1]={202,1,Kind::CHARGED,100,80,Availability::READY,1};
    s.mode=Mode::PVP; d=decide(s); assert(d.recommended.id==201);
    s.mode=Mode::PVE; d=decide(s); assert(d.recommended.id==202);
    compare[0].availability=Availability::NOT_READY; d=decide(s); assert(d.recommended.id==202);
    compare[0].availability=Availability::READY; compare[2].duration_seconds=0;
    assert(decide(s).status==Status::INVALID_DATA); // duration required only for PvE
    s.mode=Mode::PVP; assert(decide(s).status==Status::RECOMMENDED);
    s.mode=Mode::UNKNOWN; assert(decide(s).status==Status::INVALID_DATA);
    s.mode=Mode::PVE; s.count=2; assert(decide(s).status==Status::INVALID_DATA); // no fast data
    std::puts("SmartAttackDecisionTest passed");
}
