/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.project;

import de.sormuras.bach.internal.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** A source directory potentially targeting a specific Java SE release. */
public final class SourceDirectory implements Comparator<SourceDirectory> {

  private static final Pattern RELEASE_PATTERN = Pattern.compile(".*?(\\d+)$");

  private static final Comparator<SourceDirectory> COMPARATOR =
      Comparator.comparingInt(SourceDirectory::release).thenComparing(SourceDirectory::path);

  public static SourceDirectory of(Path path) {
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    var file = path.normalize().getFileName();
    var name = file != null ? file : path.toAbsolutePath().getFileName();
    return new SourceDirectory(path, parseRelease(name.toString()));
  }

  public static Set<SourceDirectory> ofAll(Path infoDirectory) {
    var source = of(infoDirectory); // contains module-info.java file
    var parent = infoDirectory.getParent();
    if (source.release() == 0 || parent == null) {
      var java = infoDirectory.resolveSibling("java");
      if (java.equals(infoDirectory) || Files.notExists(java)) return Set.of(source);
      return Set.of(new SourceDirectory(java, 0), source);
    }
    return Paths.list(parent, Files::isDirectory).stream()
        .map(SourceDirectory::of)
        .filter(SourceDirectory::isTargeted)
        .collect(Collectors.toUnmodifiableSet());
  }

  static int parseRelease(String name) {
    if (name == null || name.isEmpty()) return 0;
    var matcher = RELEASE_PATTERN.matcher(name);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  private final Path path;
  private final int release;

  /**
   * Initialize this source directory instance with the given components.
   *
   * @param path The path to this source directory, usually relative to project's base directory
   * @param release The Java SE release to compile sources for
   */
  public SourceDirectory(Path path, int release) {
    this.path = path;
    this.release = release;
  }

  public Path path() {
    return path;
  }

  public int release() {
    return release;
  }

  @Override
  public int compare(SourceDirectory o1, SourceDirectory o2) {
    return COMPARATOR.compare(o1, o2);
  }

  public boolean isTargeted() {
    return release != 0;
  }

  public boolean isModuleInfoJavaPresent() {
    return Files.isRegularFile(path.resolve("module-info.java"));
  }
}
