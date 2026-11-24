#include "map_anchor.h"

#include <atomic>
#include <cstddef>
#include <initializer_list>
#include <cmath>
#include <cstring>
#include <mutex>
#include <time.h>
#include <unistd.h>

// Feature 72 implementation. See map_anchor.h for the contract. This module installs NO hook and never
// mutates or requests gameplay state; it only reads the game's own POI/camera objects on the proven owner
// Unity thread and copies projection scalars into the caller's buffer.

namespace {

using Invoke = void *(*)(void *, void *, void **, void **);
using ClassOf = void *(*)(void *);
using Unbox = void *(*)(void *);
using NewHandle = uintptr_t (*)(void *, int);
using GetHandle = void *(*)(uintptr_t);
using FreeHandle = void (*)(uintptr_t);
using CurrentThread = void *(*)();
using ClassType = void *(*)(void *);
using TypeObject = void *(*)(void *);
using ArrayLen = uintptr_t (*)(void *);
using VirtMethod = void *(*)(void *, void *);
using StringLen = int32_t (*)(void *);
using StringChars = uint16_t *(*)(void *);
using FieldGet = void (*)(void *, void *, void *);

PgrMapAnchorConfig cfg{};
std::mutex install_mutex;
std::atomic<bool> installed{false}, busy{false};
std::atomic<uint64_t> owner{0};

uint64_t boot_ms() {
  timespec t{};
  if (clock_gettime(CLOCK_BOOTTIME, &t) != 0) return 0;
  return static_cast<uint64_t>(t.tv_sec) * 1000 + static_cast<uint64_t>(t.tv_nsec) / 1000000;
}
uint64_t tid_now() { return static_cast<uint64_t>(gettid()); }

struct Root {
  uintptr_t h = 0;
  Root() = default;
  Root(const Root &) = delete;
  Root &operator=(const Root &) = delete;
  Root(Root &&other) noexcept : h(other.h) { other.h = 0; }
  Root &operator=(Root &&other) noexcept {
    if (this != &other) { reset(); h = other.h; other.h = 0; }
    return *this;
  }
  ~Root() { reset(); }
  void reset() {
    if (h) reinterpret_cast<FreeHandle>(cfg.gchandle_free)(h);
    h = 0;
  }
  bool set(void *object) {
    if (!object) return false;
    reset();
    h = reinterpret_cast<NewHandle>(cfg.gchandle_new)(object, 0);
    return h != 0;
  }
  void *get() const { return h ? reinterpret_cast<GetHandle>(cfg.gchandle_get_target)(h) : nullptr; }
};

bool exact(void *object, void *klass) {
  return object && klass && reinterpret_cast<ClassOf>(cfg.object_get_class)(object) == klass;
}

enum CallResult { CALL_OK, CALL_THROW, CALL_GCHANDLE, CALL_DEAD };
CallResult call(void *method, Root *receiver, void **args, Root &out, bool allow_null = false) {
  if (!method) return CALL_THROW;
  void *self = receiver ? receiver->get() : nullptr;
  if (receiver && !self) return CALL_DEAD;
  void *exception = nullptr;
  void *value = reinterpret_cast<Invoke>(cfg.runtime_invoke)(method, self, args, &exception);
  if (exception || !out.set(value)) {
    if (exception) return CALL_THROW;
    if (allow_null && !value) return CALL_OK;
    return CALL_GCHANDLE;
  }
  return CALL_OK;
}

bool unbox(const Root &boxed, void *out, size_t size) {
  void *object = boxed.get();
  if (!object) return false;
  void *value = reinterpret_cast<Unbox>(cfg.object_unbox)(object);
  if (!value) return false;
  std::memcpy(out, value, size);
  return true;
}
bool field_ptr(const Root &object, void *field, void **out) {
  if (!object.get() || !field) return false;
  reinterpret_cast<FieldGet>(cfg.field_get_value)(object.get(), field, out);
  return *out != nullptr;
}
bool field_f64(const Root &object, void *field, double *out) {
  if (!object.get() || !field) return false;
  reinterpret_cast<FieldGet>(cfg.field_get_value)(object.get(), field, out);
  return std::isfinite(*out);
}
bool type_object(void *klass, Root &out, int *err) {
  void *type = klass ? reinterpret_cast<ClassType>(cfg.class_get_type)(klass) : nullptr;
  void *managed = type ? reinterpret_cast<TypeObject>(cfg.type_get_object)(type) : nullptr;
  if (!managed || !out.set(managed)) { *err = PGR_MA_ERR_GCHANDLE; return false; }
  return true;
}
bool within(uint64_t deadline, int *err) {
  uint64_t now = boot_ms();
  if (!now || now >= deadline) { *err = PGR_MA_SKIP; return false; }
  return true;
}
bool owner_ready(uint64_t tid) {
  if (!tid || tid_now() != tid) return false;
  uint64_t expected = 0;
  if (!owner.load(std::memory_order_acquire)) owner.compare_exchange_strong(expected, tid);
  return owner.load(std::memory_order_acquire) == tid;
}
bool same_id(const PgrMapAnchorPoint &p, const uint16_t *chars, uint32_t length) {
  return p.id_length == length && std::memcmp(p.id, chars, length * sizeof(uint16_t)) == 0;
}
bool seen(const PgrMapAnchorOutput *out, const uint16_t *chars, uint32_t length) {
  for (uint32_t i = 0; i < out->count; ++i) if (same_id(out->anchors[i], chars, length)) return true;
  return false;
}
struct Vec3 { float x, y, z; };

int scan_class(void *klass, uint32_t kind, uint64_t deadline, PgrMapAnchorOutput *out) {
  int err = PGR_MA_OK;
  if (!within(deadline, &err)) return err;
  Root type;
  if (!type_object(klass, type, &err)) return err;
  if (!within(deadline, &err)) return err;
  Root array;
  void *args[] = {type.get()};
  if (!type.get()) return PGR_MA_ERR_GCHANDLE;
  switch (call(cfg.m_find, nullptr, args, array, true)) {
    case CALL_THROW: return PGR_MA_ERR_MANAGED;
    case CALL_GCHANDLE:
    case CALL_DEAD: return PGR_MA_ERR_GCHANDLE;
    case CALL_OK: break;
  }
  if (!array.get()) return PGR_MA_OK;
  if (!within(deadline, &err)) return err;
  const uintptr_t length = reinterpret_cast<ArrayLen>(cfg.array_length)(array.get());
  if (length > PGR_MA_MAX_ARRAY) return PGR_MA_ERR_BAD_COUNT;

  for (uintptr_t i = 0; i < length && out->count < PGR_MA_MAX_ANCHORS; ++i) {
    if (!within(deadline, &err)) return err;
    void *base = array.get();  // re-resolve after every managed call (moving GC).
    if (!base) return PGR_MA_ERR_GCHANDLE;
    void *element = nullptr;
    std::memcpy(&element, static_cast<char *>(base) + PGR_MA_ARRAY_DATA_OFF + i * sizeof(void *), sizeof(void *));
    if (!element) continue;
    Root obj;
    if (!obj.set(element)) return PGR_MA_ERR_GCHANDLE;
    if (!exact(obj.get(), klass)) continue;

    {
      Root alive;
      void *a[] = {obj.get()};
      switch (call(cfg.m_alive, nullptr, a, alive)) {
        case CALL_THROW: return PGR_MA_ERR_MANAGED;
        case CALL_GCHANDLE:
        case CALL_DEAD: return PGR_MA_ERR_GCHANDLE;
        case CALL_OK: break;
      }
      if (!exact(alive.get(), cfg.class_bool)) return PGR_MA_ERR_METADATA;
      uint8_t value = 0;
      if (!unbox(alive, &value, sizeof(value))) return PGR_MA_ERR_GCHANDLE;
      if (value > 1) return PGR_MA_ERR_METADATA;
      if (!value) continue;
    }

    if (!within(deadline, &err)) return err;
    Root fort;
    void *fortMethod = reinterpret_cast<VirtMethod>(cfg.object_get_virtual_method)(obj.get(), cfg.m_fort);
    if (!fortMethod) return PGR_MA_ERR_METADATA;
    switch (call(fortMethod, &obj, nullptr, fort, true)) {
      case CALL_THROW: return PGR_MA_ERR_MANAGED;
      case CALL_GCHANDLE:
      case CALL_DEAD: return PGR_MA_ERR_GCHANDLE;
      case CALL_OK: break;
    }
    if (!exact(fort.get(), cfg.class_fort)) continue;

    void *idRaw = nullptr;
    if (!field_ptr(fort, cfg.f_fortId, &idRaw)) continue;
    Root id;
    if (!id.set(idRaw)) return PGR_MA_ERR_GCHANDLE;
    if (!exact(id.get(), cfg.class_string)) continue;
    const int32_t rawLength = reinterpret_cast<StringLen>(cfg.string_length)(id.get());
    if (rawLength < 1 || rawLength > static_cast<int32_t>(PGR_MA_MAX_ID)) continue;
    uint16_t *chars = reinterpret_cast<StringChars>(cfg.string_chars)(id.get());
    if (!chars) continue;
    uint16_t copied[PGR_MA_MAX_ID];
    std::memcpy(copied, chars, static_cast<size_t>(rawLength) * sizeof(uint16_t));
    if (seen(out, copied, static_cast<uint32_t>(rawLength))) continue;

    double lat = 0, lon = 0;
    if (!field_f64(fort, cfg.f_lat, &lat) || !field_f64(fort, cfg.f_lon, &lon)) continue;
    if (std::fabs(lat) > 90.0 || std::fabs(lon) > 180.0) continue;

    // Authorized projection path: Camera.main, Component.transform, Transform.position,
    // Camera.WorldToScreenPoint, Screen.width/height. All results rooted before any further managed call.
    if (!within(deadline, &err)) return err;
    Root camera;
    switch (call(cfg.camera_get_main, nullptr, nullptr, camera)) {
      case CALL_THROW: return PGR_MA_ERR_MANAGED;
      case CALL_GCHANDLE:
      case CALL_DEAD: return PGR_MA_ERR_GCHANDLE;
      case CALL_OK: break;
    }
    if (!exact(camera.get(), cfg.class_camera)) continue;

    if (!within(deadline, &err)) return err;
    Root transform;
    switch (call(cfg.component_get_transform, &obj, nullptr, transform)) {
      case CALL_THROW: return PGR_MA_ERR_MANAGED;
      case CALL_GCHANDLE:
      case CALL_DEAD: return PGR_MA_ERR_GCHANDLE;
      case CALL_OK: break;
    }
    if (!exact(transform.get(), cfg.class_transform)) continue;

    if (!within(deadline, &err)) return err;
    Root position;
    switch (call(cfg.transform_get_position, &transform, nullptr, position)) {
      case CALL_THROW: return PGR_MA_ERR_MANAGED;
      case CALL_GCHANDLE:
      case CALL_DEAD: return PGR_MA_ERR_GCHANDLE;
      case CALL_OK: break;
    }
    Vec3 world{};
    if (!exact(position.get(), cfg.class_vector) || !unbox(position, &world, sizeof(world))) continue;
    if (!std::isfinite(world.x) || !std::isfinite(world.y) || !std::isfinite(world.z)) continue;

    if (!within(deadline, &err)) return err;
    Root screen;
    void *projectArgs[] = {&world};
    switch (call(cfg.project, &camera, projectArgs, screen)) {
      case CALL_THROW: return PGR_MA_ERR_MANAGED;
      case CALL_GCHANDLE:
      case CALL_DEAD: return PGR_MA_ERR_GCHANDLE;
      case CALL_OK: break;
    }
    Vec3 projected{};
    if (!exact(screen.get(), cfg.class_vector) || !unbox(screen, &projected, sizeof(projected))) continue;
    if (!std::isfinite(projected.x) || !std::isfinite(projected.y) || !std::isfinite(projected.z) ||
        projected.z <= 0.0f) continue;

    if (!within(deadline, &err)) return err;
    int32_t width = 0, height = 0;
    Root w, h;
    for (Root *target : {&w, &h}) {
      void *method = target == &w ? cfg.screen_width : cfg.screen_height;
      switch (call(method, nullptr, nullptr, *target)) {
        case CALL_THROW: return PGR_MA_ERR_MANAGED;
        case CALL_GCHANDLE:
        case CALL_DEAD: return PGR_MA_ERR_GCHANDLE;
        case CALL_OK: break;
      }
    }
    if (!exact(w.get(), cfg.class_int) || !exact(h.get(), cfg.class_int) ||
        !unbox(w, &width, sizeof(width)) || !unbox(h, &height, sizeof(height))) continue;
    if (width <= 0 || height <= 0) continue;
    double x = static_cast<double>(projected.x) / width;
    double y = 1.0 - static_cast<double>(projected.y) / height;  // same convention as feature 28.
    if (!std::isfinite(x) || !std::isfinite(y)) continue;

    if (!within(deadline, &err)) return err;
    PgrMapAnchorPoint *anchor = &out->anchors[out->count];
    std::memset(anchor, 0, sizeof(*anchor));
    anchor->kind = kind;
    anchor->lat = lat;
    anchor->lon = lon;
    anchor->x = x;
    anchor->y = y;
    anchor->z = projected.z;
    anchor->id_length = static_cast<uint32_t>(rawLength);
    std::memcpy(anchor->id, copied, static_cast<size_t>(rawLength) * sizeof(uint16_t));
    if (x >= 0.0 && x <= 1.0 && y >= 0.0 && y <= 1.0) anchor->flags = PGR_MA_FLAG_VISIBLE;
    ++out->count;
  }
  return within(deadline, &err) ? PGR_MA_OK : err;
}

void init_output(PgrMapAnchorOutput *out) {
  std::memset(out, 0, sizeof(*out));
  out->version = PGR_MA_OUTPUT_VERSION;
  out->size = sizeof(PgrMapAnchorOutput);
}

bool check_config(const PgrMapAnchorConfig *c) {
  if (!c || c->version != PGR_MA_CONFIG_VERSION || c->size != sizeof(PgrMapAnchorConfig)) return false;
  const char *base = reinterpret_cast<const char *>(c) + offsetof(PgrMapAnchorConfig, runtime_invoke);
  void *ptrs[35];
  std::memcpy(ptrs, base, sizeof(ptrs));
  for (void *p : ptrs) if (!p) return false;
  return true;
}

int install(const PgrMapAnchorConfig *config) {
  if (!check_config(config)) return PGR_MA_ERR_MISSING_PTR;
  std::lock_guard<std::mutex> guard(install_mutex);
  if (installed.load(std::memory_order_acquire)) {
    return std::memcmp(&cfg, config, sizeof(cfg)) == 0 ? PGR_MA_OK : PGR_MA_ERR_BAD_VERSION;
  }
  cfg = *config;
  installed.store(true, std::memory_order_release);
  return PGR_MA_OK;
}

struct BusyExit { ~BusyExit() { busy.store(false, std::memory_order_release); } };

int scan(const PgrMapAnchorRequest *request, PgrMapAnchorOutput *out) {
  if (!out) return PGR_MA_ERR_NULL_ARG;
  init_output(out);
  if (!installed.load(std::memory_order_acquire)) return PGR_MA_ERR_NOT_INSTALLED;
  if (!request || request->version != PGR_MA_REQUEST_VERSION || request->size != sizeof(PgrMapAnchorRequest))
    return PGR_MA_ERR_BAD_SIZE;
  if (!owner_ready(request->tid)) return PGR_MA_SKIP;
  uint64_t now = boot_ms();
  if (!now) return PGR_MA_ERR_CLOCK;
  uint64_t remaining = request->remaining_ms > PGR_MA_MAX_LEASE_MS ? static_cast<uint64_t>(PGR_MA_MAX_LEASE_MS) : request->remaining_ms;
  if (!remaining) return PGR_MA_SKIP;
  uint64_t deadline = now + remaining;
  if (deadline <= now) return PGR_MA_ERR_CLOCK;
  bool idle = false;
  if (!busy.compare_exchange_strong(idle, true)) return PGR_MA_ERR_BUSY;
  BusyExit exit;  // roots are destroyed before this runs.
  int err = PGR_MA_OK;
  if ((err = scan_class(cfg.class_stop, PGR_MA_KIND_STOP, deadline, out)) != PGR_MA_OK) {
    init_output(out);
    return err;
  }
  if ((err = scan_class(cfg.class_gym, PGR_MA_KIND_GYM, deadline, out)) != PGR_MA_OK) {
    init_output(out);
    return err;
  }
  if (!within(deadline, &err)) { init_output(out); return err; }
  out->status = PGR_MA_OK;
  return PGR_MA_OK;
}

const PgrMapAnchorApi api = {PGR_MA_API_VERSION, sizeof(PgrMapAnchorApi), install, scan};
}  // namespace

extern "C" const void *pgrazor_map_anchor_api(uint32_t version) {
  return version == PGR_MA_API_VERSION ? &api : nullptr;
}
