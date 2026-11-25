#pragma once
// Pure geometry policy for the Rocket charged-attack tap assist (feature 6).
//
// The assist may ONLY enlarge the tap-collider geometry (BoxCollider.size /
// SphereCollider.radius) of a collider class the host resolved by exact metadata.
// It never writes Transform.localScale: scaling the transform of the GameObject that
// owns the tap collider also scales whatever that object renders, which is the
// reported white screen when the geometry is not recognised (issue #192 / PR #193).
//
// Therefore every unknown, unbound, partial, non-finite or out-of-range input makes
// this function return ok == false and the caller MUST NOT mutate anything: the game
// keeps its original presentation and the player keeps manual input. The enlargement
// factor is also bounded, so a hostile/corrupt snapshot can never produce a collider
// that covers the world.
//
// This header is Android-free so the exact same arithmetic is covered by the host
// fixture (tools/native_host_tests.py) and by the on-device harness.
#include <cmath>
#include <cstdint>

namespace pgr_rocket_shape {

constexpr int kMaxAxes = 3;
// Historical assist magnitude: a tap anywhere on screen still reaches the target.
constexpr float kEnlargeTimes = 1000.0f;
// Absolute ceiling for one enlarged axis, in local units (mirrors the old guard and
// documents that the assist is a geometry edit, not a world-scale change).
constexpr float kMaxEnlarged = 100000.0f;
// A component smaller than this cannot be compensated safely; treat it as unknown.
constexpr float kMinAxisScale = 0.001f;
// The transform snapshot already rejects |scale| >= 10000; keep both in agreement.
constexpr float kMaxAxisScale = 10000.0f;
constexpr float kMaxDimension = 1000000.0f;

inline bool usable(float value, float limit) {
  return std::isfinite(value) && value > 0.0f && value < limit;
}

struct Input {
  bool sphere = false;             // SphereCollider exposes one radius axis
  int axes = kMaxAxes;             // BoxCollider exposes three size axes
  float scale[kMaxAxes] = {0, 0, 0};        // snapped Transform.localScale (may be negative)
  float dimensions[kMaxAxes] = {0, 0, 0};   // snapped collider size/radius (local units, > 0)
  bool geometry_bound = true;      // the host resolved this collider class exactly
};

// Read the exact payload width of the validated managed value.
inline void copy_dimensions(bool sphere, const float *value, float (&out)[kMaxAxes]) {
  out[0] = value[0];
  out[1] = sphere ? 0.f : value[1];
  out[2] = sphere ? 0.f : value[2];
}

struct Result {
  bool ok = false;                 // true only for a bounded, finite enlargement
  int axes = 0;
  float enlarged[kMaxAxes] = {0, 0, 0};
};

// Computes the enlarged collider dimensions. Never returns a partially valid result:
// either every used axis is finite, positive and within kMaxEnlarged, or ok is false.
inline Result enlarge(const Input &in) {
  Result out;
  if (!in.geometry_bound) return out;
  const int axes = in.sphere ? 1 : in.axes;
  if (axes < 1 || axes > kMaxAxes) return out;

  float factor[kMaxAxes] = {0, 0, 0};
  float magnitude[kMaxAxes] = {0, 0, 0};
  for (int i = 0; i < kMaxAxes; ++i) {
    // Reject NaN/infinite scales outright; std::fabs(NaN) is NaN and fails the range.
    const float mag = std::fabs(in.scale[i]);
    if (!(mag >= kMinAxisScale) || !(mag < kMaxAxisScale)) return out;
    magnitude[i] = mag;
  }
  for (int i = 0; i < axes; ++i) {
    if (!usable(in.dimensions[i], kMaxDimension)) return out;
    const float base = magnitude[i] > 1.0f ? magnitude[i] : 1.0f;
    factor[i] = kEnlargeTimes * base / magnitude[i];
    if (!std::isfinite(factor[i]) || factor[i] < 1.0f) return out;
  }
  if (in.sphere) {
    // A sphere is scaled by the largest magnitude axis of the transform.
    float largest = magnitude[0];
    for (int i = 1; i < kMaxAxes; ++i)
      if (magnitude[i] > largest) largest = magnitude[i];
    if (!(largest >= kMinAxisScale)) return out;
    const float base = largest > 1.0f ? largest : 1.0f;
    factor[0] = kEnlargeTimes * base / largest;
    if (!std::isfinite(factor[0]) || factor[0] < 1.0f) return out;
  }
  for (int i = 0; i < axes; ++i) {
    const float value = in.dimensions[i] * factor[i];
    if (!usable(value, kMaxEnlarged)) return out;
    if (!(value >= in.dimensions[i])) return out;  // the assist never shrinks geometry
    out.enlarged[i] = value;
  }
  out.axes = axes;
  out.ok = true;
  return out;
}

}  // namespace pgr_rocket_shape
