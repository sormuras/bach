package run.bach.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.bach.Browser;
import run.bach.Folders;
import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.external.ModulesLocators;
import run.bach.internal.ModulesSupport;
import run.duke.CommandLineInterface;
import run.duke.Workbench;

public class LoadTool extends ProjectTool {
  record Options(boolean __help, String what, String that, String... more) {
    List<String> thatAndMore() {
      return Stream.concat(Stream.of(that), Stream.of(more)).toList();
    }
  }

  public LoadTool() {}

  protected LoadTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "load";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new LoadTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var options = CommandLineInterface.of(MethodHandles.lookup(), Options.class).split(args);
    if (options.__help()) {
      out.println("Usage: %s <what> <that> <more...>".formatted(name()));
      return 0;
    }
    var browser = workbench().workpiece(Browser.class);
    switch (options.what) {
      case "file" -> browser.load(URI.create(options.that), Path.of(options.more[0]));
      case "head" -> out.println(browser.head(URI.create(options.that)));
      case "headers" -> {
        for (var entry : browser.head(URI.create(options.that)).headers().map().entrySet()) {
          out.println(entry.getKey());
          for (var line : entry.getValue()) out.println("  " + line);
        }
      }
      case "module" -> loadModule(options.thatAndMore());
      case "modules" -> loadModules(options.thatAndMore());
      case "text" -> out.println(browser.read(URI.create(options.that)));
      default -> {
        err.println("Unknown load type: " + options.what);
        return 1;
      }
    }
    return 0;
  }

  void loadModule(List<String> modules) {
    var browser = workbench().workpiece(Browser.class);
    var folders = workbench().workpiece(Folders.class);
    var project = workbench().workpiece(Project.class);
    var externalModules = folders.externalModules();
    var locators =
        Stream.concat(
                project.externals().locators().list().stream(),
                ModulesLocators.of(externalModules).list().stream())
            .toList();
    with_next_module:
    for (var module : modules) {
      if (ModuleFinder.of(externalModules).find(module).isPresent()) {
        debug("Module %s is already present".formatted(module));
        continue; // with next module
      }
      for (var locator : locators) {
        var location = locator.locate(module);
        if (location == null) continue; // with next locator
        debug("Module %s located via %s".formatted(module, locator.description()));
        var source = URI.create(location);
        var target = externalModules.resolve(module + ".jar");
        browser.load(source, target); // "silent" load file ...
        continue with_next_module;
      }
      throw new RuntimeException("Module not locatable: " + module);
    }
  }

  void loadModules(List<String> modules) {
    var folders = workbench().workpiece(Folders.class);
    var externals = folders.externalModules();
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var finders = List.of(ModuleFinder.of(externals)); // recreate in every loop
      var missing = ModulesSupport.listMissingNames(finders, Set.copyOf(modules));
      if (missing.isEmpty()) break;
      var size = missing.size();
      debug("Load %d missing module%s".formatted(size, size == 1 ? "" : "s"));
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      loadModule(missing); // "silent" load module missing...
      loaded.addAll(missing);
      missing.forEach(this::debug);
    }
    info("Loaded %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
  }
}
