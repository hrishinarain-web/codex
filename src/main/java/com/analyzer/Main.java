
package com.analyzer;

import java.nio.file.Path;
import java.util.List;

import com.analyzer.scanner.RepositoryScanner;

public class Main {

    public static void main(String[] args) throws Exception {

        String repositoryPath = args.length > 0
                ? args[0]
                : ".";

        RepositoryScanner scanner = new RepositoryScanner();

        List<Path> files = scanner.scan(repositoryPath);

        System.out.println("Total Java files: " + files.size());

        for (Path file : files) {
            System.out.println(file);
        }
    }
}
