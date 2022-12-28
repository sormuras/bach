package run.bach.tool;

import java.io.PrintWriter;
import java.nio.file.Files;
import run.bach.Bach;
import run.bach.Browser;
import run.bach.external.Repository;
import run.bach.external.Walker;

public class InfoTool implements Bach.Operator {
  public InfoTool() {
    super();
  }

  @Override
  public final String name() {
    return "info";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    out.println("Project " + bach.project().toNameAndVersion());
    if (args.length == 0) {
      var start = bach.folders().root(".bach");
      if (!Files.isDirectory(start)) {
        out.println("No such directory: " + start.toUri());
        return 0;
      }
      out.println("External asset information files in " + start.toUri());
      var walker = Walker.of(start);
      out.println(walker.toString(0));
      return 0;
    }
    var browser = bach.workpiece(Browser.class);
    for (var slug : args) {
      var repository = Repository.of(slug);
      out.println("External asset information files in " + repository.home());
      var walker = Walker.of(browser.client(), repository);
      out.println(walker.toString(0));
    }
    return 0;
  }
}
