// Host-only test: actual alpha-only code with synthetic white/nonwhite materials.
#define PGR_MINI_DIRECT_TEST
#include "rocket_minigame.cpp"
#include <cassert>
int pgrazor_hook(void *,void *,void **) {return -1;}
int pgrazor_unhook(void *) {return 0;}
namespace {
int renderer_tag,bool_tag,go_tag,transform_tag,collider_tag,array_tag,type_tag,material_tag,color_tag;
int get_components_m,get_owner_m,get_material_m,get_shared_m,get_color_m,set_color_m,array_get_m,alive_m,scale_m,active_m;
struct Obj {void *klass; bool alive=true; uint8_t flag=0; Obj *owner=nullptr; float value[4]={1,1,1,0.5f};};
Obj go{&go_tag},other{&go_tag},tr{&transform_tag},collider{&collider_tag},type{&type_tag},array{&array_tag},shared_array{&array_tag};
Obj renderer{&renderer_tag},shared{&material_tag},material{&material_tag},boxed{&bool_tag},color{&color_tag};
uintptr_t count=1,material_count=1;int roots=0,scale_writes=0,color_writes=0;bool fail_alpha=false;
void *root(void *p,int){if(p)++roots;return p;}
void free_root(void *p){if(p)--roots;}
void *target(void *p){return p;}
void *klass(void *p){return static_cast<Obj*>(p)->klass;}
void *unbox(void *p){auto *o=static_cast<Obj*>(p);return o->klass==&bool_tag?static_cast<void*>(&o->flag):static_cast<void*>(o->value);}
void *type_object(void *){return &type;}
uintptr_t length(void *p){return p==&array?count:material_count;}
bool assignable(void *x,void *y){return x==y;}
void *invoke(void *m,void *obj,void **args,void **exc){
 *exc=nullptr;
 if(m==&get_components_m)return &array;
 if(m==&array_get_m)return obj==&array?static_cast<void*>(&renderer):static_cast<void*>(&shared);
 if(m==&get_owner_m)return static_cast<Obj*>(obj)->owner;
 if(m==&get_shared_m)return &shared_array;
 if(m==&get_material_m)return &material;
 if(m==&alive_m){boxed.flag=static_cast<Obj*>(args[0])->alive;return &boxed;}
 if(m==&get_color_m){std::memcpy(color.value,static_cast<Obj*>(obj)->value,16);return &color;}
 if(m==&set_color_m){
    assert(obj==&material);
    const float *v=static_cast<float*>(args[0]);
    if(fail_alpha&&v[3]==0.f){*exc=&material;return nullptr;}
    std::memcpy(material.value,v,16);++color_writes;return nullptr;
 }
 if(m==&scale_m){const float *v=static_cast<float*>(args[0]);if(v[0]>1)assert(material.value[3]==0.f);
   std::memcpy(static_cast<Obj*>(obj)->value,v,12);++scale_writes;}
 if(m==&active_m)return nullptr;
 return nullptr;
}
void setup(){
 renderer.owner=&go;g_failed.store(0);g_cfg.root=root;g_cfg.free_root=free_root;g_cfg.target=target;g_cfg.object_class=klass;
 g_cfg.object_unbox=unbox;g_cfg.runtime_invoke=invoke;g_cfg.bool_class=&bool_tag;
 g_cfg.gameObject_method=&get_owner_m;g_cfg.alive_method=&alive_m;g_cfg.setScale_method=&scale_m;g_cfg.setActive_method=&active_m;
 g_visual={&renderer_tag,&type_tag,&get_components_m,&get_shared_m,&get_material_m,&material_tag,&color_tag,&get_color_m,&set_color_m,&array_get_m,type_object,length,assignable};
}
void own(MiniState &s){s.transform=root(&tr,0);s.go=root(&go,0);s.challenge=root(&collider,0);g_mini=s;g_have_mini=true;}
}
int main(){
 setup();MiniState s{};float scale[]={1,1,1};
 shared.value[0]=material.value[0]=0.99f;shared.value[1]=material.value[1]=0.98f;
 assert(prepare_visual(&collider,&go,scale,s)&&s.material==&material&&roots==1);
 assert(material.value[3]==0.5f&&color_writes==0&&scale_writes==0);
 own(s);assert(apply_visual(g_mini));
 assert(material.value[0]==0.99f&&material.value[1]==0.98f&&material.value[2]==1.f&&material.value[3]==0.f);
 assert(shared.value[3]==0.5f&&tr.value[0]==1000.f&&scale_writes==1);
 do_restore();assert(roots==0&&material.value[3]==0.5f&&tr.value[0]==1.f&&scale_writes==2);
 s={};shared.value[0]=0.1f;assert(!prepare_visual(&collider,&go,scale,s)&&roots==0);shared.value[0]=0.99f;
 s={};shared.value[3]=1.f;assert(!prepare_visual(&collider,&go,scale,s)&&roots==0);shared.value[3]=0.5f;
 s={};renderer.owner=&other;assert(!prepare_visual(&collider,&go,scale,s)&&roots==0);renderer.owner=&go;
 s={};count=2;assert(!prepare_visual(&collider,&go,scale,s)&&roots==0);count=1;
 s={};material_count=2;assert(!prepare_visual(&collider,&go,scale,s)&&roots==0);material_count=1;
 s={};assert(prepare_visual(&collider,&go,scale,s));own(s);fail_alpha=true;
 assert(!apply_visual(g_mini)&&scale_writes==2);fail_alpha=false;
 do_restore();assert(roots==0&&material.value[3]==0.5f&&tr.value[0]==1.f&&scale_writes==2);
 const uint32_t diag=pgrazor_rocket_minigame_take_diagnostics();
 assert((diag&128u)&&(diag&4096u)&&(diag&2048u));
 assert(pgrazor_rocket_minigame_take_diagnostics()==0);
 return 0;
}
