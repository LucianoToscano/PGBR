#include <jni.h>
#include <sys/socket.h>
#include <unistd.h>
#include <cerrno>
#include <algorithm>
#include <array>
namespace {
constexpr int max_payload = 1024 * 1024;
struct Pair {
    int fd[2]{-1,-1};
    ~Pair() { for (int x:fd) if (x>=0) close(x); }
};
void fail(JNIEnv* e, const char* msg) {
    if (!e->ExceptionCheck()) { auto c=e->FindClass("java/io/IOException"); if(c) e->ThrowNew(c,msg); }
}
bool send_all(int fd, const char* p, size_t n) {
    while(n) { auto k=send(fd,p,n,MSG_NOSIGNAL); if(k<0 && errno==EINTR) continue; if(k<=0) return false; p+=k; n-=static_cast<size_t>(k); } return true;
}
bool recv_all(int fd, char* p, size_t n) {
    while(n) { auto k=recv(fd,p,n,0); if(k<0 && errno==EINTR) continue; if(k<=0) return false; p+=k; n-=static_cast<size_t>(k); } return true;
}
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_dev_pgbr_geolab_NativePipe_transfer(JNIEnv* e,jclass,jbyteArray input) {
    if(!input) { fail(e,"Null payload"); return nullptr; }
    const auto len=e->GetArrayLength(input);
    if(len>max_payload) { fail(e,"Payload exceeds limit"); return nullptr; }
    Pair p;
    if(socketpair(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0,p.fd)!=0) { fail(e,"socketpair failed"); return nullptr; }
    timeval timeout{2,0};
    if(setsockopt(p.fd[0],SOL_SOCKET,SO_SNDTIMEO,&timeout,sizeof(timeout)) || setsockopt(p.fd[1],SOL_SOCKET,SO_RCVTIMEO,&timeout,sizeof(timeout))) { fail(e,"timeout setup failed"); return nullptr; }
    auto out=e->NewByteArray(len); if(!out) return nullptr;
    std::array<char,4096> chunk{};
    for(jsize pos=0;pos<len;) {
        auto n=std::min<jsize>(len-pos,static_cast<jsize>(chunk.size()));
        e->GetByteArrayRegion(input,pos,n,reinterpret_cast<jbyte*>(chunk.data()));
        if(e->ExceptionCheck()) return nullptr;
        if(!send_all(p.fd[0],chunk.data(),n) || !recv_all(p.fd[1],chunk.data(),n)) { fail(e,"local transfer failed"); return nullptr; }
        e->SetByteArrayRegion(out,pos,n,reinterpret_cast<const jbyte*>(chunk.data()));
        if(e->ExceptionCheck()) return nullptr;
        pos+=n;
    }
    return out;
}
