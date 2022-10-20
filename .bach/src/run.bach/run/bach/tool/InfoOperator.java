package run.bach.tool;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;
import run.bach.internal.Repository;

public record InfoOperator(String name) implements ToolOperator {
  public InfoOperator() {
    this("info");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    if (arguments.isEmpty()) {
      var root = bach.paths().root();
      bach.info("External asset information files in " + root.toUri());
      bach.info(Repository.walk(root).toString(0));
      return;
    }
    for (var slug : arguments) {
      var repository = Repository.of(slug);
      bach.info("External asset information files in " + repository.home());
      var walker = Repository.walk(bach.browser().client(), repository);
      bach.info(walker.toString(0));
    }
  }
}
