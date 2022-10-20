package run.bach;

/** An operator usually returns a copy of a tool call instance with modified arguments. */
@FunctionalInterface
public interface ToolTweak {
  ToolCall tweak(ToolCall call);
}
