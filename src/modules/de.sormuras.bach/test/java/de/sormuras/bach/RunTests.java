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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class RunTests {

  private final TestRun test = new TestRun();

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
            "Running tool 'javac' with: [--version]",
            "javac " + Runtime.version().feature() + ".*",
            "Tool 'javac' successfully run."),
        test.outLines());
  }

  @Test
  void toDurationMillisReturnsNonZeroValue() throws InterruptedException {
    Thread.sleep(123);
    assertTrue(test.toDurationMillis() != 0);
  }

  @Test
  void toStringContains() {
    var run = new Run(System.Logger.Level.OFF, true, null, null);
    var expected = "Run{dryRun=true, threshold=OFF, start=" + run.start + ", out=null, err=null}";
    var actual = run.toString();
    assertEquals(expected, actual);
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
