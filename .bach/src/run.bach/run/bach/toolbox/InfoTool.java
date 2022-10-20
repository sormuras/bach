package run.bach.toolbox;

import java.util.List;
import run.bach.Bach;
import run.bach.ExternalAssetsRepository;
import run.bach.ToolOperator;

public record InfoTool(String name) implements ToolOperator {
  public InfoTool() {
    this("info");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    if (arguments.isEmpty()) {
      var root = bach.paths().root();
      bach.info("External asset information files in " + root.toUri());
      bach.info(ExternalAssetsRepository.walk(root).toString(0));
      return;
    }
    for (var slug : arguments) {
      var repository = ExternalAssetsRepository.of(slug);
      bach.info("External asset information files in " + repository.home());
      var walker = ExternalAssetsRepository.walk(bach.browser().client(), repository);
      bach.info(walker.toString(0));
    }
  }
}
