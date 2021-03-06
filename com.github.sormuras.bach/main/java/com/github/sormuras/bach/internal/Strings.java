package com.github.sormuras.bach.internal;

import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
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

  /** {@return a string composed of paths joined via the system-dependent path-separator} */
  public static String join(Collection<Path> paths) {
    return join(paths, File.pathSeparator);
  }

  /** {@return a string composed of paths joined via the given delimiter} */
  public static String join(Collection<Path> paths, CharSequence delimiter) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(delimiter));
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

  /** Hidden default constructor. */
  private Strings() {}
}
