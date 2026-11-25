#include "rocket_minigame.h"
#include "rocket_minigame_shape.h"
#ifdef PGR_MINI_DIRECT_TEST
int pgrazor_hook(void *, void *, void **);
int pgrazor_unhook(void *);
#else
#include "hook_engine.h"
#endif

#include <atomic>
#include <mutex>
#include <cstring>
#include <cmath>
#include <ctime>
#include <cstdlib>
#include <unistd.h>
#include <sys/syscall.h>
#ifdef __ANDROID__
#include <android/log.h>
#endif

// Native state is Unity-thread-only. The only cross-thread entrypoints are the table
// functions (update/tick/restore) driven from the JS pilot; those touch atomics and a
// pending flag, never the rooted handles. A busy guard prevents reentrancy because
// SetActive can re-enter the lifecycle hooks; the install mutex is never held across a
// runtime_invoke.

namespace {

typedef void (*UpdateFn)(void *, int64_t, void *);
typedef void (*LifeFn)(void *, void *);

std::mutex g_install_mutex;                    // serialises install() and publishes originals
PgrRocketMiniConfig g_cfg{};
PgrRocketMiniShape g_sphere{}, g_box{};
PgrRocketMiniVisual g_visual{};                   // validated copy, published under the mutex
bool g_install_failed = false;                 // sticky; guarded by g_install_mutex

std::atomic<int>     g_active{0};          // fully installed and live
std::atomic<int>     g_failed{0};          // latched managed-exception failure (stop collecting)
std::atomic<int>     g_policy_on{0};       // adapter says collection permitted
std::atomic<int64_t> g_deadline{0};        // BOOTTIME ms past which a cached allowed is dead
std::atomic<int>     g_restore_pending{0}; // off-thread restore request
std::atomic<int64_t> g_unity_tid{0};       // latched OS tid of the Unity thread (tick only)
std::atomic<int>     g_busy{0};            // reentrancy guard for Unity-thread state mutation

std::atomic<UpdateFn> g_orig_update{nullptr};
std::atomic<LifeFn>   g_orig_exit{nullptr};
std::atomic<LifeFn>   g_orig_hide{nullptr};
std::atomic<LifeFn>   g_orig_destroy{nullptr};

// Rooted snapshot, written and read only on the Unity thread under the busy guard.
struct MiniState {
    void *challenge;   // gchandle
    void *transform;   // gchandle
    void *go;          // gchandle
    int   active;
    void *collider; // optional shape-only path, separately rooted
    PgrRocketMiniShape shape;
    float dimensions[3];
    float enlarged[3];
    float original_scale[3];
    float visual_scale[3];
    bool visual_scaled;
    unsigned render_count;
    struct RenderSnapshot { void *root; uint8_t force_off; } renders[16];
};
MiniState g_mini{};
bool g_have_mini = false;

// Emit each fixed diagnostic once per process, without player data or addresses.
void shape_diagnostic(unsigned bit, const char *reason) {
    static std::atomic<unsigned> reported{0};
    if (reported.fetch_or(bit) & bit) return;
#ifdef __ANDROID__
    __android_log_print(ANDROID_LOG_INFO, "PGRazorMini", "%s", reason);
#else
    (void)reason;
#endif
}

int64_t boottime_ms() {
    struct timespec ts;
    clock_gettime(CLOCK_BOOTTIME, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}
int64_t native_tid() { return (int64_t)syscall(SYS_gettid); }

bool on_unity_thread() {
    int64_t u = g_unity_tid.load(std::memory_order_acquire);
    return u != 0 && native_tid() == u;
}

bool native_allowed() {
    return g_active.load() && !g_failed.load() && g_policy_on.load() &&
           boottime_ms() < g_deadline.load();
}

// runtime_invoke wrapper. A thrown managed exception latches the module into a failed
// state (collection stops; restore is preserved) and yields a null result, mirroring the
// legacy JS path where invoke() throws and rocket.js sets rocketState.failed = true.
void *inv(void *method, void *obj, void **params) {
    void *exc = nullptr;
    void *r = g_cfg.runtime_invoke(method, obj, params, &exc);
    if (exc) { g_failed.store(1); return nullptr; }
    return r;
}

bool exact(void *obj, void *klass) {
    return obj && g_cfg.object_class(obj) == klass;
}

// UnityEngine.Object.op_Implicit(object) -> bool. Static; the reference arg is passed
// directly as the object pointer.
bool obj_alive(void *o) {
    if (!o) return false;
    void *params[1] = { o };
    void *boxed = inv(g_cfg.alive_method, nullptr, params);
    if (!exact(boxed, g_cfg.bool_class)) return false;
    uint8_t *b = (uint8_t *)g_cfg.object_unbox(boxed);
    return b && *b == 1;
}

// GameObject.activeSelf -> boxed bool. Tri-state: 1/0 for a valid boxed bool, or -1 when
// the result is missing / not a boxed System.Boolean / not a 0-or-1 byte. Returning -1
// (never defaulting to 0) prevents snapshotting a bogus "inactive" original and later
// overwriting the game's real state with SetActive(false).
int obj_active(void *go) {
    void *boxed = inv(g_cfg.active_method, go, nullptr);
    if (!boxed || !exact(boxed, g_cfg.bool_class)) return -1;
    uint8_t *b = (uint8_t *)g_cfg.object_unbox(boxed);
    if (!b) return -1;
    if (*b == 0) return 0;
    if (*b == 1) return 1;
    return -1;
}

void set_active(void *go, int on) {
    uint8_t b = on ? 1 : 0;
    void *params[1] = { &b };
    inv(g_cfg.setActive_method, go, params);
}

// Resolve exact collider geometry only; never change unrelated renderers or
// global camera/shader state. Unsupported, unbound or out-of-range geometry returns
// false and the caller performs NO mutation at all (no scale, no SetActive).
bool prepare_shape(void *collider, const float scale[3], MiniState &snap) {
    if (!scale) return false;
    const bool sphere = g_sphere.klass && exact(collider, g_sphere.klass);
    const auto shape = sphere ? g_sphere : g_box;
    if (!shape.klass || !exact(collider, shape.klass)) {
        shape_diagnostic(1u, (!g_sphere.klass && !g_box.klass) ?
            "shape-unbound" : "shape-unsupported");
        return false;
    }
    // Keep a root across all subsequent invocations.
    void *h = g_cfg.root(collider, 0);
    if (!h) return false;
    void *boxed = inv(shape.get_size, g_cfg.target(h), nullptr);
    const float *v = exact(boxed, shape.value_class) ?
        static_cast<const float *>(g_cfg.object_unbox(boxed)) : nullptr;
    if (!v) { shape_diagnostic(2u, "shape-value-unavailable"); g_cfg.free_root(h); return false; }

    pgr_rocket_shape::Input in{};
    in.sphere = sphere;
    in.axes = pgr_rocket_shape::kMaxAxes;
    in.geometry_bound = true;
    for (int i = 0; i < pgr_rocket_shape::kMaxAxes; ++i) {
        in.scale[i] = scale[i];
        // Single is one float; do not read past the unboxed radius.
        in.dimensions[i] = (sphere && i != 0) ? 0.f : v[i];
    }
    if (sphere) in.dimensions[1] = in.dimensions[2] = 0.f; // radius only; not used

    const pgr_rocket_shape::Result r = pgr_rocket_shape::enlarge(in);
    if (!r.ok) { g_cfg.free_root(h); shape_diagnostic(4u, "shape-geometry-rejected"); return false; }
    shape_diagnostic(sphere ? 8u : 16u, sphere ? "shape-sphere-ready" : "shape-box-ready");
    snap.collider = h;
    snap.shape = shape;
    for (int i = 0; i < pgr_rocket_shape::kMaxAxes; ++i) {
        snap.dimensions[i] = in.dimensions[i];
        snap.enlarged[i] = r.enlarged[i];
    }
    return true;
}
void set_dimensions(const MiniState &snap, const float *v) {
    void *obj=g_cfg.target(snap.collider);
    if (!exact(obj,snap.shape.klass) || !obj_alive(obj)) return;
    void *args[]={const_cast<float *>(v)};
    inv(snap.shape.set_size,obj,args);
}

// Snapshot the complete set of renderers on this exact tap object, never the scene,
// camera, parent or another object. No writes occur during discovery.
void free_visual(MiniState &snap) {
    for (unsigned i=0; i<snap.render_count; ++i) g_cfg.free_root(snap.renders[i].root);
    snap.render_count=0;
}
int read_force_off(void *renderer) {
    void *boxed=inv(g_visual.get_force_off,renderer,nullptr);
    if (!exact(boxed,g_cfg.bool_class)) return -1;
    const auto *p=static_cast<const uint8_t *>(g_cfg.object_unbox(boxed));
    return p && *p<=1 ? *p : -1;
}
bool write_force_off(void *renderer,uint8_t value) {
    if (!renderer || !obj_alive(renderer)) return false;
    void *args[]={&value};
    void *exc=nullptr;
    g_cfg.runtime_invoke(g_visual.set_force_off,renderer,args,&exc);
    if (exc) { g_failed.store(1); return false; }
    return read_force_off(renderer)==value;
}
bool prepare_visual(void *collider,void *go_root,const float sc[3],MiniState &snap) {
    if (!g_visual.renderer_class || !collider) return false;
    for (int i=0;i<3;++i) {
        const float v=std::fmax(1.f,std::fabs(sc[i]))*1000.f;
        if (!std::isfinite(v) || v>100000.f) return false;
        snap.original_scale[i]=sc[i]; snap.visual_scale[i]=v;
    }
    void *type=g_visual.type_object(g_visual.renderer_type);
    void *type_root=type?g_cfg.root(type,0):nullptr;
    if (!type_root) return false;
    void *args[]={g_cfg.target(type_root)};
    void *array=inv(g_visual.get_components,collider,args);
    void *array_root=array?g_cfg.root(array,0):nullptr;
    g_cfg.free_root(type_root);
    if (!array_root) return false;
    const uintptr_t count=g_visual.array_length(g_cfg.target(array_root));
    bool ok=count>0 && count<=16;
    for (int i=0;ok && i<static_cast<int>(count);++i) {
        void *index[]={&i};
        void *renderer=inv(g_visual.array_get,g_cfg.target(array_root),index);
        if (!renderer || !g_visual.assignable(g_visual.renderer_class,g_cfg.object_class(renderer))) {ok=false;break;}
        void *root=g_cfg.root(renderer,0);
        if (!root) {ok=false;break;}
        void *owner=inv(g_cfg.gameObject_method,g_cfg.target(root),nullptr);
        int off=owner==g_cfg.target(go_root)?read_force_off(g_cfg.target(root)):-1;
        if (off<0 || g_failed.load()) {g_cfg.free_root(root);ok=false;break;}
        snap.renders[snap.render_count++]={root,static_cast<uint8_t>(off)};
    }
    g_cfg.free_root(array_root);
    if (!ok) free_visual(snap);
    return ok && snap.render_count>0;
}
bool write_local_scale(const MiniState &snap,const float *scale) {
    void *tr=g_cfg.target(snap.transform);
    if (!tr || !obj_alive(tr)) return false;
    void *args[]={const_cast<float *>(scale)};
    void *exc=nullptr;
    g_cfg.runtime_invoke(g_cfg.setScale_method,tr,args,&exc);
    if(exc)g_failed.store(1);
    return !exc;
}
bool apply_visual(MiniState &snap) {
    if (!snap.render_count) return false;
    // No transform write until EVERY local renderer has been suppressed and verified.
    for(unsigned i=0;i<snap.render_count;++i)
        if(!write_force_off(g_cfg.target(snap.renders[i].root),1)) return false;
    snap.visual_scaled=true; // record before call in case it partially succeeds
    return write_local_scale(snap,snap.visual_scale);
}

// Snapshot is detached before any restoring call so a SetActive-driven re-entry cannot
// see a half-restored state (it sees g_have_mini == false, intentionally). Handles are
// freed only after the restore calls complete. Restore runs regardless of the failed
// latch so an owned target is always put back. No mutex is held across runtime_invoke.
void do_restore() {
    if (g_busy.exchange(1)) { g_restore_pending.store(1); return; }
    // Nothing owned: a pending request is already satisfied, so clear it (a reentrant
    // SetActive during a prior restore can have re-raised it) rather than letting a stale
    // flag undo the next fresh collection.
    if (!g_have_mini) { g_restore_pending.store(0); g_busy.store(0); return; }
    MiniState snap = g_mini;
    g_have_mini = false;              // detach first
    g_restore_pending.store(0);
    void *go = g_cfg.target(snap.go);
    if (snap.collider) set_dimensions(snap, snap.dimensions);
    // Restore scale before making the renderer visible, including error/exit paths.
    if (snap.visual_scaled) write_local_scale(snap,snap.original_scale);
    for(unsigned i=0;i<snap.render_count;++i)
        write_force_off(g_cfg.target(snap.renders[i].root),snap.renders[i].force_off);
    free_visual(snap);
    if (go && obj_alive(go)) set_active(go, snap.active);
    if (snap.collider) g_cfg.free_root(snap.collider);
    g_cfg.free_root(snap.challenge);
    g_cfg.free_root(snap.transform);
    g_cfg.free_root(snap.go);
    g_busy.store(0);
}

// Snapshot original dimensions/active on first collect; never enlarge the visual
// transform. Only runs for a verified shape when running + alive/exact and
// external collection is allowed. Every managed reference used across an invoke is rooted
// first; the snapshot is fully validated before g_mini/g_have_mini are published, so a
// failure never leaks a root and never introduces a false "successful" collection.
void do_collect(void *challenge) {
    if (!native_allowed()) return;
    if (!exact(challenge, g_cfg.mini_class) || !obj_alive(challenge)) return;
    // running: read like present(challenge, running_field, 1) -> non-zero byte.
    uint8_t running = 0;
    g_cfg.field_get_value(challenge, g_cfg.running_field, &running);
    if (running == 0) {
        // Update can end the challenge before a lifecycle callback is delivered.
        // Do not leave an enlarged visual object alive until the policy lease expires.
        if (g_have_mini && g_cfg.target(g_mini.challenge) == challenge) do_restore();
        return;
    }

    // A different challenge means the previous one is stale: restore it first.
    if (g_have_mini && g_cfg.target(g_mini.challenge) != challenge) do_restore();

    if (g_busy.exchange(1)) return;

    if (!g_have_mini) {
        // Root the challenge BEFORE any invoke, so the collider it owns stays reachable
        // across the transform/gameObject calls (no unrooted managed refs across invokes).
        void *ch_h = g_cfg.root(challenge, 0);
        if (!ch_h) { g_busy.store(0); return; }

        void *collider = nullptr;
        g_cfg.field_get_value(challenge, g_cfg.collider_field, &collider);
        if (!collider || !obj_alive(collider)) { g_cfg.free_root(ch_h); g_busy.store(0); return; }

        void *transform = inv(g_cfg.transform_method, collider, nullptr);
        if (!transform) { g_cfg.free_root(ch_h); g_busy.store(0); return; }
        // Root the transform BEFORE invoking gameObject (the next invoke could GC it).
        void *tr_h = g_cfg.root(transform, 0);
        if (!tr_h) { g_cfg.free_root(ch_h); g_busy.store(0); return; }

        void *go = inv(g_cfg.gameObject_method, collider, nullptr);
        if (!go) { g_cfg.free_root(ch_h); g_cfg.free_root(tr_h); g_busy.store(0); return; }
        void *go_h = g_cfg.root(go, 0);
        if (!go_h) { g_cfg.free_root(ch_h); g_cfg.free_root(tr_h); g_busy.store(0); return; }

        // localScale -> boxed Vector3. Copy the three floats immediately, before the next
        // invoke (obj_active) could GC the box.
        void *boxed_scale = inv(g_cfg.scale_method, g_cfg.target(tr_h), nullptr);
        float *sv = boxed_scale ? (float *)g_cfg.object_unbox(boxed_scale) : nullptr;
        float sc[3];
        bool ok = sv != nullptr;
        for (int i = 0; i < 3 && ok; i++) {
            sc[i] = sv[i];
            if (!std::isfinite(sc[i]) || std::fabs(sc[i]) >= 10000.0f) ok = false;
        }
        // activeSelf: must be a valid boxed bool (tri-state). An unknown/invalid value
        // aborts the whole snapshot rather than guessing false.
        int act = ok ? obj_active(g_cfg.target(go_h)) : -1;
        if (!ok || act < 0) {
            g_cfg.free_root(ch_h);
            g_cfg.free_root(tr_h);
            g_cfg.free_root(go_h);
            g_busy.store(0);
            return;
        }

        // Snapshot fully valid -> publish (g_have_mini last).
        g_mini = {};
        // Re-read the rooted challenge's collider after the managed calls above.
        void *current_collider=nullptr;
        g_cfg.field_get_value(g_cfg.target(ch_h),g_cfg.collider_field,&current_collider);
        // Prefer the historical functional geometry only when the exact local
        // renderer set is independently proven and can be made invisible.
        const bool visual_ready=current_collider && prepare_visual(current_collider,go_h,sc,g_mini);
        const bool shape_ready = !visual_ready && !g_failed.load() && current_collider && prepare_shape(current_collider,sc,g_mini);
        if ((!shape_ready && !visual_ready) || g_failed.load()) {
            free_visual(g_mini);
            if (g_mini.collider) g_cfg.free_root(g_mini.collider);
            g_cfg.free_root(ch_h); g_cfg.free_root(tr_h); g_cfg.free_root(go_h);
            g_busy.store(0); return;
        }
        g_mini.challenge = ch_h;
        g_mini.transform = tr_h;
        g_mini.go = go_h;
        g_mini.active = act;
        g_have_mini = true;
    }

    if(g_mini.render_count) {
        if(!apply_visual(g_mini)) {
            shape_diagnostic(32u,"local-visual-suppression-failed");
            g_busy.store(0); do_restore(); return;
        }
        shape_diagnostic(64u,"local-visual-suppressed");
    } else {
        set_dimensions(g_mini,g_mini.enlarged);
    }
    set_active(g_cfg.target(g_mini.go), 1);
    g_busy.store(0);
}

void maybe_restore_on_thread() {
    if (!g_have_mini) return;
    if (g_restore_pending.load() || !native_allowed()) { do_restore(); return; }
    void *challenge = g_cfg.target(g_mini.challenge);
    if (!exact(challenge, g_cfg.mini_class) || !obj_alive(challenge)) {
        do_restore();
        return;
    }
    // The verified running field is also a lifecycle boundary when Exit/Hide
    // arrives late or not at all. Restore on the proven Unity thread.
    uint8_t running = 0;
    g_cfg.field_get_value(challenge, g_cfg.running_field, &running);
    if (running == 0) do_restore();
}

// --- original publication ---------------------------------------------------------
// A replacement that runs before install has published its original would otherwise
// chain through a null pointer and silently swallow the game's own method. If the slot
// is not yet visible, synchronise on the install mutex (held by install across the whole
// patch+publish sequence, and never across a runtime_invoke). After the mutex a live hook
// with a null original is impossible, so abort rather than skip the original.
template <class Fn>
Fn original_for(std::atomic<Fn> &slot) {
    Fn o = slot.load(std::memory_order_acquire);
    if (!o) {
        std::lock_guard<std::mutex> lk(g_install_mutex);
        o = slot.load(std::memory_order_acquire);
    }
    if (!o) std::abort();
    return o;
}

// --- hook replacements (run on the Unity thread) ---------------------------------

void repl_update(void *self, int64_t a1, void *method) {
    // The hook NEVER learns or overwrites the Unity tid; it only gates on the tid latched
    // by tick(). Restore/collect touch rooted state and so require the Unity thread.
    if (g_active.load() && on_unity_thread()) {
        maybe_restore_on_thread(); // clear stale/no-longer-allowed state first
    }
    // Chain the original THEN collect (original published before the hook went live).
    original_for(g_orig_update)(self, a1, method);
    if (g_active.load() && on_unity_thread() && native_allowed()) do_collect(self);
}

void restore_if_owned(void *self) {
    if (!g_active.load()) return;
    if (!on_unity_thread()) return; // only touch rooted state on the proven Unity thread
    if (g_have_mini && g_cfg.target(g_mini.challenge) == self) {
        if (g_busy.load()) g_restore_pending.store(1);
        else do_restore();
    }
    // A minigame lifecycle event ends this challenge instance; clear the transient
    // managed-exception latch so the next challenge gets a fresh attempt, matching the
    // legacy per-encounter reset. Restoration above is unaffected by the latch.
    g_failed.store(0);
}

// Lifecycle restore happens BEFORE the original runs.
void repl_exit(void *self, void *method)    { restore_if_owned(self); original_for(g_orig_exit)(self, method); }
void repl_hide(void *self, void *method)    { restore_if_owned(self); original_for(g_orig_hide)(self, method); }
void repl_destroy(void *self, void *method) { restore_if_owned(self); original_for(g_orig_destroy)(self, method); }

// --- config validation -------------------------------------------------------------

bool config_pointers_ok(const PgrRocketMiniConfig *c) {
    const void *const ptrs[] = {
        c->update_addr, c->exit_addr, c->hide_addr, c->destroy_addr,
        c->transform_method, c->gameObject_method, c->scale_method, c->active_method,
        c->setScale_method, c->setActive_method, c->alive_method,
        c->collider_field, c->running_field, c->mini_class, c->bool_class,
        (const void *)c->root, (const void *)c->free_root, (const void *)c->target,
        (const void *)c->object_class, (const void *)c->object_unbox,
        (const void *)c->runtime_invoke, (const void *)c->field_get_value};
    for (const void *p : ptrs)
        if (p == nullptr) return false;
    return true;
}

// --- table functions -------------------------------------------------------------

// Hook one site. On a reported success with a null trampoline, undo that exact site so
// the game's original runs unmodified, and report failure.
int hook_site(void *addr, void *repl, void **orig) {
    *orig = nullptr;
    int rc = pgrazor_hook(addr, repl, orig);
    if (rc == 0 && *orig == nullptr) { pgrazor_unhook(addr); return PGR_RMINI_ERR_HOOK_FAILED; }
    return rc == 0 ? PGR_RMINI_OK : PGR_RMINI_ERR_HOOK_FAILED;
}

int api_install(const PgrRocketMiniConfig *cfg) {
    if (!cfg) return PGR_RMINI_ERR_NULL_ARG;

    std::lock_guard<std::mutex> lk(g_install_mutex);
    if (g_install_failed) return PGR_RMINI_ERR_HOOK_FAILED; // never re-patch after a partial failure

    const bool v1=cfg->version==1 && cfg->size==sizeof(PgrRocketMiniConfig);
    const bool v2=cfg->version==2 && cfg->size==sizeof(PgrRocketMiniConfigV2);
    const bool v3=cfg->version==3 && cfg->size==sizeof(PgrRocketMiniConfigV3);
    if ((!v1 && !v2 && !v3) || !config_pointers_ok(cfg)) return PGR_RMINI_ERR_BAD_CONFIG;
    PgrRocketMiniShape sphere{},box{};
    PgrRocketMiniVisual visual{};
    if(v3) {
        visual=reinterpret_cast<const PgrRocketMiniConfigV3 *>(cfg)->visual;
        if(!visual.renderer_class||!visual.renderer_type||!visual.get_components||
           !visual.get_force_off||!visual.set_force_off||!visual.array_get||
           !visual.type_object||!visual.array_length||!visual.assignable)
            return PGR_RMINI_ERR_BAD_CONFIG;
    }
    if(v2 || v3) {
        const auto *ext=reinterpret_cast<const PgrRocketMiniConfigV2 *>(cfg);
        sphere=ext->sphere; box=ext->box;
        auto valid=[](const PgrRocketMiniShape &s) {
            return (!s.klass && !s.get_size && !s.set_size && !s.value_class) ||
                   (s.klass && s.get_size && s.set_size && s.value_class);
        };
        if(!valid(sphere)||!valid(box)||(sphere.klass&&sphere.klass==box.klass))
            return PGR_RMINI_ERR_BAD_CONFIG;
    }

    if (g_active.load(std::memory_order_acquire)) {
        // Hooks live for the process lifetime: an identical repeat is a no-op success, a
        // different config is refused.
        return std::memcmp(&g_cfg, cfg, sizeof(g_cfg)) == 0 &&
               std::memcmp(&g_sphere,&sphere,sizeof(sphere))==0 &&
               std::memcmp(&g_box,&box,sizeof(box))==0 &&
               std::memcmp(&g_visual,&visual,sizeof(visual))==0
                   ? PGR_RMINI_OK
                   : PGR_RMINI_ERR_ALREADY_BOUND;
    }

    g_cfg = *cfg; // publish before any callback can read it (still under the mutex)
    g_sphere=sphere; g_box=box; g_visual=visual;
    g_have_mini = false;
    g_failed.store(0);
    g_policy_on.store(0);
    g_deadline.store(0);
    g_restore_pending.store(0);
    g_busy.store(0);

    void *u = nullptr, *e = nullptr, *h = nullptr, *d = nullptr;
    int rc_u = hook_site(cfg->update_addr,  (void *)repl_update,  &u);
    int rc_e = hook_site(cfg->exit_addr,    (void *)repl_exit,    &e);
    int rc_h = hook_site(cfg->hide_addr,    (void *)repl_hide,    &h);
    int rc_d = hook_site(cfg->destroy_addr, (void *)repl_destroy, &d);

    if (rc_u != PGR_RMINI_OK || rc_e != PGR_RMINI_OK ||
        rc_h != PGR_RMINI_OK || rc_d != PGR_RMINI_OK) {
        // Partial failure is permanent and inert: cleanly-patched sites keep their valid
        // trampoline and merely chain the original (g_active stays 0, so collection can
        // never arm); sites that failed/were undone run the game's original unmodified. No
        // second engine is ever stacked and no later install repatches.
        g_orig_update.store((UpdateFn)u, std::memory_order_release);
        g_orig_exit.store((LifeFn)e, std::memory_order_release);
        g_orig_hide.store((LifeFn)h, std::memory_order_release);
        g_orig_destroy.store((LifeFn)d, std::memory_order_release);
        g_install_failed = true;
        return PGR_RMINI_ERR_HOOK_FAILED;
    }

    g_orig_update.store((UpdateFn)u, std::memory_order_release);
    g_orig_exit.store((LifeFn)e, std::memory_order_release);
    g_orig_hide.store((LifeFn)h, std::memory_order_release);
    g_orig_destroy.store((LifeFn)d, std::memory_order_release);
    g_active.store(1, std::memory_order_release); // publish last
    return PGR_RMINI_OK;
}

int api_update(int allowed, int64_t lease_remaining_ms) {
    if (allowed) {
        int64_t lease = lease_remaining_ms;
        if (lease < 0) lease = 0;
        if (lease > 2500) lease = 2500;
        g_policy_on.store(1);
        g_deadline.store(boottime_ms() + lease);
    } else {
        g_policy_on.store(0); // policy only; restoration is left to tick/lifecycle hooks
    }
    return g_active.load();
}

int api_tick(int proven_unity_tick) {
    if (!g_active.load()) return 0;
    if (proven_unity_tick) {
        // tick() is invoked by the host's verified Unity loop: the running thread IS the
        // Unity thread, so latch its tid here (the sole authority) and process a pending
        // restore. A non-proven tick touches no rooted state.
        g_unity_tid.store(native_tid(), std::memory_order_release);
        maybe_restore_on_thread();
    }
    return 1;
}

int api_restore() {
    if (!g_active.load()) return 0;
    g_policy_on.store(0); // immediate stop of collection
    if (on_unity_thread() && !g_busy.load()) do_restore();
    else g_restore_pending.store(1); // off-thread: next on-thread hook/tick restores
    return 1;
}

} // namespace

extern "C" const void *pgrazor_rocket_minigame_api(uint32_t version) {
    static const PgrRocketMiniApi api = {
        1, (uint32_t)sizeof(PgrRocketMiniApi),
        api_install, api_update, api_tick, api_restore
    };
    return version == 1 ? &api : nullptr;
}
