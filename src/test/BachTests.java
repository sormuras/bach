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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {

  @Test
  void versionIsMasterXorConsumableByRuntimeVersionParse() throws Exception {
    var actual = "" + Bach.class.getDeclaredField("VERSION").get(null);
    if (actual.equals("master")) {
      return;
    }
    Runtime.Version.parse(actual);
  }

  @Test
  void userPathIsCurrentWorkingDirectory() {
    assertEquals(Path.of(".").normalize().toAbsolutePath(), Bach.USER_PATH);
  }

  @Test
  void userHomeIsUsersHome() {
    assertEquals(Path.of(System.getProperty("user.home")), Bach.USER_HOME);
  }

  @Test
  void hasPublicStaticVoidMainWithVarArgs() throws Exception {
    var main = Bach.class.getMethod("main", String[].class);
    assertTrue(Modifier.isPublic(main.getModifiers()));
    assertTrue(Modifier.isStatic(main.getModifiers()));
    assertSame(void.class, main.getReturnType());
    assertEquals("main", main.getName());
    assertTrue(main.isVarArgs());
    assertEquals(0, main.getExceptionTypes().length);
  }

  @Test
  void constructDefaultInstance() {
    var bach = new Bach();

    assertFalse(bach.debug);
    assertEquals(Path.of(""), bach.base);
    assertNotNull(bach.log);
    assertNotNull(bach.log.out);
    assertNotNull(bach.log.err);
    assertSame(System.Logger.Level.INFO, bach.log.threshold);
  }

  @Test
  void constructInstanceInDebugModeAndDifferentBaseDirectory(@TempDir Path empty) {
    var bach = new Bach(true, empty);

    assertTrue(bach.debug);
    assertEquals(empty, bach.base);
    assertNotNull(bach.log);
    assertNotNull(bach.log.out);
    assertNotNull(bach.log.err);
    assertSame(System.Logger.Level.ALL, bach.log.threshold);
  }

  @Test
  void actionsEmptyArgsReturnsDefaultActions() {
    assertEquals(List.of(Bach.Action.Default.BUILD), new Bach().actions());
  }

  @Test
  void actionsForBuildReturnsDefaultAction() {
    assertEquals(
        List.of(Bach.Action.Default.BUILD, Bach.Action.Default.BUILD, Bach.Action.Default.BUILD),
        new Bach().actions("build", "Build", "BUILD"));
  }

  @Test
  void actionsTools() {
    assertEquals(2, new Bach().actions("build", "tool", "1", "2", "3").size());
  }

  @Test
  @SwallowSystem
  void mainWithArgumentHelp(SwallowSystem.Streams streams) {
    assertDoesNotThrow(() -> Bach.main("help"));
    assertLinesMatch(List.of(">> HELP >>"), streams.outLines());
    assertLinesMatch(List.of(), streams.errLines());
  }

  @Test
  @SwallowSystem
  void mainWithArgumentThatIsUnsupported(SwallowSystem.Streams streams) {
    var e = assertThrows(IllegalArgumentException.class, () -> Bach.main("foo"));
    assertLinesMatch(List.of(), streams.outLines());
    assertLinesMatch(List.of(), streams.errLines());

    assertEquals("No enum constant Bach.Action.Default.FOO", e.getMessage());
  }

  @Test
  void runEmptyCollectionOfActions() {
    var bach = new Bach(true, Path.of(""));
    var lines = new ArrayList<String>();
    bach.log.out = lines::add;

    bach.run(List.of());
    assertLinesMatch(List.of("Performing 0 action(s)..."), lines);
  }

  @Test
  void runNoopAction() {
    new Bach().run(List.of(bach -> {}));
  }

  @Test
  void runThrowingAction() {
    var bach = new Bach();
    var lines = new ArrayList<String>();
    bach.log.err = lines::add;

    var action = new ThrowingAction();
    var error = assertThrows(Error.class, () -> bach.run(List.of(action)));
    assertEquals("Action failed: " + action.toString(), error.getMessage());
    assertEquals(UnsupportedOperationException.class, error.getCause().getClass());
    assertEquals("123", error.getCause().getMessage());

    assertLinesMatch(List.of("123"), lines);
  }

  @Test
  @SwallowSystem
  void runToolJavaDryRun(SwallowSystem.Streams streams) {
    var bach = new Bach(true, Path.of(""));
    var code = bach.run("java", "--dry-run", "src/bach/Bach.java");
    assertEquals(0, code, streams.toString());
    assertLinesMatch(
        List.of("run(java, [--dry-run, src/bach/Bach.java])", "Running tool in a new process: .+"),
        streams.outLines());
  }

  @Test
  @SwallowSystem
  void runToolJavacVersion(SwallowSystem.Streams streams) {
    var bach = new Bach(true, Path.of(""));
    var code = bach.run("javac", "--version");
    assertEquals(0, code, streams.toString());
    assertLinesMatch(
        List.of("run(javac, [--version])", "Running provided tool in-process: .+", "javac .+"),
        streams.outLines());
  }

  @Test
  @SwallowSystem
  void runToolJavacWithUnknownOption(SwallowSystem.Streams streams) {
    var bach = new Bach(true, Path.of(""));
    bach.run(2, "javac", "--unknown-option");
    assertLinesMatch(
        List.of("run(javac, [--unknown-option])", "Running provided tool in-process: .+"),
        streams.outLines());

    var error = assertThrows(Error.class, () -> bach.run(0, "javac", "--unknown-option"));
    assertEquals("javac returned 2, but expected 0", error.getMessage());
  }

  @Test
  @SwallowSystem
  void runToolThatDoesNotExistFails(SwallowSystem.Streams streams) {
    var bach = new Bach(true, Path.of(""));
    var error = assertThrows(Error.class, () -> bach.run("does-not-exist"));
    assertEquals("Running tool does-not-exist failed!", error.getMessage());
    assertLinesMatch(List.of("run(does-not-exist, [])"), streams.outLines());
  }

  private static class ThrowingAction implements Bach.Action {

    @Override
    public void perform(Bach bach) {
      throw new UnsupportedOperationException("123");
    }
  }
}
