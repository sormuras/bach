package run.bach.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.StringJoiner;
import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.bach.external.Info;
import run.bach.external.Repository;
import run.bach.external.Walker;
import run.duke.Duke;
import run.duke.ToolCall;
import run.duke.ToolLogger;

public class ImportTool implements ProjectOperator {
  record Options(boolean __help, Optional<String> __from, String... locators) {}

  public ImportTool() {}

  @Override
  public final String name() {
    return "import";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    var options = Duke.split(MethodHandles.lookup(), Options.class, args);
    if (options.__help()) {
      logger.log("Usage: %s [--from <repository>] <locators...>".formatted(name()));
      return;
    }

    var from = options.__from().map(Repository::of).orElse(Repository.DEFAULT);
    var folders = runner.folders();

    if (options.locators().length == 0 || options.locators()[0].equals("?")) {
      var walker = Walker.of(runner.browser().client(), from);
      listImportableLocators(walker, logger.out(), from, options.__from().orElse(""));
      return;
    }

    for (var tool : options.locators()) {
      var source = from.source(Info.EXTERNAL_MODULES_LOCATOR, tool);
      var target = folders.externalModules(tool + Info.EXTERNAL_MODULES_LOCATOR.extension());
      runner.run("load", "file", source, target.toString());
    }
  }

  void listImportableLocators(Walker walker, PrintWriter out, Repository repository, String from) {
    var tools = walker.map().get(Info.EXTERNAL_MODULES_LOCATOR);
    if (tools == null || tools.isEmpty()) {
      out.println("No external modules locator index files found in " + repository);
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
    out.println(joiner);
  }
}
