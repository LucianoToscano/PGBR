// Adapted from MIT PGRazor_Pokematrix de777d2b9; PGBR adds target-bound consent and freshness.
// Fast/Charged TM shortcuts on the Pokemon screen: reports where each move row's power number is on
// screen and, on request, starts the game's own TM flow (ApplyRerollItemService) for that Pokemon.
#include "tm_shortcut.h"
#include "shared_metadata.h"
#include "runtime_bootstrap.h"
#include "runtime_transport.h"
#include "tm_request_policy.h"
#include <cmath>
#include <initializer_list>
#include <android/log.h>
#include <dlfcn.h>
#include <time.h>
#include <unistd.h>
#include <atomic>
#include <cstring>
#include <mutex>

namespace {
using namespace pgr_shared_meta;
struct Failure { const char *where; };
void require(bool ok, const char *where) { if (!ok) throw Failure{where}; }
PgrBootBinding boot{};
const PgrSharedMetadataOutput *md = nullptr;
bool initialized = false, failed = false;
uint64_t owner = 0;
template <class F> F fn(const void *p) { require(p, "missing runtime export"); return reinterpret_cast<F>(const_cast<void *>(p)); }
uint64_t clock_ms() { timespec t{}; clock_gettime(CLOCK_BOOTTIME, &t); return uint64_t(t.tv_sec) * 1000 + t.tv_nsec / 1000000; }

struct Root {
  uintptr_t h = 0;
  Root() = default;
  explicit Root(void *v, bool weak = false) {
    if (!v) return;
    h = weak ? fn<uintptr_t (*)(void *, int)>(boot.runtime.gchandle_new_weakref)(v, 0) : fn<uintptr_t (*)(void *, int)>(boot.runtime.gchandle_new)(v, 0);
    require(h, "GC root");
  }
  Root(const Root &) = delete; Root &operator=(const Root &) = delete;
  Root(Root &&r) noexcept : h(r.h) { r.h = 0; }
  Root &operator=(Root &&r) noexcept { if (this != &r) { reset(); h = r.h; r.h = 0; } return *this; }
  ~Root() { reset(); }
  void reset() { if (h) { fn<void (*)(uintptr_t)>(boot.runtime.gchandle_free)(h); h = 0; } }
  void *get() const { return h ? fn<void *(*)(uintptr_t)>(boot.runtime.gchandle_get_target)(h) : nullptr; }
};
void *klass(void *o) { require(o, "null object"); return fn<void *(*)(void *)>(boot.runtime.object_get_class)(o); }
void *cls(const char *ns, const char *name) {
  const auto *api = static_cast<const PgrMetaApi *>(pgrazor_runtime_metadata_api(1)); require(api, "metadata API");
  PgrMetaClassQuery q{}; q.version = 1; q.size = sizeof(q); q.name_space = ns; q.name = name; q.canonical_match = PGR_MD_MATCH_EXACT;
  PgrMetaClassResult out{}; require(api->resolve_class(&q, &out) == 0, name); return const_cast<void *>(out.klass);
}
void *nested(void *k, const char *name) {
  void *it = nullptr;
  while (void *n = fn<void *(*)(void *, void **)>(boot.meta.class_get_nested_types)(k, &it)) {
    char *tn = fn<char *(*)(void *)>(boot.meta.type_get_name)(fn<void *(*)(void *)>(boot.meta.class_get_type)(n));
    size_t a = tn ? strlen(tn) : 0, b = strlen(name);
    bool match = a > b && !strcmp(tn + a - b, name) && (tn[a - b - 1] == '.' || tn[a - b - 1] == '/');
    if (tn) fn<void (*)(void *)>(boot.meta.mem_free)(tn);
    if (match) return n;
  }
  throw Failure{name};
}
// Prefer the non-generic overload; class_get_method_from_name also searches base classes.
void *method(void *k, const char *name, int count) {
  void *m = fn<void *(*)(void *, const char *, int)>(boot.meta.class_get_method_from_name)(k, name, count);
  if (m && fn<bool (*)(void *)>(boot.meta.method_is_generic)(m)) {
    m = nullptr;
    void *it = nullptr;
    while (void *x = fn<void *(*)(void *, void **)>(boot.meta.class_get_methods)(k, &it))
      if (!strcmp(fn<const char *(*)(void *)>(boot.meta.method_get_name)(x), name) &&
          fn<unsigned (*)(void *)>(boot.meta.method_get_param_count)(x) == unsigned(count) &&
          !fn<bool (*)(void *)>(boot.meta.method_is_generic)(x)) { m = x; break; }
  }
  require(m, name);
  return m;
}
void *invoke(void *m, void *o, void **args = nullptr) {
  void *ex = nullptr;
  void *v = fn<void *(*)(void *, void *, void **, void **)>(boot.runtime.runtime_invoke)(m, o, args, &ex);
  if (ex) {
    char detail[512]{};
    auto format = reinterpret_cast<void (*)(void *, char *, int)>(dlsym(boot.handle, "il2cpp_format_exception"));
    if (format) format(ex, detail, sizeof(detail));
    __android_log_print(ANDROID_LOG_ERROR, "PGRazorTm", "%s: %s", fn<const char *(*)(void *)>(boot.meta.method_get_name)(m), detail);
    throw Failure{"managed call failed"};
  }
  return v;
}
void *call(void *o, const char *name, int n = 0, void **args = nullptr) { return invoke(method(klass(o), name, n), o, args); }
template <class T> T unboxed(void *o) { require(o, "boxed value"); T v{}; memcpy(&v, fn<void *(*)(void *)>(boot.runtime.object_unbox)(o), sizeof(v)); return v; }
void *field_info(void *o, const char *name) {
  void *f = fn<void *(*)(void *, const char *)>(boot.meta.class_get_field_from_name)(klass(o), name); require(f, name); return f;
}
void *field_ref(void *o, const char *name) { void *v = nullptr; fn<void (*)(void *, void *, void *)>(boot.runtime.field_get_value)(o, field_info(o, name), &v); return v; }
int field_int(void *o, const char *name) { int v = 0; fn<void (*)(void *, void *, void *)>(boot.runtime.field_get_value)(o, field_info(o, name), &v); return v; }
void *managed_string(const char *s) { return fn<void *(*)(const char *)>(boot.runtime.string_new)(s); }
void *type_object(void *k) {
  auto get = reinterpret_cast<void *(*)(void *)>(dlsym(boot.handle, "il2cpp_type_get_object")); require(get, "type_get_object");
  return get(fn<void *(*)(void *)>(boot.meta.class_get_type)(k));
}
Root resolve(void *contract) {
  auto h = [](unsigned id) { return const_cast<void *>(md->opt_handles[id]); };
  Root type(type_object(h(H_IL_MAP))); void *args[] = {type.get()};
  Root map(invoke(h(H_IL_FIND_M), nullptr, args)); if (!map.get()) return {};
  Root container(invoke(h(H_IL_CONTAINER_M), map.get())); if (!container.get()) return {};
  Root target(type_object(contract)); void *resolve_args[] = {target.get()};
  return Root(invoke(h(H_IL_RESOLVE_M), container.get(), resolve_args));
}
bool active(void *component) {
  void *go = call(component, "get_gameObject");
  return go && unboxed<bool>(call(go, "get_activeInHierarchy"));
}
void *find(void *tr, const char *path) { void *a[] = {managed_string(path)}; return call(tr, "Find", 1, a); }
struct Vec3 { float x, y, z; };

constexpr int kFast = 1201, kCharged = 1202;  // ITEM_MOVE_REROLL_FAST_ATTACK / _SPECIAL_ATTACK
std::mutex state_mutex;
PgrTmRow rows[3]{};
uint64_t snapshot_token=0,snapshot_at=0,snapshot_session=0;
PgrTmRequest pending{};
std::atomic<bool> ui_allowed{false};
Root shown_panel,shown_proto;
int shown_moves[3]{};
uint64_t serial=0;
// The game pre-builds neighbouring Pokemon pages off screen, so keep a few and use the visible one.
Root panels[4], bag;
int next_panel = 0;
uint64_t hidden_until = 0;

struct Projector {
  void *camera = nullptr; bool overlay = true; float width = 1, height = 1, clip_low = -1e9f, clip_high = 1e9f;
  Vec3 screen(Vec3 world) {
    if (overlay) return world;
    void *a[] = {&world}; return unboxed<Vec3>(call(camera, "WorldToScreenPoint", 1, a));
  }
};
Projector projector(void *any) {
  Projector p;
  void *screen = cls("UnityEngine", "Screen");
  p.width = float(unboxed<int>(invoke(method(screen, "get_width", 0), nullptr)));
  p.height = float(unboxed<int>(invoke(method(screen, "get_height", 0), nullptr)));
  void *ta[] = {type_object(cls("UnityEngine", "Canvas"))};
  void *canvas = call(any, "GetComponentInParent", 1, ta);
  require(canvas, "canvas");
  canvas = call(canvas, "get_rootCanvas");
  p.overlay = unboxed<int>(call(canvas, "get_renderMode")) == 0;
  if (!p.overlay) { p.camera = call(canvas, "get_worldCamera"); p.overlay = p.camera == nullptr; }
  void *sa[] = {type_object(cls("UnityEngine.UI", "ScrollRect"))};
  void *scroll = call(any, "GetComponentInParent", 1, sa);
  void *viewport = scroll ? call(scroll, "get_viewport") : nullptr;
  if (viewport) {
    void *corners = fn<void *(*)(void *, uintptr_t)>(boot.runtime.array_new)(cls("UnityEngine", "Vector3"), 4);
    void *a[] = {corners}; call(viewport, "GetWorldCorners", 1, a);
    Vec3 *c = reinterpret_cast<Vec3 *>(static_cast<char *>(corners) + 32);  // IL2CPP array header
    p.clip_low = p.screen(c[0]).y; p.clip_high = p.screen(c[1]).y;
  }
  return p;
}

bool rows_for_unsafe(void *panel, PgrTmRow out[3]);
// A destroyed Unity panel still has a managed object; its first Unity call throws. Forget such panels.
bool rows_for(Root &r, PgrTmRow out[3]) {
  try { return rows_for_unsafe(r.get(), out); }
  catch (const Failure &) { r.reset(); return false; }
}
bool rows_for_unsafe(void *panel, PgrTmRow out[3]) {
  if (!panel || !active(panel)) return false;
  void *section = field_ref(panel, "moveSection");
  void *proto = field_ref(panel, "<PokemonProto>k__BackingField");
  if (!section || !proto) return false;
  void *tab = nullptr;
  for (const char *name : {"movesGymRaid", "movesCombat"}) {
    void *tr = field_ref(section, name);
    if (tr && active(tr)) { tab = tr; break; }
  }
  if (!tab) return false;
  if (!bag.get()) bag = resolve(cls("Niantic.Holoholo", "IItemBag"));
  require(bag.get() != nullptr, "item bag");
  int fast = kFast, charged = kCharged;
  void *fa[] = {&fast}, *ca[] = {&charged};
  const int counts[2] = {unboxed<int>(call(bag.get(), "GetItemCount", 1, fa)), unboxed<int>(call(bag.get(), "GetItemCount", 1, ca))};
  Projector p = projector(tab);
  const char *names[3] = {"Ability1", "Ability2", "Ability3"};
  const int moves[3] = {field_int(proto, "move1_"), field_int(proto, "move2_"), field_int(proto, "move3_")};
  for (int i = 0; i < 3; i++) {
    void *row = find(tab, names[i]);
    if (!row || !moves[i] || !active(row)) continue;
    void *number = find(row, "Top/PowerCostArea/Base");
    if (!number || !active(number)) continue;
    Vec3 s = p.screen(unboxed<Vec3>(call(number, "get_position")));
    if (!std::isfinite(s.x)||!std::isfinite(s.y)||p.width<=0||p.height<=0||s.y<p.clip_low||s.y>p.clip_high) continue;
    float x=s.x/p.width,y=1.0f-s.y/p.height;
    if(x<=0.2f||x>1.0f||y<0||y>1.0f)continue;
    out[i] = PgrTmRow{1,x,y,i==0?counts[0]:counts[1]};
  }
  for (int i = 0; i < 3; i++)
    if (out[i].visible && out[i].x > 0.2f && out[i].x <= 1.0f) return true;
  return false;
}
// Do not guess which of several prebuilt/sliding pages is selected.
void *unique_panel(PgrTmRow out[3]) {
  void *selected=nullptr;
  for(Root &r:panels){
    PgrTmRow candidate[3]{};
    if(!rows_for(r,candidate))continue;
    if(selected)return nullptr;
    selected=r.get();memcpy(out,candidate,sizeof(candidate));
  }
  return selected;
}
bool gate(uint64_t &session){
  if(!ui_allowed.load())return false;
  auto tr=static_cast<const PgrRuntimeTransportApi*>(pgrazor_runtime_transport_api(1));
  PgrRuntimeTransportSample a{},z{};int ready=0;
  if(!tr||tr->sample(&a)||!a.snapshot_fresh||a.fault||tr->get_bool(16,&ready)||!ready||
     tr->sample(&z)||!z.snapshot_fresh||z.fault||a.snapshot_seq!=z.snapshot_seq)return false;
  session=z.generation;return true;
}
void clear_publication(){
  std::lock_guard<std::mutex> lock(state_mutex);
  memset(rows,0,sizeof(rows));snapshot_token=0;snapshot_at=0;snapshot_session=0;pending={};
}
void clear_target(){
  shown_panel.reset();shown_proto.reset();memset(shown_moves,0,sizeof(shown_moves));clear_publication();
}
void publish(void *panel,void *proto,const PgrTmRow next[3],uint64_t session,uint64_t now){
  const char *names[]={"move1_","move2_","move3_"};
  int moves[3]{};for(int i=0;i<3;i++)moves[i]=field_int(proto,names[i]);
  bool changed=shown_panel.get()!=panel||shown_proto.get()!=proto||memcmp(moves,shown_moves,sizeof(moves))!=0;
  if(changed){shown_panel=Root(panel,true);shown_proto=Root(proto,true);memcpy(shown_moves,moves,sizeof(moves));}
  std::lock_guard<std::mutex> lock(state_mutex);
  if(!ui_allowed.load())return;
  if(changed||snapshot_session!=session||!snapshot_token){snapshot_token=++serial;pending={};}
  memcpy(rows,next,sizeof(rows));snapshot_at=now;snapshot_session=session;
}
void use(int row,void *panel,void *proto,uint64_t session) {
  Root target(proto);Root page(panel);
  require(target.get()&&page.get(), "Pokemon screen closed");
  // Resolve the current bag again; cached stock can belong to a former account.
  bag.reset();bag=resolve(cls("Niantic.Holoholo", "IItemBag"));
  require(bag.get()!=nullptr,"item bag");
  int selected_item=row==0?kFast:kCharged;void *stock_args[]={&selected_item};
  require(unboxed<int>(call(bag.get(),"GetItemCount",1,stock_args))>0,"TM unavailable");
  void *service = cls("Niantic.Holoholo.UI.Items", "ApplyRerollItemService");
  Root factory = resolve(cls("Niantic.Holoholo.UI.Items", "IApplyRerollItemServiceFactory"));
  require(factory.get() != nullptr, "TM factory");
  void *data_class = nested(service, "Data");
  Root data(fn<void *(*)(void *)>(boot.runtime.object_new)(data_class));
  int item = row == 0 ? kFast : kCharged;
  uint64_t current=0;require(gate(current)&&current==session&&active(page.get())&&field_ref(page.get(),"<PokemonProto>k__BackingField")==target.get(),"Target changed");
  void *sound = field_ref(page.get(), "errorSound");
  void *args[] = {&item, target.get(), sound, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr};
  invoke(method(data_class, ".ctor", 9), data.get(), args);
  require(gate(current)&&current==session&&active(page.get())&&field_ref(page.get(),"<PokemonProto>k__BackingField")==target.get(),"Target changed before confirmation");
  require(unboxed<int>(call(bag.get(),"GetItemCount",1,stock_args))>0,"TM unavailable");
  const char *move_fields[]={"move1_","move2_","move3_"};
  require(shown_moves[row]==field_int(target.get(),move_fields[row]),"Move changed before confirmation");
  void *create[] = {data.get()};
  call(factory.get(), "Create", 1, create);
  hidden_until = clock_ms() + 6000;
}
}  // namespace

extern "C" void pgr_tm_note_panel(void *panel) {
  if (!initialized || failed || !panel || !ui_allowed.load()) return;
  try {
    for (Root &r : panels) if (r.get() == panel) return;
    panels[next_panel] = Root(panel, true); next_panel = (next_panel + 1) % 4;
  } catch (...) {}
}

void pgr_tm_tick(uint64_t tid,const PgrSharedMetadataOutput *metadata,bool allowed){
  if(!tid||tid!=uint64_t(gettid())||(owner&&owner!=tid))return;
  uint64_t session=0;
  if(!allowed||failed||!gate(session)){clear_target();for(Root &r:panels)r.reset();bag.reset();return;}
  try{
    if(!initialized){
      auto api=static_cast<const PgrBootApi*>(pgrazor_runtime_bootstrap_api(1));
      boot.version=1;boot.size=sizeof(boot);
      require(api&&api->snapshot(&boot)==0&&metadata,"runtime bootstrap");
      md=metadata;owner=tid;initialized=true;
    }
    static uint64_t next_scan=0;
    uint64_t now=clock_ms();PgrTmRequest request{};uint64_t token=0,at=0,session_at=0;
    {
      std::lock_guard<std::mutex> lock(state_mutex);
      request=pending;pending={};token=snapshot_token;at=snapshot_at;session_at=snapshot_session;
    }
    if(!request.token&&now<next_scan)return;
    next_scan=now+100;
    if(now<hidden_until){clear_target();return;}
    PgrTmRow next[3]{};
    void *panel=unique_panel(next);
    if(!panel){clear_target();return;}
    void *proto=field_ref(panel,"<PokemonProto>k__BackingField");
    if(!proto){clear_target();return;}
    if(request.token){
      const char *names[]={"move1_","move2_","move3_"};
      bool same=shown_panel.get()==panel&&shown_proto.get()==proto;
      const bool valid_row=request.row>=0&&request.row<3;
      if(same&&valid_row) same=shown_moves[request.row]==field_int(proto,names[request.row]);
      if(valid_row&&pgr_tm_request_valid(request,token,at,session_at,session,now,same,next[request.row].visible,next[request.row].count)){
        use(request.row,panel,proto,session);clear_target();return;
      }
    }
    publish(panel,proto,next,session,now);
  }catch(const Failure &e){
    __android_log_print(ANDROID_LOG_WARN,"PGRazorTm","%s",e.where);clear_target();
    if(!initialized)failed=true;
  }catch(...){failed=true;clear_target();}
}
void pgr_tm_set_allowed(bool allowed){
  ui_allowed.store(allowed);
  if(!allowed)clear_publication();
}
void pgr_tm_stop(uint64_t tid){
  pgr_tm_set_allowed(false);
  if(initialized&&owner==tid&&tid==uint64_t(gettid())){
    clear_target();for(Root &r:panels)r.reset();bag.reset();
  }
}
uint64_t pgr_tm_state(PgrTmRow out[3]){
  std::lock_guard<std::mutex> lock(state_mutex);
  uint64_t now=clock_ms();
  if(!ui_allowed.load()||!snapshot_token||now<snapshot_at||now-snapshot_at>500){memset(out,0,sizeof(rows));return 0;}
  memcpy(out,rows,sizeof(rows));return snapshot_token;
}
bool pgr_tm_request(int row,uint64_t token){
  std::lock_guard<std::mutex> lock(state_mutex);uint64_t now=clock_ms();
  if(!ui_allowed.load()||row<0||row>=3||!token||token!=snapshot_token||now<snapshot_at||now-snapshot_at>500||!rows[row].visible||rows[row].count<=0||pending.token)return false;
  pending={token,now,row};return true;
}
