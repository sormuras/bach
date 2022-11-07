package run.bach.project;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import run.bach.ToolOperator;
import run.bach.toolbox.TreeTool;

public record CleanTool(String name) implements ToolOperator {
  public CleanTool() {
    this("clean");
  }

  @Override
  public void run(Operation operation) throws Exception {
    var bach = operation.bach();
    var out = bach.paths().out();
    if (Files.notExists(out)) return;
    bach.run(TreeTool.clean(out));
    try /* to prune empty directories */ {
      Files.deleteIfExists(out); // usually ".bach/out/"
      var parent = out.getParent(); // also ".bach/", if empty
      if (parent != null) Files.deleteIfExists(parent);
    } catch (DirectoryNotEmptyException ignore) {
    }
  }
}
