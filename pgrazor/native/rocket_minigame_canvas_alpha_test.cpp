// Gemini V5 host-only fixture: local CanvasRenderer alpha and per-frame reinforcement.
// Adapted from 55df3948d09789488db100bf6087b3ba65635fa8. No Unity or Android.
#define PGR_MINI_DIRECT_TEST
#include "rocket_minigame.cpp"
#include <cassert>
#include <cmath>
#include <cstdio>

int pgrazor_hook(void *, void *, void **) { return -1; }
int pgrazor_unhook(void *) { return 0; }

namespace {
int renderer_tag, canvas_tag, go_tag, transform_tag, collider_tag, array_tag;
int renderer_type_tag, canvas_type_tag, material_tag, color_tag, challenge_tag, bool_tag, float_tag;
int get_in_children_m, get_enabled_m, set_enabled_m, get_alpha_m, set_alpha_m;
int array_get_m, owner_m, transform_m, scale_set_m, active_set_m, active_get_m, scale_get_m, alive_m;
int get_color_m, set_color_m, get_material_m, get_shared_m, get_components_m;
int running_f, collider_f;

struct Obj {
    void *klass;
    bool alive = true;
    uint8_t flag = 0;
    float alpha = 1.0f;
    float value[4] = {1.f, 1.f, 1.f, 1.f};
};

Obj go{&go_tag}, tr{&transform_tag}, collider{&collider_tag};
Obj renderer_type_obj{&renderer_type_tag}, canvas_type_obj{&canvas_type_tag};
Obj renderer_arr{&array_tag}, canvas_arr{&array_tag};
Obj rend_main{&renderer_tag};
Obj cr_milky{&canvas_tag}, cr_clear{&canvas_tag};
Obj boxed_bool{&bool_tag}, boxed_float{&float_tag}, challenge{&challenge_tag};

uint8_t running = 1;
int roots = 0, scale_writes = 0;
uintptr_t rend_count = 0, canvas_count = 0;
Obj *rend_items[2] = {&rend_main, nullptr};
Obj *canvas_items[2] = {&cr_milky, &cr_clear};
bool throw_on_set_alpha = false;
bool hide_canvas_until_active = false;

void *root(void *p, int) { if (p) ++roots; return p; }
void free_root(void *p) { if (p) --roots; }
void *target(void *p) { return p; }
void *obj_class(void *p) { return static_cast<Obj *>(p)->klass; }
void *unbox(void *p) {
    auto *o = static_cast<Obj *>(p);
    if (o->klass == &bool_tag) return &o->flag;
    if (o->klass == &float_tag) return &o->alpha;
    return o->value;
}
void *type_object(void *t) { return t; }
uintptr_t array_length(void *p) {
    if (p == &renderer_arr) return rend_count;
    if (p == &canvas_arr) return (hide_canvas_until_active && !go.flag) ? 0 : canvas_count;
    return 0;
}
bool assignable(void *a, void *b) { return a == b; }
void field(void *, void *f, void *out) {
    if (f == &running_f) *static_cast<uint8_t *>(out) = running;
    else if (f == &collider_f) *static_cast<void **>(out) = &collider;
}

void *invoke(void *m, void *obj, void **args, void **exc) {
    *exc = nullptr;
    auto *o = static_cast<Obj *>(obj);
    if (m == &get_in_children_m) {
        return args[0] == &canvas_type_obj ? static_cast<void *>(&canvas_arr)
                                           : static_cast<void *>(&renderer_arr);
    }
    if (m == &get_components_m) return &renderer_arr;
    if (m == &array_get_m) {
        const int i = *static_cast<int *>(args[0]);
        if (obj == &renderer_arr) return (i >= 0 && static_cast<uintptr_t>(i) < rend_count) ? rend_items[i] : nullptr;
        if (obj == &canvas_arr) return (i >= 0 && static_cast<uintptr_t>(i) < canvas_count) ? canvas_items[i] : nullptr;
        return nullptr;
    }
    if (m == &owner_m) return &go;
    if (m == &transform_m) return &tr;
    if (m == &scale_get_m) return &tr;
    if (m == &active_get_m) { boxed_bool.flag = go.flag; return &boxed_bool; }
    if (m == &get_enabled_m) { boxed_bool.flag = o->flag; return &boxed_bool; }
    if (m == &set_enabled_m) { o->flag = *static_cast<uint8_t *>(args[0]); return nullptr; }
    if (m == &get_alpha_m) { boxed_float.alpha = o->alpha; return &boxed_float; }
    if (m == &set_alpha_m) {
        if (throw_on_set_alpha) { *exc = obj; return nullptr; }
        o->alpha = *static_cast<float *>(args[0]);
        return nullptr;
    }
    if (m == &scale_set_m) {
        std::memcpy(o->value, static_cast<float *>(args[0]), 12);
        ++scale_writes;
        return nullptr;
    }
    if (m == &active_set_m) { go.flag = *static_cast<uint8_t *>(args[0]); return nullptr; }
    if (m == &alive_m) { boxed_bool.flag = static_cast<Obj *>(args[0])->alive; return &boxed_bool; }
    return nullptr;
}

void setup(bool enable_v5) {
    g_mini = {};
    g_have_mini = false;
    g_failed.store(0);
    g_busy.store(0);
    g_restore_pending.store(0);
    roots = 0;
    scale_writes = 0;
    running = 1;
    go.flag = 0;
    tr.value[0] = 2.f; tr.value[1] = 3.f; tr.value[2] = 0.5f;
    rend_main.flag = 1;
    cr_milky.alpha = 0.65f;
    cr_clear.alpha = 0.0f;
    rend_count = 0;
    canvas_count = 2;
    throw_on_set_alpha = false;
    hide_canvas_until_active = false;

    g_cfg = {};
    g_cfg.root = root; g_cfg.free_root = free_root; g_cfg.target = target;
    g_cfg.object_class = obj_class; g_cfg.object_unbox = unbox; g_cfg.runtime_invoke = invoke;
    g_cfg.bool_class = &bool_tag; g_cfg.gameObject_method = &owner_m; g_cfg.alive_method = &alive_m;
    g_cfg.setScale_method = &scale_set_m; g_cfg.setActive_method = &active_set_m;
    g_cfg.transform_method = &transform_m; g_cfg.scale_method = &scale_get_m;
    g_cfg.active_method = &active_get_m; g_cfg.mini_class = &challenge_tag;
    g_cfg.field_get_value = field; g_cfg.running_field = &running_f; g_cfg.collider_field = &collider_f;
    g_active.store(1);
    api_update(1, 2500);
    g_visual = {&renderer_tag, &renderer_type_obj, &get_components_m, &get_shared_m, &get_material_m,
                &material_tag, &color_tag, &get_color_m, &set_color_m, &array_get_m, type_object,
                array_length, assignable};
    g_visual_ex = {&get_enabled_m, &set_enabled_m, &get_in_children_m};
    g_canvas_alpha = enable_v5
        ? PgrRocketMiniCanvasAlpha{&canvas_tag, &canvas_type_obj, &get_alpha_m, &set_alpha_m, &float_tag}
        : PgrRocketMiniCanvasAlpha{};
}
} // namespace

int main() {
    // Local UI only; unchanged scale and activation, original alpha restored.
    setup(true);
    do_collect(&challenge);
    assert(tr.value[0] == 2000.f && tr.value[1] == 3000.f && tr.value[2] == 1000.f);
    assert(go.flag == 1);
    assert(std::fabs(cr_milky.alpha - 0.0f) < 1e-6f);
    assert(std::fabs(cr_clear.alpha - 0.0f) < 1e-6f);
    assert(g_mini.canvas_count == 1);
    do_restore();
    assert(std::fabs(cr_milky.alpha - 0.65f) < 1e-6f);
    assert(tr.value[0] == 2.f && go.flag == 0 && roots == 0);

    // Post-activation discovery.
    setup(true);
    hide_canvas_until_active = true;
    do_collect(&challenge);
    assert(go.flag == 1 && g_mini.canvas_count == 1);
    assert(std::fabs(cr_milky.alpha - 0.0f) < 1e-6f);
    do_restore();
    assert(std::fabs(cr_milky.alpha - 0.65f) < 1e-6f && roots == 0);

    // Later frame restores Unity visual properties; keep original snapshot.
    setup(true);
    rend_count = 1;
    do_collect(&challenge);
    assert(rend_main.flag == 0 && std::fabs(cr_milky.alpha - 0.0f) < 1e-6f);
    rend_main.flag = 1;
    cr_milky.alpha = 0.85f;
    do_collect(&challenge);
    assert(rend_main.flag == 0 && std::fabs(cr_milky.alpha - 0.0f) < 1e-6f);
    do_restore();
    assert(rend_main.flag == 1 && std::fabs(cr_milky.alpha - 0.65f) < 1e-6f && roots == 0);

    // Optional failure must not disable the collector.
    setup(true);
    throw_on_set_alpha = true;
    do_collect(&challenge);
    assert(!g_failed.load());
    assert(tr.value[0] == 2000.f && go.flag == 1 && g_mini.canvas_count == 0);
    do_restore();
    assert(roots == 0);

    // V4 fallback remains usable.
    setup(false);
    rend_count = 1;
    do_collect(&challenge);
    assert(tr.value[0] == 2000.f && go.flag == 1 && rend_main.flag == 0);
    assert(std::fabs(cr_milky.alpha - 0.65f) < 1e-6f);
    do_restore();
    assert(rend_main.flag == 1 && roots == 0);

    std::puts("rocket_minigame_canvas_alpha_test: PASS (5 scenarios)");
    return 0;
}
