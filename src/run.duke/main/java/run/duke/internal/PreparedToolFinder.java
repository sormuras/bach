package run.duke.internal;

import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import run.duke.Tool;
import run.duke.ToolCalls;
import run.duke.ToolFinder;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record PreparedToolFinder(String description, List<ToolCalls> calls) implements ToolFinder {
  public PreparedToolFinder {
    if (description == null) throw new IllegalArgumentException("description must not be null");
    if (calls == null) throw new IllegalArgumentException("calls must not be null");
  }

  @Override
  public List<String> identifiers(ToolRunner runner) {
    return calls.stream().map(ToolCalls::name).toList();
  }

  @Override
  public Optional<Tool> find(String string, ToolRunner runner) {
    for (var call : calls) {
      var test = call.name().equals(string) || call.name().endsWith('/' + string);
      if (test) {
        var operator = new Operator(string, call, runner);
        return Optional.of(new Tool(operator.name(), operator));
      }
    }
    return Optional.empty();
  }

  record Operator(String name, ToolCalls calls, ToolRunner runner) implements ToolOperator {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      for (var call : calls) runner.run(call);
      return 0;
    }
  }
}
