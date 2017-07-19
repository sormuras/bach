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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void resolveDefaultPaths() {
    Bach bach = new Bach();
    assertEquals(Paths.get("."), bach.folder.getRoot());
    assertEquals(Paths.get(".bach"), bach.folder.resolveAuxiliary());
    assertEquals(Paths.get(".bach", "resolved"), bach.folder.resolveAuxResolved());
    assertEquals(Paths.get(".bach", "tools"), bach.folder.resolveAuxTools());
    assertEquals(Paths.get("target", "bach"), bach.folder.resolveTarget());
    assertEquals(Paths.get("target", "bach", "linked"), bach.folder.resolveTargetLinked());
    assertEquals(Paths.get("target", "bach", "mods"), bach.folder.resolveTargetMods());
    assertTrue(bach.folder.resolveJdkHome().isAbsolute());
    Assumptions.assumeTrue(Files.exists(bach.folder.resolveJdkHome()));
  }

  @Test
  void resolvePathsWithCustomRoot() {
    Bach bach = new Bach();
    Path root = Paths.get("demo", "01-hello-world");
    bach.folder =
        bach.new Folder() {
          @Override
          Path getRoot() {
            return root;
          }

          @Override
          Path getTarget() {
            return Paths.get("out");
          }

          @Override
          Path getTargetMods() {
            return Paths.get("classes");
          }
        };
    assertEquals(root, bach.folder.getRoot());
    assertEquals(root.resolve(".bach/tools"), bach.folder.resolveAuxTools());
    assertEquals(root.resolve("out/classes"), bach.folder.resolveTargetMods());
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
