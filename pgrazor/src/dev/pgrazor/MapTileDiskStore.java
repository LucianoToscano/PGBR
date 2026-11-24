package dev.pgrazor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomic replacement for disposable cache entries; never delete the previous entry before publishing. */
final class MapTileDiskStore {
    private MapTileDiskStore() {}

    interface Mover { void move(Path temporary, Path destination) throws IOException; }

    static void replace(File file, byte[] bytes) throws IOException {
        replace(file, bytes, (temporary, destination) ->
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING));
    }

    static void replace(File file, byte[] bytes, Mover mover) throws IOException {
        Path destination = file.toPath();
        Path temporary = Files.createTempFile(destination.toAbsolutePath().getParent(), "tile-", ".part");
        try {
            Files.write(temporary, bytes);
            // If atomic replacement is unavailable, fail without deleting a usable old entry.
            mover.move(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
