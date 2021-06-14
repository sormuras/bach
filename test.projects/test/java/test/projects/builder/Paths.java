package test.projects.builder;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;

class Paths {

  public static boolean isModuleInfoJavaFile(Path path) {
    return Paths.name(path).equals("module-info.java") && Files.isRegularFile(path);
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

  public static Path findNameOrElse(Path path, String name, Path fallback) {
    for (int index = 0; index < path.getNameCount(); index++) {
      if (name.equals(path.getName(index).toString())) return path.subpath(0, index + 1);
    }
    return fallback;
  }

  public static String name(Path path) {
    return nameOrElse(path, null);
  }

  public static String nameOrElse(Path path, String defautName) {
    var name = path.toAbsolutePath().normalize().getFileName();
    return Optional.ofNullable(name).map(Path::toString).orElse(defautName);
  }

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

  private Paths() {}
}
