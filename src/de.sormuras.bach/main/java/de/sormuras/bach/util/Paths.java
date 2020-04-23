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

package de.sormuras.bach.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** {@link Path}-related utilities. */
public /*static*/ class Paths {
  /** Test for a regular file of size zero or an empty directory. */
  public static boolean isEmpty(Path path) {
    try {
      if (Files.isRegularFile(path)) return Files.size(path) == 0L;
      try (var stream = Files.list(path)) {
        return stream.findAny().isEmpty();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Delete a tree of directories starting with the given root directory. */
  public static void delete(Path directory, Predicate<Path> filter) throws IOException {
    // trivial case: delete existing empty directory or single file
    try {
      Files.deleteIfExists(directory);
      return;
    } catch (DirectoryNotEmptyException __) {
      // fall-through
    }
    // default case: walk the tree from leaves back to root directories...
    try (var stream = Files.walk(directory)) {
      var paths = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
    }
  }

  /** Check the size and message digest hashes of the specified file. */
  public static Path assertFileAttributes(Path file, Map<String, String> attributes) {
    if (attributes.isEmpty()) return file;

    var map = new HashMap<>(attributes);
    var size = map.remove("size");
    if (size != null) {
      var expectedSize = Long.parseLong(size);
      try {
        long fileSize = Files.size(file);
        if (expectedSize != fileSize) {
          var details = "expected " + expectedSize + " bytes\n\tactual " + fileSize + " bytes";
          throw new AssertionError("File size mismatch: " + file + "\n\t" + details);
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    map.remove("module");
    map.remove("version");

    if (map.isEmpty()) return file;

    // remaining entries are treated as message digest algorithm-value pairs
    // https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#messagedigest-algorithms
    try {
      var bytes = Files.readAllBytes(file);
      for (var expectedDigest : map.entrySet()) {
        var actual = digest(expectedDigest.getKey(), bytes);
        var expected = expectedDigest.getValue();
        if (expected.equalsIgnoreCase(actual)) continue;
        var details = "expected " + expected + ", but got " + actual;
        throw new AssertionError("File digest mismatch: " + file + "\n\t" + details);
      }
    } catch (Exception e) {
      throw new AssertionError("File digest check failed: " + file, e);
    }
    return file;
  }

  public static String digest(String algorithm, byte[] bytes) throws Exception {
    var md = MessageDigest.getInstance(algorithm);
    md.update(bytes);
    return Strings.hex(md.digest());
  }

  private Paths() {}
}
