package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * A source directory potentially targeting a specific Java SE release.
 *
 * @param path the directory
 * @param release the feature number of the Java SE release
 */
public record SourceFolder(Path path, int release) {

  /** @return {@code true} if a non-zero release is stored, else {@code false} */
  public boolean isTargeted() {
    return release != 0;
  }

  /** @return {@code true} if a regular file named {@code module-info.java} is in {@link #path()} */
  public boolean isModuleInfoJavaPresent() {
    return Files.isRegularFile(path.resolve("module-info.java"));
  }

  static final Pattern RELEASE_PATTERN = Pattern.compile(".*?(\\d+)$");

  static int parseReleaseNumber(String string) {
    if (string == null || string.isEmpty()) return 0;
    var matcher = RELEASE_PATTERN.matcher(string);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  static SourceFolder of(Path path) {
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    var file = path.normalize().getFileName();
    var name = file != null ? file : path.toAbsolutePath().getFileName();
    return new SourceFolder(path, parseReleaseNumber(name.toString()));
  }
}
