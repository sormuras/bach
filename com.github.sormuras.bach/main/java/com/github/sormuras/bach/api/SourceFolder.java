package com.github.sormuras.bach.api;

import com.github.sormuras.bach.internal.Strings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public record SourceFolder(Path path, int release) {

  private static final Pattern RELEASE_PATTERN = Pattern.compile(".*?(\\d+)$");

  public static SourceFolder of(Path path) {
    return new SourceFolder(path, parseReleaseNumber(Strings.name(path)));
  }

  public static int parseReleaseNumber(String string) {
    if (string == null || string.isEmpty()) return 0;
    var matcher = RELEASE_PATTERN.matcher(string);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  public boolean isTargeted() {
    return release != 0;
  }

  public boolean isModuleInfoJavaPresent() {
    return Files.isRegularFile(path.resolve("module-info.java"));
  }
}
