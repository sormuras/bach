package test.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.bach.Command.Mode;
import run.duke.ToolCall;
import run.duke.ToolCalls;

@Registered
@Enabled
public class CommandModeTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    checkDetect();
    checkMain();
    return 0;
  }

  void checkDetect() {
    assert Mode.detect().isEmpty();
    assert Mode.MAIN == Mode.detect("a", "b").orElseThrow();
    assert Mode.MAIN == Mode.detect("a", "b", "+", "a", "b").orElseThrow();
    assert Mode.LIST == Mode.detect("a b").orElseThrow();
    assert Mode.LIST == Mode.detect("a b", "a b").orElseThrow();
    assert Mode.LINE == Mode.detect("a b + a b + a b").orElseThrow();
  }

  void checkMain() {
    var empty = ToolCalls.of("");
    checkMain(empty);
    checkMain(empty.with(ToolCall.of("a")), "a");
    checkMain(empty.with(ToolCall.of("a", "b")), "a", "b");
    checkMain(
        empty.with(ToolCall.of("a", "b")).with(ToolCall.of("a", "b")), "a", "b", "+", "a", "b");
  }

  void checkMain(ToolCalls expected, String... args) {
    var actual = Mode.MAIN.apply(args);
    assert expected.list().equals(actual);
  }
}
