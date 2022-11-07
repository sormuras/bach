package run.bach.toolbox;

import java.nio.file.Path;
import run.bach.ToolOperator;

public record DumpTool(String name) implements ToolOperator {
  public DumpTool() {
    this("dump");
  }

  @Override
  public void run(Operation operation) throws Exception {
    var bach = operation.bach();
    var arguments = operation.arguments();
    if (arguments.isEmpty()) bach.writeLogbook();
    else bach.writeLogbook(Path.of(arguments.get(0)));
  }
}
