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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BachTests {

  @BeforeEach
  void clear() {
    Bach.log.level = Bach.Log.Level.OFF;
  }

  @Test
  @AssumeOnline
  void bootstrap() throws IOException {
    // Path target = Files.createDirectories(Paths.get("target"));
    Path target = Files.createTempDirectory("bach-bootstrap-");
    URL context = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/bach/");
    for (Path script : Set.of(target.resolve("Bach.java"), target.resolve("Bach.jsh"))) {
      // if (Files.exists(script)) continue; // uncomment to preserve existing files
      try (InputStream stream = new URL(context, script.getFileName().toString()).openStream()) {
        Files.copy(stream, script, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        System.err.println("bootstrap('" + script + "') failed: " + e.toString());
      }
    }
  }

  @Test
  void commandVisitor() {
    Bach.Command.Visitor visitor =
        command -> command.addAll(Paths.get("."), Bach.Basics::isJavaFile);
    Bach.Command command = new Bach.Command("test").add(visitor);
    assertFalse(command.arguments.isEmpty());
    assertTrue(command.arguments.contains(Paths.get("./src/bach/Bach.java").toString()));
  }

  @Test
  void moduleInfoEmpty() {
    Bach.Basics.ModuleInfo info = Bach.Basics.ModuleInfo.of(List.of("module foo {}"));
    assertEquals("foo", info.getName());
    assertTrue(info.getRequires().isEmpty());
  }

  @Test
  void moduleInfoRequiresBarAndBaz() {
    String source = "module   foo{requires a; requires static b; requires any modifier c;}";
    Bach.Basics.ModuleInfo info = Bach.Basics.ModuleInfo.of(source);
    assertEquals("foo", info.getName());
    assertEquals(3, info.getRequires().size());
    assertTrue(info.getRequires().contains("a"));
    assertTrue(info.getRequires().contains("b"));
    assertTrue(info.getRequires().contains("c"));
  }

  @Test
  void moduleInfoFromFile() {
    Path source = Paths.get("demo/02-testing/src/test/java/application");
    Bach.Basics.ModuleInfo info = Bach.Basics.ModuleInfo.of(source);
    assertEquals("application", info.getName());
    assertEquals(2, info.getRequires().size());
    assertTrue(info.getRequires().contains("application.api"));
    assertTrue(info.getRequires().contains("org.junit.jupiter.api"));
  }

  @Test
  void moduleInfoFromM1() throws Exception {
    ClassLoader loader = getClass().getClassLoader();
    URL resource = loader.getResource("data/M1.txt");
    if (resource == null) {
      fail("resource not found!");
    }
    Bach.Basics.ModuleInfo info = Bach.Basics.ModuleInfo.of(Paths.get(resource.toURI()));
    assertEquals("com.google.m", info.getName());
    assertEquals(3, info.getRequires().size());
    assertTrue(info.getRequires().contains("com.google.r1"));
    assertTrue(info.getRequires().contains("com.google.r2"));
    assertTrue(info.getRequires().contains("com.google.r3"));
  }

  @Test
  void findExternalModuleNames() {
    Set<String> names = Bach.Basics.findExternalModuleNames(Paths.get("demo"));
    assertTrue(names.contains("org.junit.jupiter.api"));
    assertFalse(names.contains("hello"));
    assertFalse(names.contains("world"));
  }
}
