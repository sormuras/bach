package com.github.sormuras.bach.internal;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Internal {@link Path}-related utilities. */
public class Paths {

  public static Path createDirectories(Path directory) {
    try {
      return Files.createDirectories(directory);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Path deleteDirectories(Path directory) {
    return deleteDirectories(directory, __ -> true);
  }

  public static Path deleteDirectories(Path directory, Predicate<Path> filter) {
    // trivial case: delete existing empty directory or single file
    try {
      Files.deleteIfExists(directory);
      return directory;
    } catch (DirectoryNotEmptyException ignored) {
      // fall-through
    } catch (Exception e) {
      throw new RuntimeException("Delete directories failed: " + directory, e);
    }
    // default case: walk the tree...
    try (var stream = Files.walk(directory)) {
      var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.toArray(Path[]::new)) Files.deleteIfExists(path);
    } catch (Exception e) {
      throw new RuntimeException("Delete directories failed: " + directory, e);
    }
    return directory;
  }

  public static boolean deleteIfExists(Path path) {
    if (Files.notExists(path)) return false;
    try {
      if (Files.isDirectory(path)) {
        Paths.deleteDirectories(path, __ -> true);
        return true;
      } else return Files.deleteIfExists(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void find(Path start, String glob, Consumer<Path> consumer) {
    var pattern = glob;
    while (pattern.startsWith(".") || pattern.startsWith("/")) pattern = pattern.substring(1);
    var matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    try (var stream =
        Files.find(start, 99, (path, bfa) -> matcher.matches(start.relativize(path)))) {
      stream.filter(Paths::isVisible).map(Path::normalize).forEach(consumer);
    } catch (Exception exception) {
      throw new RuntimeException("find failed: " + glob, exception);
    }
  }

  public static boolean isViewSupported(Path file, String view) {
    return file.getFileSystem().supportedFileAttributeViews().contains(view);
  }

  public static boolean isVisible(Path path) {
    try {
      for (int endIndex = 1; endIndex <= path.getNameCount(); endIndex++) {
        var subpath = path.subpath(0, endIndex);
        // work around https://bugs.openjdk.java.net/browse/JDK-8255576
        var probe = subpath.toString().isEmpty() ? path.toAbsolutePath() : subpath;
        if (!Files.isReadable(probe)) return false;
        if (Files.isHidden(probe)) return false;
      }
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Optional<String> readString(Path path) {
    try {
      return Optional.of(Files.readString(path));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  public static String slashed(Path path) {
    return path.toString().replace('\\', '/');
  }

  /** Hidden default constructor. */
  private Paths() {}
}
