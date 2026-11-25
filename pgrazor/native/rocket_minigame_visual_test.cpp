// Host-only test of the actual renderer suppression/restore path with synthetic objects.
#define PGR_MINI_DIRECT_TEST
#include "rocket_minigame.cpp"
#include <cassert>
int pgrazor_hook(void *,void *,void **) {return -1;}
int pgrazor_unhook(void *) {return 0;}
namespace {
int renderer_tag, bool_tag, go_tag, transform_tag, collider_tag, array_tag, type_tag;
int get_components_m, get_owner_m, get_off_m, set_off_m, array_get_m, alive_m, scale_m, active_m;
struct Obj {void *klass; bool alive=true; uint8_t off=0; Obj *owner=nullptr; float sc[3]={1,1,1};};
Obj go{&go_tag}, other{&go_tag}, tr{&transform_tag}, collider{&collider_tag}, type{&type_tag}, array{&array_tag};
Obj a{&renderer_tag},b{&renderer_tag},boxed{&bool_tag};
Obj *items[2]={&a,&b}; uintptr_t count=2; int roots=0, writes=0; bool fail_second=false;
void *root(void *p,int){if(p)++roots;return p;}
void free_root(void *p){if(p)--roots;}
void *target(void *p){return p;}
void *klass(void *p){return static_cast<Obj*>(p)->klass;}
void *unbox(void *p){return &static_cast<Obj*>(p)->off;}
void *type_object(void *){return &type;}
uintptr_t length(void *){return count;}
bool assignable(void *x,void *y){return x==y;}
void *invoke(void *m,void *obj,void **args,void **exc){
 *exc=nullptr;
 if(m==&get_components_m)return &array;
 if(m==&array_get_m)return items[*static_cast<int*>(args[0])];
 if(m==&get_owner_m)return static_cast<Obj*>(obj)->owner;
 if(m==&alive_m){boxed.off=static_cast<Obj*>(args[0])->alive;return &boxed;}
 if(m==&get_off_m){boxed.off=static_cast<Obj*>(obj)->off;return &boxed;}
 if(m==&set_off_m){if(fail_second&&obj==&b&&*static_cast<uint8_t*>(args[0])){*exc=&b;return nullptr;}
   static_cast<Obj*>(obj)->off=*static_cast<uint8_t*>(args[0]);return nullptr;}
 if(m==&scale_m){const float *v=static_cast<float*>(args[0]);if(v[0]>1)assert(a.off&&b.off);
   std::memcpy(static_cast<Obj*>(obj)->sc,v,12);++writes;}
 if(m==&active_m)return nullptr;
 return nullptr;
}
void setup(){
 a.owner=&go;b.owner=&go;a.off=0;b.off=1;g_failed.store(0);
 g_cfg.root=root;g_cfg.free_root=free_root;g_cfg.target=target;g_cfg.object_class=klass;
 g_cfg.object_unbox=unbox;g_cfg.runtime_invoke=invoke;g_cfg.bool_class=&bool_tag;
 g_cfg.gameObject_method=&get_owner_m;g_cfg.alive_method=&alive_m;g_cfg.setScale_method=&scale_m;g_cfg.setActive_method=&active_m;
 g_visual={&renderer_tag,&type_tag,&get_components_m,&get_off_m,&set_off_m,&array_get_m,type_object,length,assignable};
}
void own(MiniState &s){s.transform=root(&tr,0);s.go=root(&go,0);s.challenge=root(&collider,0);g_mini=s;g_have_mini=true;}
}
int main(){
 setup();MiniState s{};float scale[]={1,1,1};
 assert(prepare_visual(&collider,&go,scale,s)&&s.render_count==2&&roots==2);
 assert(a.off==0&&b.off==1&&writes==0);
 own(s);assert(apply_visual(g_mini));assert(a.off&&b.off&&tr.sc[0]==1000&&writes==1);
 do_restore();assert(roots==0&&a.off==0&&b.off==1&&tr.sc[0]==1&&writes==2);
 s={};b.owner=&other;assert(!prepare_visual(&collider,&go,scale,s)&&roots==0&&writes==2);b.owner=&go;
 s={};count=0;assert(!prepare_visual(&collider,&go,scale,s)&&roots==0);count=2;
 s={};assert(prepare_visual(&collider,&go,scale,s));own(s);fail_second=true;
 assert(!apply_visual(g_mini)&&writes==2);fail_second=false;
 do_restore();assert(roots==0&&a.off==0&&b.off==1&&tr.sc[0]==1&&writes==2);
 return 0;
}
