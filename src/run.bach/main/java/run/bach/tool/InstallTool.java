package run.bach.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import run.bach.Bach;
import run.bach.external.Info;
import run.bach.external.Repository;
import run.bach.external.Walker;
import run.bach.internal.PathSupport;
import run.duke.Duke;
import run.duke.ToolCall;

public class InstallTool implements Bach.Operator {
  record Options(boolean __help, Optional<String> __from, String... tools) {}

  public InstallTool() {
    super();
  }

  @Override
  public final String name() {
    return "install";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var options = Duke.split(MethodHandles.lookup(), Options.class, args);
    if (options.__help()) {
      out.println("Usage: %s [--from <repository>] <tools...>".formatted(name()));
      return 0;
    }

    var from = options.__from().map(Repository::of).orElse(Repository.DEFAULT);
    var folders = bach.folders();

    if (options.tools().length == 0 || options.tools()[0].equals("?")) {
      listInstallableTools(bach, out, from, options.__from().orElse(""));
      return 0;
    }

    for (var tool : options.tools()) {
      var source = from.source(Info.EXTERNAL_TOOL_DIRECTORY, tool);
      var target = folders.externalTools().resolve(tool + Info.EXTERNAL_TOOL_DIRECTORY.extension());
      bach.run("load", "file", source, target.toString());
      explodeToolDirectory(target).parallelStream().forEach(bach::run);
    }
    return 0;
  }

  void listInstallableTools(Bach bach, PrintWriter out, Repository repository, String from) {
    var walker = Walker.of(bach.browser().client(), repository);
    var tools = walker.map().get(Info.EXTERNAL_TOOL_DIRECTORY);
    if (tools == null || tools.isEmpty()) {
      out.println("No tool directory index files found in " + repository);
      return;
    }
    var joiner = new StringJoiner("\n");
    for (var tool : tools) {
      var command = ToolCall.of("bach");
      command = command.with(name()); // "install"
      if (!from.isEmpty()) command = command.with("--from", from);
      command = command.with(Info.EXTERNAL_TOOL_DIRECTORY.name(tool));
      joiner.add(command.toCommandLine(" "));
    }
    var size = tools.size();
    joiner.add("    %d tool directory info file%s".formatted(size, size == 1 ? "" : "s"));
    out.println(joiner);
  }

  List<ToolCall> explodeToolDirectory(Path file) {
    var calls = new ArrayList<ToolCall>();
    var properties = PathSupport.properties(file);
    var name = file.getFileName().toString();
    var extension = Info.EXTERNAL_TOOL_DIRECTORY.extension();
    var parent = file.resolveSibling(name.substring(0, name.length() - extension.length()));
    for (var key : properties.stringPropertyNames()) {
      if (key.startsWith("@")) continue;
      var value = properties.getProperty(key);
      if (!value.startsWith("http")) continue;
      var source = URI.create(value);
      var target = parent.resolve(key);
      calls.add(ToolCall.of("load").with("file").with(source).with(target));
    }
    return calls;
  }
}
