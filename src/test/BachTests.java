import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.Modifier;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

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
  void basedAbsolutePathReturnsSameInstance() {
    var absolutePath = Path.of("absolute/path").toAbsolutePath();
    assertSame(absolutePath, new Bach().based(absolutePath));
  }

  @Test
  void basedRelativePathReturnsSameInstance() {
    var relativePath = Path.of("relative/path");
    assertSame(relativePath, new Bach().based(relativePath));
  }

  @Test
  void basedRelativePath() {
    var base = Path.of("other/path");
    var relativePath = "relative/path";
    var logger = new CollectingLogger("*");
    var bach = new Bach(logger, base, List.of());
    assertEquals(base.resolve(relativePath), bach.based(relativePath));
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
  void mainWithoutArguments() {
    assertDoesNotThrow((Executable) Bach::main);
  }

  @Test
  void mainWithJavac() {
    // Emits "javac <version>" on stdout...
    assertDoesNotThrow(() -> Bach.main("tool", "javac", "--version"));
  }

  @Test
  void mainWithUnnamedToolDoesThrow() {
    var e = assertThrows(Error.class, () -> Bach.main("tool"));
    assertEquals("Bach.java failed with error code: " + 1, e.getMessage());
  }

  @Test
  void defaults() {
    var bach = new Bach();
    assertEquals("Bach.java", bach.logger.getName());
    assertEquals(System.getProperty("user.dir"), bach.base.toString());
    assertEquals(List.of(), bach.arguments);
  }

  @Test
  void defaultsWithCustomArguments() {
    var bach = new Bach(List.of("1", "2", "3"));
    assertEquals(List.of("1", "2", "3"), bach.arguments);
  }

  @Test
  void runReturnsZero() {
    var logger = new CollectingLogger("*");
    var base = Path.of(".").toAbsolutePath();
    var bach = new Bach(logger, base, List.of());
    assertEquals(0, bach.run(), logger.toString());
    assertLinesMatch(
        List.of(
            "Running action BANNER...",
            ">> BANNER >>",
            "Action BANNER succeeded.",
            ">> CHECK >>"),
        logger.getLines());
  }

  @Test
  void runReturnsOneWhenFailIsFoundInArguments() {
    var logger = new CollectingLogger("*");
    var base = Path.of(".").toAbsolutePath();
    var bach = new Bach(logger, base, List.of("FAIL"));
    assertEquals(1, bach.run());
  }

  @Test
  void runWithEmptyIterableReturnsZero() {
    var logger = new CollectingLogger("*");
    var base = Path.of(".").toAbsolutePath();
    var bach = new Bach(logger, base, List.of());
    assertEquals(0, bach.run(new Bach.Action[0]));
    assertEquals(0, bach.run(List.of()));
  }

  @TestFactory
  Stream<DynamicTest> runReturnsOneForFileSystemRoots() {
    return StreamSupport.stream(FileSystems.getDefault().getRootDirectories().spliterator(), false)
        .map(path -> dynamicTest("" + path, () -> runReturnsOneForFileSystemRoot(path)));
  }

  private void runReturnsOneForFileSystemRoot(Path root) {
    var logger = new CollectingLogger("*");
    var bach = new Bach(logger, root, List.of());
    assertEquals(1, bach.run());
  }

  @Test
  void openAndRunBachJavaInJShellReturnsZero() throws Exception {
    var builder = new ProcessBuilder("jshell");
    builder.command().add("--execution=local");
    builder.command().add("-"); // Standard input, without interactive I/O.
    var process = builder.start();
    process.getOutputStream().write("/open src/main/Bach.java\n".getBytes());
    process.getOutputStream().write("var bach = new Bach()\n".getBytes());
    process.getOutputStream().write("var code = bach.run()\n".getBytes());
    process.getOutputStream().write("/exit code\n".getBytes());
    process.getOutputStream().flush();
    process.waitFor(20, TimeUnit.SECONDS);
    var code = process.exitValue();
    assertEquals(0, code);
  }

  @Test
  void compileAndRunBachJavaWithJavaReturnsZero() throws Exception {
    var builder = new ProcessBuilder("java");
    builder.command().add("-ea");
    builder.command().add("src/main/Bach.java");
    var process = builder.start();
    process.waitFor(10, TimeUnit.SECONDS);
    var code = process.exitValue();
    assertEquals(0, code);
  }
}
