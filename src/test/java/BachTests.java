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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void customTool() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Bach bach = new Bach();
    bach.streamOut = new PrintStream(out);
    bach.tools.put("custom", new CustomTool());
    bach.call("custom", 1, 2, 3);
    assertLinesMatch(
        List.of(">> dump >>", "CustomTool with [1, 2, 3]"),
        List.of(out.toString().split(System.lineSeparator())));
  }

  @Test
  void call() {
    Bach bach = new Bach();
    bach.streamOut = new PrintStream(new ByteArrayOutputStream(2000));
    bach.call("java", "--version");
    Error e1 = assertThrows(Error.class, () -> bach.call("java", "--thisOptionDoesNotExist"));
    assertEquals("execution failed with unexpected error code: 1", e1.getMessage());
    Error e2 = assertThrows(Error.class, () -> bach.call("executable, that doesn't exist", 123));
    assertEquals("executing `executable, that doesn't exist` failed", e2.getMessage());
  }

  static class CustomTool implements ToolProvider {

    @Override
    public String name() {
      return "custom";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      out.println("CustomTool with " + Arrays.toString(args));
      return 0;
    }
  }
}
