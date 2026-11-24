#pragma once
#include <stdint.h>

// Feature 72 (API 1). Read-only geographic anchors for the experimental visual-route prototype.
//
// The module walks the SAME two on-map POI classes as feature 42 (PokeStop/Gym), proves each candidate with
// the game's own IsAlive/GetFort, and projects the candidate's own Transform.position through the game's own
// Camera.main.WorldToScreenPoint and Screen.width/height. It publishes only copied scalars and copied UTF16
// ids: no managed pointer, GC handle or hook leaves the call, and it never mutates gameplay or calls back into
// JavaScript. Every optional interface is fail-closed: if the camera, transform, projection or lease is
// unavailable/invalid the anchor is omitted rather than guessed.
//
// This is an input to an experimental, opt-in geometric validation prototype. It is NOT proof of ground
// alignment and NOT a substitute for an authorized route renderer: the caller must still validate residual,
// hull, altitude and freshness before drawing anything.

#ifdef __cplusplus
extern "C" {
#endif

static_assert(sizeof(void *) == 8, "MapAnchor native ABI requires LP64");

enum PgrMapAnchorInfo {
  PGR_MA_FEATURE_ID       = 72,
  PGR_MA_API_VERSION      = 1,
  PGR_MA_CONFIG_VERSION   = 1,
  PGR_MA_REQUEST_VERSION  = 1,
  PGR_MA_OUTPUT_VERSION   = 1,
  PGR_MA_MAX_LEASE_MS     = 2500,
  PGR_MA_MAX_ANCHORS      = 128,
  PGR_MA_MAX_ARRAY        = 2048,
  PGR_MA_MAX_ID           = 64,
  PGR_MA_ARRAY_DATA_OFF   = 32
};

enum PgrMapAnchorStatus {
  PGR_MA_OK                = 1,
  PGR_MA_SKIP              = 0,
  PGR_MA_ERR_NULL_ARG      = -1,
  PGR_MA_ERR_BAD_VERSION   = -2,
  PGR_MA_ERR_BAD_SIZE      = -3,
  PGR_MA_ERR_MISSING_PTR   = -4,
  PGR_MA_ERR_NOT_INSTALLED = -5,
  PGR_MA_ERR_CLOCK         = -6,
  PGR_MA_ERR_BUSY          = -7,
  PGR_MA_ERR_GCHANDLE      = -8,
  PGR_MA_ERR_MANAGED       = -9,
  PGR_MA_ERR_METADATA      = -10,
  PGR_MA_ERR_BAD_COUNT     = -11
};

enum PgrMapAnchorKind { PGR_MA_KIND_STOP = 0, PGR_MA_KIND_GYM = 1 };
enum PgrMapAnchorFlags { PGR_MA_FLAG_VISIBLE = 1 };

// 35 already-validated il2cpp pointers, in exact order. All are mandatory; a null pointer refuses install.
struct PgrMapAnchorConfig {
  uint32_t version, size;
  // il2cpp runtime exports (14).
  void *runtime_invoke, *object_get_class, *field_get_value, *object_unbox;
  void *gchandle_new, *gchandle_get_target, *gchandle_free, *thread_current;
  void *class_get_type, *type_get_object, *array_length, *object_get_virtual_method;
  void *string_length, *string_chars;
  // Methods (9): POI discovery/liveness/fort + camera/transform/projection/screen.
  void *m_find, *m_alive, *m_fort;
  void *camera_get_main, *component_get_transform, *transform_get_position, *project;
  void *screen_width, *screen_height;
  // Exact class identities (10).
  void *class_stop, *class_gym, *class_fort, *class_bool, *class_string;
  void *class_vector, *class_int, *class_camera, *class_transform;
  // Fort fields (3): id, latitude, longitude.
  void *f_fortId, *f_lat, *f_lon;
};
static_assert(sizeof(PgrMapAnchorConfig) == 288, "PgrMapAnchorConfig must remain 288 bytes on LP64");

// Owner-thread snapshot request (32 bytes): same lease discipline as feature 42.
struct PgrMapAnchorRequest {
  uint32_t version, size;
  uint64_t tid;
  uint64_t remaining_ms;
  uint64_t wall_ms;
};
static_assert(sizeof(PgrMapAnchorRequest) == 32, "PgrMapAnchorRequest must remain 32 bytes on LP64");

// Copied anchor (184 bytes). x/y are normalized 0..1 with y down, z is positive camera depth.
struct PgrMapAnchorPoint {
  uint32_t kind, flags;
  double lat, lon, x, y, z;
  uint32_t id_length;
  uint16_t id[PGR_MA_MAX_ID];
};
static_assert(sizeof(PgrMapAnchorPoint) == 184, "PgrMapAnchorPoint must remain 184 bytes on LP64");

struct PgrMapAnchorOutput {
  uint32_t version, size;
  int32_t status;
  uint32_t count;
  PgrMapAnchorPoint anchors[PGR_MA_MAX_ANCHORS];
};
static_assert(sizeof(PgrMapAnchorOutput) == 23568, "PgrMapAnchorOutput must remain 23568 bytes on LP64");

struct PgrMapAnchorApi {
  uint32_t version, size;
  int (*install)(const PgrMapAnchorConfig *);
  int (*scan)(const PgrMapAnchorRequest *, PgrMapAnchorOutput *);
};
static_assert(sizeof(PgrMapAnchorApi) == 24, "PgrMapAnchorApi must remain a 24-byte version-1 table");

const void *pgrazor_map_anchor_api(uint32_t version);

#ifdef __cplusplus
}
#endif
