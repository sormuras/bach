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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void defaultProjectProperties() {
    Bach bach = new Bach();
    assertEquals("bach", bach.project.name);
    assertEquals("SNAPSHOT", bach.project.version);
    assertNotNull(bach.project.charset);
    assertTrue(bach.project.mains.isEmpty());
  }

  @Test
  void resolveDefaultPaths() {
    Bach bach = new Bach();
    assertEquals(Paths.get("."), bach.root);
    assertEquals(Paths.get(".bach"), bach.project.resolveAuxiliary());
    assertEquals(Paths.get(".bach", "resolved"), bach.project.resolveAuxResolved());
    assertEquals(Paths.get(".bach", "tools"), bach.project.resolveAuxTools());
    assertEquals(Paths.get("target", "bach"), bach.project.resolveTarget());
    assertEquals(Paths.get("target", "bach", "bach"), bach.project.resolveTargetLinked());
    assertEquals(Paths.get("target", "bach", "mods"), bach.project.resolveTargetMods());
    assertTrue(bach.project.resolveJdkHome().isAbsolute());
    assumeTrue(Files.exists(bach.project.resolveJdkHome()));
  }

  @Test
  void resolvePathsWithCustomRoot() {
    Bach bach = new Bach(Paths.get("demo", "01-hello-world"));
    assertEquals("01-hello-world", bach.project.name);
    bach.project =
        bach.new Project() {

          @Override
          Path resolveTargetMods() {
            return bach.root.resolve("modules");
          }
        };
    bach.project.pathTarget = Paths.get("out");
    assertEquals(bach.root.resolve(".bach/tools"), bach.project.resolveAuxTools());
    assertEquals(bach.root.resolve("modules"), bach.project.resolveTargetMods());
    assertEquals(bach.root.resolve("out/01-hello-world"), bach.project.resolveTargetLinked());
  }

  @Test
  void customTool() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Bach bach = new Bach();
    bach.streamOut = new PrintStream(out);
    bach.tools.put("custom tool", new CustomTool());
    bach.call("custom tool", 1, 2, 3);
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
    Error e2 = assertThrows(Error.class, () -> bach.call("java", 0, UnaryOperator.identity()));
    assertEquals("execution failed with unexpected error code: 1", e2.getMessage());
    Error e3 = assertThrows(Error.class, () -> bach.call("executable, that doesn't exist", 123));
    assertEquals("executing `executable, that doesn't exist` failed", e3.getMessage());
  }

  @Test
  void isJavaFile() {
    Bach bach = new Bach();
    assertFalse(bach.isJavaFile(Paths.get("")));
    assertFalse(bach.isJavaFile(Paths.get("a/b")));
    assertTrue(bach.isJavaFile(Paths.get("src/test/java/BachTests.java")));
  }

  static class CustomTool implements ToolProvider {

    @Override
    public String name() {
      return "custom tool";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      out.println("CustomTool with " + Arrays.toString(args));
      return 0;
    }
  }
}
