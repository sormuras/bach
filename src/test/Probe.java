import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;

class Probe {

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();
  final Bach bach;

  Probe() {
    this(Path.of(""), Path.of(""));
  }

  Probe(Path home, Path work) {
    var shadow = Bach.Configuration.of(home);

    Map<String, Bach.Tool> tools = Map.of("noop", new NoopTool(), "fail", __ -> 1);
    var basic = new Bach.Configuration.Basic(System.Logger.Level.ALL, tools);
    var paths = new Bach.Configuration.Paths(home, work, shadow.paths.sources);
    var configuration = new Bach.Configuration(basic, shadow.project, paths, shadow.options);
    this.bach = new Bach(new PrintWriter(out), new PrintWriter(err), configuration);
  }

  static class NoopTool implements Bach.Tool {

    @Override
    public int run(Object... arguments) {
      return 0;
    }
  }
}
