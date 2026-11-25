// Host-only test: actual alpha-only code with synthetic white/nonwhite materials.
#define PGR_MINI_DIRECT_TEST
#include "rocket_minigame.cpp"
#include <cassert>
int pgrazor_hook(void *,void *,void **) {return -1;}
int pgrazor_unhook(void *) {return 0;}
namespace {
int renderer_tag,bool_tag,go_tag,transform_tag,collider_tag,array_tag,type_tag,material_tag,color_tag,challenge_tag;
int get_components_m,get_owner_m,get_material_m,get_shared_m,get_color_m,set_color_m,array_get_m,alive_m,scale_m,active_m,get_scale_m,get_active_m,get_transform_m,running_f,collider_f;
struct Obj {void *klass; bool alive=true; uint8_t flag=0; Obj *owner=nullptr; float value[4]={1,1,1,0.5f};};
Obj go{&go_tag},other{&go_tag},tr{&transform_tag},collider{&collider_tag},type{&type_tag},array{&array_tag},shared_array{&array_tag};
Obj renderer{&renderer_tag},shared{&material_tag},material{&material_tag},boxed{&bool_tag},color{&color_tag},challenge{&challenge_tag};
uint8_t running=1;
void field(void *,void *f,void *out){if(f==&running_f)*static_cast<uint8_t*>(out)=running;else if(f==&collider_f)*static_cast<void**>(out)=&collider;}
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
 if(m==&get_owner_m)return obj==&collider?static_cast<void*>(&go):static_cast<Obj*>(obj)->owner;
 if(m==&get_transform_m)return &tr;
 if(m==&get_scale_m)return &tr;
 if(m==&get_active_m){boxed.flag=go.flag;return &boxed;}
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
 if(m==&scale_m){const float *v=static_cast<float*>(args[0]);
   std::memcpy(static_cast<Obj*>(obj)->value,v,12);++scale_writes;}
 if(m==&active_m){go.flag=*static_cast<uint8_t*>(args[0]);return nullptr;}
 return nullptr;
}
void setup(){
 renderer.owner=&go;g_failed.store(0);g_cfg.root=root;g_cfg.free_root=free_root;g_cfg.target=target;g_cfg.object_class=klass;
 g_cfg.object_unbox=unbox;g_cfg.runtime_invoke=invoke;g_cfg.bool_class=&bool_tag;
 g_cfg.gameObject_method=&get_owner_m;g_cfg.alive_method=&alive_m;g_cfg.setScale_method=&scale_m;g_cfg.setActive_method=&active_m;
 g_cfg.transform_method=&get_transform_m;g_cfg.scale_method=&get_scale_m;g_cfg.active_method=&get_active_m;
 g_cfg.mini_class=&challenge_tag;g_cfg.field_get_value=field;g_cfg.running_field=&running_f;g_cfg.collider_field=&collider_f;
 g_active.store(1);api_update(1,2500);
 g_visual={&renderer_tag,&type_tag,&get_components_m,&get_shared_m,&get_material_m,&material_tag,&color_tag,&get_color_m,&set_color_m,&array_get_m,type_object,length,assignable};
}
}
int main(){
 setup();
 shared.value[0]=material.value[0]=0.99f;shared.value[1]=material.value[1]=0.98f;
 // Historical scale/activate must run regardless of visual success.
 do_collect(&challenge);
 assert(g_have_mini&&roots==4&&go.flag==1&&tr.value[0]==1000.f);
 assert(material.value[0]==0.99f&&material.value[1]==0.98f&&material.value[2]==1.f&&material.value[3]==0.f);
 assert(shared.value[3]==0.5f);
 do_restore();assert(roots==0&&go.flag==0&&material.value[3]==0.5f&&tr.value[0]==1.f);
 shared.value[0]=0.1f;do_collect(&challenge);assert(g_have_mini&&roots==3&&tr.value[0]==1000.f&&material.value[3]==0.5f);
 do_restore();assert(roots==0&&tr.value[0]==1.f);shared.value[0]=0.99f;
 renderer.owner=&other;do_collect(&challenge);assert(g_have_mini&&tr.value[0]==1000.f&&roots==3);
 do_restore();assert(roots==0);renderer.owner=&go;
 fail_alpha=true;do_collect(&challenge);
 assert(g_have_mini&&tr.value[0]==1000.f&&go.flag==1&&!g_failed.load());
 fail_alpha=false;do_restore();assert(roots==0&&tr.value[0]==1.f&&material.value[3]==0.5f);
 auto saved=g_visual;g_visual={};do_collect(&challenge);
 assert(g_have_mini&&tr.value[0]==1000.f&&roots==3);do_restore();assert(roots==0);g_visual=saved;
 const uint32_t diag=pgrazor_rocket_minigame_take_diagnostics();
 assert((diag&128u)&&(diag&4096u)&&(diag&2048u));
 assert(pgrazor_rocket_minigame_take_diagnostics()==0);
 return 0;
}
