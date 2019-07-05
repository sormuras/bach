package de.sormuras.bach;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Static utilities. */
class Util {

  /** Assigned returns P if P is non-nil and throws an exception if P is nil. */
  static <T> T assigned(T object) {
    return Objects.requireNonNull(object, "null pointer detected");
  }

  /** Assigned returns P if P is non-nil and throws an exception if P is nil. */
  static <T> T assigned(T object, String name) {
    return Objects.requireNonNull(object, name + " mustn't be null");
  }

  /** List names of all directories found in given directory. */
  static List<String> findDirectoryNames(Path directory) {
    return findDirectoryEntries(directory, Files::isDirectory);
  }

  /** List paths of all entries found in given directory after applying the filter. */
  static List<String> findDirectoryEntries(Path directory, DirectoryStream.Filter<Path> filter) {
    var names = new ArrayList<String>();
    try (var stream = Files.newDirectoryStream(directory, filter)) {
      stream.forEach(entry -> names.add(entry.getFileName().toString()));
    } catch (Exception e) {
      throw new Error("Scanning directory entries failed: " + e);
    }
    Collections.sort(names);
    return names;
  }

  /** List all paths matching the given filter starting at given root paths. */
  static List<Path> find(Predicate<Path> filter, Path... roots) {
    var paths = new ArrayList<Path>();
    for (var root : roots) {
      try (var stream = Files.walk(root)) {
        stream.filter(filter).forEach(paths::add);
      } catch (Exception e) {
        throw new Error("Scanning directory '" + root + "' failed: " + e, e);
      }
    }
    Collections.sort(paths);
    return paths;
  }

  /** Gets a property value indicated by the specified {@code key}. */
  static String get(String key, Properties properties, Supplier<Object> defaultValueSupplier) {
    return Optional.ofNullable(System.getProperty(assigned(key, "key")))
        .or(() -> Optional.ofNullable(assigned(properties, "properties").getProperty(key)))
        .or(() -> Optional.ofNullable(System.getenv("BACH_" + key.toUpperCase())))
        .orElse(Objects.toString(assigned(defaultValueSupplier, "defaultValueSupplier").get()));
  }

  /** Test supplied path for pointing to a Java module declaration source compilation unit. */
  static boolean isModuleInfo(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
  }

  /** Load specified properties file. */
  static Properties loadProperties(Path path) {
    var properties = new Properties();
    try (var reader = Files.newBufferedReader(path)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new UncheckedIOException("Reading properties failed: " + path, e);
    }
    return properties;
  }
}
