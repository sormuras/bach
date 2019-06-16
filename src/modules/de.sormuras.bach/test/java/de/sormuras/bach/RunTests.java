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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunTests {

  private final TestRun test = new TestRun();

  @Test
  void defaultProperties() {
    //noinspection MismatchedQueryAndUpdateOfCollection
    var defaults = new Run.DefaultProperties();
    assertEquals("", defaults.getProperty("home"));
    assertEquals("target/bach", defaults.getProperty("work"));
    assertEquals("false", defaults.getProperty("debug"));
    assertEquals("false", defaults.getProperty("dry-run"));
    assertEquals("INFO", defaults.getProperty("threshold"));
    assertEquals(5, defaults.size());
  }

  @Test
  void loadProperties(@TempDir Path home) throws Exception {
    var path = Files.write(home.resolve("bach.properties"), new byte[] {-128});
    var error = assertThrows(Error.class, () -> Run.newProperties(home));
    assertEquals("Loading properties failed: " + path, error.getMessage());
    assertEquals("Input length = 1", error.getCause().getMessage());
  }

  @Test
  void logState() {
    test.logState(System.Logger.Level.TRACE);
    assertLinesMatch(
        List.of(
            "home = <empty> (" + Path.of("").toAbsolutePath() + ")",
            "work = " + Path.of("target", "test-run"),
            "debug = true",
            "dry-run = false",
            "threshold = ALL",
            "out = java.io.PrintWriter@.+",
            "err = java.io.PrintWriter@.+",
            "start = .+"),
        test.outLines());
  }

  @Test
  void runCustomTool() {
    assertDoesNotThrow(() -> test.run(new CustomTool(0)));
    var error = assertThrows(Error.class, () -> test.run(new CustomTool(1)));
    assertEquals("Tool 'custom' run failed with error code: 1", error.getMessage());
  }

  @Test
  void runJavaCompilerWithVersionArgument() {
    var command = new Command("javac").add("--version");
    assertDoesNotThrow(() -> test.run(command));
    assertLinesMatch(
        List.of(
            "Run::run(javac, --version)",
            "javac " + Runtime.version().feature() + ".*",
            "Tool 'javac' successfully run."),
        test.outLines());
  }

  @Test
  void toDurationMillisReturnsPositiveValue() throws InterruptedException {
    Thread.sleep(9);
    assertTrue(test.toDurationMillis() > 0);
  }

  @Test
  void toStringContainsConfiguredValues() {
    var properties = new Run.DefaultProperties();
    properties.setProperty("home", "run");
    properties.setProperty("work", "space");
    properties.setProperty("dry-run", "true");
    properties.setProperty("threshold", System.Logger.Level.OFF.name());
    var run = new Run(properties, null, null);
    assertNull(run.out);
    assertNull(run.err);
    assertEquals(System.Logger.Level.OFF, run.threshold);
    assertEquals(
        "Run{home=run, work=run" + File.separator + "space, debug=false, dryRun=true}",
        run.toString());
  }

  static class CustomTool implements ToolProvider {

    final int result;

    CustomTool(int result) {
      this.result = result;
    }

    @Override
    public String name() {
      return "custom";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      return result;
    }
  }
}
