#pragma once
#include <cstddef>
#include <cstdint>

// An exact, bounded UI-only name comparison. Not suitable for replacing
// account identity, RPC fields, chat, or names of other trainers.
inline bool pgrazor_trainer_exact_name(const uint16_t *text, size_t text_length,
                                       const uint16_t *trainer, size_t trainer_length) {
    if (!text || !trainer || text_length == 0 || trainer_length == 0 ||
        text_length > 128 || trainer_length > 128 || text_length != trainer_length)
        return false;
    for (size_t i=0; i<text_length; ++i)
        if (text[i] != trainer[i]) return false;
    return true;
}
