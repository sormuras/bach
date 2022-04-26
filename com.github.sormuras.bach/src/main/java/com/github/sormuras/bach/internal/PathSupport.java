package com.github.sormuras.bach.internal;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public interface PathSupport {

  static String computeChecksum(Path path, String algorithm) {
    if (Files.notExists(path)) throw new RuntimeException("File not found: " + path);
    try {
      if ("size".equalsIgnoreCase(algorithm)) return Long.toString(Files.size(path));
      var md = MessageDigest.getInstance(algorithm);
      try (var source = new BufferedInputStream(new FileInputStream(path.toFile()));
          var target = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
        source.transferTo(target);
      }
      return String.format("%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static boolean isJarFile(Path path) {
    return nameOrElse(path, "").endsWith(".jar") && Files.isRegularFile(path);
  }

  static boolean isJavaFile(Path path) {
    return nameOrElse(path, "").endsWith(".java") && Files.isRegularFile(path);
  }

  static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
    if (Files.notExists(directory)) return List.of();
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var stream = Files.newDirectoryStream(directory, filter)) {
      stream.forEach(paths::add);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return List.copyOf(paths);
  }

  static String nameOrElse(Path path, String defautName) {
    var normalized = path.normalize();
    var candidate = normalized.toString().isEmpty() ? normalized.toAbsolutePath() : normalized;
    var name = candidate.getFileName();
    return name != null ? name.toString() : defautName;
  }
}
