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

package de.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

class ResourcesTests {

  final HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();

  @Nested
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  class MavenApacheOrg {

    final URI uri =
        URI.create(
            "https://repo.maven.apache.org/maven2"
                + "/org/junit/jupiter"
                + "/junit-jupiter"
                + "/5.4.0"
                + "/junit-jupiter-5.4.0.jar");

    @Test
    void head() throws Exception {
      var resources = new Resources(client);
      assertEquals(200, resources.head(uri, 9).statusCode());
    }

    @Test
    void read() throws Exception {
      var asc = URI.create(this.uri + ".asc");
      var resources = new Resources(client);
      var string = resources.read(asc);
      assertLinesMatch(
          List.of(
              "-----BEGIN PGP SIGNATURE-----",
              "Version: BCPG v1.60",
              "",
              ">> BASE 64 LINES >>",
              "HQGc6jYj9npZZekwuOyp",
              "=aGW0",
              "-----END PGP SIGNATURE-----"),
          string.lines().collect(Collectors.toList()));
    }

    @Test
    void copy(@TempDir Path temp) throws Exception {
      var resources = new Resources(client);
      var jar = temp.resolve("jupiter.jar");
      assertTrue(Files.notExists(jar));
      resources.copy(uri, jar);
      assertTrue(Files.exists(jar));
      resources.copy(uri, jar);
      assertSame(
          jar,
          assertFileAttributes(
              jar,
              Map.of(
                  "size",
                  "5929",
                  "md5",
                  "66739fcadcef76d68975c14c9bd68ea1",
                  "sha1",
                  "86152263dcb465a6d25db68aaab15ebbab88c691",
                  "sha3-512",
                  "1db0ca64d85540f578fa2add661fa1489ea5ac45f927000a6440a12ac7089f00f48e636ab91fc8220946a704cb5605e7887ed4b730f70cf53661020988076f81")));
    }

    @Test
    void copyOfNonExistentResourceFails() {
      var uri = URI.create("https://sormuras.de/does-not-exist");
      var file = Path.of("-");
      assertThrows(IllegalStateException.class, () -> new Resources(client).copy(uri, file));
      assertTrue(Files.notExists(file));
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
      } catch (Exception e) {
        throw new Error(e);
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
}
