package by.rayden.paracoder.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class CollectToMapFileVisitor extends SimpleFileVisitor<Path> {
    // The expected number of entries in the pathTree.
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
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        getExtension(file.getFileName().toString().toLowerCase())
            .filter(this.extensions::contains)
            .ifPresent(_ -> {
                Path fileAbsolutePath = file.toAbsolutePath();
                this.pathTree.put(fileAbsolutePath, attrs);

                // In case when program argument is a file(s) (not a dir)
                // then manually add the parent dir to the pathTree.
                this.pathTree.computeIfAbsent(fileAbsolutePath.getParent(), this::getFileAttributes);

                this.isAtLeastOneFileInDirAdded = true;
            });

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        this.isAtLeastOneFileInDirAdded = false;
        this.pathTree.put(dir.toAbsolutePath(), attrs);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) {
        if (exc != null) {
            log.error("I/O error visiting directory: {}", dir, exc);
        }

        if (!this.isAtLeastOneFileInDirAdded) {
            this.pathTree.remove(dir.toAbsolutePath());
        }
        return FileVisitResult.CONTINUE;
    }

    public Optional<String> getExtension(String filename) {
        return Optional.of(filename).filter(f -> f.contains("."))
                       .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    private BasicFileAttributes getFileAttributes(Path path) {
        try {
            BasicFileAttributes fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
            log.debug("path:{}, time:{}", path, fileAttributes.lastModifiedTime().toString());
            return fileAttributes;
        } catch (IOException e) {
            log.error("Error reading attributes: {}", path, e);
            throw new RuntimeException(e);
        }
    }

}
