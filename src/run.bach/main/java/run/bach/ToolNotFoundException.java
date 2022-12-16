package run.bach;

import java.io.Serial;

public class ToolNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 6729013019754028743L;

  public ToolNotFoundException(String tool) {
    super("No such tool found: " + tool);
  }
}
