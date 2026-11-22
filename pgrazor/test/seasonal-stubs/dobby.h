#pragma once
#include <cstdint>
// Test-only representation of the x86_64 register context used by the
// pinned Dobby ABI. The running fixture NEVER installs a hook or calls Android.
// Do not copy this file to any production/native vendor include directory.
struct DobbyRegisterContext {
    struct {
        struct {
            uintptr_t rdi = 0;
        } regs;
    } general;
};
using dobby_instrument_callback_t = void (*)(void *, DobbyRegisterContext *);
