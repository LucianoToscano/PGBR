#pragma once
// PGBR Root: fail closed if a separately installed PGSavant loader may
// inject into the same game process. Do not change the legacy pgrazor module ID.
// Adapted in concept from dojistarman/PGRazor-Savant (MIT); no runtime state shared.
namespace pgbr_root_peer {
enum class Kind { missing, file, directory, unknown };

// A missing child is meaningful only when /data/adb/modules is readable.
// Unknown paths, unexpected types and an enabled peer deny injection.
template <typename Probe> bool permits(Probe probe) {
    const char *const parent="/data/adb/modules";
    const char *const peer="/data/adb/modules/pgsavant";
    const char *const off="/data/adb/modules/pgsavant/pgrazor/injection-off";
    if(probe(parent)!=Kind::directory)return false;
    const Kind installed=probe(peer);
    if(installed==Kind::missing)return true;
    if(installed!=Kind::directory)return false;
    return probe(off)==Kind::file;
}
}
