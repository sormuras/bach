package run.bach.toolbox;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import run.bach.Bach;
import run.bach.ToolOperator;
import run.bach.internal.ModulesSupport;

public record ListTool(String name) implements ToolOperator {
  public ListTool() {
    this("list");
  }

  @Override
  public void run(Operation operation) {
    var bach = operation.bach();
    if (operation.arguments().isEmpty()) {
      bach.info("Usage: %s {tools}".formatted(name()));
      return;
    }
    if (operation.arguments().contains("modules")) {
      bach.info(toModulesString(bach));
    }
    if (operation.arguments().contains("tools")) {
      bach.info(bach.tools().toString(0));
    }
  }

  public String toModulesString(Bach bach) {
    var paths = bach.paths();
    var joiner = new StringJoiner("\n");
    for (var space : bach.project().spaces().list()) {
      var size = space.modules().list().size();
      if (size == 0) continue;
      var name = space.name();
      var spaceModules = space.modules().toModuleFinder();
      joiner.add(("Project modules in %s space".formatted(name)));
      consumeAllNames(spaceModules, joiner::add);
      joiner.add("    %d %s module%s".formatted(size, name, size == 1 ? "" : "s"));
    }
    joiner.add("External modules in " + paths.externalModules().toUri());
    var externalModuleFinder = ModuleFinder.of(paths.externalModules());
    var externalModules = externalModuleFinder.findAll();
    consumeAllNames(externalModuleFinder, joiner::add);
    joiner.add("    %d external modules".formatted(externalModules.size()));

    joiner.add("Missing external modules");
    var missingModules = ModulesSupport.listMissingNames(List.of(externalModuleFinder), Set.of());
    missingModules.forEach(joiner::add);
    joiner.add("    %d missing external modules".formatted(missingModules.size()));

    var systemModuleFinder = ModuleFinder.ofSystem();
    joiner.add("System modules in " + paths.javaHome().resolve("lib").toUri());
    var systemModules = systemModuleFinder.findAll();
    if (bach.cli().verbose()) consumeAllNames(systemModuleFinder, joiner::add);
    joiner.add("    %d system modules".formatted(systemModules.size()));
    return joiner.toString();
  }

  static void consumeAllNames(ModuleFinder finder, Consumer<String> consumer) {
    finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::toNameAndVersion)
        .sorted()
        .map(string -> string.indent(2).stripTrailing())
        .forEach(consumer);
  }
}
