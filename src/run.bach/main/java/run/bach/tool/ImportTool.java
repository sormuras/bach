package run.bach.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.StringJoiner;
import run.bach.Browser;
import run.bach.Folders;
import run.bach.ProjectTool;
import run.bach.external.Info;
import run.bach.external.Repository;
import run.bach.external.Walker;
import run.duke.CommandLineInterface;
import run.duke.ToolCall;
import run.duke.Workbench;

public class ImportTool extends ProjectTool {
  record Options(boolean __help, Optional<String> __from, String... locators) {}

  public ImportTool() {}

  protected ImportTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "import";
  }

  @Override
  public ImportTool provider(Workbench workbench) {
    return new ImportTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var options = CommandLineInterface.of(MethodHandles.lookup(), Options.class).split(args);
    if (options.__help()) {
      out.println("Usage: %s [--from <repository>] <locators...>".formatted(name()));
      return 0;
    }

    var from = options.__from().map(Repository::of).orElse(Repository.DEFAULT);
    var folders = workbench().workpiece(Folders.class);

    if (options.locators().length == 0 || options.locators()[0].equals("?")) {
      listImportableLocators(from, options.__from().orElse(""));
      return 0;
    }

    for (var tool : options.locators()) {
      var source = from.source(Info.EXTERNAL_MODULES_LOCATOR, tool);
      var target = folders.externalModules(tool + Info.EXTERNAL_MODULES_LOCATOR.extension());
      run("load", "file", source, target.toString());
    }
    return 0;
  }

  void listImportableLocators(Repository repository, String from) {
    var browser = workbench().workpiece(Browser.class);
    var walker = Walker.of(browser.client(), repository);
    var tools = walker.map().get(Info.EXTERNAL_MODULES_LOCATOR);
    if (tools == null || tools.isEmpty()) {
      info("No external modules locator index files found in " + repository);
      return;
    }
    var joiner = new StringJoiner("\n");
    for (var tool : tools) {
      var command = ToolCall.of("bach");
      command = command.with(name()); // "install"
      if (!from.isEmpty()) command = command.with("--from", from);
      command = command.with(Info.EXTERNAL_MODULES_LOCATOR.name(tool));
      joiner.add(command.toCommandLine(" "));
    }
    var size = tools.size();
    joiner.add("    %d external modules locator%s".formatted(size, size == 1 ? "" : "s"));
    info(joiner.toString());
  }
}
