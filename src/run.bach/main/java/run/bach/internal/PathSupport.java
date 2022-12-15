package run.bach.internal;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

public interface PathSupport {
  static String checksum(Path file, String algorithm) {
    if (Files.notExists(file)) throw new RuntimeException("File not found: " + file);
    try {
      if ("size".equalsIgnoreCase(algorithm)) return Long.toString(Files.size(file));
      var md = MessageDigest.getInstance(algorithm);
      try (var source = new BufferedInputStream(new FileInputStream(file.toFile()));
          var target = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
        source.transferTo(target);
      }
      var format = "%0" + (md.getDigestLength() * 2) + "x";
      return String.format(format, new BigInteger(1, md.digest()));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static boolean isJarFile(Path path) {
    return name(path, "").endsWith(".jar") && Files.isRegularFile(path);
  }

  static boolean isJavaFile(Path path) {
    return name(path, "").endsWith(".java") && Files.isRegularFile(path);
  }

  static boolean isPropertiesFile(Path path) {
    return name(path, "").endsWith(".properties") && Files.isRegularFile(path);
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

  static Properties properties(Path file) {
    var properties = new Properties();
    try {
      properties.load(new StringReader(Files.readString(file)));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return properties;
  }

  static String name(Path path, String defaultName) {
    var normalized = path.normalize();
    var candidate = normalized.toString().isEmpty() ? normalized.toAbsolutePath() : normalized;
    var name = candidate.getFileName();
    return name != null ? name.toString() : defaultName;
  }
}
