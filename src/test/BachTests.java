import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
  void openAndRunBachJavaInJShellReturnsZero(@TempDir Path temp) throws Exception {
    var builder = new ProcessBuilder("jshell");
    builder.command().add("--execution=local");
    builder.command().add("-"); // Standard input, without interactive I/O.
    var err = temp.resolve("err.txt");
    var out = temp.resolve("out.txt");
    var process = builder.redirectError(err.toFile()).redirectOutput(out.toFile()).start();
    process.getOutputStream().write("/open src/bach/Bach.java\n".getBytes());
    process.getOutputStream().write("/exit Bach.main(List.of())\n".getBytes());
    process.getOutputStream().flush();
    if (!process.waitFor(20, TimeUnit.SECONDS)) {
      process.destroy();
    }
    var code = process.exitValue();
    assertEquals(0, code, Files.readString(err));
    // assertEquals(0, Files.size(out), Files.readString(out));
    assertEquals(0, Files.size(err), Files.readString(err));
  }

  @Test
  void compileAndRunBachJavaWithJavaReturnsZero() throws Exception {
    var builder = new ProcessBuilder("java");
    builder.command().add("-ea");
    builder.command().add("src/bach/Bach.java");
    var process = builder.start();
    process.waitFor(20, TimeUnit.SECONDS);
    var code = process.exitValue();
    assertEquals(0, code);
  }

  @Test
  @SwallowSystem
  void mainWithoutArguments(SwallowSystem.Streams streams) {
    assertDoesNotThrow((Executable) Bach::main);
    assertLinesMatch(List.of(">> BANNER >>"), streams.outLines());
  }

  @Test
  @SwallowSystem
  void mainWithBanner(SwallowSystem.Streams streams) {
    assertDoesNotThrow(() -> Bach.main("banner"));
    assertLinesMatch(List.of(">> BANNER >>"), streams.outLines());
  }

  @Test
  @SwallowSystem
  void mainWithJavac(SwallowSystem.Streams streams) {
    assertDoesNotThrow(() -> Bach.main("tool", "javac", "--version"));
    assertLinesMatch(List.of("javac .+"), streams.outLines());
  }

  @Test
  @SwallowSystem
  void mainWithUnnamedToolDoesThrow(SwallowSystem.Streams streams) {
    var e = assertThrows(Error.class, () -> Bach.main("tool"));
    assertEquals("Bach.java failed -- expected exit code of 0, but got: 1", e.getMessage());
    assertLinesMatch(List.of("No name supplied for tool action!"), streams.errLines());
  }

  @Test
  @SwallowSystem
  void mainWithUnknownToolDoesThrow(SwallowSystem.Streams streams) {
    var e = assertThrows(Error.class, () -> Bach.main("t o o l"));
    assertEquals("Bach.java failed -- expected exit code of 0, but got: 1", e.getMessage());
    var line = "Unsupported action: t o o l -> No enum constant Bach.Action.T O O L";
    assertLinesMatch(List.of(line), streams.errLines());
  }
}
