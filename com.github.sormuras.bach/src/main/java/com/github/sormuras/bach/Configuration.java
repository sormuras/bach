package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import com.github.sormuras.bach.internal.ModuleLayerSupport;
import com.github.sormuras.bach.project.Project;
import java.io.PrintWriter;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** Immutable settings. */
public record Configuration(
    Printer printer,
    Flags flags,
    Paths paths,
    Project project,
    ToolFinder finder,
    List<String> remnants) {

  public static Configuration of(PrintWriter out, PrintWriter err, String... args) {
    var printer = new Printer(out, err);
    var flags = EnumSet.noneOf(Flag.class);
    var root = Path.of("");
    var bout = Path.of(".bach", "out");
    var info = Path.of("project-info");
    var projectArguments = new ArrayList<String>();

    var arguments = new ArrayDeque<>(List.of(args));
    while (!arguments.isEmpty()) {
      var argument = arguments.removeFirst();
      if (argument.startsWith("--")) {
        if (argument.equals("--verbose")) {
          flags.add(Flag.VERBOSE);
          continue;
        }
        var index = argument.indexOf('=', 2);
        var key = index == -1 ? argument : argument.substring(0, index);
        var value = index == -1 ? arguments.removeFirst() : argument.substring(index + 1);
        if (key.equals("--chroot")) {
          root = Path.of(value).normalize();
          continue;
        }
        if (key.equals("--change-bach-out")) {
          bout = Path.of(value).normalize();
          continue;
        }
        if (key.equals("--chinfo")) {
          info = Path.of(value).normalize();
          continue;
        }
        if (key.startsWith("--project-")) {
          projectArguments.add(key);
          projectArguments.add(value);
          continue;
        }
        throw new IllegalArgumentException("Unsupported option `%s`".formatted(key));
      }
      arguments.addFirst(argument); // restore `TOOL-NAME` argument
      break;
    }
    var remnants = List.copyOf(arguments);
    var paths = new Paths(root, root.resolve(bout));

    var project = Project.of().withParsingDirectory(paths.root());

    var layer = layer(printer, paths, info);
    for (var configurator : ServiceLoader.load(layer, Project.Configurator.class)) {
      project = configurator.configure(project);
    }
    project =
        project
            .withParsingArguments(paths.root("project-info.args"))
            .withParsingArguments(paths.root(".bach", "project-info.args"))
            .withParsingArguments(projectArguments);

    var finder =
        ToolFinder.compose(
            ToolFinder.of(layer),
            ToolFinder.of(ModuleFinder.of(paths.root(".bach", "external-modules")), false),
            ToolFinder.ofJavaTools(paths.root(".bach", "external-tools")),
            ToolFinder.ofSystemTools(),
            ToolFinder.of(
                Tool.ofNativeToolInJavaHome("jarsigner"),
                Tool.ofNativeToolInJavaHome("java").with(Tool.Flag.HIDDEN),
                Tool.ofNativeToolInJavaHome("jdeprscan"),
                Tool.ofNativeToolInJavaHome("jfr")));

    return new Configuration(
        printer, new Flags(Set.copyOf(flags)), paths, project, finder, remnants);
  }

  private static ModuleLayer layer(Printer printer, Paths paths, Path info) {
    var folders =
        Stream.of("", ".bach")
            .map(base -> paths.root(base).resolve(info))
            .filter(Files::isDirectory)
            .filter(directory -> Files.isRegularFile(directory.resolve("module-info.java")))
            .toList();
    if (folders.isEmpty()) return ModuleLayer.empty();
    if (folders.size() > 1) throw new RuntimeException("Expected single folder:" + folders);
    var directory = folders.get(0);
    try {
      var module = ModuleDescriptorSupport.parse(directory.resolve("module-info.java"));
      var javac = ToolProvider.findFirst("javac").orElseThrow();
      var location = Bach.class.getProtectionDomain().getCodeSource().getLocation().toURI();
      var target = paths.out(info.getFileName().toString());
      javac.run(
          printer.out(),
          printer.err(),
          "--module=" + module.name(),
          "--module-source-path=" + module.name() + '=' + directory,
          "--module-path=" + Path.of(location),
          "-d",
          target.toString());
      return ModuleLayerSupport.layer(ModuleFinder.of(target), false);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public boolean isVerbose() {
    return is(Flag.VERBOSE);
  }

  public boolean is(Flag flag) {
    return flags.set().contains(flag);
  }
}
