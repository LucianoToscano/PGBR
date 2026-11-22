package dev.pgrazor;

/** Pure, persistent quick-menu visibility policy; has no gameplay side effects. */
final class QuickMenuVisibilityPolicy {
    // These are append-only stable preference bits, not icon IDs.
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
        return bit == 0 || (validBit(bit) && (sanitize(hidden) & bit) == 0);
    }

    static int withVisibility(int hidden, int bit, boolean shouldShow) {
        int safe = sanitize(hidden);
        if (!validBit(bit)) return safe;
        return shouldShow ? safe & ~bit : safe | bit;
    }
}
