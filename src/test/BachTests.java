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
import java.nio.file.Files;
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
    assertNotNull(bach.tools);
    assertEquals(2, bach.tools.size(), bach.tools.toString());
    assertTrue(bach.tools.containsKey("format"));
    assertTrue(bach.tools.containsKey("maven"));
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
  void mainWithArgumentBuild(SwallowSystem.Streams streams) {
    assertDoesNotThrow(() -> Bach.main("build"));
    assertLinesMatch(List.of("Action BUILD disabled."), streams.outLines());
    assertLinesMatch(List.of(), streams.errLines());
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
    var bach = new Bach(true, Path.of(""));
    var logOutput = new ArrayList<String>();
    var logErrors = new ArrayList<String>();
    bach.log.out = logOutput::add;
    bach.log.err = logErrors::add;

    var action = new ThrowingAction();
    var error = assertThrows(Error.class, () -> bach.run(List.of(action)));
    assertEquals("Action failed: " + action.toString(), error.getMessage());
    assertEquals(UnsupportedOperationException.class, error.getCause().getClass());
    assertEquals("123", error.getCause().getMessage());

    assertLinesMatch(
        List.of("Performing 1 action(s)...", "\\Q>> BachTests$ThrowingAction@\\E.+"), logOutput);
    assertLinesMatch(List.of("123"), logErrors);
  }

  @Test
  void runThrowingTool() {
    var bach = new Bach(true, Path.of(""));
    var log = new ArrayList<String>();
    var tool = new ThrowingTool();
    bach.log.out = log::add;
    bach.tools.put("throws", tool);

    var error = assertThrows(Error.class, () -> bach.run("throws", "x"));
    assertEquals("Running tool throws failed!", error.getMessage());
    assertEquals(Exception.class, error.getCause().getClass());
    assertEquals("123", error.getCause().getMessage());

    assertLinesMatch(List.of("run(throws, [x])", "Running mapped tool in-process: .+"), log);
  }

  @Test
  @SwallowSystem
  void runToolJavaDryRun(SwallowSystem.Streams streams) {
    var bach = new Bach(true, Path.of(""));
    var code = bach.run("java", "--dry-run", "src/bach/Bach.java");
    assertEquals(0, code, streams.toString());
    assertLinesMatch(
        List.of(
            "run(java, [--dry-run, src/bach/Bach.java])",
            "Redirect: INHERIT",
            "Running tool in a new process: .+"),
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
    assertEquals(
        "Expected 0, but got 2 as result of: javac [--unknown-option]", error.getMessage());
  }

  @Test
  @SwallowSystem
  void runToolThatDoesNotExistFails(SwallowSystem.Streams streams) {
    var bach = new Bach(true, Path.of(""));
    var error = assertThrows(Error.class, () -> bach.run("does-not-exist"));
    assertEquals("Running tool does-not-exist failed!", error.getMessage());
    assertLinesMatch(List.of("run(does-not-exist, [])", "Redirect: INHERIT"), streams.outLines());
  }

  @Test
  @SwallowSystem
  void runToolFormat(SwallowSystem.Streams streams) throws Exception {
    var bach = new Bach(true, Path.of(""));
    bach.properties.setProperty(Bach.Property.RUN_REDIRECT_TYPE.key, "FILE");
    var code = bach.run("format", "--version");
    assertEquals(0, code, streams.toString());
    assertLinesMatch(
        List.of(
            "run(format, [--version])",
            ">> DOWNLOAD/INSTALL >>",
            "Redirect: FILE .+",
            "Running tool in a new process: .+"),
        streams.outLines());
    assertLinesMatch(
        List.of("google-java-format: Version 1.7"),
        Files.readAllLines(Path.of(bach.get(Bach.Property.RUN_REDIRECT_FILE))));
  }

  @Test
  @SwallowSystem
  void runToolFormatDryRun(SwallowSystem.Streams streams) throws Exception {
    var bach = new Bach(true, Path.of(""));
    bach.properties.setProperty(Bach.Property.RUN_REDIRECT_TYPE.key, "DISCARD");
    Bach.Tool.format(bach, false, List.of(Path.of("src", "bach")));
    assertLinesMatch(
        List.of(">> DOWNLOAD/INSTALL >>", "Redirect: DISCARD", "Running tool in a new process: .+"),
        streams.outLines());
  }

  private static class ThrowingAction implements Bach.Action {

    @Override
    public void perform(Bach bach) {
      throw new UnsupportedOperationException("123");
    }
  }

  private static class ThrowingTool implements Bach.Tool {

    @Override
    public void run(Bach bach, Object... args) throws Exception {
      throw new Exception("123");
    }
  }
}
