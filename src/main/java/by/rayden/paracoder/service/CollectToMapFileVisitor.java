package by.rayden.paracoder.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CollectToMapFileVisitor extends SimpleFileVisitor<Path> {
    private static final int NUM_MAPPINGS = 50;

    private final Map<Path, BasicFileAttributes> pathTree = HashMap.newHashMap(NUM_MAPPINGS);
    private final Set<String> extensions;
    private boolean isAtLeastOneFileInDirAdded;

    public CollectToMapFileVisitor(Set<String> extensions) {
        this.extensions = extensions;
    }

    public Map<Path, BasicFileAttributes> getPathTree() {
        return Collections.unmodifiableMap(this.pathTree);
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        getExtension(file.getFileName().toString().toLowerCase())
            .filter(this.extensions::contains)
            .ifPresent(_ -> {
                this.pathTree.put(file.toAbsolutePath(), attrs);
                this.isAtLeastOneFileInDirAdded = true;
            });

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        this.pathTree.put(dir.toAbsolutePath(), attrs);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
        if (!this.isAtLeastOneFileInDirAdded) {
            this.pathTree.remove(dir.toAbsolutePath());
        }
        return FileVisitResult.CONTINUE;
    }

    public Optional<String> getExtension(String filename) {
        return Optional.ofNullable(filename).filter(f -> f.contains("."))
                       .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}
