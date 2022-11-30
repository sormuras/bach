package run.bach.internal;

import run.bach.Command;
import run.duke.ToolCall;
import run.duke.ToolCalls;
import run.duke.ToolRunner;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public record CommandToolProvider(Command command, ToolRunner runner) implements ToolProvider {
  @Override
  public String name() {
    return command().name();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    for (var call : calls()) runner.run(call);
    return 0;
  }

  ToolCalls calls() {
    if (command.args().length > 0) return ToolCalls.of(command.args());
    var lines = command.line().lines().toList();
    var stream = lines.size() == 1 ? Stream.of(lines.get(0).split("\\+")) : lines.stream();
    return new ToolCalls(stream.map(ToolCall::ofLine).toList());
  }
}
