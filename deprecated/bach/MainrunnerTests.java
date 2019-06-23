/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package de.sormuras.bach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

class MainrunnerTests {

  /** https://github.com/sormuras/mainrunner/releases */
  private static final String VERSION = "2.0.5";

  @Test
  void buildMainrunner(@TempDir Path temp) throws Exception {
    var uri = URI.create("https://github.com/sormuras/mainrunner/archive/" + VERSION + ".zip");
    var zip = new Downloader(new TestRun(temp, temp), temp).download(uri);
    var extract =
        new ProcessBuilder("jar", "--extract", "--file", zip.getFileName().toString())
            .directory(temp.toFile())
            .inheritIO()
            .start();
    assertEquals(0, extract.waitFor(), extract.toString());

    var test = new TestRun(temp, temp);
    var bach = new Bach(test);
    assertDoesNotThrow(bach::build, test::toString); // not test.toString()

    assertLinesMatch(List.of(), test.errLines());
    assertLinesMatch(List.of("TODO"), test.outLines());
  }
}
