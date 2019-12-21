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

package test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BuildingTests {

  @Test
  void callMavenVersionViaJShell() throws Exception {
    var os = System.getProperty("os.name").toLowerCase();
    if (os.contains("nux") | os.contains("nix") | os.contains("aix"))
      Files.createDirectories(Path.of(System.getProperty("user.home"), ".java", ".userPrefs"));
    var builder = new ProcessBuilder("jshell");
    builder.command().add("--execution=local");
    builder.command().add("-J-ea");
    builder.command().add("-"); // Standard input, without interactive I/O.
    var process = builder.start();
    var source = List.of("/open BUILDING", "cmd(\"mvn\", \"--version\")", "/exit", "");
    process.getOutputStream().write(String.join("\n", source).getBytes());
    process.getOutputStream().write("/exit code\n".getBytes());
    process.getOutputStream().flush();
    process.waitFor(19, TimeUnit.SECONDS);
    assertStreams(List.of(">> EXE >>", "Apache Maven .+", ">> INFO >>"), List.of(), process);
    assertEquals(0, process.exitValue(), process.toString());
  }

  static void assertStreams(
      List<String> expectedLines, List<String> expectedErrors, Process process) {
    var out = lines(process.getInputStream());
    var err = lines(process.getErrorStream());
    try {
      assertLinesMatch(expectedLines, out);
      assertLinesMatch(expectedErrors, err);
    } catch (AssertionError e) {
      var msg = String.join("\n", err) + String.join("\n", out);
      System.out.println(msg);
      throw e;
    }
  }

  static List<String> lines(InputStream stream) {
    try (var reader = new BufferedReader(new InputStreamReader(stream))) {
      return reader.lines().collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException("Reading from stream failed!", e);
    }
  }
}
