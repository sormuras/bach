package run.bach.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.StringJoiner;
import run.bach.Bach;
import run.bach.Browser;
import run.bach.external.Info;
import run.bach.external.Repository;
import run.bach.external.Walker;
import run.duke.CommandLineInterface;
import run.duke.ToolCall;

public class ImportTool implements Bach.Operator {
  record Options(boolean __help, Optional<String> __from, String... locators) {}

  public ImportTool() {}

  @Override
  public final String name() {
    return "import";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var options = CommandLineInterface.of(MethodHandles.lookup(), Options.class).split(args);
    if (options.__help()) {
      out.println("Usage: %s [--from <repository>] <locators...>".formatted(name()));
      return 0;
    }

    var from = options.__from().map(Repository::of).orElse(Repository.DEFAULT);
    var folders = bach.folders();

    if (options.locators().length == 0 || options.locators()[0].equals("?")) {
      listImportableLocators(bach, out, from, options.__from().orElse(""));
      return 0;
    }

    for (var tool : options.locators()) {
      var source = from.source(Info.EXTERNAL_MODULES_LOCATOR, tool);
      var target = folders.externalModules(tool + Info.EXTERNAL_MODULES_LOCATOR.extension());
      bach.run("load", "file", source, target.toString());
    }
    return 0;
  }

  void listImportableLocators(Bach bach, PrintWriter out, Repository repository, String from) {
    var browser = bach.workpiece(Browser.class);
    var walker = Walker.of(browser.client(), repository);
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
