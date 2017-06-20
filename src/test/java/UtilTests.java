/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class UtilTests {

  @Test
  void download() throws IOException {
    List<String> expectedLines = List.of("Lorem", "ipsum", "dolor", "sit", "amet");
    Path tempFile = Files.createTempFile("download-", ".txt");
    Files.write(tempFile, expectedLines);
    Path tempPath = Files.createTempDirectory("download-");
    Bach.Util.download(tempFile.toUri(), tempPath);
    Path actual = tempPath.resolve(tempFile.getFileName().toString());
    assertTrue(Files.exists(actual));
    assertLinesMatch(expectedLines, Files.readAllLines(actual));
  }

  @TestFactory
  Stream<DynamicTest> uriExists() throws IOException {
    Stream<URI> uris =
        Stream.of(
            Bach.Util.jcenter("org.junit.jupiter", "junit-jupiter-api", "5.0.0-M4"),
            Bach.Util.jitpack("com.github.sormuras", "bach", "1.0.0-M0"),
            Bach.Util.jitpack("com.github.jlink", "jqwik", "0.3.0"));
    return uris.map(uri -> dynamicTest(uri.toString(), () -> assertTrue(Bach.Util.exists(uri))));
  }

  @Test
  void blank() {
    assertTrue(Bach.Util.isBlank(null));
    assertTrue(Bach.Util.isBlank(""));
    assertTrue(Bach.Util.isBlank(" "));
  }

  @Test
  void findJdkHome() {
    assertNotNull(Bach.Util.findJdkHome());
    assertTrue(Bach.Util.findJdkHome().isAbsolute());
  }
}
