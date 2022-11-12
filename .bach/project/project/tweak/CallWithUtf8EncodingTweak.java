package project.tweak;

import run.bach.ToolCall;
import run.bach.ToolTweak;

public class CallWithUtf8EncodingTweak implements ToolTweak {
  @Override
  public ToolCall tweak(ToolCall call) {
    return switch (call.name()) {
      case "javac", "jdk.compiler/javac" -> call.with(
          call.arguments().indexOf("-d"), // insert at position of `-d`, pushing it to the right
          javac -> javac.with("-encoding", "UTF-8")); // additional arguments to insert
      default -> call;
    };
  }
}
