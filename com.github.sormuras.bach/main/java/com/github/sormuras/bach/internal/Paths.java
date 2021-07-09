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
import java.util.function.Predicate;

/** {@link Path}-related utilities. */
public class Paths {

  /** Test supplied path for pointing to a Java Archive file. */
  public static boolean isJarFile(Path path) {
    var name = path.getFileName().toString();
    return name.endsWith(".jar") && Files.isRegularFile(path);
  }

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    var name = path.getFileName().toString();
    return name.endsWith(".java") && Files.isRegularFile(path);
  }

  /** Test supplied path for pointing to a Java 8 compilation unit. */
  public static boolean isJava8File(Path path) {
    var name = path.getFileName().toString();
    return name.endsWith(".java")
        && !name.equals("module-info.java") // ignore module declaration compilation units
        && Files.isRegularFile(path);
  }

  /** Walk all trees to find matching paths the given filter starting at given root path. */
  public static List<Path> find(List<Path> roots, int maxDepth, Predicate<Path> filter)
      throws Exception {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    for (var root : roots) {
      try (var stream = Files.walk(root, maxDepth)) {
        stream.filter(filter).forEach(paths::add);
      }
    }
    return List.copyOf(paths);
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

  /** {@return the message digest of the given file as a hexadecimal string} */
  public static String hash(Path file, String algorithm) throws Exception {
    var md = MessageDigest.getInstance(algorithm);
    try (var in = new BufferedInputStream(new FileInputStream(file.toFile()));
        var out = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
      in.transferTo(out);
    }
    return String.format("%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
  }

  /** Hidden default constructor. */
  private Paths() {}
}
