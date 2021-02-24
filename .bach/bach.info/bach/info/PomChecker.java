package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

public record PomChecker(Path jar, List<Argument> arguments)
    implements Command<PomChecker>, ToolProvider {

  public static PomChecker install(Bach bach) {
    var version = "1.1.0-SNAPSHOT";
    var file = "pomchecker-toolprovider-" + version + ".jar";
    var uri = "https://github.com/kordamp/pomchecker/releases/download/early-access/" + file;
    var dir = bach.folders().externalTools("kordamp-pomchecker", version);
    var jar = dir.resolve(file);
    if (!Files.exists(jar))
      try {
        Files.createDirectories(dir);
        bach.browser().load(uri, jar);
      } catch (Exception exception) {
        throw new RuntimeException("Install failed: " + exception.getMessage());
      }
    return new PomChecker(jar, List.of());
  }

  @Override
  public PomChecker arguments(List<Argument> arguments) {
    return new PomChecker(jar, arguments);
  }

  @Override
  public String name() {
    return "pomchecker";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (!Files.exists(jar)) {
      err.println("File not found: " + jar);
      return -2;
    }
    var java = Command.of("java").add("-jar", jar.toString());
    var builder = new ProcessBuilder("java");
    builder.command().addAll(java.toStrings());
    builder.command().addAll(List.of(args));
    try {
      var process = builder.start();
      new Thread(new StreamGobbler(process.getInputStream(), out::println)).start();
      new Thread(new StreamGobbler(process.getErrorStream(), err::println)).start();
      return process.waitFor();
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return -1;
    }
  }

  record StreamGobbler(InputStream stream, Consumer<String> consumer) implements Runnable {
    public void run() {
      new BufferedReader(new InputStreamReader(stream)).lines().forEach(consumer);
    }
  }
}
