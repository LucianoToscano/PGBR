// Host-only test for the V4 appearance fix: the milky-white layer must disappear while the
// historical collector keeps doing exactly what it did before (1000x scale, SetActive, collider
// untouched). Synthetic objects only; no Unity, no Android.
//
// What is proven here:
//   * an OPAQUE white material (alpha 1.0), which the alpha-only path must refuse to edit,
//     is still made invisible because the renderers are hidden;
//   * renderers that were already disabled are left alone;
//   * every original Renderer.enabled flag is restored bit-for-bit, and every GC handle is freed;
//   * a failed/unverified write is not recorded, so restore never re-enables something the
//     game had disabled;
//   * the functional part (scale, active flag, collider, running field, diagnostic latch) is
//     unchanged in all of the above;
//   * without the V4 binding the collector behaves exactly as the historical version.
#define PGR_MINI_DIRECT_TEST
#include "rocket_minigame.cpp"
#include <cassert>
#include <cstdio>

int pgrazor_hook(void *, void *, void **) { return -1; }
int pgrazor_unhook(void *) { return 0; }

namespace {
int renderer_tag, go_tag, transform_tag, collider_tag, array_tag, type_tag, material_tag, color_tag,
    challenge_tag, bool_tag;
int get_in_children_m, get_enabled_m, set_enabled_m, array_get_m, owner_m, transform_m, scale_set_m,
    active_set_m, active_get_m, scale_get_m, alive_m, get_color_m, set_color_m, get_material_m,
    get_shared_m, get_components_m;
int running_f, collider_f;

struct Obj {
    void *klass;
    bool alive = true;
    uint8_t flag = 0;
    Obj *owner = nullptr;
    float value[4] = {1.f, 1.f, 1.f, 1.f};
};

Obj go{&go_tag}, tr{&transform_tag}, collider{&collider_tag}, type{&type_tag};
Obj subtree{&array_tag}, materials{&array_tag};
Obj tap_renderer{&renderer_tag}, glow_renderer{&renderer_tag}, off_renderer{&renderer_tag};
Obj shared_material{&material_tag}, local_material{&material_tag}, boxed{&bool_tag};
Obj color_box{&color_tag}, challenge{&challenge_tag};

uint8_t running = 1;
int roots = 0, scale_writes = 0, color_writes = 0;
bool reject_hide_for_glow = false;   // set_enabled throws for one renderer
bool refuse_hide_completely = false; // every write throws

Obj *subtree_items[3] = {&tap_renderer, &glow_renderer, &off_renderer};
uintptr_t subtree_count = 3;

void *root(void *p, int) { if (p) ++roots; return p; }
void free_root(void *p) { if (p) --roots; }
void *target(void *p) { return p; }
void *obj_class(void *p) { return static_cast<Obj *>(p)->klass; }
void *unbox(void *p) {
    auto *o = static_cast<Obj *>(p);
    return o->klass == &bool_tag ? static_cast<void *>(&o->flag)
                                 : static_cast<void *>(o->value);
}
void *type_object(void *) { return &type; }
uintptr_t array_length(void *p) { return p == &subtree ? subtree_count : 1; }
bool assignable(void *a, void *b) { return a == b; }

void field(void *, void *f, void *out) {
    if (f == &running_f) *static_cast<uint8_t *>(out) = running;
    else if (f == &collider_f) *static_cast<void **>(out) = &collider;
}

void *invoke(void *m, void *obj, void **args, void **exc) {
    *exc = nullptr;
    auto *o = static_cast<Obj *>(obj);
    if (m == &get_in_children_m) return &subtree;
    if (m == &get_components_m) return &subtree;
    if (m == &array_get_m) {
        if (obj == &subtree) {
            const int i = *static_cast<int *>(args[0]);
            if (i < 0 || static_cast<uintptr_t>(i) >= subtree_count) return nullptr;
            return subtree_items[i];
        }
        return static_cast<void *>(&o[0]);
    }
    if (m == &owner_m) return obj == &collider ? static_cast<void *>(&go) : o->owner;
    if (m == &transform_m) return &tr;
    if (m == &scale_get_m) return &tr;
    if (m == &active_get_m) { boxed.flag = go.flag; return &boxed; }
    if (m == &get_shared_m) return &materials;
    if (m == &get_material_m) return &local_material;
    if (m == &get_color_m) { std::memcpy(color_box.value, o->value, sizeof(o->value)); return &color_box; }
    if (m == &set_color_m) {
        const float *v = static_cast<float *>(args[0]);
        std::memcpy(local_material.value, v, sizeof(local_material.value));
        ++color_writes;
        return nullptr;
    }
    if (m == &get_enabled_m) { boxed.flag = o->flag; return &boxed; }
    if (m == &set_enabled_m) {
        if (refuse_hide_completely || (reject_hide_for_glow && o == &glow_renderer)) {
            *exc = obj;
            return nullptr;
        }
        o->flag = *static_cast<uint8_t *>(args[0]);
        return nullptr;
    }
    if (m == &scale_set_m) {
        const float *v = static_cast<float *>(args[0]);
        std::memcpy(o->value, v, 12);
        ++scale_writes;
        return nullptr;
    }
    if (m == &active_set_m) { go.flag = *static_cast<uint8_t *>(args[0]); return nullptr; }
    if (m == &alive_m) { boxed.flag = static_cast<Obj *>(args[0])->alive; return &boxed; }
    return nullptr;
}

void base_setup() {
    g_failed.store(0);
    g_cfg.root = root;
    g_cfg.free_root = free_root;
    g_cfg.target = target;
    g_cfg.object_class = obj_class;
    g_cfg.object_unbox = unbox;
    g_cfg.runtime_invoke = invoke;
    g_cfg.bool_class = &bool_tag;
    g_cfg.gameObject_method = &owner_m;
    g_cfg.alive_method = &alive_m;
    g_cfg.setScale_method = &scale_set_m;
    g_cfg.setActive_method = &active_set_m;
    g_cfg.transform_method = &transform_m;
    g_cfg.scale_method = &scale_get_m;
    g_cfg.active_method = &active_get_m;
    g_cfg.mini_class = &challenge_tag;
    g_cfg.field_get_value = field;
    g_cfg.running_field = &running_f;
    g_cfg.collider_field = &collider_f;
    g_active.store(1);
    api_update(1, 2500);
    g_visual = {&renderer_tag, &type_tag, &get_components_m, &get_shared_m, &get_material_m,
                &material_tag, &color_tag, &get_color_m, &set_color_m, &array_get_m, type_object,
                array_length, assignable};
}

void v4_setup() {
    g_visual_ex = {&get_enabled_m, &set_enabled_m, &get_in_children_m};
}

void reset_scene() {
    go.flag = 0;
    go.value[0] = go.value[1] = go.value[2] = 1.f;
    tr.value[0] = tr.value[1] = tr.value[2] = 1.f;
    for (Obj *r : subtree_items) { r->flag = 1; r->owner = &go; }
    tap_renderer.value[0] = tap_renderer.value[1] = tap_renderer.value[2] = 1.f;
    tap_renderer.value[3] = 1.f;   // opaque white: the alpha-only path must not touch it
    shared_material.value[0] = shared_material.value[1] = shared_material.value[2] = 1.f;
    shared_material.value[3] = 1.f;
    local_material.value[0] = local_material.value[1] = local_material.value[2] = 1.f;
    local_material.value[3] = 1.f;
    off_renderer.flag = 0;         // the game had this one disabled: keep it disabled
    subtree_count = 3;
    reject_hide_for_glow = false;
    refuse_hide_completely = false;
}
} // namespace

int main() {
    base_setup();
    v4_setup();

    // 1. Opaque white: the alpha path refuses, the renderers are hidden anyway.
    reset_scene();
    do_collect(&challenge);
    assert(g_have_mini);
    assert(tr.value[0] == 1000.f);              // historical functional scale untouched
    assert(go.flag == 1);                       // historical SetActive untouched
    assert(color_writes == 0);                  // no alpha written into an opaque material
    assert(local_material.value[3] == 1.f);     // material left exactly as the game had it
    assert(tap_renderer.flag == 0);             // white layer invisible
    assert(glow_renderer.flag == 0);
    assert(off_renderer.flag == 0);             // was already off
    assert(!g_failed.load());                   // no exception latched

    // 2. Restore puts back exactly the flags the game had, and frees the array handle.
    do_restore();
    assert(!g_have_mini);
    assert(tap_renderer.flag == 1);
    assert(glow_renderer.flag == 1);
    assert(off_renderer.flag == 0);             // never re-enabled by us
    assert(tr.value[0] == 1.f);
    assert(go.flag == 0);
    assert(roots == 0);

    // 3. A failed write is not recorded; the collector still works and restore is clean.
    reset_scene();
    reject_hide_for_glow = true;
    do_collect(&challenge);
    assert(g_have_mini && tr.value[0] == 1000.f && go.flag == 1);
    assert(tap_renderer.flag == 0);             // hidden and recorded
    assert(glow_renderer.flag == 1);            // rejected -> untouched, not recorded
    do_restore();
    assert(tap_renderer.flag == 1);
    assert(glow_renderer.flag == 1);
    assert(roots == 0 && tr.value[0] == 1.f);

    // 4. Every write refused: nothing is claimed, nothing latched, collector unchanged.
    reset_scene();
    refuse_hide_completely = true;
    do_collect(&challenge);
    assert(g_have_mini && tr.value[0] == 1000.f && go.flag == 1);
    assert(tap_renderer.flag == 1 && glow_renderer.flag == 1);
    assert(!g_failed.load());
    do_restore();
    assert(roots == 0 && tr.value[0] == 1.f);

    // 5. No V4 binding: historical behavior, no crash, no latch.
    reset_scene();
    auto saved_ex = g_visual_ex;
    g_visual_ex = {};
    do_collect(&challenge);
    assert(g_have_mini && tr.value[0] == 1000.f && go.flag == 1);
    do_restore();
    assert(roots == 0 && tr.value[0] == 1.f);
    g_visual_ex = saved_ex;

    // 6. Absurd component count is rejected instead of walking the scene.
    reset_scene();
    subtree_count = static_cast<uintptr_t>(PGR_MINI_MAX_HIDDEN) + 1u;
    do_collect(&challenge);
    assert(g_have_mini && tr.value[0] == 1000.f);
    assert(tap_renderer.flag == 1);             // nothing touched
    do_restore();
    assert(roots == 0);

    // 7. A running==0 challenge restores immediately (unchanged lifecycle).
    reset_scene();
    do_collect(&challenge);
    assert(tap_renderer.flag == 0);
    running = 0;
    do_collect(&challenge);
    assert(!g_have_mini && tap_renderer.flag == 1 && tr.value[0] == 1.f && roots == 0);
    running = 1;

    const uint32_t diag = pgrazor_rocket_minigame_take_diagnostics();
    assert(diag & (1u << 19));                  // white-layer-hidden observed at least once
    std::puts("rocket_minigame_white_layer_test: OK");
    return 0;
}
