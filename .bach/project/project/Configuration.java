package project;

import run.bach.Configurator;
import run.bach.Tweaks;
import run.bach.Workbench;
import run.duke.ToolCall;

public class Configuration implements Configurator {
  @Override
  public void configure(Workbench bench) {
    bench.put(Tweaks.class, Tweaks::new, tweaks -> tweaks.with(new ToolCallTweak()));
  }

  static class ToolCallTweak implements ToolCall.Tweak {
    @Override
    public ToolCall tweak(ToolCall call) {
      return switch (call.tool()) {
        case "javac" -> call.withTweak(0, tweak -> tweak.with("-g").with("-parameters"));
        case "junit" -> call.with("--details", "NONE").with("--disable-banner");
        default -> call;
      };
    }
  }
}
