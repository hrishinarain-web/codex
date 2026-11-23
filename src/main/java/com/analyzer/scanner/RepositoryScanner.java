
package com.analyzer.scanner;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class RepositoryScanner {

    private static final Set<String> IGNORED_DIRECTORIES =
            Set.of(".git", "target", "build", "node_modules",
                   ".idea", ".gradle");

    public List<Path> scan(String repositoryPath) throws IOException {

        Path root = Paths.get(repositoryPath)
                         .toAbsolutePath()
                         .normalize();

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException(
                    "Invalid repository directory: " + root
            );
        }

        List<Path> javaFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(root)) {

            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !containsIgnoredDirectory(root, path))
                 .forEach(javaFiles::add);
        }

        return javaFiles;
    }

    private boolean containsIgnoredDirectory(Path root, Path file) {

        Path relativePath = root.relativize(file);

        for (Path part : relativePath) {
            if (IGNORED_DIRECTORIES.contains(part.toString())) {
                return true;
            }
        }

        return false;
    }
}
