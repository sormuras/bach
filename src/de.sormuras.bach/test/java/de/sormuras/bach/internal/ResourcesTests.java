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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import test.base.Log;

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
      var log = new Log();
      var resources = new Resources(log, client);
      assertEquals(200, resources.head(uri, 9).statusCode());
      log.assertThatEverythingIsFine();
      assertLinesMatch(List.of(), log.lines());
    }

    @Test
    void copy(@TempDir Path temp) throws Exception {
      var log = new Log();
      var resources = new Resources(log, client);
      var jar =
          resources.copy(uri, temp.resolve("jupiter.jar"), StandardCopyOption.COPY_ATTRIBUTES);
      assertTrue(Files.exists(jar));
      resources.copy(uri, temp.resolve("jupiter.jar"));
      log.assertThatEverythingIsFine();
      assertLinesMatch(
          List.of("L " + jar + " copied 5929 bytes from " + uri, "L " + uri + " not modified"),
          log.lines());
    }

    @Test
    void read() throws Exception {
      var asc = URI.create(this.uri + ".asc");
      var log = new Log();
      var resources = new Resources(log, client);
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
      log.assertThatEverythingIsFine();
      assertLinesMatch(List.of(), log.lines());
    }
  }
}
