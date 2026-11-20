#include "trainer_privacy_ui.h"
#include "trainer_privacy_policy.h"
#include "runtime_metadata.h"
#include "runtime_bootstrap.h"
#include "hook_engine.h"

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <dlfcn.h>
#include <cstring>
#include <mutex>
#include <string>

namespace {
using TextSetter = void (*)(void *, void *, const void *);
using StringChars = const uint16_t *(*)(void *);
using StringLength = int32_t (*)(void *);
using StringNewUtf16 = void *(*)(const uint16_t *, int32_t);
using StrongRoot = uintptr_t (*)(void *, int);
using RootTarget = void *(*)(uintptr_t);

std::mutex name_lock;
std::u16string trainer_name;
std::atomic<int> privacy_enabled{0};
std::atomic<int> installed_count{0};
TextSetter original_ui = nullptr;
TextSetter original_tmp = nullptr;
TextSetter original_tmp_settext = nullptr;
StringChars string_chars = nullptr;
StringLength string_length = nullptr;
RootTarget root_target = nullptr;
uintptr_t mask_root = 0;
thread_local bool inside_setter = false;
constexpr uint16_t kFiveStars[] = {'*', '*', '*', '*', '*'};
constexpr uint32_t kMaxTrainerUnits = 128;

// A short exact match is intentionally conservative. Never mask another
// player's name, a chat line containing the name, or an account/RPC object.
bool is_own_name(void *value) {
    if (!privacy_enabled.load(std::memory_order_acquire) || value == nullptr ||
        string_chars == nullptr || string_length == nullptr) return false;
    int32_t length = string_length(value);
    if (length <= 0 || length > int32_t(kMaxTrainerUnits)) return false;
    const uint16_t *chars = string_chars(value);
    if (chars == nullptr) return false;
    std::lock_guard<std::mutex> hold(name_lock);
    return pgrazor_trainer_exact_name(
        chars, static_cast<size_t>(length),
        reinterpret_cast<const uint16_t *>(trainer_name.data()), trainer_name.size());
}

void *display_value(void *value) {
    if (inside_setter || !mask_root || !root_target || !is_own_name(value)) return value;
    // The managed five-star string is kept alive by a strong IL2CPP GC handle.
    // No allocation and no identity/network writes occur in a render callback.
    void *masked = root_target(mask_root);
    return masked != nullptr ? masked : value;
}

void ui_text(void *self, void *value, const void *method) {
    if (!original_ui) return;
    void *visual = display_value(value);
    const bool was_inside = inside_setter;
    inside_setter = true;
    original_ui(self, visual, method);
    inside_setter = was_inside;
}
void tmp_text(void *self, void *value, const void *method) {
    if (!original_tmp) return;
    void *visual = display_value(value);
    const bool was_inside = inside_setter;
    inside_setter = true;
    original_tmp(self, visual, method);
    inside_setter = was_inside;
}
void tmp_set_text(void *self, void *value, const void *method) {
    if (!original_tmp_settext) return;
    void *visual = display_value(value);
    const bool was_inside = inside_setter;
    inside_setter = true;
    original_tmp_settext(self, visual, method);
    inside_setter = was_inside;
}

// Discover only a validated presentation-only setter. IL2CPP MethodInfo and its
// methodPointer are verified against the active loaded module before Dobby.
void *find_text_setter(const PgrMetaApi *md, const PgrBootBinding &boot,
                       const char *ns, const char *klass, const char *method) {
    PgrMetaClassQuery cls{};
    cls.version = 1;
    cls.size = sizeof(cls);
    cls.name_space = ns;
    cls.name = klass;
    PgrMetaClassResult found{};
    if (md->resolve_class(&cls, &found) != 0 || found.klass == nullptr) return nullptr;
    PgrMetaMethodQuery query{};
    query.version = 1;
    query.size = sizeof(query);
    query.owner = found.klass;
    query.name = method;
    query.return_type = "System.Void";
    query.param_count = 1;
    query.param_types[0] = "System.String";
    query.is_static = 0;
    PgrMetaMethodResult result{};
    if (md->validate_method(&query, &result) != 0 || result.method == nullptr) return nullptr;
    using DebugInfo = bool (*)(void *, void *);
    auto debug = reinterpret_cast<DebugInfo>(boot.meta.debug_get_method_info);
    if (!debug) return nullptr;
    void *info[3] = {};
    if (!debug(const_cast<void *>(result.method), info) || !info[0]) return nullptr;
    PgrMetaCodeQuery code{};
    code.version = 1;
    code.size = sizeof(code);
    code.address = info[0];
    code.length = 16;
    if (md->validate_code(&code) != 0) return nullptr;
    return info[0];
}

int attach_text(const PgrMetaApi *md, const PgrBootBinding &boot,
                const char *ns, const char *klass, const char *method,
                void *callback, TextSetter *original) {
    if (*original != nullptr) return 0;
    void *target = find_text_setter(md, boot, ns, klass, method);
    if (!target) return 0;
    void *backup = nullptr;
    if (pgrazor_hook(target, callback, &backup) != 0 || !backup) return 0;
    *original = reinterpret_cast<TextSetter>(backup);
    installed_count.fetch_add(1, std::memory_order_release);
    return 1;
}
} // namespace

int pgrazor_trainer_privacy_configure(int enabled, const uint16_t *trainer, uint32_t length) {
    // Configuration is accepted off the Unity thread; all managed calls stay in tick().
    if (length > kMaxTrainerUnits || (length > 0 && !trainer)) return -1;
    std::u16string next;
    if (length) {
        next.reserve(length);
        for (uint32_t i=0; i<length; ++i) {
            if (trainer[i] == 0 || (trainer[i] < 32)) return -1;
            next.push_back(static_cast<char16_t>(trainer[i]));
        }
    }
    {
        std::lock_guard<std::mutex> hold(name_lock);
        trainer_name.swap(next);
    }
    privacy_enabled.store(enabled ? 1 : 0, std::memory_order_release);
    return 0;
}

int pgrazor_trainer_privacy_status() {
    int status = installed_count.load(std::memory_order_acquire);
    std::lock_guard<std::mutex> hold(name_lock);
    if (!trainer_name.empty()) status |= 0x10;
    if (privacy_enabled.load(std::memory_order_acquire)) status |= 0x20;
    // Hook presence is NOT evidence that every game screen is protected.
    return status;
}

void pgrazor_trainer_privacy_tick() {
    if (!privacy_enabled.load(std::memory_order_acquire) || installed_count.load() == 3) return;
    {
        std::lock_guard<std::mutex> hold(name_lock);
        if (trainer_name.empty()) return;
    }
    // Already called exclusively on a validated attached Unity scheduler thread.
    const auto *boot_api = static_cast<const PgrBootApi *>(pgrazor_runtime_bootstrap_api(1));
    const auto *md = static_cast<const PgrMetaApi *>(pgrazor_runtime_metadata_api(1));
    if (!boot_api || !md) return;
    PgrBootBinding boot{};
    if (boot_api->snapshot(&boot) != 0 || boot.status != 0 || boot.handle == nullptr ||
        boot.runtime.string_chars == nullptr || boot.runtime.gchandle_new == nullptr ||
        boot.runtime.gchandle_get_target == nullptr || boot.runtime.thread_current == nullptr ||
        !reinterpret_cast<void *(*)()>(boot.runtime.thread_current)()) return;

    // Created only once on the attached Unity owner, then pinned through a
    // process-lifetime strong GC handle (no per-frame managed allocation).
    if (!mask_root) {
        using Dlsym = void *(*)(void *, const char *);
        // The bootstrap handle is the validated IL2CPP loader handle.
        // dlsym remains read-only discovery; it never loads a second game.
        void *sym = dlsym(boot.handle, "il2cpp_string_new_utf16");
        void *len = dlsym(boot.handle, "il2cpp_string_length");
        if (!sym || !len) return;
        auto create = reinterpret_cast<StringNewUtf16>(sym);
        void *masked = create(kFiveStars, 5);
        if (!masked) return;
        mask_root = reinterpret_cast<StrongRoot>(boot.runtime.gchandle_new)(masked, 0);
        if (!mask_root) return;
        string_chars = reinterpret_cast<StringChars>(boot.runtime.string_chars);
        string_length = reinterpret_cast<StringLength>(len);
        root_target = reinterpret_cast<RootTarget>(boot.runtime.gchandle_get_target);
    }

    // A missing class simply remains unavailable. Do not fall back to changing
    // the PlayerService or any string in the network transport.
    attach_text(md, boot, "UnityEngine.UI", "Text", "set_text",
                reinterpret_cast<void *>(&ui_text), &original_ui);
    attach_text(md, boot, "TMPro", "TMP_Text", "set_text",
                reinterpret_cast<void *>(&tmp_text), &original_tmp);
    attach_text(md, boot, "TMPro", "TMP_Text", "SetText",
                reinterpret_cast<void *>(&tmp_set_text), &original_tmp_settext);
}
