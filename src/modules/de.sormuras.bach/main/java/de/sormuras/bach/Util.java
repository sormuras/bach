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

  /** Gets a property value indicated by the specified {@code key}. */
  static String get(String key, Properties properties, Supplier<Object> defaultValueSupplier) {
    return Optional.ofNullable(System.getProperty(assigned(key, "key")))
        .or(() -> Optional.ofNullable(assigned(properties, "properties").getProperty(key)))
        .or(() -> Optional.ofNullable(System.getenv("BACH_" + key.toUpperCase())))
        .orElse(Objects.toString(assigned(defaultValueSupplier, "defaultValueSupplier").get()));
  }

  static Properties newProperties(Path path) {
    var properties = new Properties();
    try (var reader = Files.newBufferedReader(path)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new UncheckedIOException("Reading properties failed: " + path, e);
    }
    return properties;
  }
}
