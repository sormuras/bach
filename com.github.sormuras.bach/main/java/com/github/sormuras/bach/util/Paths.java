package com.github.sormuras.bach.util;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Directory and file related utilities. */
public final class Paths {

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

  /** Walk all trees to find matching paths the given filter starting at given root path. */
  public static List<Path> find(Path root, int maxDepth, Predicate<Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var stream = Files.walk(root, maxDepth)) {
      stream.filter(filter).forEach(paths::add);
    } catch (Exception e) {
      throw new RuntimeException("Walk directory '" + root + "' failed: " + e, e);
    }
    return List.copyOf(paths);
  }

  public static List<Path> findModuleInfoJavaFiles(Path directory, int limit) {
    var files = find(directory, limit, Paths::isModuleInfoJavaFile);
    return List.copyOf(files);
  }

  /** Test supplied path for pointing to a Java Archive file. */
  public static boolean isJarFile(Path path) {
    return name(path).endsWith(".jar") && Files.isRegularFile(path);
  }

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    return name(path).endsWith(".java") && Files.isRegularFile(path);
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleInfoJavaFile(Path path) {
    return name(path).equals("module-info.java") && Files.isRegularFile(path);
  }

  /** {@return a listing of the directory in natural order with the given filter applied} */
  public static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
    if (Files.notExists(directory)) return List.of();
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var stream = Files.newDirectoryStream(directory, filter)) {
      stream.forEach(paths::add);
    } catch (Exception e) {
      throw new RuntimeException("Stream directory '" + directory + "' failed: " + e, e);
    }
    return List.copyOf(paths);
  }

  /** {@return the file name of the path as a string, or {@code null}} */
  public static String name(Path path) {
    return nameOrElse(path, null);
  }

  /** {@return the file name of the path as a string, or the given default name} */
  public static String nameOrElse(Path path, String defautName) {
    var name = path.toAbsolutePath().getFileName();
    return Optional.ofNullable(name).map(Path::toString).orElse(defautName);
  }

  /** Hidden default constructor. */
  private Paths() {}
}
