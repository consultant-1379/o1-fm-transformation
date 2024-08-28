
package com.ericsson.oss.mediation.fm.o1.engine.service.config.jar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarFile;

import com.ericsson.oss.mediation.fm.o1.engine.service.config.MediationConfigurationException;

public class JarNioUtil {

    private static final String JAR_EXTENSION = ".jar";
    private static final String GLOP_SYNTAX = "glob:";
    private static final String JAR_FILE_URI_PREFIX = "jar:file:";

    private JarNioUtil() {

    }

    /**
     * Creates a java.nio.file.DirectoryStream.Filter that matches the given string pattern.
     *
     * @param fs
     *            the file system
     * @param pattern
     *            the file pattern
     * @return the filter object.
     */
    public static DirectoryStream.Filter<Path> createFilter(final FileSystem fs, final String pattern) {
        final PathMatcher matcher = fs.getPathMatcher(toGlopSyntax(pattern));
        final DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(final Path entry) {
                return matcher.matches(entry);
            }
        };
        return filter;
    }

    /**
     * Returns a URL to represent this path.
     *
     * @param path
     *            the path
     * @return the URL representing this path
     */
    public static URL toURL(final Path path) {
        try {
            return path.toUri().toURL();
        } catch (final MalformedURLException ex) {
            throw new MediationConfigurationException(ex);
        }
    }

    /**
     * Tests whether a file with the given path is a jar file.
     *
     * @param file
     *            the path to the file to test
     * @return if the file is a valid jar file; false if the file does not exist or is not a valid jar.
     */
    public static boolean isJarFile(final Path file) {
        if (!Files.exists(file)) {
            return false;
        }
        if (!file.getFileName().toString().endsWith(JAR_EXTENSION)) {
            return false;
        }

        try (JarFile jarFile = new JarFile(file.toFile())) {
            return jarFile.getManifest() != null;
        } catch (final IOException ex) {
            return false;
        }
    }

    /**
     * Return all entries of the given jar that match the specified filter.
     *
     * @param jarFile
     *            the jar to inspect
     * @param filter
     *            the filter
     * @return a list of jar entries that match the filter
     * @throws IOException
     *             if an I/O error occurs
     */
    public static List<Path> getJarEntries(final Path jarFile, final String filter) throws IOException {
        try (FileSystem fileSystem = createJarFileSystem(jarFile)) {
            return getJarEntries(fileSystem, createFilter(fileSystem, filter));
        }
    }

    private static FileSystem createJarFileSystem(final Path jarFile) throws IOException {
        final URI uri = URI.create(JAR_FILE_URI_PREFIX.concat(jarFile.toUri().getPath()));
        final HashMap<String, String> env = new HashMap<>();
        env.put("create", "false");
        return FileSystems.newFileSystem(uri, env);
    }

    private static List<Path> getJarEntries(final FileSystem fileSystem, final DirectoryStream.Filter<Path> filter) throws IOException {
        final List<Path> paths = new ArrayList<>();
        final Path root = fileSystem.getPath("/");
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (filter.accept(file)) {
                    paths.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                if (filter.accept(dir)) {
                    paths.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    private static String toGlopSyntax(final String pattern) {
        return GLOP_SYNTAX.concat(pattern);
    }

}
