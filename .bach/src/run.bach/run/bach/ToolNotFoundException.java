package run.bach;

/** An exception thrown to indicate that no tool could be found via the given name. */
public final class ToolNotFoundException extends RuntimeException {
  @java.io.Serial private static final long serialVersionUID = -417539767734303099L;

  ToolNotFoundException(String name) {
    super("Tool named `%s` not found".formatted(name));
  }
}
