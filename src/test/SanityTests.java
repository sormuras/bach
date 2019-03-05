import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SanityTests {
  @Test
  void openAndRunBachJavaInJShellReturnsZero(@TempDir Path temp) throws Exception {
    var builder = new ProcessBuilder("jshell");
    builder.command().add("--execution=local");
    builder.command().add("-"); // Standard input, without interactive I/O.
    var err = temp.resolve("err.txt");
    var out = temp.resolve("out.txt");
    var process = builder.redirectError(err.toFile()).redirectOutput(out.toFile()).start();
    process.getOutputStream().write("/open src/bach/Bach.java\n".getBytes());
    process.getOutputStream().write("var bach = new Bach()\n".getBytes());
    process.getOutputStream().write("/exit\n".getBytes());
    process.getOutputStream().flush();
    if (!process.waitFor(23, TimeUnit.SECONDS)) {
      process.destroy();
    }
    var code = process.exitValue();
    assertEquals(0, code, Files.readString(err));
    assertEquals(0, Files.size(out), Files.readString(out));
    assertEquals(0, Files.size(err), Files.readString(err));
  }

  @Test
  void compileAndRunBachJavaWithJavaReturnsZero() throws Exception {
    var builder = new ProcessBuilder("java");
    builder.command().add("-ea");
    builder.command().add("src/bach/Bach.java");
    builder.command().add("help");
    var process = builder.start();
    process.waitFor(23, TimeUnit.SECONDS);
    var code = process.exitValue();
    assertEquals(0, code);
  }
}
