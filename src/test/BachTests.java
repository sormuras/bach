import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
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
}
