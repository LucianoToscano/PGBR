#pragma once
// Compile-only test seam for the shared hook-engine declaration.
// Production uses the pinned Dobby header supplied by native_build.py.
using dobby_instrument_callback_t = void (*)(void *, void *);
