import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

class Probe {
  final StringWriter out = new StringWriter();
  final StringWriter err = new StringWriter();
  final Map<String, Bach.Tool> tools = Map.of("noop", new NoopTool(), "fail", __ -> 1);
  final Bach bach = new Bach(new PrintWriter(out), new PrintWriter(err), tools);

  static class NoopTool implements Bach.Tool {

    @Override
    public int run(Object... arguments) {
      return 0;
    }
  }
}
