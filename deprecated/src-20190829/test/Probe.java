import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

class Probe extends Bach.Configuration.Basic {

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();
  final Path redirected; // for external processes
  final Bach bach;

  Probe() {
    this(Path.of(""), Path.of("target/probe"));
  }

  Probe(Path home, Path work) {
    try {
      Files.createDirectories(work);
    } catch (IOException e) {
      throw new UncheckedIOException("Creating work directory failed!", e);
    }
    var random = Math.random();
    this.redirected = work.resolve("probe-output-" + random + ".txt");
    var configuration = Bach.Configuration.of(this, home, work);
    this.bach = new Bach(new PrintWriter(out), new PrintWriter(err), configuration);
  }

  List<String> lines() {
    return out.toString().lines().collect(Collectors.toList());
  }

  List<String> errors() {
    return err.toString().lines().collect(Collectors.toList());
  }

  @Override
  UnaryOperator<ProcessBuilder> redirectIO() {
    return it -> it.redirectOutput(redirected.toFile()).redirectErrorStream(true);
  }

  @Override
  System.Logger.Level threshold() {
    return System.Logger.Level.ALL;
  }

  @Override
  Map<String, Bach.Tool> tools() {
    return Map.of("noop", new NoopTool(), "fail", __ -> 1);
  }

  @Override
  public String toString() {
    return "\n\n___err\n" + err + "\n\n___out\n" + out;
  }

  static class NoopTool implements Bach.Tool {

    @Override
    public int run(Bach __) {
      return 0;
    }
  }
}
