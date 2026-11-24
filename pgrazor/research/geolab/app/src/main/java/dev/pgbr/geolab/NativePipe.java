package dev.pgbr.geolab;
public final class NativePipe {
    static { System.loadLibrary("geolab_pipe"); }
    private NativePipe() {}
    public static native byte[] transfer(byte[] payload);
}
