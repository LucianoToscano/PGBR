// Local, self-contained harness for feature 72 (map anchor projection). It uses a fake IL2CPP heap and the
// module's real ABI; no Dobby, no Android runtime and no gameplay calls. Build:
// Build: g++ -std=c++17 -Wall -Wextra -Werror -I pgrazor/native (test + module sources).
//       pgrazor/native/map_anchor_test.cpp plus map_anchor.cpp, output /tmp/map_anchor_test
#include "map_anchor.h"

#include <cassert>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <map>
#include <string>
#include <unistd.h>

namespace {

const char *m_find = "find", *m_alive = "alive", *m_fort_token = "fort", *m_camera_token = "camera";
const char *m_transform_token = "transform", *m_position = "position", *m_project = "project";
const char *m_width = "width", *m_height = "height";
const char *f_id = "id", *f_lat = "lat", *f_lon = "lon";

struct FakeString { void *klass; int32_t length; uint16_t chars[32]; };
struct FakeBox { void *klass; uint8_t value; };
struct FakeIntBox { void *klass; int32_t value; };
struct FakeVectorBox { void *klass; float x, y, z; };
struct FakeFort { void *klass; void *id; double lat, lon; };
struct FakeCandidate { void *klass; void *fort; };
struct FakeArray { void *klass; uint64_t pad0, pad1; uint64_t length; void *data[8]; };
struct FakeObject { void *klass; };

// Stable identities. Class tokens are addresses of these ints (exactly as Il2CppClass* are compared).
int K_STOP, K_GYM, K_FORT, K_BOOL, K_STRING, K_VECTOR, K_INT, K_CAMERA, K_TRANSFORM, K_TYPE;

FakeString id_a{0, 0, {}}, id_b{0, 0, {}}, id_c{0, 0, {}}, id_d{0, 0, {}};
FakeBox alive{0, 1};
FakeIntBox width_box{0, 1000}, height_box{0, 2000};
FakeVectorBox world_a{0, 1, 2, 3}, world_b{0, 4, 5, 6}, screen_a{0, 250, 500, 10},
    screen_b{0, 1500, -1000, 10}, screen_bad{0, 100, 100, 0};
FakeObject camera_obj{0}, transform_a{0}, transform_b{0}, type_stop_obj{0}, type_gym_obj{0};

std::map<uintptr_t, void *> handles;
uintptr_t next_handle = 1;
bool throw_alive = false, throw_fort = false, throw_project = false;
FakeArray stop_array{0, 0, 0, 0, {}}, gym_array{0, 0, 0, 0, {}};
FakeFort fort_a{0, nullptr, -23.550520, -46.633308}, fort_b{0, nullptr, -23.551000, -46.634000},
    fort_c{0, nullptr, -23.552000, -46.635000}, fort_bad{0, nullptr, 200.0, 0.0};
FakeCandidate stop_a{0, nullptr}, stop_b{0, nullptr}, stop_bad{0, nullptr}, gym_a{0, nullptr};

void set_id(FakeString &s, const char *ascii) {
  s.klass = &K_STRING;
  s.length = static_cast<int32_t>(std::strlen(ascii));
  for (int i = 0; i < s.length && i < 32; ++i) s.chars[i] = static_cast<uint16_t>(ascii[i]);
}

void reset_heap() {
  handles.clear();
  next_handle = 1;
  throw_alive = throw_fort = throw_project = false;
  set_id(id_a, "stop-a"); set_id(id_b, "stop-b"); set_id(id_c, "gym-c"); set_id(id_d, "bad");
  alive.klass = &K_BOOL; alive.value = 1;
  width_box.klass = height_box.klass = &K_INT;
  world_a.klass = world_b.klass = screen_a.klass = screen_b.klass = screen_bad.klass = &K_VECTOR;
  camera_obj.klass = &K_CAMERA; transform_a.klass = transform_b.klass = &K_TRANSFORM;
  type_stop_obj.klass = &K_TYPE; type_gym_obj.klass = &K_TYPE;
  fort_a.klass = fort_b.klass = fort_c.klass = fort_bad.klass = &K_FORT;
  fort_a.id = &id_a; fort_b.id = &id_b; fort_c.id = &id_c; fort_bad.id = &id_d;
  stop_a.klass = stop_b.klass = stop_bad.klass = &K_STOP; gym_a.klass = &K_GYM;
  stop_a.fort = &fort_a; stop_b.fort = &fort_b; stop_bad.fort = &fort_bad; gym_a.fort = &fort_c;
  stop_array.klass = gym_array.klass = &K_TYPE;
  stop_array.pad0 = stop_array.pad1 = gym_array.pad0 = gym_array.pad1 = 0;
  stop_array.length = 3; gym_array.length = 1;
  stop_array.data[0] = &stop_a; stop_array.data[1] = &stop_b; stop_array.data[2] = &stop_bad;
  gym_array.data[0] = &gym_a;
  world_a.x = 1; world_a.y = 2; world_a.z = 3; world_b.x = 4; world_b.y = 5; world_b.z = 6;
  screen_a.x = 250; screen_a.y = 500; screen_a.z = 10;
  screen_b.x = 1500; screen_b.y = -1000; screen_b.z = 10;
  screen_bad.z = 0;
  width_box.value = 1000; height_box.value = 2000;
}

void *klass_of(void *object) {
  return object ? *reinterpret_cast<void **>(object) : nullptr;
}
uintptr_t gc_new(void *object, int pinned) {
  (void)pinned;
  if (!object) return 0;
  uintptr_t h = next_handle++;
  handles[h] = object;
  return h;
}
void *gc_get(uintptr_t h) {
  auto it = handles.find(h);
  return it == handles.end() ? nullptr : it->second;
}
void gc_free(uintptr_t h) { handles.erase(h); }
void *invoke(void *method, void *self, void **args, void **exception) {
  *exception = nullptr;
  const char *name = static_cast<const char *>(method);
  if (!std::strcmp(name, m_find)) {
    FakeArray *a = args && args[0] == &type_gym_obj ? &gym_array : &stop_array;
    return a;
  }
  if (!std::strcmp(name, m_fort_token)) {
    return self ? reinterpret_cast<FakeCandidate *>(self)->fort : nullptr;
  }
  if (!std::strcmp(name, m_alive)) {
    if (throw_alive) { *exception = reinterpret_cast<void *>(1); return nullptr; }
    return &alive;
  }
  if (!std::strcmp(name, m_camera_token)) return &camera_obj;
  if (!std::strcmp(name, m_transform_token)) return self == &stop_b || self == &gym_a ? &transform_b : &transform_a;
  if (!std::strcmp(name, m_position)) return self == &transform_b ? static_cast<void *>(&world_b) : static_cast<void *>(&world_a);
  if (!std::strcmp(name, m_width)) return &width_box;
  if (!std::strcmp(name, m_height)) return &height_box;
  if (!std::strcmp(name, m_project)) {
    if (throw_project) { *exception = reinterpret_cast<void *>(1); return nullptr; }
    return self == &camera_obj && args && args[0] == &world_b
               ? static_cast<void *>(&screen_b)
               : static_cast<void *>(&screen_a);
  }
  *exception = reinterpret_cast<void *>(1);
  return nullptr;
}
void *virtual_method(void *object, void *method) {
  return std::strcmp(static_cast<const char *>(method), m_fort_token) == 0 && object ? const_cast<char *>(m_fort_token) : nullptr;
}
void field_get(void *object, void *field, void *out) {
  const char *name = static_cast<const char *>(field);
  auto *fort = static_cast<FakeFort *>(object);
  if (!std::strcmp(name, f_id)) {
    void *v = fort->id;
    std::memcpy(out, &v, sizeof(v));
  } else if (!std::strcmp(name, f_lat)) {
    double v = fort->lat;
    std::memcpy(out, &v, sizeof(v));
  } else if (!std::strcmp(name, f_lon)) {
    double v = fort->lon;
    std::memcpy(out, &v, sizeof(v));
  } else assert(false);
}
PgrMapAnchorConfig config() {
  PgrMapAnchorConfig c = {};
  c.version = PGR_MA_CONFIG_VERSION; c.size = sizeof(c);
  c.runtime_invoke = reinterpret_cast<void *>(invoke);
  c.object_get_class = reinterpret_cast<void *>(klass_of);
  c.field_get_value = reinterpret_cast<void *>(field_get);
  c.object_unbox = reinterpret_cast<void *>(+[](void *o) -> void * { return static_cast<char *>(o) + 8; });
  c.gchandle_new = reinterpret_cast<void *>(gc_new);
  c.gchandle_get_target = reinterpret_cast<void *>(gc_get);
  c.gchandle_free = reinterpret_cast<void *>(gc_free);
  c.thread_current = reinterpret_cast<void *>(+[]() -> void * { return reinterpret_cast<void *>(1); });
  c.class_get_type = reinterpret_cast<void *>(+[](void *k) -> void * { return k; });
  c.type_get_object = reinterpret_cast<void *>(+[](void *k) -> void * { return k == &K_GYM ? static_cast<void *>(&type_gym_obj) : static_cast<void *>(&type_stop_obj); });
  c.array_length = reinterpret_cast<void *>(+[](void *a) -> uintptr_t { return reinterpret_cast<FakeArray *>(a)->length; });
  c.object_get_virtual_method = reinterpret_cast<void *>(virtual_method);
  c.string_length = reinterpret_cast<void *>(+[](void *s) -> int32_t { return reinterpret_cast<FakeString *>(s)->length; });
  c.string_chars = reinterpret_cast<void *>(+[](void *s) -> uint16_t * { return reinterpret_cast<FakeString *>(s)->chars; });
  c.m_find = const_cast<char *>(m_find); c.m_alive = const_cast<char *>(m_alive);
  c.m_fort = const_cast<char *>(m_fort_token); c.camera_get_main = const_cast<char *>(m_camera_token);
  c.component_get_transform = const_cast<char *>(m_transform_token);
  c.transform_get_position = const_cast<char *>(m_position); c.project = const_cast<char *>(m_project);
  c.screen_width = const_cast<char *>(m_width); c.screen_height = const_cast<char *>(m_height);
  c.class_stop = &K_STOP; c.class_gym = &K_GYM; c.class_fort = &K_FORT; c.class_bool = &K_BOOL;
  c.class_string = &K_STRING; c.class_vector = &K_VECTOR; c.class_int = &K_INT;
  c.class_camera = &K_CAMERA; c.class_transform = &K_TRANSFORM;
  c.f_fortId = const_cast<char *>(f_id); c.f_lat = const_cast<char *>(f_lat); c.f_lon = const_cast<char *>(f_lon);
  return c;
}

PgrMapAnchorRequest request(uint64_t lease = 2000) {
  PgrMapAnchorRequest r = {};
  r.version = PGR_MA_REQUEST_VERSION; r.size = sizeof(r);
  r.tid = static_cast<uint64_t>(gettid()); r.remaining_ms = lease; r.wall_ms = 1700000000000ULL;
  return r;
}

bool has_id(const PgrMapAnchorOutput &out, const char *id) {
  size_t n = std::strlen(id);
  for (uint32_t i = 0; i < out.count; ++i) {
    if (out.anchors[i].id_length != n) continue;
    bool same = true;
    for (size_t j = 0; j < n; ++j) same = same && out.anchors[i].id[j] == static_cast<uint16_t>(id[j]);
    if (same) return true;
  }
  return false;
}
}  // namespace

int main() {
  reset_heap();
  const auto *api = static_cast<const PgrMapAnchorApi *>(pgrazor_map_anchor_api(1));
  assert(api && api->size == 24 && !pgrazor_map_anchor_api(2));
  PgrMapAnchorConfig c = config();
  assert(api->install(nullptr) < 0);
  PgrMapAnchorConfig bad = c; bad.version = 9;
  assert(api->install(&bad) < 0);
  bad = c; void *saved = bad.m_find; bad.m_find = nullptr;
  assert(api->install(&bad) < 0 && bad.m_find == nullptr && saved != nullptr);
  assert(api->install(&c) == PGR_MA_OK);
  PgrMapAnchorConfig other = c; other.screen_width = nullptr;
  assert(api->install(&other) < 0);
  assert(api->install(&c) == PGR_MA_OK);

  static PgrMapAnchorOutput out;
  PgrMapAnchorRequest r = request();
  assert(api->scan(nullptr, &out) < 0);
  r.version = 9; assert(api->scan(&r, &out) < 0);
  r = request(); r.tid = 0; assert(api->scan(&r, &out) == PGR_MA_SKIP && out.count == 0);
  r = request(); r.remaining_ms = 0; assert(api->scan(&r, &out) == PGR_MA_SKIP);

  r = request();
  int rc=api->scan(&r, &out); 
  assert(rc == PGR_MA_OK);
  assert(out.status == PGR_MA_OK && out.count == 3);
  assert(has_id(out, "stop-a") && has_id(out, "stop-b") && has_id(out, "gym-c"));
  assert(!has_id(out, "bad"));  // out-of-range latitude refused
  for (uint32_t i = 0; i < out.count; ++i) {
    const auto &a = out.anchors[i];
    assert(std::isfinite(a.lat) && std::isfinite(a.lon) && std::isfinite(a.x) && std::isfinite(a.y));
    assert(a.lat > -90 && a.lat < 0 && a.lon < 0);
    assert(std::fabs(a.z - 10.0) < 1e-6);
  }
  // First two POIs use screen (250,500)/1000x2000 -> (0.25,0.75). The gym uses (1500,-1000) -> offscreen.
  for (uint32_t i = 0; i < out.count; ++i) {
    if (out.anchors[i].flags & PGR_MA_FLAG_VISIBLE) {
      assert(std::fabs(out.anchors[i].x - 0.25) < 1e-6);
      assert(std::fabs(out.anchors[i].y - 0.75) < 1e-6);
    }
  }

  // Dedup by fort id: two stop elements with the same id publish one anchor.
  FakeFort duplicate{&K_FORT, &id_a, -23.7, -46.7};
  FakeCandidate stop_dup{&K_STOP, &duplicate};
  stop_array.data[2] = &stop_dup;
  r = request(); assert(api->scan(&r, &out) == PGR_MA_OK && out.count == 3);
  stop_array.data[2] = &stop_bad;

  // Bad projection depth omits candidates instead of inventing a coordinate.
  screen_a.z = 0; screen_b.z = 0;
  r = request(); assert(api->scan(&r, &out) == PGR_MA_OK && out.count == 0);
  screen_a.z = 10; screen_b.z = 10;

  // Main-path managed throw: empty snapshot, no partial publication.
  throw_alive = true;
  r = request(); assert(api->scan(&r, &out) == PGR_MA_ERR_MANAGED && out.count == 0 && out.status == 0);
  throw_alive = false;
  throw_project = true;
  r = request(); assert(api->scan(&r, &out) == PGR_MA_ERR_MANAGED && out.count == 0);
  throw_project = false;

  // Moving GC: relocating every rooted object between calls must not leak stale raw pointers.
  static FakeFort moved_fort[4];
  static FakeCandidate moved_stop[3], moved_gym[1];
  std::memcpy(moved_fort, &fort_a, sizeof(fort_a));
  std::memcpy(moved_fort + 1, &fort_b, sizeof(fort_b));
  std::memcpy(moved_fort + 2, &fort_c, sizeof(fort_c));
  std::memcpy(moved_fort + 3, &fort_bad, sizeof(fort_bad));
  moved_fort[0].klass = moved_fort[1].klass = moved_fort[2].klass = moved_fort[3].klass = &K_FORT;
  moved_fort[0].id = &id_a; moved_fort[1].id = &id_b; moved_fort[2].id = &id_c; moved_fort[3].id = &id_d;
  moved_stop[0].klass = moved_stop[1].klass = moved_stop[2].klass = &K_STOP;
  moved_stop[0].fort = &moved_fort[0]; moved_stop[1].fort = &moved_fort[1]; moved_stop[2].fort = &moved_fort[3];
  moved_gym[0].klass = &K_GYM; moved_gym[0].fort = &moved_fort[2];
  stop_array.data[0] = &moved_stop[0]; stop_array.data[1] = &moved_stop[1]; stop_array.data[2] = &moved_stop[2];
  gym_array.data[0] = &moved_gym[0];
  r = request(); assert(api->scan(&r, &out) == PGR_MA_OK && out.count == 3);
  assert(has_id(out, "stop-a") && has_id(out, "gym-c"));
  assert(handles.empty());  // every temporary root was released

  std::puts("PASS map anchor: ABI 288/32/184/23568/24, install validation, owner+lease gates, "
            "projection normalization, dedup, invalid depth/geometry, managed faults, moving GC, root balance");
  return 0;
}
