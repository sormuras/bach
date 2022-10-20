package run.bach.tool;

import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.Set;
import run.bach.Bach;
import run.bach.ToolOperator;
import run.bach.internal.ModulesSupport;

public record ListModulesOperator(String name) implements ToolOperator {

  public ListModulesOperator() {
    this("list-modules");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.info("# Modules");
    bach.info("## Project modules");
    bach.info("    // TODO");
    // TODO bach.info("    %d project modules".formatted(project.size()));
    bach.info("## External modules in " + bach.paths().externalModules().toUri());
    var externalModuleFinder = ModuleFinder.of(bach.paths().externalModules());
    var externalModules = externalModuleFinder.findAll();
    ModulesSupport.consumeAllNames(externalModuleFinder, bach::info);
    bach.info("    %d external modules".formatted(externalModules.size()));

    bach.info("## Missing external modules");
    var missingModules = ModulesSupport.listMissingNames(List.of(externalModuleFinder), Set.of());
    missingModules.forEach(bach::info);
    bach.info("    %d missing external modules".formatted(missingModules.size()));

    var systemModuleFinder = ModuleFinder.ofSystem();
    bach.info("## System modules in " + bach.paths().javaHome().resolve("lib").toUri());
    var systemModules = systemModuleFinder.findAll();
    ModulesSupport.consumeAllNames(systemModuleFinder, bach::debug);
    bach.info("    %d system modules".formatted(systemModules.size()));
  }
}
