// Host-only fixture for the Rocket charged-attack tap-assist geometry policy.
//
// It covers the white-screen root cause directly: an unrecognised collider geometry
// must produce no mutation at all (the caller then leaves the visual transform and the
// activation state untouched), and no input may ever produce an unbounded enlargement.
// No Android, no hooks, no managed runtime: this is the same arithmetic the on-device
// collector uses through rocket_minigame.cpp.
#include "rocket_minigame_shape.h"

#include <cmath>
#include <cstdio>
#include <cstring>
#include <limits>

namespace {

int failures = 0;

void check(bool condition, const char *what) {
    if (!condition) {
        std::printf("FAIL %s\n", what);
        ++failures;
    }
}

pgr_rocket_shape::Input box(float sx, float sy, float sz,
                            float dx, float dy, float dz) {
    pgr_rocket_shape::Input in{};
    in.sphere = false;
    in.axes = 3;
    in.geometry_bound = true;
    in.scale[0] = sx; in.scale[1] = sy; in.scale[2] = sz;
    in.dimensions[0] = dx;
    in.dimensions[1] = dy;
    in.dimensions[2] = dz;
    return in;
}

pgr_rocket_shape::Input sphere(float sx, float sy, float sz, float radius) {
    pgr_rocket_shape::Input in{};
    in.sphere = true;
    in.axes = 3;
    in.geometry_bound = true;
    in.scale[0] = sx; in.scale[1] = sy; in.scale[2] = sz;
    in.dimensions[0] = radius;
    return in;
}

void unbound_geometry_never_mutates() {
    auto in = box(1.f, 1.f, 1.f, 2.f, 3.f, 4.f);
    in.geometry_bound = false;
    const auto r = pgr_rocket_shape::enlarge(in);
    check(!r.ok, "unbound geometry is refused");
    for (float value : r.enlarged) check(value == 0.f, "refused result is zeroed");
}

void box_geometry_is_enlarged_not_scaled() {
    const auto r = pgr_rocket_shape::enlarge(box(2.f, -3.f, 0.5f, 2.f, 3.f, 4.f));
    check(r.ok, "verified box geometry is accepted");
    check(r.axes == 3, "box uses three axes");
    // factor = 1000 * max(|s|,1) / |s| -> 1000, 1000 and 2000; a sub-unit scale cannot
    // be compensated by less than the 1000x floor, so its magnitude dominates as before.
    check(std::fabs(r.enlarged[0] - 2000.f) < 0.01f, "axis 0 = 2 * 1000");
    check(std::fabs(r.enlarged[1] - 3000.f) < 0.01f, "axis 1 = 3 * 1000");
    check(std::fabs(r.enlarged[2] - 8000.f) < 0.01f, "axis 2 = 4 * 2000");
    for (int i = 0; i < 3; ++i) check(r.enlarged[i] > 0.f, "enlargement is positive");
}

void unit_scale_is_the_historical_magnitude() {
    const auto r = pgr_rocket_shape::enlarge(box(1.f, 1.f, 1.f, 0.5f, 0.5f, 0.5f));
    check(r.ok, "unit scale accepted");
    for (float value : r.enlarged)
        check(std::fabs(value - 500.f) < 0.01f, "0.5 * 1000 = 500");
}

void sphere_uses_the_largest_scale_axis() {
    const auto r = pgr_rocket_shape::enlarge(sphere(1.f, -3.f, 1.f, 0.25f));
    check(r.ok, "sphere accepted");
    check(r.axes == 1, "sphere exposes one axis");
    // largest |scale| = 3 -> factor 1000 * 3 / 3 = 1000 -> 0.25 * 1000 = 250.
    check(std::fabs(r.enlarged[0] - 250.f) < 0.01f, "sphere radius 250");
    check(r.enlarged[1] == 0.f && r.enlarged[2] == 0.f, "sphere leaves other axes zero");
}

void sphere_uses_nonzero_largest_axis_only() {
    const auto r = pgr_rocket_shape::enlarge(sphere(0.f, 2.f, 0.f, 0.25f));
    check(r.ok && r.axes == 1 && std::fabs(r.enlarged[0] - 250.f) < 0.01f,
          "flattened unused sphere axes accepted without scaling transform");
    check(!pgr_rocket_shape::enlarge(sphere(0.f, 0.f, 0.f, 0.25f)).ok,
          "fully collapsed sphere refused");
    check(!pgr_rocket_shape::enlarge(sphere(0.f, 2.f, std::numeric_limits<float>::quiet_NaN(), 0.25f)).ok,
          "nonfinite sphere axis refused");
}

void invalid_numbers_are_refused() {
    check(!pgr_rocket_shape::enlarge(box(1.f, 1.f, 1.f, 0.f, 1.f, 1.f)).ok,
          "zero dimension refused");
    check(!pgr_rocket_shape::enlarge(box(1.f, 1.f, 1.f, -1.f, 1.f, 1.f)).ok,
          "negative dimension refused");
    check(!pgr_rocket_shape::enlarge(box(1.f, 1.f, 1.f,
                                         std::numeric_limits<float>::quiet_NaN(), 1.f, 1.f)).ok,
          "NaN dimension refused");
    check(!pgr_rocket_shape::enlarge(box(1.f, 1.f, 1.f,
                                         std::numeric_limits<float>::infinity(), 1.f, 1.f)).ok,
          "infinite dimension refused");
    check(!pgr_rocket_shape::enlarge(box(0.f, 1.f, 1.f, 1.f, 1.f, 1.f)).ok,
          "zero scale refused");
    check(!pgr_rocket_shape::enlarge(box(0.0001f, 1.f, 1.f, 1.f, 1.f, 1.f)).ok,
          "tiny scale refused");
    check(!pgr_rocket_shape::enlarge(box(10000.f, 1.f, 1.f, 1.f, 1.f, 1.f)).ok,
          "scale at the snapshot ceiling refused");
    check(!pgr_rocket_shape::enlarge(box(std::numeric_limits<float>::quiet_NaN(), 1.f, 1.f,
                                         1.f, 1.f, 1.f)).ok,
          "NaN scale refused");
    check(!pgr_rocket_shape::enlarge(box(1.f, 1.f, 1.f, 1e7f, 1.f, 1.f)).ok,
          "absurd dimension refused");
    auto no_axes = box(1.f, 1.f, 1.f, 1.f, 1.f, 1.f);
    no_axes.axes = 0;
    check(!pgr_rocket_shape::enlarge(no_axes).ok, "zero axes refused");
    auto too_many = box(1.f, 1.f, 1.f, 1.f, 1.f, 1.f);
    too_many.axes = 4;
    check(!pgr_rocket_shape::enlarge(too_many).ok, "four axes refused");
}

void enlargement_is_always_bounded_and_never_shrinks() {
    // Sweep every combination of accepted scales/dimensions around the boundaries.
    const float scales[] = {0.001f, 0.01f, 0.5f, 1.f, 2.f, 100.f, 9999.f};
    const float dims[] = {0.001f, 0.5f, 1.f, 10.f, 99.f, 100000.f};
    for (float s : scales) {
        for (float d : dims) {
            const auto r = pgr_rocket_shape::enlarge(box(s, s, s, d, d, d));
            if (!r.ok) continue;
            for (int i = 0; i < r.axes; ++i) {
                check(std::isfinite(r.enlarged[i]), "bounded: finite");
                check(r.enlarged[i] > 0.f, "bounded: positive");
                check(r.enlarged[i] <= pgr_rocket_shape::kMaxEnlarged, "bounded: ceiling");
                check(r.enlarged[i] >= d, "bounded: never shrinks");
            }
        }
    }
    // 100 * 1000 exceeds the ceiling and must be refused instead of clamped.
    check(!pgr_rocket_shape::enlarge(box(1.f, 1.f, 1.f, 100.f, 1.f, 1.f)).ok,
          "over-ceiling enlargement refused, not clamped");
}

void result_is_all_or_nothing() {
    const auto r = pgr_rocket_shape::enlarge(box(1.f, 1.f, 1.f, 1.f, 0.f, 1.f));
    check(!r.ok, "one invalid axis invalidates the whole result");
    check(r.axes == 0, "invalid result reports no axes");
    for (float value : r.enlarged) check(value == 0.f, "invalid result never publishes values");
}

}  // namespace

int main() {
    unbound_geometry_never_mutates();
    box_geometry_is_enlarged_not_scaled();
    unit_scale_is_the_historical_magnitude();
    sphere_uses_the_largest_scale_axis();
    sphere_uses_nonzero_largest_axis_only();
    invalid_numbers_are_refused();
    enlargement_is_always_bounded_and_never_shrinks();
    result_is_all_or_nothing();
    if (failures) {
        std::printf("%d failure(s)\n", failures);
        return 1;
    }
    std::puts("PASS rocket minigame shape: collider-only enlargement, unknown geometry "
              "untouched, bounded factor, all-or-nothing result");
    return 0;
}
