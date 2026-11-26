package dev.pgrazor;

/** Presentation only; no saved preference or input dispatch is owned by this class. */
final class JoystickDisplayPolicy {
    private JoystickDisplayPolicy() {}
    static boolean visible(boolean preferred, boolean hidden, boolean settings,
                           boolean menu, boolean encounter, boolean focused, boolean black) {
        return preferred && !hidden && !settings && !menu && !encounter && focused && !black;
    }
}
