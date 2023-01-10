package run.bach.tool;

import java.nio.file.Files;
import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.bach.external.Repository;
import run.bach.external.Walker;
import run.duke.ToolLogger;

public class InfoTool implements ProjectOperator {
  public InfoTool() {
    super();
  }

  @Override
  public final String name() {
    return "info";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    logger.log("Project " + runner.project().toNameAndVersion());
    if (args.length == 0) {
      var start = runner.folders().root(".bach");
      if (!Files.isDirectory(start)) {
        logger.log("No such directory: " + start.toUri());
        return;
      }
      logger.log("External asset information files in " + start.toUri());
      var walker = Walker.of(start);
      logger.log(walker.toString(0));
      return;
    }
    for (var slug : args) {
      var repository = Repository.of(slug);
      logger.log("External asset information files in " + repository.home());
      var walker = Walker.of(runner.browser().client(), repository);
      logger.log(walker.toString(0));
    }
  }
}
