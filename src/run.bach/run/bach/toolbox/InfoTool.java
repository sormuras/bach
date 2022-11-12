package run.bach.toolbox;

import java.nio.file.Files;
import run.bach.ExternalAssetsRepository;
import run.bach.ToolOperator;

public record InfoTool(String name) implements ToolOperator {
  public InfoTool() {
    this("info");
  }

  @Override
  public void run(Operation operation) {
    var bach = operation.bach();
    if (operation.arguments().isEmpty()) {
      var start = bach.paths().root(".bach");
      if (!Files.isDirectory(start)) {
        bach.info("No such directory: " + start.toUri());
        return;
      }
      bach.info("External asset information files in " + start.toUri());
      bach.info(ExternalAssetsRepository.walk(start).toString(0));
      return;
    }
    for (var slug : operation.arguments()) {
      var repository = ExternalAssetsRepository.of(slug);
      bach.info("External asset information files in " + repository.home());
      var walker = ExternalAssetsRepository.walk(bach.browser().client(), repository);
      bach.info(walker.toString(0));
    }
  }
}
