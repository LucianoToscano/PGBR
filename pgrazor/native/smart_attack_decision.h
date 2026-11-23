#pragma once
// Pure, stateless recommendation policy. This file neither observes nor controls the game.
#include "battle_counter.h"
#include <cmath>
#include <cstddef>
#include <cstdint>

namespace pgr_smart_attack {

enum class Kind { FAST, CHARGED };
// Explicit strategy: unknown modes never silently use PvP or PvE rules.
enum class Mode { UNKNOWN, PVP, PVE };
enum class Availability { UNKNOWN, READY, NOT_READY };
enum class Status { DISABLED, INVALID_DATA, NO_READY_MOVE, RECOMMENDED };
enum class Reason { DISABLED, INVALID_DATA, NO_READY_MOVE, IDEAL_READY, FALLBACK_READY };
enum class Blocked { NONE, UNKNOWN, NOT_READY, INSUFFICIENT_ENERGY };

struct Move {
    int id = 0;                 // provided by the caller's versioned catalog
    int type = 0;               // HoloPokemonType (1..18)
    Kind kind = Kind::CHARGED;
    double power = 0;           // base power for this mode, not dealt damage
    int energy = 0;             // charged: cost; fast: energy gained
    Availability availability = Availability::UNKNOWN;
    double duration_seconds = 0; // required for PvE cycle model; supplied by mode-specific catalog
};
struct Snapshot {
    bool enabled = false;
    Mode mode = Mode::UNKNOWN;
    // Runtime battle instance identifiers are uint64_t (e.g. ActivePokemonId), not species IDs.
    // Zero is unknown. Preserve all bits for session/change tracking by future providers.
    std::uint64_t active_id = 0, opponent_id = 0;
    int active_types[2] = {0, 0}, opponent_types[2] = {0, 0};
    int energy = -1, energy_cap = 0; // explicit mode-specific rules
    double stab = 0;                  // explicit mode-specific rules, e.g. 1.2
    const Move* moves = nullptr;
    std::size_t count = 0;            // at most one fast and two charged moves
};
struct Evaluation {
    int id = 0;
    Kind kind = Kind::CHARGED;
    double effectiveness = 0, stab = 0, adjusted_power = 0, efficiency = 0;
    double score = 0; // heuristic: PvP adjusted power per energy; PvE adjusted power per second in a cycle
    Blocked blocked = Blocked::UNKNOWN;
};
struct Decision {
    Status status = Status::INVALID_DATA;
    Reason reason = Reason::INVALID_DATA;
    Evaluation ideal;              // highest-ranked charged move, or fast if no charged moves
    Evaluation recommended;        // only a READY move with sufficient energy
};

inline bool types_valid(const int types[2]) {
    return types[0] >= 1 && types[0] <= 18 && types[1] >= 0 && types[1] <= 18 &&
           types[0] != types[1];
}
inline bool better(const Evaluation& a, const Evaluation& b) {
    if (!b.id) return true;
    // Charged attacks are prioritized over fast attacks, which are the fallback.
    if (a.kind != b.kind) return a.kind == Kind::CHARGED;
    if (a.score != b.score) return a.score > b.score;
    if (a.efficiency != b.efficiency) return a.efficiency > b.efficiency;
    if (a.adjusted_power != b.adjusted_power) return a.adjusted_power > b.adjusted_power;
    return a.id < b.id; // deterministic tie independent of input order
}
inline Decision decide(const Snapshot& s) {
    Decision result;
    if (!s.enabled) { result.status = Status::DISABLED; result.reason = Reason::DISABLED; return result; }
    if ((s.mode != Mode::PVP && s.mode != Mode::PVE) ||
        s.active_id == 0 || s.opponent_id == 0 || !types_valid(s.active_types) ||
        !types_valid(s.opponent_types) || s.energy_cap <= 0 || s.energy_cap > 1000 ||
        s.energy < 0 || s.energy > s.energy_cap || !std::isfinite(s.stab) ||
        s.stab < 1 || s.stab > 2 || !s.moves || s.count == 0 || s.count > 3) return result;
    int fast_count = 0, charged_count = 0;
    const Move* fast = nullptr;
    // Validate the complete snapshot first; never silently rank a partial catalog.
    for (std::size_t i = 0; i < s.count; ++i) {
        const Move& m = s.moves[i];
        if (m.id <= 0 || m.type < 1 || m.type > 18 || !std::isfinite(m.power) ||
            m.power <= 0 || m.power > 10000 || m.energy <= 0 || m.energy > s.energy_cap ||
            (s.mode == Mode::PVE && (!std::isfinite(m.duration_seconds) ||
             m.duration_seconds < 0.001 || m.duration_seconds > 120)) ||
            (m.kind != Kind::FAST && m.kind != Kind::CHARGED) ||
            (m.availability != Availability::READY && m.availability != Availability::NOT_READY &&
             m.availability != Availability::UNKNOWN)) return result;
        if (m.kind == Kind::FAST) { ++fast_count; fast = &m; } else ++charged_count;
        for (std::size_t j = 0; j < i; ++j) if (s.moves[j].id == m.id) return result;
    }
    if (fast_count > 1 || charged_count > 2 || (s.mode == Mode::PVE && !fast)) return result;
    // A PvE cycle assumes this fast move is repeatedly usable; it is an estimate,
    // not a forecast of readiness, exact damage, or a guarantee of game behavior.
    double fast_power = 0;
    if (s.mode == Mode::PVE) {
        const double fast_stab = (fast->type == s.active_types[0] || fast->type == s.active_types[1]) ? s.stab : 1.0;
        fast_power = fast->power * fast_stab * pgr_counter::effectiveness(fast->type, s.opponent_types);
    }
    for (std::size_t i = 0; i < s.count; ++i) {
        const Move& m = s.moves[i];
        Evaluation e;
        e.id = m.id; e.kind = m.kind;
        e.effectiveness = pgr_counter::effectiveness(m.type, s.opponent_types);
        e.stab = (m.type == s.active_types[0] || m.type == s.active_types[1]) ? s.stab : 1.0;
        e.adjusted_power = m.power * e.stab * e.effectiveness;
        e.efficiency = m.kind == Kind::CHARGED ? e.adjusted_power / m.energy : e.adjusted_power;
        if (s.mode == Mode::PVP) e.score = e.efficiency;
        else if (m.kind == Kind::FAST) e.score = e.adjusted_power / m.duration_seconds;
        else {
            const int fasts = (m.energy + fast->energy - 1) / fast->energy;
            e.score = (e.adjusted_power + fasts * fast_power) /
                      (m.duration_seconds + fasts * fast->duration_seconds);
        }
        e.blocked = m.availability == Availability::UNKNOWN ? Blocked::UNKNOWN :
                    m.availability == Availability::NOT_READY ? Blocked::NOT_READY :
                    m.kind == Kind::CHARGED && s.energy < m.energy ? Blocked::INSUFFICIENT_ENERGY : Blocked::NONE;
        if (better(e, result.ideal)) result.ideal = e;
        if (e.blocked == Blocked::NONE && better(e, result.recommended)) result.recommended = e;
    }
    if (!result.recommended.id) { result.status = Status::NO_READY_MOVE; result.reason = Reason::NO_READY_MOVE; }
    else { result.status = Status::RECOMMENDED; result.reason = result.ideal.id == result.recommended.id ? Reason::IDEAL_READY : Reason::FALLBACK_READY; }
    return result;
}
} // namespace pgr_smart_attack
