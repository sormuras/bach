package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Targets a folder to a Java feature version, with {@code 0} indicating no target version.
 *
 * @param path the directory
 * @param release the Java feature release version to target, 0 for none
 * @param types the set of types associated with the directory
 */
public record Folder(Path path, int release, FolderTypes types) implements Comparable<Folder> {
  static final Pattern RELEASE_PATTERN = Pattern.compile(".*?(\\d+)$");

  static int parseReleaseNumber(String string) {
    var matcher = RELEASE_PATTERN.matcher(string);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  public static Folder of(Path path, FolderType... folderTypes) {
    if (Files.notExists(path)) throw new IllegalArgumentException("No such path: " + path);
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    var release = parseReleaseNumber(path.getFileName().toString());
    var types = FolderTypes.of(folderTypes);
    return new Folder(path, release, types);
  }

  @Override
  public int compareTo(Folder other) {
    return release - other.release;
  }
}
