package com.github.sormuras.bach.internal;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Static utility methods for operating on instances of {@link Path}. */
public sealed interface PathSupport permits ConstantInterface {

  static String computeChecksum(Path path, String algorithm) {
    if (Files.notExists(path)) throw new RuntimeException(path.toString());
    try {
      if ("size".equalsIgnoreCase(algorithm)) return Long.toString(Files.size(path));
      var md = MessageDigest.getInstance(algorithm);
      try (var source = new BufferedInputStream(new FileInputStream(path.toFile()));
           var target = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
        source.transferTo(target);
      }
      return String.format("%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalArgumentException(exception);
    }
  }

  static Properties properties(Path path) {
    var properties = new Properties();
    try {
    properties.load(new FileInputStream(path.toFile()));
    return properties;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  /** Walk the file tree starting at the root path and return paths matching the given filter. */
  static List<Path> find(Path root, int limit, Predicate<Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var stream = Files.walk(root, limit)) {
      stream.filter(filter).forEach(paths::add);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return List.copyOf(paths);
  }

  /** Test supplied path for pointing to a regular Java Archive file. */
  static boolean isJarFile(Path path) {
    return nameOrElse(path, "").endsWith(".jar") && Files.isRegularFile(path);
  }

  /** Test supplied path for pointing to a regular Java compilation unit file. */
  static boolean isJavaFile(Path path) {
    return nameOrElse(path, "").endsWith(".java") && Files.isRegularFile(path);
  }

  /** Test supplied path for pointing to a regular Java module declaration compilation unit file. */
  static boolean isModuleInfoJavaFile(Path path) {
    return "module-info.java".equals(name(path)) && Files.isRegularFile(path);
  }

  /** {@return a listing of the directory in natural order with the given filter applied} */
  static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
    if (Files.notExists(directory)) return List.of();
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var stream = Files.newDirectoryStream(directory, filter)) {
      stream.forEach(paths::add);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return List.copyOf(paths);
  }

  /** {@return the file name of the path as a string, or {@code null}} */
  static String name(Path path) {
    return nameOrElse(path, null);
  }

  /** {@return the file name of the path as a string, or the given default name} */
  static String nameOrElse(Path path, String defautName) {
    var normalized = path.normalize();
    var candidate = normalized.getNameCount() == 0 ? normalized.toAbsolutePath() : normalized;
    var name = candidate.getFileName();
    return Optional.ofNullable(name).map(Path::toString).orElse(defautName);
  }
}
