package run.bach.tool;

import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import run.bach.Browser;
import run.bach.Folders;
import run.bach.ProjectTool;
import run.bach.external.Info;
import run.bach.external.Repository;
import run.bach.external.Walker;
import run.duke.CommandLineInterface;
import run.duke.ToolCall;
import run.duke.Workbench;

public class InstallTool extends ProjectTool {
  record Options(boolean __help, Optional<String> __from, String... tools) {}

  public InstallTool() {
    super();
  }

  protected InstallTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "install";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new InstallTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var options = CommandLineInterface.of(MethodHandles.lookup(), Options.class).split(args);
    if (options.__help()) {
      out.println("Usage: %s [--from <repository>] <tools...>".formatted(name()));
      return 0;
    }

    var from = options.__from().map(Repository::of).orElse(Repository.DEFAULT);
    var folders = workbench().workpiece(Folders.class);

    if (options.tools().length == 0 || options.tools()[0].equals("?")) {
      listInstallableTools(from, options.__from().orElse(""));
      return 0;
    }

    for (var tool : options.tools()) {
      var source = from.source(Info.EXTERNAL_TOOL_DIRECTORY, tool);
      var target = folders.externalTools().resolve(tool + Info.EXTERNAL_TOOL_DIRECTORY.extension());
      acquireToolDirectory(source, target);
      explodeToolDirectory(target);
    }
    return 0;
  }

  void listInstallableTools(Repository repository, String from) {
    var browser = workbench().workpiece(Browser.class);
    var walker = Walker.of(browser.client(), repository);
    var tools = walker.map().get(Info.EXTERNAL_TOOL_DIRECTORY);
    if (tools == null || tools.isEmpty()) {
      info("No tool directory index files found in " + repository);
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
    info(joiner.toString());
  }

  void acquireToolDirectory(String source, Path target) {
    run("load", "file", source, target.toString());
  }

  void explodeToolDirectory(Path file) {
    var properties = properties(file);
    var name = file.getFileName().toString();
    var extension = Info.EXTERNAL_TOOL_DIRECTORY.extension();
    var parent = file.resolveSibling(name.substring(0, name.length() - extension.length()));
    for (var key : properties.stringPropertyNames()) {
      if (key.startsWith("@")) continue;
      var value = properties.getProperty(key);
      if (!value.startsWith("http")) {
        info("Unsupported protocol: " + value);
        continue;
      }
      var source = URI.create(value);
      var target = parent.resolve(key);
      run("load", "file", source.toString(), target.toString());
    }
  }

  private static Properties properties(Path file) {
    var properties = new Properties();
    try {
      properties.load(new StringReader(Files.readString(file)));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return properties;
  }
}
