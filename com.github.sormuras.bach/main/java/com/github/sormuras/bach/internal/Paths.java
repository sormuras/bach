package com.github.sormuras.bach.internal;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.math.BigInteger;
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

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    var name = path.getFileName().toString();
    return name.endsWith(".java") && Files.isRegularFile(path);
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
