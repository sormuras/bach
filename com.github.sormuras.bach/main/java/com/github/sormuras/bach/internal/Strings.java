package com.github.sormuras.bach.internal;

import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** String-related helpers. */
public class Strings {

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

  /** {@return the file name of the path as a string, or {@code null}} */
  public static String name(Path path) {
    return nameOrElse(path, null);
  }

  /** {@return the file name of the path as a string, or the given default name} */
  public static String nameOrElse(Path path, String defautName) {
    var name = path.toAbsolutePath().getFileName();
    return Optional.ofNullable(name).map(Path::toString).orElse(defautName);
  }

  /** {@return a string composed of paths joined via the system-dependent path-separator} */
  public static String join(Collection<Path> paths) {
    return join(paths, File.pathSeparator);
  }

  /** {@return a string composed of paths joined via the given delimiter} */
  public static String join(Collection<Path> paths, CharSequence delimiter) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(delimiter));
  }

  /** Hidden default constructor. */
  private Strings() {}
}
