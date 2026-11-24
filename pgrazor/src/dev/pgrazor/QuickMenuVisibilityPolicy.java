package dev.pgrazor;

/** Pure, persistent quick-menu visibility policy; has no gameplay side effects. */
final class QuickMenuVisibilityPolicy {
    // These are append-only stable preference bits, not icon IDs.
    // Keep both historical bits. Locais is hidden only if both former shortcuts were hidden.
    static final int LEGACY_FAVORITES_BIT=1<<5;
    static final int LOCATIONS_BIT=1<<6;
    static final int LOCATIONS_MASK=LEGACY_FAVORITES_BIT|LOCATIONS_BIT;
    static final int EVENTS_BIT = 1 << 9;
    static final int SELECTOR_BIT = 1 << 13;
    static final int MAX_BIT = SELECTOR_BIT;
    static final int MASK = (MAX_BIT << 1) - 1;

    private QuickMenuVisibilityPolicy() {}

    static boolean validBit(int bit) {
        return bit > 0 && bit <= MAX_BIT && (bit & (bit - 1)) == 0;
    }

    static int sanitize(int hidden) {
        return hidden >= 0 && (hidden & ~MASK) == 0 ? hidden : 0;
    }

    static boolean visible(int hidden, int bit) {
        // Settings has visibility bit zero and must remain unhideable.
        if(bit==LOCATIONS_BIT)return (sanitize(hidden)&LOCATIONS_MASK)!=LOCATIONS_MASK;
        return bit == 0 || (validBit(bit) && (sanitize(hidden) & bit) == 0);
    }

    static int withVisibility(int hidden, int bit, boolean shouldShow) {
        int safe = sanitize(hidden);
        if (!validBit(bit)) return safe;
        int bits=bit==LOCATIONS_BIT?LOCATIONS_MASK:bit;
        return shouldShow ? safe & ~bits : safe | bits;
    }
}
