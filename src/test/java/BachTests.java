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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void isInterface() {
    assertTrue(Bach.class.isInterface());
  }

  @Test
  void builder() {
    assertNotNull(new Bach.Builder().toString());
    assertNotNull(new Bach.Builder().level(Level.OFF).build());
  }

  @Test
  void defaultConfiguration() {
    Bach.Configuration configuration = new Bach.Builder().level(Level.OFF).build().configuration();
    assertTrue(System.getProperty("user.dir").endsWith(configuration.name()), configuration.name());
    assertEquals("1.0.0-SNAPSHOT", configuration.version());
  }

  @Test
  void configurationPropertiesAreImmutable() {
    Bach.Configuration configuration = new Bach.Builder().level(Level.OFF).build().configuration();
    assertThrows(UnsupportedOperationException.class, () -> configuration.folders().clear());
  }

  @Test
  void customConfiguration() {
    Bach.Builder builder =
        new Bach.Builder()
            .name("kernel")
            .version("4.12-rc5")
            .level(Level.WARNING)
            .folder(Bach.Folder.AUXILIARY, Paths.get("aux"))
            .folder(Bach.Folder.DEPENDENCIES, Bach.Folder.Location.of(Paths.get("mods")))
            .streamErr(System.err)
            .streamOut(System.out)
            .tool(new CustomTool());
    Bach bach = builder.build();
    assertNotNull(bach.toString());
    Bach.Configuration custom = bach.configuration();
    assertEquals("kernel", custom.name());
    assertEquals("4.12-rc5", custom.version());
    assertSame(Level.WARNING, custom.level());
    assertEquals(Paths.get("aux"), custom.folders().get(Bach.Folder.AUXILIARY));
    assertEquals(Paths.get("mods"), custom.folders().get(Bach.Folder.DEPENDENCIES));
    for (Bach.Folder folder : Bach.Folder.values()) {
      assertEquals(bach.path(folder), custom.folders().get(folder));
    }
    assertEquals(System.err, custom.streamErr());
    assertEquals(System.out, custom.streamOut());
    assertEquals(CustomTool.class, custom.tools().get("custom").getClass());
  }

  @Test
  void call() {
    OutputStream out = new ByteArrayOutputStream(2000);
    Bach bach = new Bach.Builder().streamOut(new PrintStream(out)).build();
    assertEquals(0, bach.call("java", "--version"));
    assertThrows(Error.class, () -> bach.call("java", "--thisOptionDoesNotExist"));
    assertThrows(Error.class, () -> bach.call("executable, that does not exist", 1, 2, 3));
    String result = out.toString();
    assertTrue(result.contains("--thisOptionDoesNotExist"));
    assertTrue(result.contains("executable, that does not exist"));
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

  @Test
  void layout() {
    assertEquals(Bach.Layout.AUTO, Bach.Layout.of(Paths.get("non/existing/path")));
    assertEquals(Bach.Layout.BASIC, Bach.Layout.of(Paths.get("deprecated/demo/basic")));
    assertEquals(Bach.Layout.FIRST, Bach.Layout.of(Paths.get("deprecated/demo/idea")));
    assertEquals(Bach.Layout.TRAIL, Bach.Layout.of(Paths.get("deprecated/demo/common")));
  }
}
