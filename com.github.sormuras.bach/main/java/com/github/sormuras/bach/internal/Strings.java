package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.api.BachException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Version;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** String-related helpers. */
public class Strings {

  public static Stream<String> unroll(String string) {
    return string.lines().map(String::strip);
  }

  public static Stream<String> unroll(String... strings) {
    if (strings.length == 0) return Stream.empty();
    if (strings.length == 1) return unroll(strings[0]);
    return unroll(List.of(strings));
  }

  public static Stream<String> unroll(Collection<String> strings) {
    return strings.stream().flatMap(String::lines).map(String::strip);
  }

  public static String toEnumName(String string) {
    return string.toUpperCase(Locale.ROOT).replace('-', '_');
  }

  public static <E extends Enum<E>> E toEnum(Class<E> enumClass, String string) {
    return Enum.valueOf(enumClass, toEnumName(string));
  }

  /** {@return a human-readable representation of the given duration} */
  public static String toString(Duration duration) {
    return duration
        .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
        .toString()
        .substring(2)
        .replaceAll("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase();
  }

  /**
   * {@return a string containing the version number and, if present, the pre-release version}
   *
   * @param version the module's version
   */
  public static String toNumberAndPreRelease(Version version) {
    var string = version.toString();
    var firstPlus = string.indexOf('+');
    if (firstPlus == -1) return string;
    var secondPlus = string.indexOf('+', firstPlus + 1);
    return string.substring(0, secondPlus == -1 ? firstPlus : secondPlus);
  }

  /** {@return a string composed of paths joined via the system-dependent path-separator} */
  public static String join(Collection<Path> paths) {
    return join(paths, File.pathSeparator);
  }

  /** {@return a string composed of paths joined via the given delimiter} */
  public static String join(Collection<Path> paths, CharSequence delimiter) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(delimiter));
  }

  /** {@return the file name of the path as a string, or {@code null}} */
  public static String name(Path path) {
    return nameOrElse(path, null);
  }

  /** {@return the file name of the path as a string, or the given default name} */
  public static String nameOrElse(Path path, String defautName) {
    var name = path.toAbsolutePath().normalize().getFileName();
    return Optional.ofNullable(name).map(Path::toString).orElse(defautName);
  }

  /** {@return the message digest of the given file as a hexadecimal string} */
  public static String hash(String algorithm, Path file) throws Exception {
    var md = MessageDigest.getInstance(algorithm);
    try (var in = new BufferedInputStream(new FileInputStream(file.toFile()));
        var out = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
      in.transferTo(out);
    }
    return String.format("%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
  }

  /** {@return a string in module-pattern form usable as a {@code --module-source-path} value} */
  public static String toModuleSourcePathPatternForm(Path info, String module) {
    var deque = new ArrayDeque<String>();
    for (var element : info.normalize()) {
      var name = element.toString();
      if (name.equals("module-info.java")) continue;
      deque.addLast(name.equals(module) ? "*" : name);
    }
    var pattern = String.join(File.separator, deque);
    if (!pattern.contains("*")) throw new FindException("Name '" + module + "' not found: " + info);
    if (pattern.equals("*")) return ".";
    if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
    if (pattern.startsWith("*")) return "." + File.separator + pattern;
    return pattern;
  }

  public static List<String> lines(Path file) {
    try {
      return Files.readAllLines(file);
    } catch (Exception e) {
      throw new BachException(e);
    }
  }

  public static List<String> arguments(Path file) {
    return arguments(lines(file)).toList();
  }

  public static Stream<String> arguments(List<String> lines) {
    return lines.stream()
        .map(String::strip)
        .filter(line -> !line.isEmpty())
        .filter(line -> !line.startsWith("#"))
        .map(String::translateEscapes);
  }

  /** Hidden default constructor. */
  private Strings() {}
}
