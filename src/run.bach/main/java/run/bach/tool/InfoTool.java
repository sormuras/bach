package run.bach.tool;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.spi.ToolProvider;
import run.bach.Browser;
import run.bach.ProjectTool;
import run.bach.external.Repository;
import run.bach.external.Walker;
import run.duke.Workbench;

public class InfoTool extends ProjectTool {
  public InfoTool() {
    super();
  }

  protected InfoTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "info";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new InfoTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("Project " + project().toNameAndVersion());
    if (args.length == 0) {
      var start = folders().root(".bach");
      if (!Files.isDirectory(start)) {
        info("No such directory: " + start.toUri());
        return 0;
      }
      info("External asset information files in " + start.toUri());
      var walker = Walker.of(start);
      info(walker.toString(0));
      return 0;
    }
    var browser = workbench().workpiece(Browser.class);
    for (var slug : args) {
      var repository = Repository.of(slug);
      info("External asset information files in " + repository.home());
      var walker = Walker.of(browser.client(), repository);
      info(walker.toString(0));
    }
    return 0;
  }
}
