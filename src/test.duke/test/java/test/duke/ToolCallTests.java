package test.duke;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.ToolCall;

@Registered
@Enabled
public class ToolCallTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    testCallWithoutArguments();
    testCallWithOneArgument();
    testCallWithTwoArguments();
    testCallWithPositionalTweak();
    testCallWithTweaks();
    return 0;
  }

  void testCallWithoutArguments() {
    var call = ToolCall.of("tool");
    assert "tool".equals(call.tool());
    assert List.of().equals(call.arguments());
    assert "tool".equals(call.toCommandLine());
    assert "tool".equals(call.toCommandLine("~"));
  }

  void testCallWithOneArgument() {
    var call = ToolCall.of("tool", 1);
    assert "tool".equals(call.tool());
    assert List.of("1").equals(call.arguments());
    assert "tool 1".equals(call.toCommandLine());
    assert "tool~1".equals(call.toCommandLine("~"));
  }

  void testCallWithTwoArguments() {
    var call = ToolCall.of("tool", 1, '2');
    assert "tool".equals(call.tool());
    assert List.of("1", "2").equals(call.arguments());
    assert "tool 1 2".equals(call.toCommandLine());
    assert "tool~1~2".equals(call.toCommandLine("~"));
  }

  void testCallWithPositionalTweak() {
    var base = ToolCall.of("tool", "(", ")");
    assert "tool()".equals(base.toCommandLine(""));
    assert "tool|()".equals(base.withTweak(0, tweak -> tweak.with("|")).toCommandLine(""));
    assert "tool(|)".equals(base.withTweak(1, tweak -> tweak.with("|")).toCommandLine(""));
    assert "tool()|".equals(base.withTweak(2, tweak -> tweak.with("|")).toCommandLine(""));
  }

  void testCallWithTweaks() {
    var base = ToolCall.of("tool", "(", ")");
    assert "tool()".equals(base.toCommandLine(""));
    assert "tool()".equals(base.withTweaks(List.of()).toCommandLine(""));
    assert "tool()1".equals(base.withTweaks(List.of(t -> t.with("1"))).toCommandLine(""));
    assert "tool[()12]"
        .equals(
            base.withTweaks(
                    List.of(
                        t1 -> t1.with("1"),
                        t2 -> t2.with("2"),
                        t0 -> t0.withTweak(0, t -> t.with("[")),
                        t3 -> t3.with("]")))
                .toCommandLine(""));
  }
}
