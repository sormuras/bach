import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class Probe {

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
    var basic =
        new Bach.Configuration.Basic(
            System.Logger.Level.ALL,
            Map.of("noop", new NoopTool(), "fail", __ -> 1),
            it -> it.redirectOutput(redirected.toFile()).redirectErrorStream(true));
    var configuration = Bach.Configuration.of(basic, home, work);
    this.bach = new Bach(new PrintWriter(out), new PrintWriter(err), configuration);
  }

  List<String> lines() {
    return out.toString().lines().collect(Collectors.toList());
  }

  List<String> errors() {
    return err.toString().lines().collect(Collectors.toList());
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
