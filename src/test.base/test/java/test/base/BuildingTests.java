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

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BuildingTests {

  @Test
  void callMavenVersionViaJShell() throws Exception {
    var builder = new ProcessBuilder("jshell");
    builder.command().add("--execution=local");
    builder.command().add("-J-ea");
    builder.command().add("-"); // Standard input, without interactive I/O.
    var process = builder.start();
    var source = List.of("/open BUILDING", "env(\"mvn\", \"--version\")", "/exit", "");
    process.getOutputStream().write(String.join("\n", source).getBytes());
    process.getOutputStream().flush();
    process.waitFor(19, TimeUnit.SECONDS);
    assertStreams(List.of(">> EXE >>", "Apache Maven .+", ">> INFO >>"), List.of(), process);
    assertEquals(0, process.exitValue(), process.toString());
  }

  static void assertStreams(
      List<String> expectedLines, List<String> expectedErrors, Process process) {
    var out = Strings.lines(process.getInputStream());
    var err = Strings.lines(process.getErrorStream());
    try {
      assertLinesMatch(expectedLines, out);
      assertLinesMatch(expectedErrors, err);
    } catch (AssertionError e) {
      var msg = String.join("\n", err) + String.join("\n", out);
      System.out.println(msg);
      throw e;
    }
  }
}
