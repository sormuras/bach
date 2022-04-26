package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public record Folder(Path path, int release) implements Comparable<Folder> {
  static final Pattern RELEASE_PATTERN = Pattern.compile(".*?(\\d+)$");

  static int parseReleaseNumber(String string) {
    var matcher = RELEASE_PATTERN.matcher(string);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  public static Folder of(Path path) {
    if (Files.notExists(path)) throw new IllegalArgumentException("No such path: " + path);
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    return new Folder(path, parseReleaseNumber(path.getFileName().toString()));
  }

  @Override
  public int compareTo(Folder other) {
    return release - other.release;
  }
}
