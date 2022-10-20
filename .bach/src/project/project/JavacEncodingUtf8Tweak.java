package project;

import run.bach.ToolCall;
import run.bach.ToolTweak;

public class JavacEncodingUtf8Tweak implements ToolTweak {
  @Override
  public ToolCall tweak(ToolCall call) {
    return switch (call.name()) {
      case "javac", "jdk.compiler/javac" -> call.with(0, javac -> javac.with("-encoding", "UTF-8"));
      default -> call;
    };
  }
}
