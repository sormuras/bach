package com.github.sormuras.bach.internal;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Internal {@link Path}-related utilities. */
class PathsScratches {

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
        PathsScratches.deleteDirectories(path, __ -> true);
        return true;
      } else return Files.deleteIfExists(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  /** Convert path element names of the given unit into a reversed deque. */
  public static Deque<String> deque(Path path) {
    var deque = new ArrayDeque<String>();
    path.forEach(name -> deque.addFirst(name.toString()));
    return deque;
  }

  public static void find(Path start, String glob, Consumer<Path> consumer) {
    var pattern = glob;
    while (pattern.startsWith(".") || pattern.startsWith("/")) pattern = pattern.substring(1);
    var matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    try (var stream =
        Files.find(start, 99, (path, bfa) -> matcher.matches(start.relativize(path)))) {
      stream.filter(PathsScratches::isVisible).map(Path::normalize).forEach(consumer);
    } catch (Exception exception) {
      throw new RuntimeException("find failed: " + start + " -> " + glob, exception);
    }
  }

  /** Walk all trees to find matching paths the given filter starting at given root paths. */
  public static List<Path> find(Collection<Path> roots, int maxDepth, Predicate<Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    for (var root : roots) {
      try (var stream = Files.walk(root, maxDepth)) {
        stream.filter(filter).forEach(paths::add);
      } catch (Exception e) {
        throw new Error("Walk directory '" + root + "' failed: " + e, e);
      }
    }
    return List.copyOf(paths);
  }

  public static List<Path> findModuleInfoJavaFiles(Path directory, int limit) {
    if (isRoot(directory)) throw new IllegalStateException("Root directory: " + directory.toUri());
    var files = find(List.of(directory), limit, PathsScratches::isModuleInfoJavaFile);
    return List.copyOf(files);
  }

  /** Test supplied path for pointing to a Java Archive file. */
  public static boolean isJarFile(Path path) {
    return Files.isRegularFile(path) && name(path).endsWith(".jar");
  }

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    return Files.isRegularFile(path) && name(path).endsWith(".java");
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleInfoJavaFile(Path path) {
    return isJavaFile(path) && name(path).equals("module-info.java");
  }

  /** Test supplied path for pointing to a Java module declaration for a given space name. */
  public static boolean isModuleInfoJavaFileForCodeSpace(Path info, String space) {
    return isModuleInfoJavaFile(info) && Collections.frequency(deque(info), space) == 1;
  }

  /** Test for a path pointing to a file system root like {@code /} or {@code C:\}. */
  public static boolean isRoot(Path path) {
    return path.toAbsolutePath().normalize().getNameCount() == 0;
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

  /** @return the file name of the path as a string */
  public static String name(Path path) {
    return name(path, null);
  }

  public static String name(Path path, String defautName) {
    var name = path.toAbsolutePath().getFileName();
    return Optional.ofNullable(name).map(Path::toString).orElse(defautName);
  }

  /** Return the size of a file in bytes. */
  public static long size(Path path) {
    try {
      return Files.size(path);
    } catch (Exception e) {
      throw new Error("Size of file failed: " + e, e);
    }
  }

  public static String slashed(Path path) {
    return path.toString().replace('\\', '/');
  }

  /** Hidden default constructor. */
  private PathsScratches() {}
}
