package com.github.sormuras.bach.internal;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

public class Paths {

  public static Path createDirectories(Path directory) {
    try {
      return Files.createDirectories(directory);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Path createTempDirectory(String prefix) {
    try {
      return Files.createTempDirectory(prefix);
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

  /** Test supplied path for pointing to a Java Archive file. */
  public static boolean isJarFile(Path path) {
    return Strings.name(path).endsWith(".jar") && Files.isRegularFile(path);
  }

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    return Strings.name(path).endsWith(".java") && Files.isRegularFile(path);
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleInfoJavaFile(Path path) {
    return Strings.name(path).equals("module-info.java") && Files.isRegularFile(path);
  }

  /** {@return the number of name elements in the path that are equal to the given name} */
  public static int countName(Path path, String name) {
    var count = 0;
    for (var element : path) if (element.toString().equals(name)) count++;
    return count;
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

  /** {@return a subpath of the given path ending with the given name, or the fall-back path} */
  public static Path findNameOrElse(Path path, String name, Path fallback) {
    for (int index = 0; index < path.getNameCount(); index++) {
      if (name.equals(path.getName(index).toString())) return path.subpath(0, index + 1);
    }
    return fallback;
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

  /** Hidden default constructor. */
  private Paths() {}
}
