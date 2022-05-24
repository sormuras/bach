package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Targets a folder to a Java feature version, with {@code 0} indicating no target version.
 *
 * @param path the directory
 * @param release the Java feature release version to target, 0 for none
 * @param type the type associated with the directory
 */
public record Folder(Path path, int release, FolderType type) implements Comparable<Folder> {
  static final Pattern RELEASE_PATTERN = Pattern.compile(".*?(\\d+)$");

  static int parseReleaseNumber(String name) {
    var matcher = RELEASE_PATTERN.matcher(name);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  static FolderType parseFolderType(String name) {
    if (name.startsWith("resources")) return FolderType.RESOURCES;
    return FolderType.SOURCES; // includes "java[-...]" names
  }

  public static Folder of(Path path) {
    if (Files.notExists(path)) throw new IllegalArgumentException("No such path: " + path);
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    var name = path.getFileName().toString();
    var release = parseReleaseNumber(name);
    var type = parseFolderType(name);
    return new Folder(path, release, type);
  }

  @Override
  public int compareTo(Folder other) {
    return release - other.release;
  }

  public boolean isSources() {
    return type == FolderType.SOURCES;
  }

  public boolean isResources() {
    return type == FolderType.RESOURCES;
  }
}
