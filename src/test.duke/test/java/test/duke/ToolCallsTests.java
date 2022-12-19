package test.duke;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.ToolCall;
import run.duke.ToolCalls;

@Registered
@Enabled
public class ToolCallsTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    testEmpty();
    testOneToolCall();
    testTwoToolCalls();
    testCustomCommandLinePattern();
    return 0;
  }

  void testEmpty() {
    var calls = ToolCalls.of();
    assert calls.list().isEmpty();
    assert !calls.iterator().hasNext();
  }

  void testOneToolCall() {
    var calls = ToolCalls.of("tool");
    assert List.of(ToolCall.of("tool")).equals(calls.list());
  }

  void testTwoToolCalls() {
    var calls = ToolCalls.of("tool", "+", "tool");
    assert List.of(ToolCall.of("tool"), ToolCall.of("tool")).equals(calls.list());
  }

  void testCustomCommandLinePattern() {
    var calls = ToolCalls.of("++", false, List.of("+", "++", " + "));
    assert List.of(ToolCall.of("+"), ToolCall.of(" + ")).equals(calls.list());
  }
}
